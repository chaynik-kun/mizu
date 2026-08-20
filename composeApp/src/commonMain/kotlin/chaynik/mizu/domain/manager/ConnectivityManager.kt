package chaynik.mizu.domain.manager

import kotlinx.coroutines.flow.StateFlow
import chaynik.mizu.domain.models.NetworkState
import chaynik.mizu.domain.models.ServerConnectionState

expect class ConnectivityManager {
	val networkState: StateFlow<NetworkState>
	val serverState: StateFlow<ServerConnectionState>
	val isCellular: StateFlow<Boolean>
	val isOnline: StateFlow<Boolean>
}
