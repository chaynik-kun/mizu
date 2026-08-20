package chaynik.mizu.domain.manager

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertEquals
import chaynik.mizu.shared.playbackCacheKey
import chaynik.mizu.shared.serverNamespace

class PlaybackCacheKeyTest {
	@Test fun sameSongOnDifferentServersHasDifferentKey() {
		assertNotEquals(
			playbackCacheKey(serverNamespace("https://a.test", "u"), "123", null, null),
			playbackCacheKey(serverNamespace("https://b.test", "u"), "123", null, null)
		)
	}

	@Test fun originalAndTranscodedHaveDifferentKeys() {
		val namespace = serverNamespace("https://a.test", "u")
		assertNotEquals(
			playbackCacheKey(namespace, "123", null, null),
			playbackCacheKey(namespace, "123", "320", "mp3")
		)
	}

	@Test fun prebufferUsesTheExactPlaybackCacheKey() {
		val namespace = serverNamespace("https://a.test", "u")
		val playbackKey = playbackCacheKey(namespace, "123", "192", "mp3")
		val candidate = PrebufferCandidate("123", "https://a.test/stream?id=123&maxBitRate=192&format=mp3", playbackKey, namespace)
		assertEquals(playbackKey, candidate.cacheKey)
	}
}
