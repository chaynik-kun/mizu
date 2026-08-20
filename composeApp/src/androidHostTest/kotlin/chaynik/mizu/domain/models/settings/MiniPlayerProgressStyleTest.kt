package chaynik.mizu.domain.models.settings

import chaynik.mizu.domain.manager.PreferenceManager
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class MiniPlayerProgressStyleTest {
	@Test fun progressAtZero() = assertEquals(0f, miniPlayerProgressFraction(0, 1_000))
	@Test fun progressAtQuarter() = assertEquals(.25f, miniPlayerProgressFraction(250, 1_000))
	@Test fun progressAtHalf() = assertEquals(.5f, miniPlayerProgressFraction(500, 1_000))
	@Test fun progressAtEnd() = assertEquals(1f, miniPlayerProgressFraction(1_000, 1_000))
	@Test fun unknownDurationIsZero() = assertEquals(0f, miniPlayerProgressFraction(500, 0))
	@Test fun positionPastDurationIsClamped() = assertEquals(1f, miniPlayerProgressFraction(2_000, 1_000))
	@Test fun nonFiniteProgressIsZero() = assertEquals(0f, sanitizedMiniPlayerProgress(Float.NaN))
	@Test fun fullBackgroundModeIsAvailable() = assertEquals(
		MiniPlayerProgressStyle.FullBackground,
		MiniPlayerProgressStyle.entries.first()
	)
	@Test fun bottomBarModeIsAvailable() = assertEquals(
		MiniPlayerProgressStyle.BottomBar,
		MiniPlayerProgressStyle.entries.last()
	)

	@Test fun preferencePersistsBothModes() {
		val settings = MapSettings()
		val first = PreferenceManager(settings)
		assertEquals(MiniPlayerProgressStyle.FullBackground, first.miniPlayerProgressStyle)
		first.miniPlayerProgressStyle = MiniPlayerProgressStyle.BottomBar
		assertEquals(
			MiniPlayerProgressStyle.BottomBar,
			PreferenceManager(settings).miniPlayerProgressStyle
		)
	}

	@Test fun legacyPreferenceMigratesToBottomBar() {
		val settings = MapSettings("miniPlayerProgressStyle" to 2)
		assertEquals(
			MiniPlayerProgressStyle.BottomBar,
			PreferenceManager(settings).miniPlayerProgressStyle
		)
	}
}
