package chaynik.mizu.ui.screens.lyrics

import chaynik.mizu.ui.screens.lyrics.viewmodels.LatestLyricsRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LatestLyricsRequestTest {
	@Test
	fun oldTrackCannotCommitAfterNewTrackWasRequested() {
		val requests = LatestLyricsRequest()
		val trackA = requests.begin("A")
		val trackB = requests.begin("B")

		assertFalse(requests.accepts(trackA))
		assertTrue(requests.accepts(trackB))
	}

	@Test
	fun refreshInvalidatesEarlierRequestForSameTrack() {
		val requests = LatestLyricsRequest()
		val first = requests.begin("A")
		val refresh = requests.begin("A")

		assertFalse(requests.accepts(first))
		assertTrue(requests.accepts(refresh))
	}
}
