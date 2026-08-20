package chaynik.mizu.external

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.w3c.dom.Document
import java.net.*
import chaynik.mizu.domain.manager.*
import chaynik.mizu.domain.models.*

private data class RendererRecord(
	val renderer: ParsedRenderer,
	val capabilities: RemotePlaybackCapabilities,
	var lastSeenMs: Long
)

class DlnaBackend(private val context: Context) : RemotePlaybackBackend {
	override val kind = BackendKind.DLNA
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val mutableTargets = MutableStateFlow<List<PlaybackTarget>>(emptyList())
	override val targets: StateFlow<List<PlaybackTarget>> = mutableTargets
	private val mutableStatus = MutableStateFlow(BackendPlaybackStatus())
	override val status: StateFlow<BackendPlaybackStatus> = mutableStatus
	private val records = mutableMapOf<String, RendererRecord>()
	private val seenLocations = mutableMapOf<String, Long>()
	private val locations = Channel<Pair<String, String?>>(Channel.BUFFERED)
	private val fetchLimit = Semaphore(4)
	private var selected: RendererRecord? = null
	private var discoveryJob: Job? = null
	private var pollingJob: Job? = null
	private var queue = emptyList<RemotePlaybackItem>()
	private var queueIndex = 0
	private val advanceGuard = DlnaQueueAdvanceGuard()
	private var closed = false

	override fun setDiscoveryActive(active: Boolean) {
		if (closed) return
		if (active && discoveryJob == null) discoveryJob = scope.launch {
			repeat(4) { launch { for ((location, usn) in locations) fetchDescription(location, usn) } }
			discover()
		}
		if (!active) { discoveryJob?.cancel(); discoveryJob = null }
	}

	private suspend fun discover() = withContext(Dispatchers.IO) {
		val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
		val lock = wifi.createMulticastLock("mizu-dlna-discovery").apply { setReferenceCounted(false); acquire() }
		val socket = DatagramSocket().apply { soTimeout = 1200; broadcast = true }
		try {
			while (isActive) {
				val search = "M-SEARCH * HTTP/1.1\r\nHOST:239.255.255.250:1900\r\nMAN:\"ssdp:discover\"\r\nMX:2\r\nST:urn:schemas-upnp-org:device:MediaRenderer:1\r\n\r\n".encodeToByteArray()
				socket.send(DatagramPacket(search, search.size, InetAddress.getByName("239.255.255.250"), 1900))
				val deadline = System.currentTimeMillis() + 3500
				while (System.currentTimeMillis() < deadline && isActive) runCatching {
					val bytes = ByteArray(8192); val packet = DatagramPacket(bytes, bytes.size); socket.receive(packet)
					val headers = String(packet.data, 0, packet.length).lineSequence().mapNotNull { line ->
						val split = line.indexOf(':'); if (split > 0) line.substring(0, split).trim().lowercase() to line.substring(split + 1).trim() else null
					}.toMap()
					val location = headers["location"] ?: return@runCatching
					val now = System.currentTimeMillis(); val key = headers["usn"] ?: location
					if ((seenLocations[key] ?: 0) + 10_000 < now) { seenLocations[key] = now; locations.trySend(location to headers["usn"]) }
				}
				expireRenderers()
				delay(25_000)
			}
		} finally { socket.close(); if (lock.isHeld) lock.release() }
	}

	private suspend fun fetchDescription(location: String, usn: String?) { fetchLimit.withPermit {
		runCatching {
			val parsed = UpnpDescriptionParser.parse(httpGet(location), location) ?: return@runCatching
			val capabilities = parsed.connectionManager?.let { getProtocolInfo(it) } ?: RemotePlaybackCapabilities()
			val now = System.currentTimeMillis()
			synchronized(records) {
				records[parsed.target.id] = RendererRecord(parsed, capabilities, now)
				publishTargets()
			}
		}; Unit
	} }

	private fun expireRenderers() = synchronized(records) {
		val now = System.currentTimeMillis(); val activeId = selected?.renderer?.target?.id
		records.entries.removeAll { (id, record) -> id != activeId && now - record.lastSeenMs > 90_000 }
		publishTargets()
	}
	private fun publishTargets() { mutableTargets.value = records.values.map { it.renderer.target }.distinctBy { it.id } }

	override suspend fun connect(target: PlaybackTarget) {
		selected = synchronized(records) { records[(target as PlaybackTarget.Dlna).id] } ?: error("Device unavailable")
		mutableStatus.value = BackendPlaybackStatus(connected = true)
		startPolling()
	}
	override suspend fun disconnect() { pollingJob?.cancel(); pollingJob = null; selected = null; mutableStatus.value = BackendPlaybackStatus() }
	override suspend fun play() { action("Play", "<Speed>1</Speed>") }
	override suspend fun pause() { action("Pause") }
	override suspend fun stop() { advanceGuard.onManualStop(); action("Stop") }
	override suspend fun seekTo(positionMs: Long) { action("Seek", "<Unit>REL_TIME</Unit><Target>${formatTime(positionMs)}</Target>") }
	override suspend fun next() { if (queueIndex + 1 < queue.size) loadIndex(queueIndex + 1, 0) }
	override suspend fun previous() { if (queueIndex > 0) loadIndex(queueIndex - 1, 0) else seekTo(0) }

	override suspend fun load(item: RemotePlaybackItem, queue: List<RemotePlaybackItem>, startIndex: Int, positionMs: Long) {
		require(startIndex in queue.indices && queue[startIndex].track.id == item.track.id) { "Queue/current item mismatch" }
		this.queue = queue; loadIndex(startIndex, positionMs)
	}
	private suspend fun loadIndex(index: Int, positionMs: Long) {
		advanceGuard.onLoadStarted(); queueIndex = index
		val item = queue[index]
		val metadata = "&lt;DIDL-Lite xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\" xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\"&gt;&lt;item id=\"${escape(item.track.id)}\" parentID=\"0\" restricted=\"1\"&gt;&lt;dc:title&gt;${escape(item.track.title)}&lt;/dc:title&gt;&lt;upnp:artist&gt;${escape(item.track.artist.orEmpty())}&lt;/upnp:artist&gt;&lt;upnp:class&gt;object.item.audioItem.musicTrack&lt;/upnp:class&gt;&lt;res protocolInfo=\"http-get:*:${item.mimeType.orEmpty()}:*\"&gt;${escape(item.uri)}&lt;/res&gt;&lt;/item&gt;&lt;/DIDL-Lite&gt;"
		action("SetAVTransportURI", "<CurrentURI>${escape(item.uri)}</CurrentURI><CurrentURIMetaData>$metadata</CurrentURIMetaData>")
		if (positionMs > 0) seekTo(positionMs)
		play(); advanceGuard.onLoadReady()
		mutableStatus.value = BackendPlaybackStatus(true, RemotePlaybackState.PLAYING, positionMs, item.track.durationMs, item.track.id, index)
	}

	private suspend fun action(name: String, body: String = "") {
		withContext(Dispatchers.IO) {
			val renderer = selected?.renderer ?: error("Device unavailable")
			soap(renderer.avTransport, "urn:schemas-upnp-org:service:AVTransport:1#$name", envelope(name, body, "AVTransport"))
		}
	}

	private fun startPolling() { pollingJob?.cancel(); pollingJob = scope.launch {
		while (isActive && selected != null) {
			runCatching {
				val renderer = selected?.renderer ?: return@runCatching
				val position = soapDocument(renderer.avTransport, "urn:schemas-upnp-org:service:AVTransport:1#GetPositionInfo", envelope("GetPositionInfo", "", "AVTransport"))
				val transport = soapDocument(renderer.avTransport, "urn:schemas-upnp-org:service:AVTransport:1#GetTransportInfo", envelope("GetTransportInfo", "", "AVTransport"))
				val positionMs = position.text("RelTime")?.let(::parseTime) ?: mutableStatus.value.positionMs
				val dlnaState = transport.text("CurrentTransportState").toDlnaState()
				if (advanceGuard.onState(dlnaState, queueIndex + 1 < queue.size)) {
					loadIndex(queueIndex + 1, 0)
				} else {
					mutableStatus.value = mutableStatus.value.copy(connected = true, state = dlnaState.toRemoteState(), positionMs = positionMs, currentItemId = queue.getOrNull(queueIndex)?.track?.id, queueIndex = queueIndex, error = null)
				}
			}.onFailure { mutableStatus.value = mutableStatus.value.copy(connected = false, state = RemotePlaybackState.ERROR, error = "Device disconnected") }
			delay(2_000)
		}
	} }

	private fun getProtocolInfo(url: String): RemotePlaybackCapabilities {
		val document = soapDocument(url, "urn:schemas-upnp-org:service:ConnectionManager:1#GetProtocolInfo", envelope("GetProtocolInfo", "", "ConnectionManager"))
		val mimeTypes = document.text("Sink").orEmpty().split(',').mapNotNull { entry -> entry.split(':').getOrNull(2)?.lowercase()?.takeIf { it.startsWith("audio/") } }.toSet()
		return RemotePlaybackCapabilities(mimeTypes = mimeTypes)
	}
	override fun capabilities(target: PlaybackTarget) = synchronized(records) { records[(target as? PlaybackTarget.Dlna)?.id]?.capabilities } ?: RemotePlaybackCapabilities()

	private fun envelope(name: String, body: String, service: String) = "<?xml version=\"1.0\"?><s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:$name xmlns:u=\"urn:schemas-upnp-org:service:$service:1\"><InstanceID>0</InstanceID>$body</u:$name></s:Body></s:Envelope>"
	private fun httpGet(url: String): ByteArray = (URL(url).openConnection() as HttpURLConnection).run { connectTimeout = 4000; readTimeout = 4000; inputStream.use { it.readBytes() } }
	private fun soap(url: String, action: String, data: String): ByteArray = (URL(url).openConnection() as HttpURLConnection).run { requestMethod = "POST"; connectTimeout = 5000; readTimeout = 5000; doOutput = true; setRequestProperty("Content-Type", "text/xml; charset=utf-8"); setRequestProperty("SOAPAction", "\"$action\""); outputStream.use { it.write(data.encodeToByteArray()) }; inputStream.use { it.readBytes() } }
	private fun soapDocument(url: String, action: String, data: String) = SecureXmlParser.parse(soap(url, action, data))
	private fun Document.text(localName: String) = getElementsByTagNameNS("*", localName).item(0)?.textContent?.trim()
	private fun String?.toDlnaState() = runCatching { DlnaTransportState.valueOf(this.orEmpty().uppercase()) }.getOrDefault(DlnaTransportState.UNKNOWN)
	private fun DlnaTransportState.toRemoteState() = when (this) { DlnaTransportState.PLAYING -> RemotePlaybackState.PLAYING; DlnaTransportState.PAUSED_PLAYBACK -> RemotePlaybackState.PAUSED; DlnaTransportState.TRANSITIONING -> RemotePlaybackState.BUFFERING; DlnaTransportState.STOPPED, DlnaTransportState.NO_MEDIA_PRESENT -> RemotePlaybackState.STOPPED; else -> RemotePlaybackState.IDLE }
	private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
	private fun formatTime(ms: Long) = "%02d:%02d:%02d".format(ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60)
	private fun parseTime(value: String): Long { val p = value.substringBefore('.').split(':').mapNotNull(String::toLongOrNull); return if (p.size == 3) (p[0] * 3600 + p[1] * 60 + p[2]) * 1000 else 0 }
	override fun close() { closed = true; discoveryJob?.cancel(); pollingJob?.cancel(); locations.close(); scope.cancel() }
}
