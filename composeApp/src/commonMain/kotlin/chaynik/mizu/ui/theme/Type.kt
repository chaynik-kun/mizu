package chaynik.mizu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.figtree
import mizu.composeapp.generated.resources.inter
import mizu.composeapp.generated.resources.lato
import mizu.composeapp.generated.resources.lexend
import mizu.composeapp.generated.resources.manrope
import mizu.composeapp.generated.resources.montserrat
import mizu.composeapp.generated.resources.nunito
import mizu.composeapp.generated.resources.outfit
import mizu.composeapp.generated.resources.poppins
import mizu.composeapp.generated.resources.rubik
import mizu.composeapp.generated.resources.space_grotesk
import mizu.composeapp.generated.resources.urbanist
import mizu.composeapp.generated.resources.dm_sans
import mizu.composeapp.generated.resources.instrument_sans
import mizu.composeapp.generated.resources.onest
import mizu.composeapp.generated.resources.plus_jakarta_sans
import mizu.composeapp.generated.resources.source_sans_3
import mizu.composeapp.generated.resources.work_sans
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.FontOption

private val defaultTypography = Typography()

@Composable
fun bundledFont(resource: FontResource): FontFamily {
	val font = Font(resource)
	return remember(resource) { FontFamily(font) }
}

@Composable
fun defaultFont(
	grade: Int = 0,
	width: Float = 100f,
	round: Float = 0f
): FontFamily {
	val preferenceManager = koinInject<PreferenceManager>()
	return when (preferenceManager.font) {
		FontOption.System -> FontFamily.Default
		FontOption.Inter -> bundledFont(Res.font.inter)
		FontOption.Manrope -> bundledFont(Res.font.manrope)
		FontOption.Outfit -> bundledFont(Res.font.outfit)
		FontOption.Figtree -> bundledFont(Res.font.figtree)
		FontOption.SpaceGrotesk -> bundledFont(Res.font.space_grotesk)
		FontOption.Nunito -> bundledFont(Res.font.nunito)
		FontOption.Lexend -> bundledFont(Res.font.lexend)
		FontOption.Rubik -> bundledFont(Res.font.rubik)
		FontOption.Montserrat -> bundledFont(Res.font.montserrat)
		FontOption.Poppins -> bundledFont(Res.font.poppins)
		FontOption.Lato -> bundledFont(Res.font.lato)
		FontOption.Urbanist -> bundledFont(Res.font.urbanist)
		FontOption.Onest -> bundledFont(Res.font.onest)
		FontOption.DMSans -> bundledFont(Res.font.dm_sans)
		FontOption.WorkSans -> bundledFont(Res.font.work_sans)
		FontOption.PlusJakartaSans -> bundledFont(Res.font.plus_jakarta_sans)
		FontOption.SourceSans3 -> bundledFont(Res.font.source_sans_3)
		FontOption.InstrumentSans -> bundledFont(Res.font.instrument_sans)
		FontOption.Custom -> FontFamily.Default
	}
}

@Composable
fun typography(): Typography {
	val fontFamily = defaultFont()
	return Typography(
		displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
		displayLargeEmphasized = defaultTypography.displayLargeEmphasized.copy(fontFamily = fontFamily),
		displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
		displayMediumEmphasized = defaultTypography.displayMediumEmphasized.copy(fontFamily = fontFamily),
		displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
		displaySmallEmphasized = defaultTypography.displaySmallEmphasized.copy(fontFamily = fontFamily),

		headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
		headlineLargeEmphasized = defaultTypography.headlineLargeEmphasized.copy(fontFamily = fontFamily),
		headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
		headlineMediumEmphasized = defaultTypography.headlineMediumEmphasized.copy(fontFamily = fontFamily),
		headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
		headlineSmallEmphasized = defaultTypography.headlineSmallEmphasized.copy(fontFamily = fontFamily),

		titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
		titleLargeEmphasized = defaultTypography.titleLargeEmphasized.copy(fontFamily = fontFamily),
		titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
		titleMediumEmphasized = defaultTypography.titleMediumEmphasized.copy(fontFamily = fontFamily),
		titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
		titleSmallEmphasized = defaultTypography.titleSmallEmphasized.copy(fontFamily = fontFamily),

		bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
		bodyLargeEmphasized = defaultTypography.bodyLargeEmphasized.copy(fontFamily = fontFamily),
		bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
		bodyMediumEmphasized = defaultTypography.bodyMediumEmphasized.copy(fontFamily = fontFamily),
		bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
		bodySmallEmphasized = defaultTypography.bodySmallEmphasized.copy(fontFamily = fontFamily),

		labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
		labelLargeEmphasized = defaultTypography.labelLargeEmphasized.copy(fontFamily = fontFamily),
		labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
		labelMediumEmphasized = defaultTypography.labelMediumEmphasized.copy(fontFamily = fontFamily),
		labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily),
		labelSmallEmphasized = defaultTypography.labelSmallEmphasized.copy(fontFamily = fontFamily)
	)
}
