package chaynik.mizu.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.kyant.capsule.ContinuousRoundedRectangle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import dev.zt64.compose.pipette.HsvColor
import dev.zt64.compose.pipette.RingColorPicker
import kotlinx.collections.immutable.toImmutableList
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.option_accent_colour
import mizu.composeapp.generated.resources.option_choose_theme
import mizu.composeapp.generated.resources.option_dynamic_theming
import mizu.composeapp.generated.resources.option_palette_specification
import mizu.composeapp.generated.resources.option_palette_style
import mizu.composeapp.generated.resources.palette_group_multicolor
import mizu.composeapp.generated.resources.palette_group_single_color
import mizu.composeapp.generated.resources.title_palette
import mizu.composeapp.generated.resources.title_theme_mode
import mizu.composeapp.generated.resources.subtitle_dynamic_theming
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.Theme
import chaynik.mizu.domain.models.settings.ThemeMode
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Check
import chaynik.mizu.icons.outlined.Picker
import chaynik.mizu.ui.components.common.Dropdown
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.common.FormTitle
import chaynik.mizu.ui.components.common.TooltipBox
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.screens.settings.components.SettingSelectionRow
import chaynik.mizu.ui.screens.settings.components.SettingSwitchRow
import chaynik.mizu.util.core.label

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsThemesScreen() {
	val preferenceManager = koinInject<PreferenceManager>()
	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.option_choose_theme)) }) },
		contentWindowInsets = WindowInsets.statusBars
	) { innerPadding ->
		CompositionLocalProvider(
			LocalMinimumInteractiveComponentSize provides 0.dp
		) {
			Column(
				Modifier
					.padding(innerPadding)
					.verticalScroll(rememberScrollState())
					.padding(top = 16.dp, end = 16.dp, start = 16.dp)
			) {
				Form {
					SettingSelectionRow(
						title = { Text(stringResource(Res.string.title_theme_mode)) },
						items = ThemeMode.entries.toImmutableList(),
						label = { stringResource(it.title) },
						selection = preferenceManager.themeMode,
						onSelect = { preferenceManager.themeMode = it }
					)
					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_dynamic_theming)) },
						subtitle = { Text(stringResource(Res.string.subtitle_dynamic_theming)) },
						value = preferenceManager.dynamicTheming,
						onSetValue = { preferenceManager.dynamicTheming = it }
					)
				}

				val paletteEnabled = !preferenceManager.dynamicTheming
				Column(
					Modifier
						.alpha(if (paletteEnabled) 1f else 0.38f)
						.then(if (paletteEnabled) Modifier else Modifier.semantics { disabled() })
				) {
				FormTitle(stringResource(Res.string.title_palette))
				Form {
					FormRow {
						LazyRow(
							modifier = Modifier
								.fillMaxWidth()
								.selectableGroup(),
							horizontalArrangement = Arrangement.SpaceBetween
						) {
							items(Theme.entries) { theme ->
								ThemeCard(
									theme = theme,
									isSelected = preferenceManager.theme == theme,
									enabled = paletteEnabled,
									onSelect = { preferenceManager.theme = theme }
								)
							}
						}
					}

					AnimatedVisibility(
						modifier = Modifier.fillMaxWidth(),
						visible = paletteEnabled && preferenceManager.theme == Theme.Seeded
					) {
						ThemeAccentPicker(enabled = paletteEnabled)
					}

					AnimatedVisibility(
						modifier = Modifier.fillMaxWidth(),
						visible = paletteEnabled && preferenceManager.theme == Theme.Seeded
					) {
						Column {
							PaletteStyleGroup(
								title = stringResource(Res.string.palette_group_single_color),
								styles = PaletteStyle.entries.filterNot { it in multicolorPaletteStyles },
								selection = preferenceManager.paletteStyle,
								onSelect = { preferenceManager.paletteStyle = it }
							)
							PaletteStyleGroup(
								title = stringResource(Res.string.palette_group_multicolor),
								styles = PaletteStyle.entries.filter { it in multicolorPaletteStyles },
								selection = preferenceManager.paletteStyle,
								onSelect = { preferenceManager.paletteStyle = it }
							)
						}
					}

					AnimatedVisibility(
						modifier = Modifier.fillMaxWidth(),
						visible = paletteEnabled && preferenceManager.theme == Theme.Seeded
					) {
						SettingSelectionRow(
							title = { Text(stringResource(Res.string.option_palette_specification)) },
							items = ColorSpec.SpecVersion.entries.toImmutableList(),
							label = { it.label() },
							selection = preferenceManager.paletteSpec,
							onSelect = { preferenceManager.paletteSpec = it }
						)
					}
				}
			}
		}
	}
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteStyleGroup(
	title: String,
	styles: List<PaletteStyle>,
	selection: PaletteStyle,
	onSelect: (PaletteStyle) -> Unit
) {
	Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
		Text(
			text = title,
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.primary
		)
		FlowRow(
			modifier = Modifier.padding(top = 8.dp).selectableGroup(),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			styles.forEach { style ->
				val selected = style == selection
				Row(
					modifier = Modifier
						.clip(MaterialTheme.shapes.large)
						.background(
							if (selected) MaterialTheme.colorScheme.primary
							else MaterialTheme.colorScheme.surfaceContainerHighest
						)
						.selectable(
							selected = selected,
							onClick = { onSelect(style) },
							role = Role.RadioButton
						)
						.padding(horizontal = 12.dp, vertical = 9.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(6.dp)
				) {
					if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp))
					Text(
						style.label(),
						color = if (selected) MaterialTheme.colorScheme.onPrimary
						else MaterialTheme.colorScheme.onSurface
					)
				}
				}
			}
		}
	}
private val multicolorPaletteStyles = setOf(
	PaletteStyle.Expressive,
	PaletteStyle.Rainbow,
	PaletteStyle.FruitSalad
)

@Composable
private fun BaseCard(
	modifier: Modifier,
	isSelected: Boolean,
	onSelect: () -> Unit,
	enabled: Boolean = true,
	square: Boolean = false,
	content: @Composable ColumnScope.() -> Unit
) {
	val haptics = LocalHapticFeedback.current
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()

	val radius by animateDpAsState(
		if (square || isPressed || isSelected) 16.dp else 36.dp
	)
	val borderColor by animateColorAsState(
		if (isSelected)
			MaterialTheme.colorScheme.primary
		else Color.Transparent
	)
	val shape = ContinuousRoundedRectangle(radius)

	Box(
		modifier = modifier
			.size(64.dp)
			.border(4.dp, borderColor, shape)
			.clip(shape)
			.selectable(
				selected = isSelected,
				interactionSource = interactionSource,
				indication = ripple(),
				enabled = enabled,
				role = Role.ValuePicker,
				onClick = dropUnlessResumed {
					haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
					onSelect()
				}
			),
		contentAlignment = Alignment.Center
	) {
		Column(
			modifier = Modifier
				.size(50.dp)
				.clip(ContinuousRoundedRectangle(radius - 7.dp))
		) {
			content()
		}
	}
}

@Composable
private fun ThemeCard(
	theme: Theme,
	isSelected: Boolean,
	enabled: Boolean,
	onSelect: () -> Unit
) {
	val colorScheme = theme.colorScheme()
	val title = stringResource(theme.title)

	TooltipBox(title) {
		BaseCard(
			modifier = Modifier.semantics {
				contentDescription = title
			},
			isSelected = isSelected,
			enabled = enabled,
			onSelect = onSelect,
		) {
			if (theme != Theme.Seeded) {
				Box(
					modifier = Modifier
						.weight(1f)
						.fillMaxSize()
						.background(colorScheme.primary)
				)

				Row(modifier = Modifier.weight(1f)) {
					Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxSize()
							.background(colorScheme.secondary)
					)
					Box(
						modifier = Modifier
							.weight(1f)
							.fillMaxSize()
							.background(colorScheme.tertiary)
					)
				}
			} else {
				Box(
					modifier = Modifier
						.weight(1f)
						.fillMaxSize()
						.background(MaterialTheme.colorScheme.primaryContainer),
					contentAlignment = Alignment.Center
				) {
					Icon(
						Icons.Outlined.Picker,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onPrimaryContainer
					)
				}
			}
		}
	}
}

@Composable
private fun ThemeAccentPicker(enabled: Boolean) {
	val preferenceManager = koinInject<PreferenceManager>()
	var expanded by remember { mutableStateOf(false) }

	FormRow(
		onClick = if (enabled) ({ expanded = true }) else null
	) {
		Text(stringResource(Res.string.option_accent_colour))
		Box {
			Box(
				Modifier
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.primary)
					.size(40.dp)
					.clickable {
						expanded = true
					}
			)
			Dropdown(
				expanded = expanded,
				onDismissRequest = { expanded = false }
			) {
				FormRow(
					color = MaterialTheme.colorScheme.surfaceContainerHigh,
					horizontalArrangement = Arrangement.Center
				) {
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {
						AccentPresetSwatches(
							selectedColor = preferenceManager.paletteAccentColor,
							onSelect = { preferenceManager.paletteAccentColor = it }
						)
						RingColorPicker(
							color = {
								HsvColor(Color(preferenceManager.paletteAccentColor))
							},
							onColorChange = { color ->
								preferenceManager.paletteAccentColor = color.toColor().toArgb()
							}
						)
					}
				}
			}
		}
	}
}

private val accentPresets = listOf(
	"Turquoise" to Color(0xFF1DE9B6),
	"Purple" to Color(0xFF9B5DE5),
	"Pink" to Color(0xFFFF4D8D),
	"Brown" to Color(0xFF8D6E63),
	"Orange" to Color(0xFFFF7A00),
	"Yellow" to Color(0xFFFFCA28)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPresetSwatches(
	selectedColor: Int,
	onSelect: (Int) -> Unit
) {
	FlowRow(
		horizontalArrangement = Arrangement.spacedBy(10.dp),
		verticalArrangement = Arrangement.spacedBy(10.dp),
		maxItemsInEachRow = 3
	) {
		accentPresets.forEach { (name, color) ->
			val argb = color.toArgb()
			val selected = argb == selectedColor
			Box(
				modifier = Modifier
					.size(44.dp)
					.clip(CircleShape)
					.background(color)
					.then(
						if (selected) {
							Modifier.border(
								3.dp,
								MaterialTheme.colorScheme.onSurface,
								CircleShape
							)
						} else Modifier
					)
					.clickable { onSelect(argb) }
					.semantics { contentDescription = name },
				contentAlignment = Alignment.Center
			) {
				if (selected) {
					Icon(
						Icons.Outlined.Check,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurface,
						modifier = Modifier.size(20.dp)
					)
				}
			}
		}
	}
}
