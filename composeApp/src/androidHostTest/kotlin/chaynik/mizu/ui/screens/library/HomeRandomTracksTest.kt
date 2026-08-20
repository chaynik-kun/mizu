package chaynik.mizu.ui.screens.library

import chaynik.mizu.ui.screens.library.components.homeRandomTracks
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRandomTracksTest {
	@Test fun previewLimitDoesNotLimitPlaybackQueue() {
		val tracks = (1..100).toList()
		val result = homeRandomTracks(tracks)
		assertEquals(15, result.preview.size)
		assertEquals(tracks, result.playbackQueue)
	}
}
