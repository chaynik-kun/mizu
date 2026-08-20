package chaynik.mizu.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_home
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import chaynik.mizu.LocalBottomBarScrollManager
import chaynik.mizu.domain.models.DomainAlbumListType
import chaynik.mizu.domain.models.DomainArtistListType
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.domain.models.DomainSongListType
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.dialogs.DeletionDialog
import chaynik.mizu.ui.components.dialogs.DeletionEndpoint
import chaynik.mizu.ui.components.layouts.PullToRefreshBox
import chaynik.mizu.ui.components.layouts.RootBottomBar
import chaynik.mizu.ui.components.layouts.RootTopBar
import chaynik.mizu.ui.components.snackbars.ErrorSnackBar
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.PersistentViewModelStoreOwner
import chaynik.mizu.ui.screens.album.viewmodels.AlbumListViewModel
import chaynik.mizu.ui.screens.artist.viewmodels.ArtistListViewModel
import chaynik.mizu.ui.screens.genre.viewmodels.GenreListViewModel
import chaynik.mizu.ui.screens.library.components.LibraryScreenContent
import chaynik.mizu.ui.screens.playlist.dialogs.PlaylistCreateDialog
import chaynik.mizu.ui.screens.playlist.viewmodels.PlaylistListViewModel
import chaynik.mizu.ui.screens.share.dialogs.ShareDialog
import chaynik.mizu.ui.screens.song.viewmodels.SongListViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
	val persistentViewModelStoreOwner = koinInject<PersistentViewModelStoreOwner>()

	val albumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "libraryAlbums",
		parameters = { parametersOf(DomainAlbumListType.Newest) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val albumsState by albumsViewModel.albumsState.collectAsStateWithLifecycle()
	val selectedAlbum by albumsViewModel.selectedAlbum.collectAsStateWithLifecycle()
	val selectedAlbumIsStarred by albumsViewModel.starred.collectAsStateWithLifecycle()
	val selectedAlbumRating by albumsViewModel.rating.collectAsStateWithLifecycle()

	val recentAlbumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "homeRecentAlbums",
		parameters = { parametersOf(DomainAlbumListType.Recent) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val recentAlbumsState by recentAlbumsViewModel.albumsState.collectAsStateWithLifecycle()
	val randomAlbumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "homeRandomAlbums",
		parameters = { parametersOf(DomainAlbumListType.Random) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val randomAlbumsState by randomAlbumsViewModel.albumsState.collectAsStateWithLifecycle()
	val frequentAlbumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "homeFrequentAlbums",
		parameters = { parametersOf(DomainAlbumListType.Frequent) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val frequentAlbumsState by frequentAlbumsViewModel.albumsState.collectAsStateWithLifecycle()
	val favoriteAlbumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "homeFavoriteAlbums",
		parameters = { parametersOf(DomainAlbumListType.Starred) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val favoriteAlbumsState by favoriteAlbumsViewModel.albumsState.collectAsStateWithLifecycle()
	val releaseAlbumsViewModel = koinViewModel<AlbumListViewModel>(
		key = "homeReleaseAlbums",
		parameters = { parametersOf(DomainAlbumListType.Year) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val releaseAlbumsState by releaseAlbumsViewModel.albumsState.collectAsStateWithLifecycle()

	val randomSongsViewModel = koinViewModel<SongListViewModel>(
		key = "homeRandomSongs",
		parameters = { parametersOf(DomainSongListType.Random) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val randomSongsState by randomSongsViewModel.songsState.collectAsStateWithLifecycle()

	val playlistsViewModel = koinViewModel<PlaylistListViewModel>(
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val playlistsState by playlistsViewModel.playlistsState.collectAsStateWithLifecycle()
	val selectedPlaylist by playlistsViewModel.selectedPlaylist.collectAsStateWithLifecycle()

	val artistsViewModel = koinViewModel<ArtistListViewModel>(
		key = "libraryArtists",
		parameters = { parametersOf(DomainArtistListType.AlphabeticalByName) },
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val artistsState by artistsViewModel.artistsState.collectAsStateWithLifecycle()
	val selectedArtist by artistsViewModel.selectedArtist.collectAsStateWithLifecycle()
	val selectedArtistAlbums by artistsViewModel.selectedArtistAlbums.collectAsStateWithLifecycle()
	val selectedArtistIsStarred by artistsViewModel.starred.collectAsStateWithLifecycle()

	val genresViewModel = koinViewModel<GenreListViewModel>(
		viewModelStoreOwner = persistentViewModelStoreOwner
	)
	val genresState by genresViewModel.genresState.collectAsStateWithLifecycle()

	var shareId by rememberSaveable { mutableStateOf<String?>(null) }
	var shareExpiry by remember { mutableStateOf<Duration?>(null) }
	var playlistDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
	var playlistCreateDialogShown by rememberSaveable { mutableStateOf(false) }

	val player = koinInject<MediaPlayerViewModel>()

	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

	LaunchedEffect(Unit) {
		while (true) {
			delay(15.minutes)
			randomSongsViewModel.refreshSongs(false)
		}
	}

	Scaffold(
		topBar = { RootTopBar({ Text(stringResource(Res.string.title_home)) }, scrollBehavior) },
		bottomBar = {
			val scrollManager = LocalBottomBarScrollManager.current
			RootBottomBar(scrolled = scrollManager.isTriggered)
		}
	) { innerPadding ->
		PullToRefreshBox(
			modifier = Modifier
				.padding(top = innerPadding.calculateTopPadding())
				.background(MaterialTheme.colorScheme.surface),
			finished = randomSongsState !is UiState.Loading &&
				albumsState !is UiState.Loading &&
				playlistsState !is UiState.Loading &&
				artistsState !is UiState.Loading &&
				genresState !is UiState.Loading,
			onRefresh = {
				randomSongsViewModel.refreshSongs(true)
				albumsViewModel.refreshAlbums(true)
				recentAlbumsViewModel.refreshAlbums(true)
				randomAlbumsViewModel.refreshAlbums(true)
				frequentAlbumsViewModel.refreshAlbums(true)
				favoriteAlbumsViewModel.refreshAlbums(true)
				releaseAlbumsViewModel.refreshAlbums(true)
				playlistsViewModel.refreshPlaylists(true)
				artistsViewModel.refreshArtists(true)
				genresViewModel.refreshGenres(true)
			},
			key = listOf(randomSongsState, albumsState, playlistsState, artistsState, genresState)
		) {
			LibraryScreenContent(
				scrollBehavior = scrollBehavior,
				innerPadding = innerPadding,
				onSetShareId = { shareId = it },
				randomSongsState = randomSongsState,
				onPlayRandomSongs = { songs, index -> player.playNow(songs, index) },
				recentAlbumsState = recentAlbumsState,
				randomAlbumsState = randomAlbumsState,
				frequentAlbumsState = frequentAlbumsState,
				favoriteAlbumsState = favoriteAlbumsState,
				releaseAlbumsState = releaseAlbumsState,

				albumsState = albumsState,
				selectedAlbum = selectedAlbum,
				selectedAlbumIsStarred = selectedAlbumIsStarred,
				selectedAlbumRating = selectedAlbumRating,
				onSelectAlbum = { albumsViewModel.selectAlbum(it) },
				onClearAlbumSelection = { albumsViewModel.clearSelection() },
				onStarSelectedAlbum = { albumsViewModel.starAlbum(it) },
				onPlayAlbumNext = { if (selectedAlbum != null) player.playNext(selectedAlbum as DomainSongCollection) },
				onAddAlbumToQueue = { if (selectedAlbum != null) player.addToQueue(selectedAlbum as DomainSongCollection) },
				onRateSelectedAlbum = { albumsViewModel.setRating(it) },

				artistsState = artistsState,
				selectedArtist = selectedArtist,
				selectedArtistAlbums = selectedArtistAlbums,
				selectedArtistIsStarred = selectedArtistIsStarred,
				onSelectArtist = { artistsViewModel.selectArtist(it) },
				onClearArtistSelection = { artistsViewModel.clearSelection() },
				onStarSelectedArtist = { artistsViewModel.starArtist(it) },
				onPlayArtistNext = {
					if (selectedArtist != null) artistsViewModel.playArtistAlbumsNext(
						player
					)
				},
				onAddArtistToQueue = {
					if (selectedArtist != null) artistsViewModel.addArtistAlbumsToQueue(
						player
					)
				},

				playlistsState = playlistsState,
				selectedPlaylist = selectedPlaylist,
				onSelectPlaylist = { playlistsViewModel.selectPlaylist(it) },
				onClearPlaylistSelection = { playlistsViewModel.clearSelection() },
				onDeletePlaylist = { playlistDeletionId = it },
				onPlayPlaylistNext = {
					if (selectedPlaylist != null) player.playNext(
						selectedPlaylist as DomainSongCollection
					)
				},
				onAddPlaylistToQueue = {
					if (selectedPlaylist != null) player.addToQueue(
						selectedPlaylist as DomainSongCollection
					)
				},

				genresState = genresState
			)
		}
	}

	val flattenedErrors = listOf(
		(randomSongsState as? UiState.Error)?.error,
		(albumsState as? UiState.Error)?.error,
		(playlistsState as? UiState.Error)?.error,
		(artistsState as? UiState.Error)?.error,
		(genresState as? UiState.Error)?.error
	).mapNotNull { it?.stackTraceToString() }.takeIf { it.isNotEmpty() }?.joinToString("\n\n")

	ErrorSnackBar(
		error = flattenedErrors?.let { Error(it) },
		onClearError = {
			randomSongsViewModel.clearError()
			albumsViewModel.clearError()
			playlistsViewModel.clearError()
			artistsViewModel.clearError()
			genresViewModel.clearError()
		}
	)

	ShareDialog(
		id = shareId,
		onIdClear = { shareId = null },
		expiry = shareExpiry,
		onExpiryChange = { shareExpiry = it }
	)

	DeletionDialog(
		endpoint = DeletionEndpoint.PLAYLIST,
		id = playlistDeletionId,
		onIdClear = { playlistDeletionId = null },
		onRefresh = { playlistsViewModel.refreshPlaylists(false) }
	)

	if (playlistCreateDialogShown) {
		PlaylistCreateDialog(
			onDismissRequest = { playlistCreateDialogShown = false },
			onRefresh = { playlistsViewModel.refreshPlaylists(true) }
		)
	}
}
