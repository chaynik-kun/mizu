package chaynik.mizu.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackThemeSourceTest {
	@Test fun materialYouAlwaysWinsOverArtworkTheme() {
		assertEquals(PlaybackThemeSource.ParentMaterialYou, selectPlaybackThemeSource(true, true))
		assertEquals(PlaybackThemeSource.ParentMaterialYou, selectPlaybackThemeSource(true, false))
	}

	@Test fun artworkAndMizuBehaviorRemainWhenMaterialYouIsOff() {
		assertEquals(PlaybackThemeSource.Artwork, selectPlaybackThemeSource(false, true))
		assertEquals(PlaybackThemeSource.Mizu, selectPlaybackThemeSource(false, false))
	}
}
