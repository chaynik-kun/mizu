package chaynik.mizu.domain.models

data class NetworkState(
	val available: Boolean = false,
	val validated: Boolean = false,
	val transport: NetworkTransport = NetworkTransport.NONE,
	val metered: Boolean = false,
	val known: Boolean = false
)

enum class NetworkTransport { WIFI, CELLULAR, ETHERNET, VPN, OTHER, NONE }

sealed interface ServerConnectionState {
	data object Unknown : ServerConnectionState
	data object Connecting : ServerConnectionState
	data object Reachable : ServerConnectionState
	data object Unreachable : ServerConnectionState
	data object AuthenticationFailed : ServerConnectionState
	data object TlsFailure : ServerConnectionState
	data object IncompatibleServer : ServerConnectionState
}

enum class ConnectionDisplayState { CHECKING, ONLINE, OFFLINE, ERROR }

fun connectionDisplayState(network: NetworkState, server: ServerConnectionState): ConnectionDisplayState = when {
	server == ServerConnectionState.Reachable -> ConnectionDisplayState.ONLINE
	server == ServerConnectionState.AuthenticationFailed || server == ServerConnectionState.TlsFailure || server == ServerConnectionState.IncompatibleServer -> ConnectionDisplayState.ERROR
	server == ServerConnectionState.Unreachable -> ConnectionDisplayState.OFFLINE
	network.known && !network.available -> ConnectionDisplayState.OFFLINE
	else -> ConnectionDisplayState.CHECKING
}
