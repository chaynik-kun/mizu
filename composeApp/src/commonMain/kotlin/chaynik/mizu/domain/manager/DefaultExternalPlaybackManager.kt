package chaynik.mizu.domain.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import chaynik.mizu.domain.models.*

class DefaultExternalPlaybackManager(
	private val dlna: RemotePlaybackBackend,
	private val resolver: RemoteStreamResolver
) : ExternalPlaybackManager {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
	private val mutableState = MutableStateFlow(PlaybackTargetState())
	override val state: StateFlow<PlaybackTargetState> = mutableState
	private var backend: RemotePlaybackBackend? = null
	private var bridge: LocalPlaybackBridge? = null
	private var handoffSnapshot: LocalPlaybackSnapshot? = null

	init {
		scope.launch { dlna.targets.collect { mutableState.value = mutableState.value.copy(availableTargets = it) } }
		listOf(dlna).forEach { candidate -> scope.launch { candidate.status.collect { status ->
			if (candidate == backend) mutableState.value = mutableState.value.copy(
				connectionState = when { status.error != null -> TargetConnectionState.ERROR; status.connected -> TargetConnectionState.CONNECTED; else -> TargetConnectionState.DISCONNECTED },
				errorMessage = status.error, remotePositionMs = status.positionMs, isPlaying = status.playing,
				currentItemId = status.currentItemId, queueIndex = status.queueIndex
			)
		} } }
	}

	fun attachLocalBridge(bridge: LocalPlaybackBridge) { this.bridge = bridge }
	override fun setDiscoveryActive(active: Boolean) { dlna.setDiscoveryActive(active) }

	override suspend fun connect(target: PlaybackTarget) {
		if (target is PlaybackTarget.Local) return transferToLocal()
		val selected = when (target) { is PlaybackTarget.Dlna -> dlna; PlaybackTarget.Local -> return }
		val previousBackend = backend
		val previousTarget = mutableState.value.activeTarget
		val baseSnapshot = bridge?.snapshot() ?: handoffSnapshot ?: return fail("Nothing is playing")
		val remoteStatus = previousBackend?.status?.value
		val snapshot = baseSnapshot.copy(
			currentTrack = remoteStatus?.currentItemId?.let { id -> baseSnapshot.queue.firstOrNull { it.id == id } } ?: baseSnapshot.currentTrack,
			index = remoteStatus?.queueIndex ?: baseSnapshot.index,
			positionMs = remoteStatus?.positionMs ?: baseSnapshot.positionMs,
			wasPlaying = remoteStatus?.state?.let { it == RemotePlaybackState.PLAYING } ?: baseSnapshot.wasPlaying
		).normalized()
		val resolvedQueue = snapshot.queue.map { resolver.resolve(it, target, selected.capabilities(target)) }
		val current = resolvedQueue[snapshot.index]
		mutableState.value = mutableState.value.copy(activeTarget = target, connectionState = TargetConnectionState.CONNECTING, errorMessage = null)
		val result = runCatching {
			selected.connect(target)
			selected.load(current, resolvedQueue, snapshot.index, snapshot.positionMs)
			if (snapshot.wasPlaying) selected.play() else selected.pause()
			if (previousBackend == null) bridge?.pauseLocal()
			backend = selected
			handoffSnapshot = snapshot
			applyStatus(selected.status.value)
			previousBackend?.takeIf { it != selected }?.stop()
			previousBackend?.takeIf { it != selected }?.disconnect()
		}
		result.onFailure {
			runCatching { selected.disconnect() }
			backend = previousBackend
			mutableState.value = mutableState.value.copy(activeTarget = previousTarget)
			fail(userMessage(it))
		}
	}

	override suspend fun disconnect() = transferToLocal()
	private suspend fun transferToLocal() {
		val selected = backend
		val base = handoffSnapshot
		val remote = selected?.status?.value
		val snapshot = base?.copy(
			currentTrack = remote?.currentItemId?.let { id -> base.queue.firstOrNull { it.id == id } } ?: base.currentTrack,
			index = remote?.queueIndex ?: base.index,
			wasPlaying = remote?.state?.let { it == RemotePlaybackState.PLAYING } ?: base.wasPlaying
		)?.normalized()
		val position = remote?.positionMs ?: snapshot?.positionMs ?: 0
		selected?.stop(); selected?.disconnect()
		if (snapshot != null) bridge?.restoreLocal(snapshot, position)
		backend = null; handoffSnapshot = null
		mutableState.value = mutableState.value.copy(activeTarget = PlaybackTarget.Local, connectionState = TargetConnectionState.DISCONNECTED, errorMessage = null)
	}

	override suspend fun play() { backend?.play() }
	override suspend fun pause() { backend?.pause() }
	override suspend fun seekTo(positionMs: Long) { backend?.seekTo(positionMs) }
	override suspend fun next() { backend?.next() }
	override suspend fun previous() { backend?.previous() }
	override suspend fun playItem(item: RemotePlaybackItem, queue: List<RemotePlaybackItem>, startIndex: Int, positionMs: Long) { backend?.load(item, queue, startIndex, positionMs) }
	private fun fail(message: String) { mutableState.value = mutableState.value.copy(connectionState = TargetConnectionState.ERROR, errorMessage = message) }
	private fun applyStatus(status: BackendPlaybackStatus) {
		mutableState.value = mutableState.value.copy(
			connectionState = when { status.error != null -> TargetConnectionState.ERROR; status.connected -> TargetConnectionState.CONNECTED; else -> TargetConnectionState.DISCONNECTED },
			errorMessage = status.error, remotePositionMs = status.positionMs, isPlaying = status.playing,
			currentItemId = status.currentItemId, queueIndex = status.queueIndex
		)
	}
	private fun userMessage(error: Throwable) = when { error.message?.contains("format", true) == true -> "Unsupported media format"; error.message?.contains("reach", true) == true -> "Renderer cannot reach Navidrome"; else -> "Cannot connect to device" }
}
