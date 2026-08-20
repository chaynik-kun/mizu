package chaynik.mizu.ui.screens.playlist.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.notice_created_playlist
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.data.database.mappers.toEntity
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.manager.SnackBarManager
import chaynik.mizu.domain.models.DomainPlaylist
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.ui.core.UiState

class PlaylistCreateDialogViewModel(
	private val songs: List<DomainSong>,
	private val playlistDao: PlaylistDao,
	private val sessionManager: SessionManager,
	private val snackBarManager: SnackBarManager
) : ViewModel() {
	val creationState: StateFlow<UiState<Nothing?>>
		field = MutableStateFlow<UiState<Nothing?>>(UiState.Success(null))

	private val _events = Channel<Event>()
	val events = _events.receiveAsFlow()

	val name = TextFieldState()

	fun create() {
		viewModelScope.launch {
			creationState.value = UiState.Loading()
			try {
				val playlist = sessionManager.api.createPlaylist(
					name = name.text.toString(),
					songIds = songs.map { it.id }
				)
				playlistDao.insertPlaylist(playlist.toEntity())
				val persistedPlaylist = playlistDao.getPlaylistById(playlist.id)?.toDomainModel()
					?: throw IllegalStateException("Playlist was not persisted after creation")
				_events.send(Event.Dismiss(persistedPlaylist))
				creationState.value = UiState.Success(null)
				snackBarManager.notify(Res.string.notice_created_playlist, playlist.name)
			} catch (e: Exception) {
				creationState.value = UiState.Error(e)
			}
		}
	}

	sealed class Event {
		data class Dismiss(val playlist: DomainPlaylist) : Event()
	}
}
