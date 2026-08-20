package chaynik.mizu.domain.models.settings

import chaynik.mizu.domain.manager.PreferenceManager
import com.russhwolf.settings.MapSettings
import kotlin.test.*

class NowPlayingActionVisibilityTest {
	@Test fun allActionsAreVisibleByDefault() = assertTrue(NowPlayingActionVisibility().anyVisible)
	@Test fun eachActionCanBeHidden() {
		assertFalse(NowPlayingActionVisibility(lyrics = false).lyrics)
		assertFalse(NowPlayingActionVisibility(equalizer = false).equalizer)
		assertFalse(NowPlayingActionVisibility(outputDevices = false).outputDevices)
		assertFalse(NowPlayingActionVisibility(sleepTimer = false).sleepTimer)
		assertFalse(NowPlayingActionVisibility(queue = false).queue)
	}
	@Test fun multipleAndAllHiddenAreSupported() {
		assertTrue(NowPlayingActionVisibility(lyrics = false, queue = false).anyVisible)
		assertFalse(NowPlayingActionVisibility(false, false, false, false, false).anyVisible)
	}
	@Test fun preferencesPersist() {
		val settings = MapSettings()
		PreferenceManager(settings).apply {
			showNowPlayingLyrics = false
			showNowPlayingEqualizer = false
			showNowPlayingOutput = false
			showNowPlayingSleepTimer = false
			showNowPlayingQueue = false
		}
		val restored = PreferenceManager(settings)
		assertFalse(restored.showNowPlayingLyrics)
		assertFalse(restored.showNowPlayingEqualizer)
		assertFalse(restored.showNowPlayingOutput)
		assertFalse(restored.showNowPlayingSleepTimer)
		assertFalse(restored.showNowPlayingQueue)
	}
}
