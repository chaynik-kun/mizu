package chaynik.mizu.ui.screens.song.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.DownloadManager
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.manager.PlaybackCacheManager
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongListType
import chaynik.mizu.domain.repositories.SongRepository
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.data.database.entities.DownloadStatus

private data class SongRefreshTrigger(
	val loggedIn: Boolean,
	val online: Boolean,
	val downloadAvailability: List<Pair<String, DownloadStatus>>,
	val cachedSongIds: Set<String>
)

class SongListViewModel(
	initialListType: DomainSongListType = DomainSongListType.Alphabetical,
	private val repository: SongRepository,
	private val downloadManager: DownloadManager,
	private val sessionManager: SessionManager,
	connectivityManager: ConnectivityManager,
	playbackCacheManager: PlaybackCacheManager
) : ViewModel() {
	val songsState: StateFlow<UiState<ImmutableList<DomainSong>>>
		field = MutableStateFlow<UiState<ImmutableList<DomainSong>>>(UiState.Loading())

	val allDownloads = downloadManager.allDownloads
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.Lazily,
			initialValue = persistentListOf()
		)

	val selectedSong: StateFlow<DomainSong?>
		field = MutableStateFlow(null)

	val starred: StateFlow<Boolean>
		field = MutableStateFlow(false)

	val selectedSongRating: StateFlow<Int>
		field = MutableStateFlow(0)

	val selectedSorting: StateFlow<DomainSongListType>
		field = MutableStateFlow(initialListType)

	val selectedReversed: StateFlow<Boolean>
		field = MutableStateFlow(false)

	val listState = LazyListState()

	val isOnline = connectivityManager.isOnline

	init {
		viewModelScope.launch {
			combine(
				sessionManager.isLoggedIn,
				connectivityManager.isOnline,
				downloadManager.allDownloads,
				playbackCacheManager.fullyCachedSongIds
			) { loggedIn, online, downloads, cached ->
				SongRefreshTrigger(
					loggedIn,
					online,
					downloads.map { it.songId to it.status },
					cached
				)
			}.distinctUntilChanged().collect { trigger ->
				if (trigger.loggedIn) refreshSongs(false)
			}
		}
	}

	fun selectSong(song: DomainSong) {
		viewModelScope.launch {
			selectedSong.value = song
			starred.value = repository.isSongStarred(song)
			selectedSongRating.value = repository.getSongRating(song)
		}
	}

	fun clearSelection() {
		selectedSong.value = null
	}

	fun refreshSongs(fullRefresh: Boolean) {
		viewModelScope.launch {
			repository.getSongsFlow(
				fullRefresh,
				selectedSorting.value,
				selectedReversed.value
			).collect {
				songsState.value = it
			}
		}
	}

	fun starSong(isStarred: Boolean) {
		viewModelScope.launch {
			val selection = selectedSong.value ?: return@launch
			runCatching {
				if (isStarred) {
					repository.starSong(selection)
				} else {
					repository.unstarSong(selection)
				}
				starred.value = isStarred
				refreshSongs(false)
			}
		}
	}

	fun rateSelectedSong(rating: Int) {
		viewModelScope.launch {
			val selection = selectedSong.value ?: return@launch
			runCatching {
				repository.rateSong(selection, rating)
				selectedSongRating.value = rating
			}
		}
	}

	fun setSorting(sorting: DomainSongListType) {
		selectedSorting.value = sorting
		refreshSongs(false)
	}

	fun setReversed(reversed: Boolean) {
		selectedReversed.value = reversed
		refreshSongs(false)
	}

	fun clearError() {
		songsState.value = UiState.Success(songsState.value.data ?: persistentListOf())
	}

	fun downloadSong(song: DomainSong) {
		downloadManager.downloadSong(song)
	}

	fun cancelDownload(songId: String) {
		downloadManager.cancelDownload(songId)
	}

	fun deleteDownload(songId: String) {
		downloadManager.deleteDownload(songId)
	}
}
