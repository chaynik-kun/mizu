package chaynik.mizu.ui.screens.lyrics

import chaynik.mizu.ui.screens.lyrics.components.lyricsSeekPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class LyricsSeekPositionTest {
	@Test fun nullTimestampDoesNotSeek() = assertNull(lyricsSeekPosition(null))
	@Test fun zeroTimestampSeeksToBeginning() = assertEquals(0.seconds, lyricsSeekPosition(0.seconds))
	@Test fun timestampIsNotClampedToPossiblyStaleMetadataDuration() {
		assertEquals(0.seconds, lyricsSeekPosition((-3).seconds))
		assertEquals(200.seconds, lyricsSeekPosition(200.seconds))
	}
	@Test fun validTimestampIsPreserved() = assertEquals(42.seconds, lyricsSeekPosition(42.seconds))
}
