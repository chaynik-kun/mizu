package chaynik.mizu.ui.theme

enum class ThemeSource {
	Mizu,
	DynamicLight,
	DynamicDark
}

fun selectThemeSource(
	dynamicTheming: Boolean,
	isDark: Boolean,
	androidSdk: Int
): ThemeSource = when {
	!dynamicTheming || androidSdk < 31 -> ThemeSource.Mizu
	isDark -> ThemeSource.DynamicDark
	else -> ThemeSource.DynamicLight
}
