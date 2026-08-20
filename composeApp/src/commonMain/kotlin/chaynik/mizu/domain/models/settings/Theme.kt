package chaynik.mizu.domain.models.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.theme_apple_music
import mizu.composeapp.generated.resources.theme_dynamic
import mizu.composeapp.generated.resources.theme_ios
import mizu.composeapp.generated.resources.theme_seeded
import mizu.composeapp.generated.resources.theme_spotify
import mizu.composeapp.generated.resources.theme_brown
import mizu.composeapp.generated.resources.theme_orange
import mizu.composeapp.generated.resources.theme_pink
import mizu.composeapp.generated.resources.theme_purple
import mizu.composeapp.generated.resources.theme_turquoise
import mizu.composeapp.generated.resources.theme_yellow
import mizu.composeapp.generated.resources.theme_cream
import mizu.composeapp.generated.resources.theme_monochrome
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalPlatformContext
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.util.ui.darkIosColorScheme
import chaynik.mizu.util.ui.lightIosColorScheme

/**
 * Theme choices that the user can choose from
 */
enum class Theme(val title: StringResource) {

	/**
	 * The app will be themed based on a "seed" colour.
	 *
	 * When this is selected, `accentColor(H/S/V)` settings
	 * will be exposed in the UI as a colour picker.
	 */
	Seeded(Res.string.theme_seeded),

	/**
	 * The app will be themed based on whatever the user
	 * chose in system settings. Android only.
	 */
	Dynamic(Res.string.theme_dynamic),

	Turquoise(Res.string.theme_turquoise),

	Purple(Res.string.theme_purple),

	Pink(Res.string.theme_pink),

	Brown(Res.string.theme_brown),

	Orange(Res.string.theme_orange),

	Yellow(Res.string.theme_yellow),

	Cream(Res.string.theme_cream),

	Monochrome(Res.string.theme_monochrome),

	/**
	 * The app will be themed according to Apple's HIG.
	 * TODO: this should pull from UIColor
	 */
	@Suppress("EnumEntryName")
	iOS(Res.string.theme_ios),

	/**
	 * The same as iOS, but with a pink-ish accent.
	 */
	AppleMusic(Res.string.theme_apple_music),

	/**
	 * The same as iOS, but with a green accent.
	 */
	Spotify(Res.string.theme_spotify);

	@OptIn(ExperimentalMaterial3ExpressiveApi::class)
	@Composable
	fun colorScheme(): ColorScheme {
		val platformContext = LocalPlatformContext.current
		val inDarkTheme = isSystemInDarkTheme()
		val preferenceManager = koinInject<PreferenceManager>()
		val isDark = remember(preferenceManager.themeMode) {
			when (preferenceManager.themeMode) {
				ThemeMode.System -> inDarkTheme
				ThemeMode.Dark -> true
				ThemeMode.Light -> false
			}
		}
		return when (this) {
			Dynamic -> platformContext.colorScheme ?: remember(isDark) {
				if (isDark)
					darkColorScheme()
				else expressiveLightColorScheme()
			}

			Seeded -> rememberDynamicColorScheme(
				seedColor = Color(preferenceManager.paletteAccentColor),
				isDark = isDark,
				specVersion = ColorSpec.SpecVersion.SPEC_2025,
				style = preferenceManager.paletteStyle
			)

			Turquoise -> fixedAccentColorScheme(isDark, Color(0xFF00CFC8))
			Purple -> fixedAccentColorScheme(isDark, Color(0xFF9B5DE5))
			Pink -> fixedAccentColorScheme(isDark, Color(0xFFFF4D8D))
			Brown -> fixedAccentColorScheme(isDark, Color(0xFF8D6E63))
			Orange -> fixedAccentColorScheme(isDark, Color(0xFFFF7A00))
			Yellow -> fixedAccentColorScheme(isDark, Color(0xFFFFCA28))
			Cream -> rememberDynamicColorScheme(
				seedColor = Color(0xFFFFE4C4),
				isDark = isDark,
				style = PaletteStyle.TonalSpot,
				specVersion = ColorSpec.SpecVersion.SPEC_2025
			)
			Monochrome -> rememberDynamicColorScheme(
				seedColor = if (isDark) Color.White else Color.Black,
				isDark = isDark,
				style = PaletteStyle.Monochrome,
				specVersion = ColorSpec.SpecVersion.SPEC_2025
			)

			iOS -> if (isDark)
				darkIosColorScheme(Color(0, 145, 255))
			else lightIosColorScheme(Color(0, 136, 255))

			AppleMusic -> if (isDark)
				darkIosColorScheme(Color(255, 55, 95))
			else lightIosColorScheme(Color(255, 45, 85))

			Spotify -> if (isDark)
				darkIosColorScheme(Color(30, 215, 96))
			else lightIosColorScheme(Color(30, 215, 96))
		}
	}

	fun isMaterialLike(): Boolean = when (this) {
		Dynamic,
		Seeded,
		Turquoise,
		Purple,
		Pink,
		Brown,
		Orange,
		Yellow,
		Cream,
		Monochrome -> true

		else -> false
	}
}

@Composable
private fun fixedAccentColorScheme(isDark: Boolean, accent: Color): ColorScheme =
	if (isDark) darkIosColorScheme(accent) else lightIosColorScheme(accent)
