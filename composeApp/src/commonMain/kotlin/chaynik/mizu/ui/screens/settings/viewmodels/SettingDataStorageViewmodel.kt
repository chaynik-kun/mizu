package chaynik.mizu.ui.screens.settings.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import chaynik.mizu.data.database.dao.SyncActionDao
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.DownloadManager
import chaynik.mizu.domain.manager.SyncManager
import chaynik.mizu.domain.manager.PlaybackCacheManager
import chaynik.mizu.domain.repositories.DbRepository
import chaynik.mizu.domain.repositories.SongRepository

class SettingsDataStorageViewModel(
	private val syncManager: SyncManager,
	private val dbRepository: DbRepository,
	private val syncDao: SyncActionDao,
	private val downloadManager: DownloadManager,
	private val playbackCacheManager: PlaybackCacheManager,
	private val songRepository: SongRepository,
	connectivityManager: ConnectivityManager
) : ViewModel() {

	val syncState = syncManager.syncState
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5000),
			initialValue = syncManager.syncState.value
		)

	val pendingActionCount: StateFlow<Int>
		field = MutableStateFlow(0)

	val downloadCount = downloadManager.downloadCount.stateIn(
		viewModelScope, SharingStarted.WhileSubscribed(5000), 0
	)
	val downloadSize = downloadManager.downloadSize.stateIn(
		viewModelScope, SharingStarted.WhileSubscribed(5000), 0L
	)
	val playbackCacheSize: StateFlow<Long>
		field = MutableStateFlow(0L)

	val isDownloadingLibrary = downloadManager.isDownloadingLibrary
	val libraryDownloadProgress = downloadManager.libraryDownloadProgress
	val isOnline = connectivityManager.isOnline

	init {
		loadPendingActions()
		viewModelScope.launch(Dispatchers.IO) {
			while (true) {
				playbackCacheSize.value = playbackCacheManager.sizeBytes
				delay(5_000)
			}
		}
	}

	private fun loadPendingActions() {
		viewModelScope.launch(Dispatchers.IO) {
			pendingActionCount.value = syncDao.getPendingActions().size
		}
	}

	fun triggerManualSync() {
		syncManager.triggerManualSync()
	}

	fun rebuildDatabase() {
		viewModelScope.launch(Dispatchers.IO) {
			dbRepository.removeEverything()
			syncManager.stopPeriodicSync()
			pendingActionCount.value = 0
		}
		triggerManualSync()
	}

	fun removeAllActions() {
		viewModelScope.launch(Dispatchers.IO) {
			syncDao.clearAllActions()
			pendingActionCount.value = 0
		}
	}

	fun clearAllDownloads() {
		downloadManager.clearAllDownloads()
	}

	fun clearPlaybackCache() {
		viewModelScope.launch(Dispatchers.IO) {
			playbackCacheManager.clear()
			playbackCacheSize.value = playbackCacheManager.sizeBytes
		}
	}

	fun downloadEntireLibrary() {
		viewModelScope.launch(Dispatchers.IO) {
			val allSongs = songRepository.getAllSongs()
			downloadManager.downloadEntireLibrary(allSongs)
		}
	}

	fun cancelLibraryDownload() {
		downloadManager.cancelAllActiveDownloads()
	}
}
