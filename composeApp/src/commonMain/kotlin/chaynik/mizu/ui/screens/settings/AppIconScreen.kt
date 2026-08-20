package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_app_icon
import mizu.composeapp.generated.resources.option_choose_app_icon
import mizu.composeapp.generated.resources.app_icon_original
import mizu.composeapp.generated.resources.app_icon_monochrome
import mizu.composeapp.generated.resources.app_icon_black
import mizu.composeapp.generated.resources.app_icon_white
import mizu.composeapp.generated.resources.app_icon_apple_music
import mizu.composeapp.generated.resources.app_icon_purple
import mizu.composeapp.generated.resources.app_icon_yellow
import mizu.composeapp.generated.resources.app_icon_green
import mizu.composeapp.generated.resources.app_icon_brown
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.AppIconManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.AppIconVariant
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Info
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.layouts.NestedTopBar

@Composable
fun SettingsAppIconScreen() {
	val appIconManager = koinInject<AppIconManager>()
	val preferenceManager = koinInject<PreferenceManager>()
	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.option_choose_app_icon)) }) }
	) { innerPadding ->
		CompositionLocalProvider(
			LocalMinimumInteractiveComponentSize provides 0.dp
		) {
			Column(
				Modifier
					.padding(innerPadding)
					.verticalScroll(rememberScrollState())
					.padding(16.dp)
			) {
				Form(Modifier.selectableGroup()) {
					AppIconVariant.entries.forEach { variant ->
						FormRow(
							onClick = {
								if (preferenceManager.appIconVariant != variant) {
									appIconManager.setVariant(variant)
								}
							},
							modifier = Modifier.semantics {
								selected = preferenceManager.appIconVariant == variant
							},
							horizontalArrangement = Arrangement.spacedBy(14.dp),
						) {
							RadioButton(
								selected = preferenceManager.appIconVariant == variant,
								onClick = null
							)
							Column(Modifier.weight(1f)) {
								Text(stringResource(variant.titleResource))
							}
							AppIconItemPreview(variant)
						}
					}
				}

				Spacer(Modifier.height(24.dp))
				Row(
					modifier = Modifier.padding(horizontal = 8.dp),
					horizontalArrangement = Arrangement.spacedBy(16.dp)
				) {
					Icon(
						Icons.Outlined.Info,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Text(
						stringResource(Res.string.info_app_icon),
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						style = MaterialTheme.typography.bodyMedium
					)
				}
			}
		}
	}
}

private val AppIconVariant.titleResource
	get() = when (this) {
		AppIconVariant.Original -> Res.string.app_icon_original
		AppIconVariant.Monochrome -> Res.string.app_icon_monochrome
		AppIconVariant.Black -> Res.string.app_icon_black
		AppIconVariant.White -> Res.string.app_icon_white
		AppIconVariant.AppleMusic -> Res.string.app_icon_apple_music
		AppIconVariant.Purple -> Res.string.app_icon_purple
		AppIconVariant.Yellow -> Res.string.app_icon_yellow
		AppIconVariant.Green -> Res.string.app_icon_green
		AppIconVariant.Brown -> Res.string.app_icon_brown
	}

@Composable
fun AppIconItemPreview(variant: AppIconVariant, modifier: Modifier = Modifier) {
	val appIconManager = koinInject<AppIconManager>()
	val icon = remember(variant) { appIconManager.getIcon(variant) }

	val iconModifier = modifier
		.size(48.dp)
		.clip(MaterialTheme.shapes.medium)

	when (icon) {
		is ImageBitmap -> Image(
			bitmap = icon,
			contentDescription = null,
			modifier = iconModifier
		)
		is Painter -> Image(
			painter = icon,
			contentDescription = null,
			modifier = iconModifier
		)
	}
}
