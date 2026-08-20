package chaynik.mizu.domain.manager

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import chaynik.mizu.domain.models.NetworkState
import chaynik.mizu.domain.models.NetworkTransport
import chaynik.mizu.domain.models.ServerConnectionState

class NetworkAndHeaderPolicyTest {
	@Test fun sameOriginReceivesHeaders() {
		val headers = mapOf("X-Token" to "secret")
		assertEquals(headers, headersForDestination("https://music.example.com", "https://music.example.com/rest/stream", headers))
	}

	@Test fun sameHostDifferentPortDoesNotReceiveHeaders() {
		assertTrue(headersForDestination("https://music.example.com:443", "https://music.example.com:8443/stream", mapOf("X" to "y")).isEmpty())
	}

	@Test fun redirectToAnotherOriginDoesNotReceiveHeaders() {
		assertTrue(headersForDestination("https://music.example.com", "https://cdn.example.com/audio", mapOf("X" to "y")).isEmpty())
	}

	@Test fun backoffGrowsAndSuccessResetRestartsIt() {
		val backoff = ReachabilityBackoff(jitterFraction = 0.0)
		assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L), List(4) { backoff.nextDelayMillis() })
		backoff.reset()
		assertEquals(1_000L, backoff.nextDelayMillis())
	}

	@Test fun autoCacheOffStillReadsButDoesNotWrite() {
		val policy = playbackCachePolicy(false)
		assertTrue(policy.readEnabled)
		assertFalse(policy.writeEnabled)
	}

	@Test fun unsafeHeadersAreRejected() {
		assertEquals(
			mapOf("Good" to "yes"),
			parseCustomHeaders("Good: yes\nHost: evil\nX-Injection: hello\rworld\nContent-Length: 4")
		)
	}

	@Test fun defaultNetworkChangeTriggersImmediateProbe() {
		assertTrue(shouldProbeImmediately(NetworkState(), NetworkState(true, false, NetworkTransport.WIFI, false)))
	}

	@Test fun authenticationFailureIsNotAggressivelyRetried() {
		assertEquals(60_000L, reachabilityRetryDelay(ServerConnectionState.AuthenticationFailed, ReachabilityBackoff(jitterFraction = 0.0)))
	}
}
