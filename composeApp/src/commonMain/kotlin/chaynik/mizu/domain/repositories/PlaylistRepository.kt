package chaynik.mizu.domain.repositories

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import chaynik.mizu.data.database.dao.DownloadDao
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.entities.DownloadStatus
import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.domain.models.DomainPlaylist
import chaynik.mizu.domain.models.DomainPlaylistListType
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.PlaybackCacheManager

class PlaylistRepository(
	private val playlistDao: PlaylistDao,
	private val dbRepository: DbRepository,
	private val downloadDao: DownloadDao,
	private val connectivityManager: ConnectivityManager,
	private val playbackCacheManager: PlaybackCacheManager
) {
	private suspend fun getLocalData(
		listType: DomainPlaylistListType,
		reversed: Boolean
	): ImmutableList<DomainPlaylist> {
		val sorted = when (listType) {
			DomainPlaylistListType.Name -> playlistDao.getAllPlaylistsByName()
			DomainPlaylistListType.DateAdded -> playlistDao.getAllPlaylistsByDateAdded()
			DomainPlaylistListType.Duration -> playlistDao.getAllPlaylistsByDuration()
			DomainPlaylistListType.Random -> playlistDao.getAllPlaylistsRandom()
			DomainPlaylistListType.Downloaded -> {
				playlistDao.getAllPlaylistsByDateAdded().filter { (_, songs) ->
					downloadDao.getAllDownloadsList()
						.filter { it.status == DownloadStatus.DOWNLOADED }
						.map { it.songId }
						.containsAll(songs.map { it.song.songId })
				}
			}
		}.map { it.toDomainModel() }
			.let { playlists ->
				if (connectivityManager.isOnline.value) playlists else {
					val availableIds = downloadDao.getAllDownloadsList()
						.filter { it.status == DownloadStatus.DOWNLOADED }
						.mapTo(mutableSetOf()) { it.songId } + playbackCacheManager.fullyCachedSongIds.value
					playlists.mapNotNull { playlist ->
						val songs = playlist.songs.filter { it.id in availableIds }
						playlist.takeIf { songs.isNotEmpty() }?.copy(songs = songs, songCount = songs.size)
					}
				}
			}.toImmutableList()
		return if (reversed) {
			sorted.reversed().toImmutableList()
		} else {
			sorted
		}
	}

	private suspend fun refreshLocalData(
		listType: DomainPlaylistListType,
		reversed: Boolean
	): ImmutableList<DomainPlaylist> {
		dbRepository.syncPlaylists().getOrThrow().forEach { playlist ->
			dbRepository.syncPlaylistSongs(playlist.playlistId).getOrThrow()
		}
		return getLocalData(listType, reversed)
	}

	fun getPlaylistsFlow(
		fullRefresh: Boolean,
		listType: DomainPlaylistListType,
		reversed: Boolean
	): Flow<UiState<ImmutableList<DomainPlaylist>>> = flow {
		val localData = getLocalData(listType, reversed)
		if (fullRefresh) {
			emit(UiState.Loading(data = localData))
			try {
				emit(UiState.Success(data = refreshLocalData(listType, reversed)))
			} catch (error: Exception) {
				emit(UiState.Error(error = error, data = localData))
			}
		} else {
			emit(UiState.Success(data = localData))
		}
	}.flowOn(Dispatchers.IO)
}
