package chaynik.mizu.ui.screens.settings.dialogs

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_ok
import mizu.composeapp.generated.resources.action_reorder
import mizu.composeapp.generated.resources.option_navigation_bar_tabs
import mizu.composeapp.generated.resources.title_home
import mizu.composeapp.generated.resources.title_library
import mizu.composeapp.generated.resources.title_albums
import mizu.composeapp.generated.resources.title_playlists
import mizu.composeapp.generated.resources.title_artists
import mizu.composeapp.generated.resources.title_search
import mizu.composeapp.generated.resources.title_genres
import mizu.composeapp.generated.resources.title_songs
import mizu.composeapp.generated.resources.title_radios
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.domain.models.settings.NavbarTab
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.DragHandle
import chaynik.mizu.ui.components.common.ErrorBox
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.screens.settings.viewmodels.NavtabsViewModel
import chaynik.mizu.util.ui.DraggableListState
import chaynik.mizu.util.ui.dragHandle
import chaynik.mizu.util.ui.draggableItems
import chaynik.mizu.util.ui.rememberDraggableListState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavtabsDialog(
	presented: Boolean,
	onDismissRequest: () -> Unit
) {
	if (!presented) return

	val haptic = LocalHapticFeedback.current
	val viewModel = koinViewModel<NavtabsViewModel>()
	val state by viewModel.state.collectAsState()

	val draggableState = rememberDraggableListState { from, to ->
		viewModel.move(from, to)
		haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
	}

	when (state) {
		is UiState.Loading -> return
		is UiState.Error -> ErrorBox(state as UiState.Error)
		is UiState.Success -> {
			val config = (state as UiState.Success).data
			val visibleRegularTabs = config.tabs.count {
				it.visible && it.id != NavbarTab.Id.SEARCH
			}
			AlertDialog(
				title = {
					Text(stringResource(Res.string.option_navigation_bar_tabs))
				},
				text = {
					LazyColumn(
						modifier = Modifier
							.fillMaxWidth()
							.heightIn(max = 300.dp),
						state = draggableState.listState,
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						draggableItems(
							state = draggableState,
							items = config.tabs,
							key = { tab -> tab.id }
						) { tab, isDragging ->
							NavtabRow(
								tab = tab,
								state = draggableState,
								isDragging = isDragging,
								canEnable = tab.visible || tab.id == NavbarTab.Id.SEARCH || visibleRegularTabs < 4,
								onToggleVisibility = {
									viewModel.toggleVisibility(tab.id)
								}
							)
						}
					}
				},
				onDismissRequest = onDismissRequest,
				confirmButton = {
					Button(onClick = onDismissRequest) {
						Text(stringResource(Res.string.action_ok))
					}
				}
			)
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavtabRow(
	tab: NavbarTab,
	state: DraggableListState,
	isDragging: Boolean,
	canEnable: Boolean,
	onToggleVisibility: () -> Unit
) {
	val elevation by animateDpAsState(
		if (isDragging) 4.dp else 0.dp,
		animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
	)

	Surface(
		shadowElevation = elevation,
		modifier = Modifier.fillMaxWidth(),
		shape = MaterialTheme.shapes.large
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(8.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Checkbox(
				enabled = tab.id != NavbarTab.Id.HOME && canEnable,
				checked = tab.visible,
				onCheckedChange = { _ ->
					onToggleVisibility()
				}
			)
			Text(stringResource(tab.id.titleResource()))
			IconButton(
				modifier = Modifier.dragHandle(
					state = state,
					key = tab.id
				),
				onClick = {}
			) {
				Icon(
					Icons.Outlined.DragHandle,
					contentDescription = stringResource(Res.string.action_reorder)
				)
			}
		}
	}
}

private fun NavbarTab.Id.titleResource(): StringResource = when (this) {
	NavbarTab.Id.HOME -> Res.string.title_home
	NavbarTab.Id.LIBRARY -> Res.string.title_library
	NavbarTab.Id.ALBUMS -> Res.string.title_albums
	NavbarTab.Id.PLAYLISTS -> Res.string.title_playlists
	NavbarTab.Id.ARTISTS -> Res.string.title_artists
	NavbarTab.Id.SEARCH -> Res.string.title_search
	NavbarTab.Id.GENRES -> Res.string.title_genres
	NavbarTab.Id.SONGS -> Res.string.title_songs
	NavbarTab.Id.RADIOS -> Res.string.title_radios
}
