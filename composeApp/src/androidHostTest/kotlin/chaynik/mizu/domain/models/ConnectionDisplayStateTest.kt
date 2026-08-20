package chaynik.mizu.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionDisplayStateTest {
	@Test fun initialUnknownIsCheckingNotOffline() = assertEquals(ConnectionDisplayState.CHECKING, connectionDisplayState(NetworkState(), ServerConnectionState.Unknown))
	@Test fun connectingIsChecking() = assertEquals(ConnectionDisplayState.CHECKING, connectionDisplayState(NetworkState(available = true), ServerConnectionState.Connecting))
	@Test fun reachableIsOnline() = assertEquals(ConnectionDisplayState.ONLINE, connectionDisplayState(NetworkState(available = true), ServerConnectionState.Reachable))
	@Test fun confirmedUnreachableIsOffline() = assertEquals(ConnectionDisplayState.OFFLINE, connectionDisplayState(NetworkState(available = true), ServerConnectionState.Unreachable))
	@Test fun authenticationFailureIsError() = assertEquals(ConnectionDisplayState.ERROR, connectionDisplayState(NetworkState(available = true), ServerConnectionState.AuthenticationFailed))
	@Test fun tlsFailureIsError() = assertEquals(ConnectionDisplayState.ERROR, connectionDisplayState(NetworkState(available = true), ServerConnectionState.TlsFailure))
	@Test fun confirmedNoNetworkIsOffline() = assertEquals(ConnectionDisplayState.OFFLINE, connectionDisplayState(NetworkState(known = true), ServerConnectionState.Unknown))
}
