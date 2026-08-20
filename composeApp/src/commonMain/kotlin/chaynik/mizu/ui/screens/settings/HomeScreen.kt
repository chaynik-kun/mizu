package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_reorder
import mizu.composeapp.generated.resources.title_home_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.HomeSection
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.DragHandle
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.util.ui.dragHandle
import chaynik.mizu.util.ui.draggableItems
import chaynik.mizu.util.ui.rememberDraggableListState

@Composable
fun SettingsHomeScreen() {
	val preferences = koinInject<PreferenceManager>()
	val haptics = LocalHapticFeedback.current
	val sections = HomeSection.decode(preferences.homeSectionOrder)
	val hidden = preferences.homeHiddenSections.split(',').filter { it.isNotBlank() }.toSet()
	val draggableState = rememberDraggableListState { from, to ->
		val reordered = sections.toMutableList().apply { add(to, removeAt(from)) }
		preferences.homeSectionOrder = HomeSection.encode(reordered)
		haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
	}

	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.title_home_settings)) }) }
	) { padding ->
		LazyColumn(
			modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
			state = draggableState.listState,
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			draggableItems(
				state = draggableState,
				items = sections,
				key = { it.name }
			) { section, _ ->
				FormRow {
					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically
					) {
						Checkbox(
							checked = section.name !in hidden,
							onCheckedChange = { visible ->
								val updated = hidden.toMutableSet().apply {
									if (visible) remove(section.name) else add(section.name)
								}
								preferences.homeHiddenSections = updated.joinToString(",")
							}
						)
						Text(stringResource(section.title), Modifier.weight(1f))
						IconButton(
							modifier = Modifier.dragHandle(draggableState, section.name),
							onClick = {}
						) {
							Icon(Icons.Outlined.DragHandle, stringResource(Res.string.action_reorder))
						}
					}
				}
			}
		}
	}
}
