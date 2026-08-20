package chaynik.mizu.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.ui.components.dialogs.DeletionViewModel
import chaynik.mizu.ui.screens.album.viewmodels.AlbumListViewModel
import chaynik.mizu.ui.screens.artist.viewmodels.ArtistDetailViewModel
import chaynik.mizu.ui.screens.artist.viewmodels.ArtistListViewModel
import chaynik.mizu.ui.screens.collection.viewmodels.CollectionDetailViewModel
import chaynik.mizu.ui.screens.genre.viewmodels.GenreListViewModel
import chaynik.mizu.ui.screens.lyrics.viewmodels.LyricsScreenViewModel
import chaynik.mizu.ui.screens.nowPlaying.viewmodels.NowPlayingViewModel
import chaynik.mizu.ui.screens.playlist.viewmodels.PlaylistCreateDialogViewModel
import chaynik.mizu.ui.screens.playlist.viewmodels.PlaylistListViewModel
import chaynik.mizu.ui.screens.playlist.viewmodels.PlaylistUpdateDialogViewModel
import chaynik.mizu.ui.screens.queue.viewmodels.QueueViewModel
import chaynik.mizu.ui.screens.radio.viewmodels.RadioCreateDialogViewModel
import chaynik.mizu.ui.screens.radio.viewmodels.RadioListViewModel
import chaynik.mizu.ui.screens.search.viewmodels.SearchViewModel
import chaynik.mizu.ui.screens.settings.viewmodels.LyricsPriorityViewModel
import chaynik.mizu.ui.screens.settings.viewmodels.NavtabsViewModel
import chaynik.mizu.ui.screens.settings.viewmodels.SettingsDataStorageViewModel
import chaynik.mizu.ui.screens.share.viewmodels.ShareDialogViewModel
import chaynik.mizu.ui.screens.share.viewmodels.ShareListViewModel
import chaynik.mizu.ui.screens.song.viewmodels.SongDetailViewModel
import chaynik.mizu.ui.screens.song.viewmodels.SongListViewModel

val viewModelModule = module {
	viewModel { (artistId: String) ->
		ArtistDetailViewModel(
			artistId = artistId,
			repository = get(),
			artistRepository = get(),
			songRepository = get(),
			albumRepository = get(),
			artistDao = get(),
			albumDao = get(),
			downloadManager = get(),
			snackBarManager = get(),
			connectivityManager = get()
		)
	}

	viewModel {
		LyricsScreenViewModel(
			repository = get()
		)
	}

	viewModel { (songs: List<DomainSong>, playlistToExclude: String?) ->
		PlaylistUpdateDialogViewModel(
			songs = songs,
			playlistToExclude = playlistToExclude,
			sessionManager = get(),
			snackBarManager = get()
		)
	}

	viewModelOf(::AlbumListViewModel)
	viewModelOf(::SongListViewModel)
	viewModelOf(::ArtistListViewModel)
	viewModelOf(::SearchViewModel)
	viewModelOf(::GenreListViewModel)
	viewModelOf(::RadioListViewModel)
	viewModelOf(::RadioCreateDialogViewModel)
	viewModelOf(::PlaylistListViewModel)
	viewModelOf(::QueueViewModel)
	viewModelOf(::ShareListViewModel)
	viewModelOf(::DeletionViewModel)
	viewModelOf(::ShareDialogViewModel)
	viewModel { (songs: List<DomainSong>) ->
		PlaylistCreateDialogViewModel(
			songs = songs,
			playlistDao = get(),
			sessionManager = get(),
			snackBarManager = get()
		)
	}
	viewModel { params ->
		CollectionDetailViewModel(
			collectionId = params.get(),
			repository = get(),
			songRepository = get(),
			albumRepository = get(),
			downloadManager = get(),
			sessionManager = get(),
			snackBarManager = get(),
			connectivityManager = get(),
			playbackCacheManager = get()
		)
	}
	viewModelOf(::SongDetailViewModel)
	viewModelOf(::SettingsDataStorageViewModel)
	viewModel { params ->
		NowPlayingViewModel(
			player = params.get(),
			songRepository = get()
		)
	}
	viewModelOf(::NavtabsViewModel)
	viewModelOf(::LyricsPriorityViewModel)
}
