package chaynik.mizu.domain.manager

import android.annotation.SuppressLint
import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.russhwolf.settings.Settings
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import chaynik.mizu.domain.models.NetworkState
import chaynik.mizu.domain.models.NetworkTransport
import chaynik.mizu.domain.models.ServerConnectionState
import chaynik.mizu.domain.models.settings.OfflineMode
import android.net.ConnectivityManager as AndroidConnectivityManager

@SuppressLint("MissingPermission")
actual class ConnectivityManager(
	context: Context,
	private val preferenceManager: PreferenceManager,
	private val settings: Settings,
	private val httpClientFactory: ServerHttpClientFactory
) {
	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
	private val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as AndroidConnectivityManager
	private val probeNow = Channel<Unit>(Channel.CONFLATED)
	private var probeClientSignature: String? = null
	private var probeClient: io.ktor.client.HttpClient? = null
	private val mutableServerState = kotlinx.coroutines.flow.MutableStateFlow<ServerConnectionState>(ServerConnectionState.Unknown)
	actual val serverState: StateFlow<ServerConnectionState> = mutableServerState

	actual val networkState: StateFlow<NetworkState> = callbackFlow {
		fun publishActiveNetwork() {
			val network = connectivity.activeNetwork
			val caps = network?.let(connectivity::getNetworkCapabilities)
			trySend(caps?.toNetworkState(connectivity.isActiveNetworkMetered) ?: NetworkState(known = true))
		}
		val callback = object : AndroidConnectivityManager.NetworkCallback() {
			override fun onAvailable(network: Network) = publishActiveNetwork()
			override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = publishActiveNetwork()
			override fun onLost(network: Network) = publishActiveNetwork()
		}
		connectivity.registerDefaultNetworkCallback(callback)
		publishActiveNetwork()
		awaitClose { connectivity.unregisterNetworkCallback(callback) }
	}.conflate().distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, NetworkState())

	actual val isCellular = networkState.map { it.transport == NetworkTransport.CELLULAR }
		.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, false)

	actual val isOnline = combine(networkState, serverState, preferenceManager.offlineModeFlow) { network, server, mode ->
		when (mode) {
			OfflineMode.Forced -> false
			OfflineMode.NoWiFi -> network.available && network.transport != NetworkTransport.CELLULAR && server == ServerConnectionState.Reachable
			else -> network.available && server == ServerConnectionState.Reachable
		}
	}.distinctUntilChanged().stateIn(scope, SharingStarted.Eagerly, false)

	init {
		scope.launch {
			var previous = NetworkState()
			networkState.collect {
				if (shouldProbeImmediately(previous, it)) probeNow.trySend(Unit)
				if (!it.available) mutableServerState.value = ServerConnectionState.Unknown
				previous = it
			}
		}
		scope.launch {
			val backoff = ReachabilityBackoff()
			probeNow.trySend(Unit)
			while (true) {
				probeNow.receive()
				if (!networkState.value.available) continue
				mutableServerState.value = ServerConnectionState.Connecting
				val instanceUrl = settings.getString("instanceUrl", "").trimEnd('/')
				val result = if (instanceUrl.isBlank()) ServerConnectionState.Unknown else runCatching {
					val status = probeClient(instanceUrl).get("$instanceUrl/rest/ping.view").status
					when {
						status.isSuccess() -> ServerConnectionState.Reachable
						status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden -> ServerConnectionState.AuthenticationFailed
						else -> ServerConnectionState.Unreachable
					}
				}.getOrElse { error ->
					if (error::class.simpleName?.contains("SSL", true) == true) ServerConnectionState.TlsFailure
					else ServerConnectionState.Unreachable
				}
				mutableServerState.value = result
				if (result == ServerConnectionState.Reachable) backoff.reset()
				val delay = reachabilityRetryDelay(result, backoff)
				withTimeoutOrNull(delay) { probeNow.receive() }
				probeNow.trySend(Unit)
			}
		}
	}

	@Synchronized
	private fun probeClient(instanceUrl: String): io.ktor.client.HttpClient {
		val headers = preferenceManager.customHeadersMap()
		val signature = "$instanceUrl\n${headers.entries.sortedBy { it.key }.joinToString()}"
		if (probeClientSignature != signature) {
			probeClient?.close()
			probeClient = httpClientFactory.create(
				connectTimeoutMillis = 5_000,
				requestTimeoutMillis = 8_000,
				socketTimeoutMillis = 8_000,
				baseUrl = instanceUrl,
				headers = headers
			)
			probeClientSignature = signature
		}
		return checkNotNull(probeClient)
	}
}

private fun NetworkCapabilities.toNetworkState(metered: Boolean): NetworkState {
	val transport = when {
		hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkTransport.VPN
		hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkTransport.WIFI
		hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkTransport.CELLULAR
		hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkTransport.ETHERNET
		else -> NetworkTransport.OTHER
	}
	return NetworkState(
		available = true,
		validated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
		transport = transport,
		metered = metered,
		known = true
	)
}
