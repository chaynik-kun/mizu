package chaynik.mizu.domain.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import chaynik.mizu.data.database.dao.AlbumDao
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.dao.SongDao
import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.data.database.mappers.toEntity
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.PlaybackCacheManager
import chaynik.mizu.data.database.dao.DownloadDao
import chaynik.mizu.data.database.entities.DownloadStatus
import chaynik.mizu.domain.models.DomainAlbum
import chaynik.mizu.domain.models.DomainPlaylist
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.ui.core.UiState
import dev.zt64.subsonic.api.model.AlbumInfo as ApiAlbumInfo

class CollectionRepository(
	private val albumDao: AlbumDao,
	private val playlistDao: PlaylistDao,
	private val songDao: SongDao,
	private val dbRepository: DbRepository,
	private val sessionManager: SessionManager,
	private val downloadDao: DownloadDao,
	private val connectivityManager: ConnectivityManager,
	private val playbackCacheManager: PlaybackCacheManager
) {
	suspend fun getLocalData(collectionId: String): DomainSongCollection {
		val collection = albumDao.getAlbumById(collectionId)?.toDomainModel()
			?: playlistDao.getPlaylistById(collectionId)?.toDomainModel()
			?: throw Error("Collection ID $collectionId is neither a known album or playlist")
		if (connectivityManager.isOnline.value) return collection
		val availableIds = downloadDao.getAllDownloadsList()
			.filter { it.status == DownloadStatus.DOWNLOADED }
			.mapTo(mutableSetOf()) { it.songId } + playbackCacheManager.fullyCachedSongIds.value
		val songs = collection.songs.filter { it.id in availableIds }
		return when (collection) {
			is DomainAlbum -> collection.copy(songs = songs, songCount = songs.size)
			is DomainPlaylist -> collection.copy(songs = songs, songCount = songs.size)
		}
	}

	private suspend fun refreshLocalData(collectionId: String): DomainSongCollection {
		when (val collection = getLocalData(collectionId)) {
			is DomainAlbum -> {
				val album = sessionManager.api.getAlbum(collection.id)
				songDao.updateSongsByAlbumId(album.id, album.songs.map { it.toEntity() })
				albumDao.insertAlbum(album.toEntity())
				albumDao.getAlbumById(album.id)?.toDomainModel()
					?: throw IllegalStateException("Album was not persisted after refresh")
			}

			is DomainPlaylist -> {
				val playlist = sessionManager.api.getPlaylist(collection.id)
				playlistDao.insertPlaylist(playlist.toEntity())
				dbRepository.syncPlaylistSongs(collection.id)
				playlistDao.getPlaylistById(playlist.id)?.toDomainModel()
					?: throw IllegalStateException("Playlist was not persisted after refresh")
			}
		}
		return getLocalData(collectionId)
	}

	fun getCollectionFlow(
		fullRefresh: Boolean,
		collectionId: String
	): Flow<UiState<DomainSongCollection>> = flow {
		val localData = getLocalData(collectionId)
		if (fullRefresh) {
			emit(UiState.Loading(data = localData))
			try {
				emit(UiState.Success(data = refreshLocalData(collectionId)))
			} catch (error: Exception) {
				emit(UiState.Error(error = error, data = localData))
			}
		} else {
			emit(UiState.Success(data = localData))
		}
	}.flowOn(Dispatchers.IO)

	fun getOtherAlbums(artistId: String, albumId: String) = albumDao
		.getAlbumsByArtistExcluding(artistId, albumId)
		.map { it.map { album -> album.toDomainModel() } }

	suspend fun getSongById(songId: String) = songDao
		.getSongById(songId)
		?.toDomainModel()

	suspend fun getAlbumInfo(albumId: String): ApiAlbumInfo {
		return sessionManager.api.getAlbumInfo(albumId)
	}
}
