package chaynik.mizu.util.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kmpalette.rememberDominantColorState
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.models.settings.ThemeMode
import chaynik.mizu.shared.MediaPlayerViewModel
import coil3.compose.LocalPlatformContext as LocalCoilPlatformContext

@Composable
fun rememberColorSchemeFromCoverArt(
	coverArtId: String?,
	forceDark: Boolean = false,
	style: PaletteStyle = PaletteStyle.Content,
	specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
	preserveSeedHue: Boolean = false
): ColorScheme {
	val sessionManager = koinInject<SessionManager>()
	val coverArtUri = remember(coverArtId) {
		coverArtId?.let { sessionManager.getCoverArtUrl(it) }
	}

	val coilPlatformContext = LocalCoilPlatformContext.current
	val loader = SingletonImageLoader.get(coilPlatformContext)
	val model = remember(coverArtUri) {
		ImageRequest.Builder(coilPlatformContext)
			.data(coverArtUri)
			// Palette extraction needs representative colours, not a full-resolution bitmap.
			.size(128, 128)
			.memoryCacheKey(coverArtId?.let { "$it#palette-128" })
			.diskCacheKey(coverArtId)
			.diskCachePolicy(CachePolicy.ENABLED)
			.memoryCachePolicy(CachePolicy.ENABLED)
			.build()
	}
	val dominantColorState = rememberDominantColorState(cacheSize = 4)
	var hasCoverColor by remember(coverArtId) { mutableStateOf(false) }

	LaunchedEffect(model) {
		val result = loader.execute(model)
		result.image?.toImageBitmap()?.let { imageBitmap ->
			dominantColorState.updateFrom(imageBitmap)
			hasCoverColor = true
		}
	}

	// Follow the colour occupying most of the artwork. Picking the most vibrant
	// swatch made small skin/orange details turn neutral covers red.
	val seedColor = dominantColorState.color

	val preferenceManager = koinInject<PreferenceManager>()
	val inDarkTheme = isSystemInDarkTheme()
	val isDark = remember(forceDark, preferenceManager.themeMode) {
		forceDark || when (preferenceManager.themeMode) {
			ThemeMode.System -> inDarkTheme
			ThemeMode.Dark -> true
			ThemeMode.Light -> false
		}
	}
	val scheme = rememberDynamicColorScheme(
		seedColor = seedColor,
		isDark = isDark,
		style = style,
		specVersion = specVersion,
		modifyColorScheme = { generated ->
			if (!preserveSeedHue || !hasCoverColor) {
				generated
			} else {
				// Material's Content palette can desaturate dark blue artwork into
				// neutral grey. Build the background container directly from the
				// extracted swatch while keeping a readable light/dark tone.
				val tonedSeed = if (isDark) {
					lerp(seedColor, Color.Black, 0.48f)
				} else {
					lerp(seedColor, Color.White, 0.62f)
				}
				generated.copy(primaryContainer = tonedSeed)
			}
		}
	)

	return scheme
}

@Composable
fun rememberColorSchemeForCurrentSong(forceDark: Boolean = true): ColorScheme {
	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsState()
	val coverArtId = playerState.currentSong?.coverArtId
	return rememberColorSchemeFromCoverArt(
		coverArtId = coverArtId,
		forceDark = forceDark,
		style = if (coverArtId != null) PaletteStyle.Content else PaletteStyle.Monochrome,
		preserveSeedHue = coverArtId != null
	)
}

private val IosRed = Color(255, 66, 69)

@Composable
fun lightIosColorScheme(
	accent: Color
): ColorScheme {
	return rememberDynamicColorScheme(
		primary = Color.White,
		isDark = false,
		isAmoled = true,
		specVersion = ColorSpec.SpecVersion.SPEC_2021,
		style = PaletteStyle.Content,
		modifyColorScheme = { scheme ->
			scheme.copy(
				primary = accent,
				onPrimary = Color.White,
				primaryContainer = accent.copy(alpha = .3f),
				onPrimaryContainer = accent,
				secondaryContainer = accent.copy(alpha = .3f),
				onSecondaryContainer = accent,
				secondary = accent,
				tertiaryContainer = accent.copy(alpha = .3f),
				onTertiaryContainer = accent,
				tertiary = accent,
				error = IosRed,
				onError = Color.White,
				errorContainer = IosRed,
				onErrorContainer = Color.White,
				surfaceVariant = Color(224, 221, 220)
			)
		}
	)
}

@Composable
fun darkIosColorScheme(
	accent: Color
): ColorScheme {
	return rememberDynamicColorScheme(
		primary = Color.White,
		isDark = true,
		isAmoled = true,
		specVersion = ColorSpec.SpecVersion.SPEC_2021,
		style = PaletteStyle.Content,
		modifyColorScheme = { scheme ->
			scheme.copy(
				primary = accent,
				onPrimary = Color.White,
				primaryContainer = accent.copy(alpha = .3f),
				onPrimaryContainer = accent,
				secondaryContainer = accent.copy(alpha = .3f),
				onSecondaryContainer = accent,
				secondary = accent,
				tertiaryContainer = accent.copy(alpha = .3f),
				onTertiaryContainer = accent,
				tertiary = accent,
				error = IosRed,
				onError = Color.White,
				errorContainer = IosRed,
				onErrorContainer = Color.White,
				surfaceVariant = Color(44, 44, 46),
				onSurfaceVariant = Color(142, 142, 147)
			)
		}
	)
}
