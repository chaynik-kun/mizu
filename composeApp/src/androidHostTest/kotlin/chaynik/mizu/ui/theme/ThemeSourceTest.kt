package chaynik.mizu.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeSourceTest {
	@Test
	fun android12DynamicLightUsesDynamicLight() {
		assertEquals(ThemeSource.DynamicLight, selectThemeSource(true, false, 31))
	}

	@Test
	fun android12DynamicDarkUsesDynamicDark() {
		assertEquals(ThemeSource.DynamicDark, selectThemeSource(true, true, 31))
	}

	@Test
	fun disabledDynamicThemingUsesMizu() {
		assertEquals(ThemeSource.Mizu, selectThemeSource(false, false, 35))
	}

	@Test
	fun preAndroid12UsesMizu() {
		assertEquals(ThemeSource.Mizu, selectThemeSource(true, false, 30))
	}
}
