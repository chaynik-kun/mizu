package chaynik.mizu.util.core

import kotlin.test.*

class BuildEnvironmentTest {
	@Test fun debugFlagExposesDeveloperSettings() = assertTrue(shouldExposeDeveloperSettings(true))
	@Test fun releaseFlagHidesDeveloperSettings() = assertFalse(shouldExposeDeveloperSettings(false))
}
