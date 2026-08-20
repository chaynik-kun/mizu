package chaynik.mizu.ui.screens.lyrics.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.lyrics.LyricsResult
import chaynik.mizu.domain.repositories.LyricsRepository
import chaynik.mizu.ui.core.UiState

class LyricsScreenViewModel(
	private val repository: LyricsRepository
) : ViewModel() {
	val lyricsState: StateFlow<LyricsLoadState>
		field = MutableStateFlow(LyricsLoadState(songId = null, content = UiState.Loading()))

	private val requests = LatestLyricsRequest()
	private var currentSong: DomainSong? = null
	private var loadJob: Job? = null

	fun loadLyrics(song: DomainSong?, force: Boolean = false) {
		if (!force && currentSong?.id == song?.id) return
		currentSong = song
		loadJob?.cancel()
		val request = requests.begin(song?.id)
		LyricsPlaybackDiagnostics.requestedSongId = song?.id
		lyricsState.value = LyricsLoadState(song?.id, UiState.Loading())
		loadJob = viewModelScope.launch {
			if (song == null) {
				commit(request, UiState.Success(null))
				return@launch
			}
			try {
				commit(request, UiState.Success(repository.fetchLyrics(song)))
			} catch (e: CancellationException) {
				throw e
			} catch (e: Exception) {
				commit(request, UiState.Error(e))
			}
		}
	}

	fun refreshResults() = loadLyrics(currentSong, force = true)

	private fun commit(request: LyricsRequest, content: UiState<LyricsResult?>) {
		if (requests.accepts(request)) {
			lyricsState.value = LyricsLoadState(request.songId, content)
			if (content is UiState.Success) {
				LyricsPlaybackDiagnostics.loadedSongId = request.songId
				LyricsPlaybackDiagnostics.result = content.data
			}
		}
	}
}

/** Debug snapshot only; the Android logger reads it on Media3 transitions. */
internal object LyricsPlaybackDiagnostics {
	var requestedSongId: String? = null
	var loadedSongId: String? = null
	var result: LyricsResult? = null
}

data class LyricsLoadState(
	val songId: String?,
	val content: UiState<LyricsResult?>
)

internal data class LyricsRequest(val songId: String?, val generation: Long)

internal class LatestLyricsRequest {
	private var generation = 0L
	private var songId: String? = null

	fun begin(songId: String?): LyricsRequest {
		this.songId = songId
		return LyricsRequest(songId, ++generation)
	}

	fun accepts(request: LyricsRequest): Boolean =
		request.generation == generation && request.songId == songId
}
