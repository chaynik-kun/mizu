package chaynik.mizu.ui.screens.playlist.components

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Modifier
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_no_playlists_short
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.domain.models.DomainPlaylist
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.PlaylistRemove
import chaynik.mizu.ui.components.common.ContentUnavailable
import chaynik.mizu.ui.components.layouts.artGridPlaceholder
import chaynik.mizu.ui.core.UiState

fun LazyGridScope.playlistListScreenContent(
	state: UiState<List<DomainPlaylist>>,
	selectedPlaylist: DomainPlaylist?,
	onUpdateSelection: (DomainPlaylist) -> Unit,
	onClearSelection: () -> Unit,
	onSetShareId: (String) -> Unit,
	onSetDeletionId: (String) -> Unit,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
) {
	val data = state.data.orEmpty()
	if (data.isNotEmpty()) {
		items(data, { it.id }) { playlist ->
			PlaylistListScreenItem(
				modifier = Modifier.animateItem(),
				tab = "playlists",
				playlist = playlist,
				selected = playlist == selectedPlaylist,
				onSelect = { onUpdateSelection(playlist) },
				onDeselect = { onClearSelection() },
				onSetShareId = onSetShareId,
				onSetDeletionId = onSetDeletionId,
				onPlayNext = onPlayNext,
				onAddToQueue = onAddToQueue,
			)
		}
	} else {
		when (state) {
			is UiState.Loading -> {
				artGridPlaceholder()
			}

			else -> {
				item(span = { GridItemSpan(maxLineSpan) }) {
					ContentUnavailable(
						icon = Icons.Outlined.PlaylistRemove,
						label = stringResource(Res.string.info_no_playlists_short)
					)
				}
			}
		}
	}
}
