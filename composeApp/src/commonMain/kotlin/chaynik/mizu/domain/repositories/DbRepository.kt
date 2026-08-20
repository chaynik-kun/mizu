package chaynik.mizu.domain.repositories

import androidx.room3.concurrent.AtomicInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_syncing
import mizu.composeapp.generated.resources.info_syncing_albums
import mizu.composeapp.generated.resources.info_syncing_artists
import mizu.composeapp.generated.resources.info_syncing_finished
import mizu.composeapp.generated.resources.info_syncing_genres
import mizu.composeapp.generated.resources.info_syncing_playlists
import mizu.composeapp.generated.resources.info_syncing_radios
import mizu.composeapp.generated.resources.info_syncing_saved
import org.jetbrains.compose.resources.StringResource
import chaynik.mizu.data.database.dao.AlbumDao
import chaynik.mizu.data.database.dao.ArtistDao
import chaynik.mizu.data.database.dao.GenreDao
import chaynik.mizu.data.database.dao.LyricDao
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.dao.RadioDao
import chaynik.mizu.data.database.dao.SongDao
import chaynik.mizu.data.database.dao.SyncActionDao
import chaynik.mizu.data.database.entities.AlbumEntity
import chaynik.mizu.data.database.entities.PlaylistEntity
import chaynik.mizu.data.database.entities.PlaylistSongCrossRef
import chaynik.mizu.data.database.entities.SongEntity
import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.data.database.mappers.toEntity
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.models.DomainArtist
import chaynik.mizu.util.core.Logger
import kotlin.coroutines.cancellation.CancellationException
import dev.zt64.subsonic.api.model.Album as ApiAlbum
import dev.zt64.subsonic.api.model.AlbumListType as ApiAlbumListType

class DbRepository(
	private val albumDao: AlbumDao,
	private val playlistDao: PlaylistDao,
	private val songDao: SongDao,
	private val genreDao: GenreDao,
	private val artistDao: ArtistDao,
	private val radioDao: RadioDao,
	private val lyricDao: LyricDao,
	private val syncDao: SyncActionDao,
	private val sessionManager: SessionManager
) {
	private val concurrentRequestLimit = Semaphore(8)

	private val dbChunkSize = 500 // should be enough

	private suspend fun <T> runDbOp(block: suspend () -> T): Result<T> =
		withContext(Dispatchers.IO) {
			try {
				Result.success(block())
			} catch (e: Exception) {
				if (e is CancellationException) throw e
				Result.failure(e)
			}
		}

	suspend fun removeEverything(): Result<Unit> = runDbOp {
		albumDao.clearAllAlbums()
		playlistDao.clearAllPlaylists()
		songDao.clearAllSongs()
		genreDao.clearAllGenres()
		artistDao.clearAllArtists()
		radioDao.clearAllRadios()
		lyricDao.clearAllLyrics()
		syncDao.clearAllActions()
		Logger.i("DbRepository", "Database wiped completely.")
	}

	suspend fun syncEverything(
		onProgress: (Float, StringResource) -> Unit = { _, _ -> }
	): Result<Unit> = runDbOp {
		val progressCallback = { progress: Float, message: StringResource ->
			Logger.i("DbRepository", "$progress $message")
			onProgress(progress, message)
		}

		progressCallback(0.0f, Res.string.info_syncing)

		progressCallback(0.01f, Res.string.info_syncing_genres)
		syncGenres().getOrThrow()

		progressCallback(0.02f, Res.string.info_syncing_radios)
		syncRadios().getOrThrow()

		progressCallback(0.04f, Res.string.info_syncing_artists)
		syncArtists().getOrThrow()

		progressCallback(0.07f, Res.string.info_syncing_playlists)
		val playlists = syncPlaylists().getOrThrow()

		val validAlbumIds = mutableSetOf<String>()
		val validSongIds = mutableSetOf<String>()

		val libraryResult = syncLibrarySongs { localProgress, message ->
			val globalProgress = 0.10f + (localProgress * 0.65f)
			progressCallback(globalProgress, message)
		}.getOrThrow()

		validAlbumIds.addAll(libraryResult.first)
		validSongIds.addAll(libraryResult.second)

		val totalPlaylists = playlists.size
		if (totalPlaylists > 0) {
			val completedPlaylists = AtomicInt(0)

			val playlistSongIds = coroutineScope {
				playlists.map { playlist ->
					async {
						concurrentRequestLimit.withPermit {
							val playlistSongIds = syncPlaylistSongs(playlist.playlistId).getOrThrow()
							val done = completedPlaylists.incrementAndGet()
							val globalProgress = 0.75f + (0.25f * (done.toFloat() / totalPlaylists))
							progressCallback(globalProgress, Res.string.info_syncing_playlists)
							playlistSongIds
						}
					}
				}.awaitAll()
			}
			validSongIds.addAll(playlistSongIds.flatten())
		}

		albumDao.deleteObsoleteAlbums(validAlbumIds)
		songDao.deleteObsoleteSongs(validSongIds)

		progressCallback(1.0f, Res.string.info_syncing_finished)
	}

	suspend fun syncLibrarySongs(
		onProgress: (Float, StringResource) -> Unit = { _, _ -> }
	): Result<Pair<Set<String>, Set<String>>> = runDbOp {
		val pageSize = 500
		var offset = 0
		val allAlbumSummaries = mutableListOf<ApiAlbum>()

		onProgress(0.0f, Res.string.info_syncing_albums)
		while (true) {
			val batch =
				sessionManager.api.getAlbums(ApiAlbumListType.AlphabeticalByName, pageSize, offset)
			if (batch.isEmpty()) break
			allAlbumSummaries.addAll(batch)
			if (batch.size < pageSize) break
			offset += pageSize
		}

		if (allAlbumSummaries.isEmpty()) return@runDbOp emptySet<String>() to emptySet()

		val totalAlbums = allAlbumSummaries.size
		val completedAlbums = AtomicInt(0)
		var finalSongsSynced = 0

		val allValidAlbumIds = mutableSetOf<String>()
		val allValidSongIds = mutableSetOf<String>()

		onProgress(0.1f, Res.string.info_syncing_albums)

		val albumChannel = Channel<ApiAlbum>(capacity = 100)

		coroutineScope {
			launch(Dispatchers.IO) {
				val summaryChannel = Channel<ApiAlbum>(capacity = 100)
				launch {
					allAlbumSummaries.forEach { summaryChannel.send(it) }
					summaryChannel.close()
				}
				val workers = List(8) {
					launch {
						for (summary in summaryChannel) {
							try {
								val album = sessionManager.api.getAlbum(summary.id)

								val done = completedAlbums.incrementAndGet()
								val fetchProgress = 0.1f + (0.8f * (done.toFloat() / totalAlbums))
								onProgress(fetchProgress, Res.string.info_syncing_albums)

								albumChannel.send(album)
							} catch (e: Exception) {
								if (e is SerializationException) {
									Logger.e(
										"DbRepository",
										"could not deserialize album ${summary.id} (${summary.name}); skipping it",
										e
									)
								} else {
									throw e
								}
							}
						}
					}
				}
				workers.joinAll()
				albumChannel.close()
			}

			launch(Dispatchers.IO) {
				val albumBatch = mutableListOf<AlbumEntity>()
				val songBatch = mutableListOf<SongEntity>()
				val summariesMap = allAlbumSummaries.associateBy { it.id }

				for (album in albumChannel) {
					val summary = summariesMap[album.id]
					val albumEntity = album.toEntity(
						artistIdOverride = summary?.artistId,
						artistNameOverride = summary?.artistName
					)
					albumBatch.add(albumEntity)
					allValidAlbumIds.add(albumEntity.albumId)

					album.songs.forEach { song ->
						val songEntity = song.toEntity(
							artistIdOverride = albumEntity.artistId,
							artistNameOverride = albumEntity.artistName
						)
						songBatch.add(songEntity)
						allValidSongIds.add(songEntity.songId)
					}

					if (albumBatch.size >= dbChunkSize || songBatch.size >= 1500) {
						albumDao.insertAlbums(albumBatch)
						songDao.insertSongs(songBatch)

						finalSongsSynced += songBatch.size
						albumBatch.clear()
						songBatch.clear()
					}
				}

				if (albumBatch.isNotEmpty() || songBatch.isNotEmpty()) {
					if (albumBatch.isNotEmpty()) albumDao.insertAlbums(albumBatch)
					if (songBatch.isNotEmpty()) songDao.insertSongs(songBatch)
					finalSongsSynced += songBatch.size
				}
			}
		}

		Logger.i(
			"DbRepository",
			"- Songs Synced: $totalAlbums albums, $finalSongsSynced songs"
		)

		onProgress(1.0f, Res.string.info_syncing_saved)
		allValidAlbumIds to allValidSongIds
	}

	suspend fun syncPlaylists(): Result<List<PlaylistEntity>> = runDbOp {
		val remotePlaylists = sessionManager.api.getPlaylists()
		val playlistEntities = remotePlaylists.map { it.toEntity() }
		val validPlaylistIds = playlistEntities.map { it.playlistId }.toSet()

		playlistEntities.chunked(dbChunkSize).forEach { chunk ->
			playlistDao.insertPlaylists(chunk)
		}

		playlistDao.deleteObsoletePlaylists(validPlaylistIds)

		Logger.i("DbRepository", "- Playlists Synced: ${playlistEntities.size} playlists found")

		playlistEntities
	}

	suspend fun syncPlaylistSongs(playlistId: String): Result<Set<String>> = runDbOp {
		val playlist = try {
			sessionManager.api.getPlaylist(playlistId)
		} catch (e: Exception) {
			if (e is SerializationException) {
				Logger.e(
					"DbRepository",
					"could not deserialize playlist $playlistId; skipping it",
					e
				)
				return@runDbOp emptySet<String>()
			} else {
				throw e
			}
		}
		val songEntities = playlist.songs.map { it.toEntity() }
		val songIds = songEntities.map { it.songId }.toSet()

		if (songEntities.isNotEmpty()) {
			songEntities.chunked(dbChunkSize).forEach { chunk ->
				songDao.insertSongs(chunk)
			}

			val crossRefs = songEntities.mapIndexed { index, it ->
				PlaylistSongCrossRef(playlistId = playlistId, songId = it.songId, position = index)
			}

			playlistDao.replacePlaylistSongs(playlistId, crossRefs)
		} else {
			playlistDao.deletePlaylistSongCrossRefs(playlistId)
		}

		Logger.i("DbRepository", "- Playlist [$playlistId] synced: ${songEntities.size} songs")
		songIds
	}

	suspend fun syncGenres(): Result<Unit> = runDbOp {
		val remoteGenres = sessionManager.api.getGenres()
		val entities = remoteGenres.map { it.toEntity() }

		entities.chunked(dbChunkSize).forEach { chunk ->
			genreDao.insertGenres(chunk)
		}
		genreDao.deleteObsoleteGenres(entities.map { it.genreName }.toSet())

		Logger.i("DbRepository", "- Genres Synced: ${entities.size} genres found")
	}

	suspend fun syncArtists(): Result<Unit> = runDbOp {
		val remoteArtistsWrapper = sessionManager.api.getArtists()
		val flatArtists = remoteArtistsWrapper.flatMap { indexGroup ->
			indexGroup.artists
		}
		val entities = flatArtists.map { it.toEntity() }

		entities.chunked(dbChunkSize).forEach { chunk ->
			artistDao.insertArtists(chunk)
		}
		artistDao.deleteObsoleteArtists(entities.map { it.artistId }.toSet())

		Logger.i("DbRepository", "- Artists Synced: ${entities.size} artists found")
	}

	suspend fun syncRadios(): Result<Unit> = runDbOp {
		val remoteRadios = sessionManager.api.getInternetRadioStations()
		val entities = remoteRadios.map { it.toEntity() }

		entities.chunked(dbChunkSize).forEach { chunk ->
			radioDao.insertRadios(chunk)
		}
		radioDao.deleteObsoleteRadios(entities.map { it.radioId }.toSet())

		Logger.i("DbRepository", "- Radios Synced: ${entities.size} stations found")
	}

	suspend fun fetchArtistMetadata(artistId: String): Result<DomainArtist> = runDbOp {
		val artistInfo = sessionManager.api.getArtistInfo(artistId)
		val simIds = artistInfo.similarArtists.map { it.id }

		val currentEntity = artistDao.getArtistById(artistId)
			?: throw Exception("Artist not found in local DB")

		val updatedEntity = currentEntity.copy(
			biography = artistInfo.biography,
			similarArtistIds = simIds,
			lastFmUrl = artistInfo.lastFmUrl
		)

		artistDao.insertArtist(updatedEntity)

		updatedEntity.toDomainModel()
	}
}
