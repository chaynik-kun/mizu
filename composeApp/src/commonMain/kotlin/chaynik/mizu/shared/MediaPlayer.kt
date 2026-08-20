package chaynik.mizu.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.DownloadManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.DomainExplicitStatus
import chaynik.mizu.domain.models.DomainRadio
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.domain.models.settings.ExplicitContentPlayback
import chaynik.mizu.domain.repositories.PlayerStateRepository
import chaynik.mizu.ui.core.PlayerUiState
import chaynik.mizu.util.core.Logger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration

internal enum class PreviousPlaybackAction { PreviousItem, RestartCurrent }

internal fun previousPlaybackAction(hasPreviousMediaItem: Boolean): PreviousPlaybackAction =
	if (hasPreviousMediaItem) PreviousPlaybackAction.PreviousItem else PreviousPlaybackAction.RestartCurrent

internal fun timelineSongAt(index: Int, mediaId: String?, queue: List<DomainSong>): DomainSong? =
	queue.getOrNull(index)?.takeIf { mediaId == null || it.id == mediaId }

internal fun timelineItemMatches(index: Int, mediaId: String?, queueIds: List<String>): Boolean =
	queueIds.getOrNull(index) == mediaId

abstract class MediaPlayerViewModel(
	private val stateRepository: PlayerStateRepository,
	protected val connectivityManager: ConnectivityManager,
	protected val downloadManager: DownloadManager,
	protected val preferenceManager: PreferenceManager
) : ViewModel() {

	@Suppress("PropertyName")
	protected val _uiState = MutableStateFlow(PlayerUiState())
	val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

	protected fun isExplicit(song: DomainSong): Boolean {
		return song.explicitStatus == DomainExplicitStatus.Explicit
			&& preferenceManager.explicitContentPlayback != ExplicitContentPlayback.Allowed
	}

	init {
		viewModelScope.launch {
			restoreState()
			observeAndSaveState()
		}
	}

	abstract fun addToQueueSingle(song: DomainSong, notify: Boolean = true)
	abstract fun addToQueue(collection: DomainSongCollection, notify: Boolean = true)
	abstract fun addToQueue(songs: List<DomainSong>, notify: Boolean = true)
	abstract fun removeFromQueue(index: Int)
	abstract fun moveQueueItem(fromIndex: Int, toIndex: Int)
	abstract fun clearQueue()
	abstract fun playAt(index: Int)
	abstract fun playNextSingle(song: DomainSong)
	abstract fun playNext(collection: DomainSongCollection)
	abstract fun playRadio(radio: DomainRadio)
	abstract fun pause()
	abstract fun resume()
	abstract fun seek(normalized: Float)
	abstract fun seekTo(position: Duration)
	open fun debugLyricsTap(songId: String, lineIndex: Int, text: String, timestamp: Duration?, metadataDuration: Duration) = Unit
	abstract fun next()
	abstract fun previous()
	abstract fun toggleShuffle()
	abstract fun toggleRepeat()
	abstract fun shufflePlay(collection: DomainSongCollection)
	abstract fun setPlaybackSpeed(value: Float)

	fun playNow(song: DomainSong) {
		clearQueue()
		addToQueueSingle(song, notify = false)
		playAt(0)
	}

	fun playNow(collection: DomainSongCollection, startIndex: Int = 0) {
		clearQueue()
		addToQueue(collection, notify = false)
		playAt(startIndex)
	}

	fun playNow(songs: List<DomainSong>, startIndex: Int = 0) {
		clearQueue()
		addToQueue(songs, notify = false)
		playAt(startIndex)
	}

	fun togglePlay() {
		if (!uiState.value.isPaused) {
			pause()
		} else {
			resume()
		}
	}

	abstract fun syncPlayerWithState(state: PlayerUiState)

	private suspend fun restoreState() {
		val savedJson = stateRepository.loadState()
		if (!savedJson.isNullOrBlank()) {
			try {
				val restoredState = Json.decodeFromJsonElement<PlayerUiState>(
					Json.parseToJsonElement(savedJson)
				)
				val stateToApply = restoredState.copy(isPaused = true, isLoading = false)

				_uiState.value = stateToApply

				syncPlayerWithState(stateToApply)

			} catch (e: Exception) {
				Logger.e("MediaPlayerViewModel", "Failed to restore state!", e)
				_uiState.value = PlayerUiState()
			}
		}
	}

	@OptIn(FlowPreview::class)
	private fun observeAndSaveState() {
		viewModelScope.launch {
			uiState
				.distinctUntilChanged { old, new ->
					old.currentIndex == new.currentIndex &&
						old.queue == new.queue &&
						old.isPaused == new.isPaused &&
						old.repeatMode == new.repeatMode &&
						old.isShuffleEnabled == new.isShuffleEnabled
				}
				.collect { state ->
					saveStateInternal(state)
				}
		}

		viewModelScope.launch {
			uiState
				.debounce(2.seconds)
				.collect { state ->
					saveStateInternal(state)
				}
		}
	}

	private suspend fun saveStateInternal(state: PlayerUiState) {
		try {
			val jsonString = Json.encodeToString(state)
			stateRepository.saveState(jsonString)
		} catch (e: Exception) {
			Logger.e("MediaPlayerViewModel", "Failed to save state!", e)
		}
	}
}
