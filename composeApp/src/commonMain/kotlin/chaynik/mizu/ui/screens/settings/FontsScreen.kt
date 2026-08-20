package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
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
import mizu.composeapp.generated.resources.title_choose_font
import mizu.composeapp.generated.resources.title_fonts_inbuilt
import mizu.composeapp.generated.resources.urbanist
import mizu.composeapp.generated.resources.dm_sans
import mizu.composeapp.generated.resources.instrument_sans
import mizu.composeapp.generated.resources.onest
import mizu.composeapp.generated.resources.plus_jakarta_sans
import mizu.composeapp.generated.resources.source_sans_3
import mizu.composeapp.generated.resources.work_sans
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.FontOption
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Check
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.theme.bundledFont

@Composable
fun FontsScreen() {
	val preferenceManager = koinInject<PreferenceManager>()
	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.title_choose_font)) }) }
	) { contentPadding ->
		CompositionLocalProvider(
			LocalMinimumInteractiveComponentSize provides 0.dp
		) {
			LazyColumn(
				modifier = Modifier.selectableGroup(),
				verticalArrangement = Arrangement.spacedBy(3.dp),
				contentPadding = contentPadding + PaddingValues(
					top = 16.dp, end = 16.dp, start = 16.dp
				)
			) {
				inbuiltFonts(
					onSelectFont = { preferenceManager.font = it },
					selectedFont = preferenceManager.font
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.heading(resource: StringResource) {
	item {
		Text(
			stringResource(resource),
			style = MaterialTheme.typography.titleSmallEmphasized,
			modifier = Modifier.padding(horizontal = 12.dp).semantics {
				heading()
			}
		)
	}
}

private data class InbuiltFont(
	val label: String,
	val option: FontOption,
	val resource: FontResource?
)

private fun LazyListScope.inbuiltFonts(
	onSelectFont: (FontOption) -> Unit,
	selectedFont: FontOption
) {
	heading(Res.string.title_fonts_inbuilt)
	val fonts = listOf(
		InbuiltFont("System", FontOption.System, null),
		InbuiltFont("Inter", FontOption.Inter, Res.font.inter),
		InbuiltFont("Manrope", FontOption.Manrope, Res.font.manrope),
		InbuiltFont("Outfit", FontOption.Outfit, Res.font.outfit),
		InbuiltFont("Figtree", FontOption.Figtree, Res.font.figtree),
		InbuiltFont("Space Grotesk", FontOption.SpaceGrotesk, Res.font.space_grotesk),
		InbuiltFont("Nunito", FontOption.Nunito, Res.font.nunito),
		InbuiltFont("Lexend", FontOption.Lexend, Res.font.lexend),
		InbuiltFont("Rubik", FontOption.Rubik, Res.font.rubik),
		InbuiltFont("Montserrat", FontOption.Montserrat, Res.font.montserrat),
		InbuiltFont("Poppins", FontOption.Poppins, Res.font.poppins),
		InbuiltFont("Lato", FontOption.Lato, Res.font.lato),
		InbuiltFont("Urbanist", FontOption.Urbanist, Res.font.urbanist),
		InbuiltFont("Onest", FontOption.Onest, Res.font.onest),
		InbuiltFont("DM Sans", FontOption.DMSans, Res.font.dm_sans),
		InbuiltFont("Work Sans", FontOption.WorkSans, Res.font.work_sans),
		InbuiltFont("Plus Jakarta Sans", FontOption.PlusJakartaSans, Res.font.plus_jakarta_sans),
		InbuiltFont("Source Sans 3", FontOption.SourceSans3, Res.font.source_sans_3),
		InbuiltFont("Instrument Sans", FontOption.InstrumentSans, Res.font.instrument_sans)
	)
	itemsIndexed(fonts) { index, font ->
		val fontFamily = when (font.option) {
			FontOption.System -> FontFamily.Default
			else -> font.resource?.let { bundledFont(it) }
		}
		FontRow(
			fontName = font.label,
			fontFamily = fontFamily,
			index = index,
			count = fonts.size,
			onClick = { onSelectFont(font.option) },
			selected = selectedFont == font.option
		)
		if (index == fonts.lastIndex) {
			Spacer(Modifier.height(10.dp))
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FontRow(
	fontName: String,
	fontFamily: FontFamily?,
	selected: Boolean,
	index: Int,
	@Suppress("SameParameterValue")
	count: Int,
	onClick: () -> Unit
) {
	val color = MaterialTheme.colorScheme.surfaceContainer
	SegmentedListItem(
		onClick = {
			onClick()
		},
		selected = selected,
		colors = ListItemDefaults.colors(
			containerColor = color,
		),
		shapes = ListItemDefaults.segmentedShapes(
			index = index,
			count = count
		),
		content = {
			Text(fontName)
		},
		supportingContent = {
			Text(
				"The quick brown fox jumps over the lazy dog",
				fontFamily = fontFamily,
				modifier = Modifier.semantics { hideFromAccessibility() }
			)
		},
		trailingContent = {
			if (selected) {
				Box(Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)) {
					Icon(
						Icons.Outlined.Check,
						contentDescription = null,
						modifier = Modifier.size(16.dp),
						tint = MaterialTheme.colorScheme.onPrimary
					)
				}
			}
		}
	)
}
