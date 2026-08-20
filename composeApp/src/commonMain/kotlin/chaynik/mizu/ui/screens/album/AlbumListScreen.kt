package chaynik.mizu.ui.screens.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_albums
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import chaynik.mizu.LocalBottomBarScrollManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.DomainAlbumListType
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.domain.models.settings.BottomBarVisibilityMode
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.layouts.ArtGrid
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.components.layouts.PullToRefreshBox
import chaynik.mizu.ui.components.layouts.RootBottomBar
import chaynik.mizu.ui.components.layouts.RootTopBar
import chaynik.mizu.ui.components.snackbars.ErrorSnackBar
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.PersistentViewModelStoreOwner
import chaynik.mizu.ui.screens.album.components.AlbumListScreenSortButton
import chaynik.mizu.ui.screens.album.components.albumListScreenContent
import chaynik.mizu.ui.screens.album.viewmodels.AlbumListViewModel
import chaynik.mizu.ui.screens.share.dialogs.ShareDialog
import chaynik.mizu.util.ui.withoutTop
import mizu.composeapp.generated.resources.title_random_albums
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumListScreen(
	nested: Boolean = false,
	listType: DomainAlbumListType
) {
	val preferenceManager = koinInject<PreferenceManager>()

	val viewModel = koinViewModel<AlbumListViewModel>(
		key = listType.toString(),
		parameters = { parametersOf(listType) },
		viewModelStoreOwner = if (nested) {
			LocalViewModelStoreOwner.current!!
		} else {
			koinInject<PersistentViewModelStoreOwner>()
		}
	)
	val player = koinInject<MediaPlayerViewModel>()
	val selectedSorting by viewModel.listType.collectAsStateWithLifecycle()
	val selectedReversed by viewModel.selectedReversed.collectAsStateWithLifecycle()
	val albumsState by viewModel.albumsState.collectAsStateWithLifecycle()
	val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()
	val starred by viewModel.starred.collectAsStateWithLifecycle()
	val rating by viewModel.rating.collectAsStateWithLifecycle()
	var shareId by remember { mutableStateOf<String?>(null) }
	var shareExpiry by remember { mutableStateOf<Duration?>(null) }
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	val screenTitle = if (listType == DomainAlbumListType.Random)
		Res.string.title_random_albums else Res.string.title_albums

	val actions: @Composable RowScope.() -> Unit = {
		AlbumListScreenSortButton(
			nested = nested,
			selectedSorting = selectedSorting,
			onSetSorting = { viewModel.setListType(it) },
			selectedReversed = selectedReversed,
			onSetReversed = { viewModel.setReversed(it) }
		)
	}

	Scaffold(
		topBar = {
			if (!nested) {
				RootTopBar(
					{ Text(stringResource(screenTitle)) },
					scrollBehavior,
					actions
				)
			} else {
				NestedTopBar({ Text(stringResource(screenTitle)) }, actions)
			}
		},
		bottomBar = {
			val scrollManager = LocalBottomBarScrollManager.current
			if (!nested || preferenceManager.bottomBarVisibilityMode == BottomBarVisibilityMode.AllScreens) {
				RootBottomBar(scrolled = scrollManager.isTriggered)
			}
		}
	) { innerPadding ->
		PullToRefreshBox(
			modifier = Modifier
				.padding(top = innerPadding.calculateTopPadding())
				.background(MaterialTheme.colorScheme.surface),
			finished = albumsState !is UiState.Loading,
			onRefresh = { viewModel.refreshAlbums(true) },
			key = albumsState
		) {
			ArtGrid(
				modifier = if (!nested)
					Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
				else Modifier,
				state = viewModel.gridState,
				contentPadding = innerPadding.withoutTop(),
				verticalArrangement = if ((albumsState as? UiState.Success)?.data?.isEmpty() == true)
					Arrangement.Center
				else Arrangement.spacedBy(12.dp)
			) {
				albumListScreenContent(
					state = albumsState,
					starred = starred,
					selectedAlbum = selectedAlbum,
					selectedAlbumRating = rating,
					onPlayNext = { if (selectedAlbum != null) player.playNext(selectedAlbum as DomainSongCollection) },
					onAddToQueue = { if (selectedAlbum != null) player.addToQueue(selectedAlbum as DomainSongCollection) },
					onUpdateSelection = { viewModel.selectAlbum(it) },
					onClearSelection = { viewModel.clearSelection() },
					onSetShareId = { newShareId ->
						shareId = newShareId
					},
					onSetStarred = { viewModel.starAlbum(it) },
					onRateSelectedAlbum = { viewModel.setRating(it) }
				)
			}
		}
	}

	ErrorSnackBar(
		error = (albumsState as? UiState.Error)?.error,
		onClearError = { viewModel.clearError() }
	)

	ShareDialog(
		id = shareId,
		onIdClear = { shareId = null },
		expiry = shareExpiry,
		onExpiryChange = { shareExpiry = it }
	)
}
