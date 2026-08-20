package chaynik.mizu.ui.nowplaying

import chaynik.mizu.ui.screens.nowPlaying.components.rows.NowPlayingSheet
import chaynik.mizu.ui.screens.nowPlaying.components.rows.NowPlayingSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NowPlayingSheetStateTest {
	@Test fun equalizerOpensAsOnlyActiveSheet() {
		assertEquals(NowPlayingSheet.Equalizer, NowPlayingSheetState().open(NowPlayingSheet.Equalizer).activeSheet)
	}

	@Test fun dismissRemovesOverlayState() {
		assertNull(NowPlayingSheetState().open(NowPlayingSheet.Equalizer).dismiss().activeSheet)
	}

	@Test fun sleepCanOpenImmediatelyAfterEqualizerDismiss() {
		val state = NowPlayingSheetState().open(NowPlayingSheet.Equalizer).dismiss().open(NowPlayingSheet.SleepTimer)
		assertEquals(NowPlayingSheet.SleepTimer, state.activeSheet)
	}

	@Test fun outputDeviceCanOpenImmediatelyAfterEqualizerDismiss() {
		val state = NowPlayingSheetState().open(NowPlayingSheet.Equalizer).dismiss().open(NowPlayingSheet.PlaybackTarget)
		assertEquals(NowPlayingSheet.PlaybackTarget, state.activeSheet)
	}

	@Test fun repeatedEqualizerOpenCloseAlwaysReturnsToNull() {
		var state = NowPlayingSheetState()
		repeat(3) { state = state.open(NowPlayingSheet.Equalizer).dismiss(); assertNull(state.activeSheet) }
	}
}
