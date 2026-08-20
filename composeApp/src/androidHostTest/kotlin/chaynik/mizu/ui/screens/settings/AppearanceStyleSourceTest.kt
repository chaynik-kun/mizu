package chaynik.mizu.ui.screens.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceStyleSourceTest {
	@Test
	fun materialYouOverridesOnlyDisplayedStyleSummary() {
		assertEquals(AppearanceStyleSource.MaterialYou, appearanceStyleSource(dynamicTheming = true))
		assertEquals(AppearanceStyleSource.SavedMizuStyle, appearanceStyleSource(dynamicTheming = false))
	}
}
