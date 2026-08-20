package chaynik.mizu.shared

import android.app.Application
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.LibraryResult
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import chaynik.mizu.data.database.dao.AlbumDao
import chaynik.mizu.data.database.dao.ArtistDao
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.dao.SongDao
import chaynik.mizu.data.database.entities.SongEntity
import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.domain.manager.AndroidScrobbleManager
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.DownloadManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.AndroidPlaybackCacheManager
import chaynik.mizu.domain.manager.AndroidPrebufferManager
import chaynik.mizu.domain.manager.PrebufferCandidate
import chaynik.mizu.domain.manager.PrebufferContext
import chaynik.mizu.domain.manager.PrebufferManager
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.manager.SnackBarManager
import chaynik.mizu.domain.manager.SyncManager
import chaynik.mizu.domain.models.DomainAlbum
import chaynik.mizu.domain.models.DomainExplicitStatus
import chaynik.mizu.domain.models.DomainRadio
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.domain.models.settings.ReplayGainMode
import chaynik.mizu.domain.repositories.PlayerStateRepository
import chaynik.mizu.ui.core.PlayerUiState
import chaynik.mizu.ui.screens.lyrics.viewmodels.LyricsPlaybackDiagnostics
import chaynik.mizu.domain.models.settings.miniPlayerProgressFraction
import chaynik.mizu.util.core.Logger
import chaynik.mizu.util.core.ResourceProvider
import chaynik.mizu.util.core.effectiveGain
import java.io.File
import java.security.MessageDigest
import okhttp3.OkHttpClient
import chaynik.mizu.domain.manager.headersForDestination
import chaynik.mizu.domain.manager.normalizedOrigin
import chaynik.mizu.domain.manager.playbackCachePolicy
import chaynik.mizu.domain.manager.ExternalPlaybackManager
import chaynik.mizu.domain.manager.DefaultExternalPlaybackManager
import chaynik.mizu.domain.manager.LocalPlaybackBridge
import chaynik.mizu.domain.manager.LocalPlaybackSnapshot
import chaynik.mizu.domain.manager.normalized
import chaynik.mizu.domain.manager.EqualizerController
import chaynik.mizu.domain.models.PlaybackTarget
import chaynik.mizu.domain.models.settings.AndroidAutoSettings
import chaynik.mizu.domain.models.settings.AndroidAutoSection
import chaynik.mizu.domain.models.settings.enabledSections
import chaynik.mizu.domain.models.settings.validAndroidAutoLimit
import chaynik.mizu.domain.models.RemoteTrack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import coil3.PlatformContext as CoilPlatformContext

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService(), KoinComponent {
	private var mediaSession: MediaLibrarySession? = null
	private val serviceScope = MainScope()
	private var scrobbleManager: AndroidScrobbleManager? = null
	private val resourceProvider: ResourceProvider by inject()

	private val connectivityManager: ConnectivityManager by inject()

	private val syncManager: SyncManager by inject()
	private val sessionManager: SessionManager by inject()
	private val preferenceManager: PreferenceManager by inject()
	private val playbackCacheManager: AndroidPlaybackCacheManager by inject()
	private val albumDao: AlbumDao by inject()
	private val artistDao: ArtistDao by inject()
	private val playlistDao: PlaylistDao by inject()
	private val songDao: SongDao by inject()
	private val equalizerController: EqualizerController by inject()
	private val externalPlaybackManager: ExternalPlaybackManager by inject()
	private var prebufferManager: PrebufferManager? = null
	private var prebufferHealthCheckJob: Job? = null

	override fun onCreate() {
		super.onCreate()
		val cacheNamespace = serverNamespace(sessionManager.instanceUrl, sessionManager.username)
		playbackCacheManager.useNamespace(cacheNamespace)
		val loadControl = DefaultLoadControl.Builder()
			.setBufferDurationsMs(
				/* minBufferMs = */ 32_000,
				/* maxBufferMs = */ 64_000,
				/* bufferForPlaybackMs = */ 2_500,
				/* bufferForPlaybackAfterRebufferMs = */ 5_000
			)
			.setBackBuffer(10_000, true)
			.build()

		val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
			.build().apply {
				setSmallIcon(resourceProvider.icMizu)
			}

		val customHeaders = preferenceManager.customHeadersMap()
		val serverUrl = sessionManager.instanceUrl
		val playbackHttpClient = OkHttpClient.Builder().addNetworkInterceptor { chain ->
			val request = chain.request()
			val safeHeaders = headersForDestination(serverUrl, request.url.toString(), customHeaders)
			val builder = request.newBuilder()
			safeHeaders.forEach { (name, value) -> builder.header(name, value) }
			chain.proceed(builder.build())
		}.build()
		val httpDataSourceFactory = OkHttpDataSource.Factory(playbackHttpClient)
		val upstreamDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
		prebufferManager = AndroidPrebufferManager(serviceScope, playbackCacheManager.cache, upstreamDataSourceFactory)
		val cacheDataSourceFactory = CacheDataSource.Factory()
			.setCache(playbackCacheManager.cache)
			.setUpstreamDataSourceFactory(upstreamDataSourceFactory)
			.setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
		val readOnlyCacheDataSourceFactory = CacheDataSource.Factory()
			.setCache(playbackCacheManager.cache)
			.setUpstreamDataSourceFactory(upstreamDataSourceFactory)
			.setCacheWriteDataSinkFactory(null)
			.setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
		val dataSourceFactory = androidx.media3.datasource.DataSource.Factory {
			if (playbackCachePolicy(preferenceManager.automaticPlaybackCache).writeEnabled) {
				cacheDataSourceFactory.createDataSource()
			} else {
				readOnlyCacheDataSourceFactory.createDataSource()
			}
		}
		val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

		val player = ExoPlayer.Builder(this)
			.setLoadControl(loadControl)
			.setMediaSourceFactory(mediaSourceFactory)
			.setHandleAudioBecomingNoisy(true)
			.setWakeMode(C.WAKE_MODE_NETWORK)
			.build()
			.apply {
				setAudioAttributes(
					AudioAttributes.Builder()
						.setUsage(C.USAGE_MEDIA)
						.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
						.build(),
					true
				)
				setMediaNotificationProvider(notificationProvider)
				trackSelectionParameters =
					trackSelectionParameters.buildUpon().setAudioOffloadPreferences(
						TrackSelectionParameters.AudioOffloadPreferences
							.Builder()
							.setIsGaplessSupportRequired(preferenceManager.gaplessPlayback)
							.setAudioOffloadMode(
								if (preferenceManager.audioOffload) {
									TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
								} else {
									TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
								}
							)
							.build()
					).build()
			}

		scrobbleManager =
			AndroidScrobbleManager(
				player,
				serviceScope,
				connectivityManager,
				syncManager,
				sessionManager,
				preferenceManager
			)

		val sessionIntent = applicationContext.packageManager
			.getLaunchIntentForPackage(applicationContext.packageName)
			?.apply {
				flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or
					Intent.FLAG_ACTIVITY_CLEAR_TOP
			}

		val sessionPendingIntent = PendingIntent.getActivity(
			this,
			0,
			sessionIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		mediaSession = MediaLibrarySession.Builder(this, player, MediaSessionCallback(player))
			.setSessionActivity(sessionPendingIntent)
			.setCustomLayout(makeButtons(player))
			.build()

		player.addListener(object : Player.Listener {
			private var activeCacheKey: String? = player.currentMediaItem?.localConfiguration?.customCacheKey

			override fun onAudioSessionIdChanged(audioSessionId: Int) {
				if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) equalizerController.detach()
				else equalizerController.attachToAudioSession(audioSessionId)
			}

			override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
				if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) activeCacheKey?.let(playbackCacheManager::markCompleted)
				activeCacheKey = mediaItem?.localConfiguration?.customCacheKey
				updatePrebuffer(player)
			}

			override fun onTimelineChanged(timeline: Timeline, reason: Int) = updatePrebuffer(player)
			override fun onIsPlayingChanged(isPlaying: Boolean) = updatePrebuffer(player)

			override fun onPlaybackStateChanged(playbackState: Int) {
				if (playbackState == Player.STATE_ENDED) activeCacheKey?.let(playbackCacheManager::markCompleted)
				updatePrebuffer(player)
			}

			override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
				mediaSession?.setCustomLayout(makeButtons(player))
			}

			override fun onRepeatModeChanged(repeatMode: Int) {
				mediaSession?.setCustomLayout(makeButtons(player))
			}
		})
		serviceScope.launch {
			combine(
				connectivityManager.isOnline,
				preferenceManager.preloadNextTrackFlow,
				externalPlaybackManager.state
			) { _, _, _ -> Unit }.collect { updatePrebuffer(player) }
		}
	}

	private fun updatePrebuffer(player: Player, scheduleHealthCheck: Boolean = true) {
		val nextIndex = player.nextMediaItemIndex
		val next = nextIndex.takeIf { it != C.INDEX_UNSET && it != player.currentMediaItemIndex }
			?.let(player::getMediaItemAt)
		val local = next?.localConfiguration
		val namespace = serverNamespace(sessionManager.instanceUrl, sessionManager.username)
		val candidate = if (next != null && local != null && local.uri.scheme in setOf("http", "https") && local.customCacheKey != null) {
			PrebufferCandidate(next.mediaId, local.uri.toString(), checkNotNull(local.customCacheKey), namespace)
		} else null
		val bufferedAheadMs = player.bufferedPosition - player.currentPosition
		if (scheduleHealthCheck && player.isPlaying && bufferedAheadMs < PREBUFFER_MIN_CURRENT_BUFFER_MS && prebufferHealthCheckJob?.isActive != true) {
			prebufferHealthCheckJob = serviceScope.launch {
				delay(PREBUFFER_RECHECK_MS)
				prebufferHealthCheckJob = null
				updatePrebuffer(player, scheduleHealthCheck = false)
			}
		} else if (!player.isPlaying) {
			prebufferHealthCheckJob?.cancel()
			prebufferHealthCheckJob = null
		}
		prebufferManager?.updatePlaybackContext(
			PrebufferContext(
				candidate = candidate,
				enabled = preferenceManager.preloadNextTrack,
				online = connectivityManager.isOnline.value,
				localPlayback = externalPlaybackManager.state.value.activeTarget is PlaybackTarget.Local,
				currentPlaybackHealthy = player.isPlaying && bufferedAheadMs >= PREBUFFER_MIN_CURRENT_BUFFER_MS
			)
		)
	}

	override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
		return mediaSession
	}

	override fun onTaskRemoved(rootIntent: Intent?) {
		// Stop playback and let the system tear the service down. Calling
		// onDestroy() directly here double-dispatches the LifecycleService
		// state and crashes with "State is 'DESTROYED' and cannot be moved
		// to CREATED" when the framework then calls onDestroy() again.
		mediaSession?.player?.run {
			pause()
			stop()
		}
		stopSelf()
	}

	override fun onDestroy() {
		prebufferManager?.cancel()
		equalizerController.detach()
		scrobbleManager?.release()
		serviceScope.cancel()
		stopForeground(STOP_FOREGROUND_REMOVE)
		mediaSession?.run {
			player.stop()
			player.release()
			release()
		}
		super.onDestroy()
		mediaSession = null
	}

	inner class MediaSessionCallback(private val player: ExoPlayer) : MediaLibrarySession.Callback {
		private fun androidAutoSettings() = AndroidAutoSettings(
			showAlbums = preferenceManager.androidAutoShowAlbums,
			showArtists = preferenceManager.androidAutoShowArtists,
			showPlaylists = preferenceManager.androidAutoShowPlaylists,
			showSongs = preferenceManager.androidAutoShowSongs,
			showRecentlyAdded = preferenceManager.androidAutoShowRecentlyAdded,
			showRandomTracks = preferenceManager.androidAutoShowRandomTracks,
			itemsPerSection = preferenceManager.androidAutoItemsPerSection.validAndroidAutoLimit()
		)
		override fun onConnect(
			session: MediaSession,
			controller: MediaSession.ControllerInfo
		): MediaSession.ConnectionResult {
			val allowed = controller.isTrusted ||
				controller.packageName == packageName ||
				controller.packageName == MediaSession.ControllerInfo.LEGACY_CONTROLLER_PACKAGE_NAME
			if (!allowed) return MediaSession.ConnectionResult.reject()
			val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
				.buildUpon()
				.add(SessionCommand(COMMAND_SHUFFLE, Bundle.EMPTY))
				.add(SessionCommand(COMMAND_REPEAT, Bundle.EMPTY))
				.build()

			return MediaSession.ConnectionResult.accept(
				sessionCommands,
				MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
			)
		}

		override fun onCustomCommand(
			session: MediaSession,
			controller: MediaSession.ControllerInfo,
			customCommand: SessionCommand,
			args: Bundle
		): ListenableFuture<SessionResult> {
			when (customCommand.customAction) {
				COMMAND_SHUFFLE -> {
					player.shuffleModeEnabled = !player.shuffleModeEnabled
				}

				COMMAND_REPEAT -> {
					player.repeatMode = when (player.repeatMode) {
						Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
						Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
						else -> Player.REPEAT_MODE_OFF
					}
				}
			}

			return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
		}

		override fun onGetLibraryRoot(
			session: MediaLibrarySession,
			browser: MediaSession.ControllerInfo,
			params: LibraryParams?
		): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
			LibraryResult.ofItem(browsableItem(ROOT_ID, "Mizu"), params)
		)

		override fun onGetChildren(
			session: MediaLibrarySession,
			browser: MediaSession.ControllerInfo,
			parentId: String,
			page: Int,
			pageSize: Int,
			params: LibraryParams?
		): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
			val future = SettableFuture.create<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>>()
			serviceScope.launch {
				try {
				val safePage = page.coerceAtLeast(0)
				val settings = androidAutoSettings()
				val safePageSize = pageSize.coerceIn(1, settings.itemsPerSection)
				val offset = safePage * safePageSize
				val items = when {
					parentId == ROOT_ID -> settings.enabledSections().map { section -> when (section) {
						AndroidAutoSection.ALBUMS -> browsableItem(ALBUMS_ID, resourceProvider.autoAlbums)
						AndroidAutoSection.ARTISTS -> browsableItem(ARTISTS_ID, "Artists")
						AndroidAutoSection.PLAYLISTS -> browsableItem(PLAYLISTS_ID, resourceProvider.autoPlaylists)
						AndroidAutoSection.SONGS -> browsableItem(SONGS_ID, resourceProvider.autoSongs)
						AndroidAutoSection.RECENTLY_ADDED -> browsableItem(RECENT_ID, "Recently added")
						AndroidAutoSection.RANDOM_TRACKS -> browsableItem(RANDOM_ID, "Random tracks")
					} }
					parentId == ALBUMS_ID -> albumDao.getAlbumsPage(safePageSize, offset).map {
						browsableItem("album:${it.album.albumId}", it.album.name, it.album.artistName)
					}
					parentId == PLAYLISTS_ID -> playlistDao.getPlaylistsPage(safePageSize, offset).map {
						browsableItem("playlist:${it.playlist.playlistId}", it.playlist.name)
					}
					parentId == ARTISTS_ID -> artistDao.getArtistsPage(safePageSize, offset).map {
						browsableItem("artist:${it.artistId}", it.name)
					}
					parentId == SONGS_ID -> songDao.getSongsPage(safePageSize, offset).map(::playableItem)
					parentId == RECENT_ID -> songDao.getRecentlyAddedPage(safePageSize, offset).map(::playableItem)
					parentId == RANDOM_ID -> if (safePage == 0) songDao.getRandomSongs(safePageSize).map(::playableItem) else emptyList()
					parentId.startsWith("artist:") -> songDao.getSongsByArtistPage(parentId.removePrefix("artist:"), safePageSize, offset).map(::playableItem)
					parentId.startsWith("album:") -> albumDao.getAlbumById(parentId.removePrefix("album:"))
						?.songs.orEmpty().map(::playableItem)
					parentId.startsWith("playlist:") -> playlistDao.getPlaylistById(parentId.removePrefix("playlist:"))
						?.songs.orEmpty().map { playableItem(it.song) }
					else -> {
						future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, params))
						return@launch
					}
				}
				val pagedItems = if (parentId == ROOT_ID || parentId.startsWith("album:") || parentId.startsWith("playlist:")) {
					val from = offset.coerceAtMost(items.size)
					val to = (from + safePageSize).coerceAtMost(items.size)
					items.subList(from, to)
				} else items
				future.set(LibraryResult.ofItemList(pagedItems, params))
				} catch (error: Throwable) {
					Logger.e("PlaybackService", "Failed to browse $parentId", error)
					future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params))
				}
			}
			return future
		}

		override fun onSearch(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, params: LibraryParams?): ListenableFuture<LibraryResult<Void>> {
			if (query.isBlank()) return Futures.immediateFuture(LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, params))
			serviceScope.launch {
				val count = runCatching { songDao.searchSongs(query.trim(), androidAutoSettings().itemsPerSection).size }.getOrDefault(0)
				session.notifySearchResultChanged(browser, query, count, params)
			}
			return Futures.immediateFuture(LibraryResult.ofVoid(params))
		}

		override fun onGetSearchResult(session: MediaLibrarySession, browser: MediaSession.ControllerInfo, query: String, page: Int, pageSize: Int, params: LibraryParams?): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
			val future = SettableFuture.create<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>>()
			serviceScope.launch {
				val limit = minOf(pageSize.coerceAtLeast(1), androidAutoSettings().itemsPerSection)
				val all = runCatching { songDao.searchSongs(query.trim(), androidAutoSettings().itemsPerSection).map(::playableItem) }.getOrElse {
					future.set(LibraryResult.ofError(LibraryResult.RESULT_ERROR_IO, params)); return@launch
				}
				val from = (page.coerceAtLeast(0) * limit).coerceAtMost(all.size)
				future.set(LibraryResult.ofItemList(all.subList(from, (from + limit).coerceAtMost(all.size)), params))
			}
			return future
		}

		override fun onSetMediaItems(session: MediaSession, controller: MediaSession.ControllerInfo, mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
			val query = mediaItems.firstOrNull()?.requestMetadata?.searchQuery
			if (query.isNullOrBlank()) return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
			val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
			serviceScope.launch {
				val result = songDao.searchSongs(query, 1).firstOrNull()?.let(::playableItem)
				future.set(MediaSession.MediaItemsWithStartPosition(result?.let(::listOf).orEmpty(), 0, 0))
			}
			return future
		}

		private fun browsableItem(id: String, title: String, subtitle: String? = null) =
			MediaItem.Builder().setMediaId(id).setMediaMetadata(
				MediaMetadata.Builder().setTitle(title).setArtist(subtitle)
					.setIsBrowsable(true).setIsPlayable(false).build()
			).build()

		private fun playableItem(song: SongEntity): MediaItem {
			val uri = sessionManager.api.getStreamUrl(song.songId).toUri()
			return MediaItem.Builder()
			.setMediaId(song.songId)
			.setUri(uri)
			.setCustomCacheKey(playbackCacheKey(serverNamespace(sessionManager.instanceUrl, sessionManager.username), song.songId, uri))
			.setMediaMetadata(
				MediaMetadata.Builder().setTitle(song.title).setArtist(song.artistName)
					.setAlbumTitle(song.albumTitle)
					.setArtworkUri(song.coverArtId?.let { sessionManager.getCoverArtUrl(it).toUri() })
					.setIsBrowsable(false).setIsPlayable(true).build()
			).build()
		}
	}

	companion object {
		const val COMMAND_SHUFFLE = "COMMAND_SHUFFLE"
		const val COMMAND_REPEAT = "COMMAND_REPEAT"
		const val ROOT_ID = "mizu:root"
		const val ALBUMS_ID = "mizu:albums"
		const val PLAYLISTS_ID = "mizu:playlists"
		const val SONGS_ID = "mizu:songs"
		const val ARTISTS_ID = "mizu:artists"
		const val RECENT_ID = "mizu:recent"
		const val RANDOM_ID = "mizu:random"
		const val PREBUFFER_MIN_CURRENT_BUFFER_MS = 15_000L
		const val PREBUFFER_RECHECK_MS = 3_000L

		fun makeShuffleButton(enabled: Boolean): CommandButton {
			val icon = if (enabled) {
				CommandButton.ICON_SHUFFLE_ON
			} else {
				CommandButton.ICON_SHUFFLE_OFF
			}
			return CommandButton.Builder(icon)
				.setDisplayName("Shuffle")
				.setSessionCommand(SessionCommand(COMMAND_SHUFFLE, Bundle.EMPTY))
				.build()
		}

		fun makeRepeatButton(mode: Int): CommandButton {
			val icon = when (mode) {
				Player.REPEAT_MODE_OFF -> CommandButton.ICON_REPEAT_OFF
				Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
				else -> CommandButton.ICON_REPEAT_ONE
			}
			return CommandButton.Builder(icon)
				.setDisplayName("Repeat")
				.setSessionCommand(SessionCommand(COMMAND_REPEAT, Bundle.EMPTY))
				.build()
		}

		fun makeButtons(player: Player) = listOf(
			makeShuffleButton(player.shuffleModeEnabled),
			makeRepeatButton(player.repeatMode)
		)

		fun newSessionToken(context: Context): SessionToken {
			return SessionToken(context, ComponentName(context, PlaybackService::class.java))
		}
	}
}

class AndroidMediaPlayerViewModel(
	stateRepository: PlayerStateRepository,
	downloadManager: DownloadManager,
	connectivityManager: ConnectivityManager,
	preferenceManager: PreferenceManager,
	private val application: Application,
	private val albumDao: AlbumDao,
	private val platformContext: CoilPlatformContext,
	private val sessionManager: SessionManager,
	private val snackBarManager: SnackBarManager,
	val externalPlaybackManager: ExternalPlaybackManager
) : MediaPlayerViewModel(
	stateRepository = stateRepository,
	connectivityManager = connectivityManager,
	downloadManager = downloadManager,
	preferenceManager = preferenceManager
) {
	private var controller: MediaController? = null
	private var controllerFuture: ListenableFuture<MediaController>? = null

	private var loadingCollectionId: String? = null

	private var pendingSyncState: PlayerUiState? = null

	init {
		(externalPlaybackManager as? DefaultExternalPlaybackManager)?.attachLocalBridge(object : LocalPlaybackBridge {
			override fun snapshot(): LocalPlaybackSnapshot? {
				val state = uiState.value
				val item = state.currentSong ?: return null
				val tracks = state.queue.map(::remoteTrack)
				return LocalPlaybackSnapshot(remoteTrack(item), tracks, state.currentIndex, controller?.currentPosition ?: 0, !state.isPaused).normalized()
			}
			override suspend fun pauseLocal() = withContext(Dispatchers.Main) { controller?.pause() ?: Unit }
			override suspend fun restoreLocal(snapshot: LocalPlaybackSnapshot, positionMs: Long) = withContext(Dispatchers.Main) {
				controller?.seekTo(snapshot.index, positionMs)
				if (snapshot.wasPlaying) controller?.play() else controller?.pause()
				Unit
			}
		})
		viewModelScope.launch {
			externalPlaybackManager.state.collect { remote ->
				if (remote.activeTarget !is PlaybackTarget.Local) {
					val duration = uiState.value.currentSong?.duration?.inWholeMilliseconds ?: 0L
					val remoteIndex = remote.queueIndex?.takeIf { it in uiState.value.queue.indices }
					val itemIndex = remote.currentItemId?.let { id -> uiState.value.queue.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
					val index = itemIndex ?: remoteIndex
					_uiState.update { it.copy(
						isPaused = !remote.isPlaying,
						isLoading = remote.connectionState == chaynik.mizu.domain.models.TargetConnectionState.CONNECTING,
						progress = miniPlayerProgressFraction(remote.remotePositionMs, duration),
						currentIndex = index ?: it.currentIndex,
						currentSong = index?.let(it.queue::getOrNull) ?: it.currentSong
					) }
				}
			}
		}
		connectToService()
	}

	private fun remoteTrack(song: DomainSong) = RemoteTrack(
		song.id, song.title, song.artistName, song.albumTitle,
		song.coverArtId?.let(sessionManager::getCoverArtUrl), song.duration.inWholeMilliseconds, song.mimeType
	)

	private fun connectToService() {
		viewModelScope.launch {
			val sessionToken = PlaybackService.newSessionToken(application)
			controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
			controllerFuture?.addListener({
				controller = controllerFuture?.get()
				setupController()
			}, MoreExecutors.directExecutor())
		}
	}

	private fun getStreamUrl(id: String): Uri {
		val isCellular = connectivityManager.isCellular.value
		val bitrate = if (preferenceManager.isAdvancedTranscodingActive) {
			if (isCellular) preferenceManager.customMaxBitrateCellular else preferenceManager.customMaxBitrateWifi
		} else {
			if (isCellular) preferenceManager.streamingQualityCellular.bitrateAndroid else preferenceManager.streamingQualityWifi.bitrateAndroid
		}
		val container = if (preferenceManager.isAdvancedTranscodingActive) {
			if (isCellular) preferenceManager.customFormatCellular else preferenceManager.customFormatWifi
		} else {
			if (isCellular) preferenceManager.streamingQualityCellular.containerAndroid else preferenceManager.streamingQualityWifi.containerAndroid
		}
		return sessionManager.api.getStreamUrl(id, bitrate, container?.takeIf { it.isNotBlank() })
			.toUri()
			.buildUpon()
			.appendQueryParameter("estimateContentLength", "true")
			.build()
	}

	private fun setupController() {
		viewModelScope.launch {
			controller?.apply {
				addListener(object : Player.Listener {
					override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
						updatePlaybackState()
						logPlaybackTimelineSnapshot("transition(reason=$reason)")
						skipUnavailableSong()
					}

					override fun onIsPlayingChanged(isPlaying: Boolean) {
						if (isPlaying) startProgressLoop()
						val intent =
							Intent("${application.packageName}.NOW_PLAYING_UPDATED").apply {
								setPackage(application.packageName)
								putExtra("isPlaying", isPlaying)
								putExtra(
									"title",
									_uiState.value.currentSong?.title ?: "Unknown song"
								)
								putExtra(
									"artist",
									_uiState.value.currentSong?.artistName ?: "Unknown artist"
								)
								putExtra(
									"artUrl",
									_uiState.value.currentSong?.coverArtId?.let {
										sessionManager.getCoverArtUrl(it)
									})
							}

						application.sendBroadcast(intent)
						updatePlaybackState()
					}

					override fun onPlaybackStateChanged(playbackState: Int) {
						_uiState.update { it.copy(isLoading = playbackState == Player.STATE_BUFFERING) }
						updatePlaybackState()
					}

					override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
						_uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
					}

					override fun onRepeatModeChanged(repeatMode: Int) {
						_uiState.update { it.copy(repeatMode = repeatMode) }
					}

					override fun onTracksChanged(tracks: Tracks) {
						updatePlaybackProperties(tracks)
					}

					override fun onTimelineChanged(timeline: Timeline, reason: Int) {
						updatePlaybackState()
					}
				})
				updatePlaybackState()
				updatePlaybackProperties(currentTracks)

				downloadManager.allDownloads.first()
				pendingSyncState?.let { state ->
					syncPlayerWithState(state)
					pendingSyncState = null
				}

				combine(
					downloadManager.downloadedSongs,
					connectivityManager.isCellular,
					snapshotFlow { preferenceManager.streamingQualityWifi },
					snapshotFlow { preferenceManager.streamingQualityCellular },
					snapshotFlow { preferenceManager.isAdvancedTranscodingActive },
					snapshotFlow { preferenceManager.customMaxBitrateWifi },
					snapshotFlow { preferenceManager.customMaxBitrateCellular },
					snapshotFlow { preferenceManager.customFormatWifi },
					snapshotFlow { preferenceManager.customFormatCellular }
				) { it }.collectLatest { args ->
					@Suppress("UNCHECKED_CAST")
					val downloadedMap = args[0] as Map<String, String>
					val player = controller ?: return@collectLatest
					val currentIndex = player.currentMediaItemIndex

					for (i in 0 until player.mediaItemCount) {
						if (i == currentIndex) continue

						val item = player.getMediaItemAt(i)
						val id = item.mediaId
						val localPath = downloadedMap[id]

						val isCurrentlyLocal = item.localConfiguration?.uri?.scheme == "file"

						val newItem = if (localPath != null) {
							if (!isCurrentlyLocal) {
								item.buildUpon()
									.setUri(File(localPath).toUri())
									.build()
							} else null
						} else {
							val newUri = getStreamUrl(id)
							if (isCurrentlyLocal || item.localConfiguration?.uri != newUri) {
								item.buildUpon()
									.setUri(newUri)
									.build()
							} else null
						}

						if (newItem != null) {
							player.replaceMediaItem(i, newItem)
						}
					}
				}
			}
		}
	}

	/**
	 * strategically skip around in the queue until the
	 * current song is available while avoiding infinite
	 * loops
	 *
	 * this **INTENTIONALLY** does not check for if the song
	 * is not downloaded and if the device is offline
	 *
	 * this used to check for that but because there have
	 * been cases where the device is falsely identified
	 * as being offline that's no longer the case, so we
	 * just try to play the song anyway
	 */
	private fun skipUnavailableSong() {
		val currentSong = _uiState.value.currentSong ?: return
		if (!isExplicit(currentSong)) return
		Logger.i("MediaPlayer", "trying to skip unavailable song")
		val queue = _uiState.value.queue
		val currentIdx = queue.indexOf(currentSong)

		// look for the next available song, wrapping around, but stop before
		// we loop back past our own starting point
		val nextAvailableIdx = (1..queue.size)
			.map { offset -> (currentIdx + offset) % queue.size }
			.firstOrNull { index -> !isExplicit(queue[index]) }

		if (nextAvailableIdx == null) {
			Logger.i("MediaPlayer", "pausing because this song is unavailable and there isn't anything to skip to")
			controller?.pause()
			return
		}

		if (nextAvailableIdx <= currentIdx) {
			Logger.i("MediaPlayer", "skipping and pausing because the last song in the queue was unavailable")
			controller?.seekTo(nextAvailableIdx, 0L)
			controller?.pause()
		} else {
			// just skip to the next song
			controller?.seekTo(nextAvailableIdx, 0L)
		}
	}

	private fun refreshCurrentCollection(albumId: String) {
		if (loadingCollectionId == albumId) return
		loadingCollectionId = albumId

		viewModelScope.launch {
			runCatching {
				val album = albumDao.getAlbumById(albumId)

				_uiState.update { it.copy(currentCollection = album?.toDomainModel()) }
			}.onFailure {
				loadingCollectionId = null
			}
		}
	}

	private fun updatePlaybackState() {
		val controller = controller ?: return
		val index = controller.currentMediaItemIndex
		if (index == C.INDEX_UNSET) return

		val currentSong = timelineSongAt(index, controller.currentMediaItem?.mediaId, _uiState.value.queue)

		val derivedCollection = currentSong?.let { song ->
			val stateCollection = _uiState.value.currentCollection

			if (stateCollection?.id == song.albumId.toString()) {
				stateCollection
			} else {
				refreshCurrentCollection(song.albumId.toString())
				null
			}
		}

		_uiState.update { state ->
			state.copy(
				currentIndex = index,
				currentSong = currentSong,
				currentCollection = derivedCollection ?: state.currentCollection,
				isPaused = !controller.playWhenReady,
				isShuffleEnabled = controller.shuffleModeEnabled,
				repeatMode = controller.repeatMode
			)
		}
		applyReplayGain()
		updateProgress()
	}

	private fun logPlaybackTimelineSnapshot(event: String) {
		if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
		val player = controller ?: return
		val state = _uiState.value
		val mediaIndex = player.currentMediaItemIndex
		val previousIndex = player.previousMediaItemIndex
		val nextIndex = player.nextMediaItemIndex
		val lyrics = LyricsPlaybackDiagnostics.result
		Logger.i(
			"PlaybackTimeline",
			"$event Media3(index=$mediaIndex, mediaId=${player.currentMediaItem?.mediaId}, " +
				"count=${player.mediaItemCount}, hasPrevious=${player.hasPreviousMediaItem()}, " +
				"hasNext=${player.hasNextMediaItem()}, previousIndex=$previousIndex, nextIndex=$nextIndex, " +
				"shuffle=${player.shuffleModeEnabled}, repeat=${player.repeatMode}, playbackState=${player.playbackState}) " +
				"Mizu(currentSongId=${state.currentSong?.id}, currentIndex=${state.currentIndex}, " +
				"queueSize=${state.queue.size}, previousExpectedId=${state.queue.getOrNull(previousIndex)?.id}, " +
				"nextExpectedId=${state.queue.getOrNull(nextIndex)?.id}) " +
				"Lyrics(requestedSongId=${LyricsPlaybackDiagnostics.requestedSongId}, " +
				"loadedSongId=${LyricsPlaybackDiagnostics.loadedSongId}, source=${lyrics?.providerName}, " +
				"type=${lyrics?.type}, lines=${lyrics?.lines?.size ?: 0}, " +
				"timedLines=${lyrics?.lines?.count { it.time != null } ?: 0})"
		)
	}

	private fun applyReplayGain() {
		if (preferenceManager.replayGainMode != ReplayGainMode.Off) {
			(_uiState.value.currentSong)?.replayGain?.let { replayGain ->
				controller?.volume = replayGain.effectiveGain(preferenceManager.replayGainMode)
			}
		} else {
			controller?.volume = 1f
		}
	}

	override fun syncPlayerWithState(state: PlayerUiState) {
		viewModelScope.launch {
			val player = controller

			if (player == null) {
				pendingSyncState = state
				return@launch
			}

			if (state.queue.isEmpty() || player.mediaItemCount > 0) {
				updatePlaybackState()
				return@launch
			}

			val mediaItems = withContext(Dispatchers.Default) {
				state.queue.map { it.toMediaItem() }
			}

			player.setMediaItems(mediaItems)

			player.shuffleModeEnabled = state.isShuffleEnabled
			player.repeatMode = state.repeatMode
			player.playbackParameters = PlaybackParameters(state.playbackSpeed)

			val index = if (state.currentIndex in mediaItems.indices) state.currentIndex else 0

			val songDurationMs = state.queue.getOrNull(index)?.duration?.inWholeMilliseconds ?: 0L

			val position = if (songDurationMs > 0) {
				(state.progress * songDurationMs).toLong()
			} else {
				0L
			}

			player.seekTo(index, position)
			player.prepare()
			if (!state.isPaused) {
				player.play()
			}
		}
	}

	private fun startProgressLoop() {
		viewModelScope.launch {
			while (controller?.isPlaying == true) {
				val player = controller ?: break
				val positionMs = player.currentPosition.coerceAtLeast(0L)
				val duration = _uiState.value.currentSong?.duration?.inWholeMilliseconds ?: player.duration
				_uiState.update {
					it.copy(
						progress = miniPlayerProgressFraction(positionMs, duration),
						currentPositionMs = positionMs
					)
				}
				delay(200.milliseconds)
			}
		}
	}

	private fun updateProgress() {
		controller?.let { player ->
			val positionMs = player.currentPosition.coerceAtLeast(0L)
			val duration = _uiState.value.currentSong?.duration?.inWholeMilliseconds ?: player.duration
			_uiState.update {
				it.copy(
					progress = miniPlayerProgressFraction(positionMs, duration),
					currentPositionMs = positionMs
				)
			}
		}
	}

	@OptIn(UnstableApi::class)
	private fun updatePlaybackProperties(tracks: Tracks) {
		val audioGroup =
			tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
		if (audioGroup != null) {
			for (i in 0 until audioGroup.length) {
				if (audioGroup.isTrackSelected(i)) {
					val format = audioGroup.getTrackFormat(i)
					Logger.i("MediaPlayer", "Active Track Format: $format")
					_uiState.update { state ->
						state.copy(
							playbackBitrate = format.bitrate.takeIf { it > 0 },
							playbackSampleRate = format.sampleRate.takeIf { it > 0 },
							playbackMimeType = format.sampleMimeType
						)
					}
					break
				}
			}
		}
	}

	override fun addToQueueSingle(song: DomainSong, notify: Boolean) {
		viewModelScope.launch {
			controller?.addMediaItem(song.toMediaItem())
			_uiState.update { state ->
				val newQueue = state.queue + song
				state.copy(
					queue = newQueue,
					currentIndex = if (state.currentIndex == -1) 0 else state.currentIndex,
					currentSong = if (state.currentIndex == -1) song else state.currentSong
				)
			}
			if (notify) snackBarManager.notifyAddedToQueue()
		}
	}

	override fun addToQueue(collection: DomainSongCollection, notify: Boolean) {
		addToQueue(
			if (collection is DomainAlbum) collection.songs.sortedWith(
				compareBy(
					{ it.discNumber },
					{ it.trackNumber }
				)
			) else collection.songs,
			notify
		)
	}

	override fun addToQueue(songs: List<DomainSong>, notify: Boolean) {
		viewModelScope.launch {
			val items = songs.map { it.toMediaItem() }
			controller?.addMediaItems(items)
			_uiState.update { state ->
				val newQueue = state.queue + songs
				state.copy(
					queue = newQueue,
					currentIndex = if (state.currentIndex == -1) 0 else state.currentIndex,
					currentSong = if (state.currentIndex == -1) songs.firstOrNull() else state.currentSong
				)
			}
			if (notify) snackBarManager.notifyAddedToQueue()
		}
	}

	override fun removeFromQueue(index: Int) {
		viewModelScope.launch {
			controller?.removeMediaItem(index)
			_uiState.update { state ->
				val newQueue = state.queue.toMutableList().apply { removeAt(index) }
				val newIndex = when {
					index < state.currentIndex -> state.currentIndex - 1
					index == state.currentIndex -> if (newQueue.isEmpty()) -1 else state.currentIndex.coerceAtMost(
						newQueue.size - 1
					)

					else -> state.currentIndex
				}
				state.copy(
					queue = newQueue,
					currentIndex = newIndex,
					currentSong = if (newIndex == -1) null else newQueue[newIndex]
				)
			}
		}
	}

	override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
		viewModelScope.launch {
			controller?.moveMediaItem(fromIndex, toIndex)
			_uiState.update { state ->
				val newQueue = state.queue.toMutableList().apply {
					val item = removeAt(fromIndex)
					add(toIndex, item)
				}
				val newIndex = when (state.currentIndex) {
					fromIndex -> toIndex
					in (fromIndex + 1)..toIndex -> state.currentIndex - 1
					in toIndex until fromIndex -> state.currentIndex + 1
					else -> state.currentIndex
				}
				state.copy(
					queue = newQueue,
					currentIndex = newIndex,
					currentSong = if (newIndex == -1) null else newQueue[newIndex]
				)
			}
		}
	}

	override fun clearQueue() {
		viewModelScope.launch {
			_uiState.update {
				it.copy(
					queue = emptyList(),
					currentSong = null,
					currentIndex = -1,
					progress = 0f,
					currentPositionMs = 0L
				)
			}
			controller?.clearMediaItems()
		}
	}

	override fun playAt(index: Int) {
		viewModelScope.launch {
			controller?.let { player ->
				if (index in 0 until player.mediaItemCount) {
					player.seekTo(index, 0L)
					player.play()
				}
			}
		}
	}

	override fun playNextSingle(song: DomainSong) {
		viewModelScope.launch {
			controller?.addMediaItem(
				_uiState.value.currentIndex + 1,
				withContext(Dispatchers.Default) { song.toMediaItem() }
			)
			_uiState.update { state ->
				val newQueue =
					if (state.queue.isEmpty())
						state.queue + song
					else
						state.queue.slice(0..state.currentIndex) + song + state.queue.slice(state.currentIndex + 1..<state.queue.size)
				state.copy(
					queue = newQueue,
					currentIndex = if (state.currentIndex == -1) 0 else state.currentIndex,
					currentSong = if (state.currentIndex == -1) song else state.currentSong
				)
			}
			snackBarManager.notifyPlayNext()
		}
	}

	override fun playNext(collection: DomainSongCollection) {
		viewModelScope.launch {
			val (items, newCollection) = withContext(Dispatchers.Default) {
				val newCollection =
					if (collection is DomainAlbum) collection.songs.sortedWith(
						compareBy(
							{ it.discNumber },
							{ it.trackNumber }
						)) else collection.songs
				newCollection.map { it.toMediaItem() } to newCollection
			}
			controller?.addMediaItems(_uiState.value.currentIndex + 1, items)
			_uiState.update { state ->
				val newQueue =
					if (state.queue.isEmpty())
						state.queue + newCollection
					else
						state.queue.slice(0..state.currentIndex) + newCollection + state.queue.slice(
							state.currentIndex + 1..<state.queue.size
						)
				state.copy(
					queue = newQueue,
					currentIndex = if (state.currentIndex == -1) 0 else state.currentIndex,
					currentSong = if (state.currentIndex == -1) newCollection.firstOrNull() else state.currentSong
				)
			}
			snackBarManager.notifyPlayNext()
		}
	}

	override fun playRadio(radio: DomainRadio) {
		viewModelScope.launch {
			val radioId = "radio_${radio.name.hashCode()}"

			val dummyRadioSong = DomainSong(
				id = radioId,
				title = radio.name,
				artistName = "Live Radio",
				albumId = "radio_album",
				albumTitle = "Live Stream",
				duration = Duration.ZERO,
				trackNumber = 1,
				coverArtId = radio.id,
				artistId = "",
				parentId = "",
				comment = null,
				discNumber = null,
				isrc = emptyList(),
				year = null,
				genre = null,
				genres = emptyList(),
				moods = emptyList(),
				bpm = null,
				contributors = emptyList(),
				playCount = 0,
				userRating = 0,
				averageRating = null,
				bitRate = null,
				bitDepth = null,
				sampleRate = null,
				audioChannelCount = null,
				replayGain = null,
				fileSize = 0,
				fileExtension = "",
				mimeType = "",
				filePath = radio.streamUrl,
				starredAt = null,
				musicBrainzId = null,
				explicitStatus = DomainExplicitStatus.Unknown
			)

			val metadata = MediaMetadata.Builder()
				.setTitle(radio.name)
				.setArtist("Live Radio")
				.setArtworkUri(sessionManager.getCoverArtUrl(radio.id).toUri())
				.setIsPlayable(true)
				.build()

			val mediaItem = MediaItem.Builder()
				.setUri(radio.streamUrl)
				.setMediaId("radio_${radio.name.hashCode()}")
				.setMediaMetadata(metadata)
				.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
				.build()

			controller?.let { player ->
				player.stop()
				player.clearMediaItems()
				player.setMediaItem(mediaItem)
				player.prepare()
				player.play()
			}

			_uiState.update { state ->
				state.copy(
					queue = listOf(dummyRadioSong),
					currentIndex = 0,
					currentSong = dummyRadioSong,
					isLoading = true
				)
			}
		}
	}

	override fun shufflePlay(collection: DomainSongCollection) {
		viewModelScope.launch {
			val (shuffledSongs, mediaItems) = withContext(Dispatchers.Default) {
				val songs = collection.songs.shuffled()
				songs to songs.map { it.toMediaItem() }
			}

			controller?.let { player ->
				player.shuffleModeEnabled = false
				player.setMediaItems(mediaItems, 0, 0L)
				player.prepare()
				player.play()
			}

			_uiState.update { state ->
				state.copy(
					queue = shuffledSongs,
					currentIndex = 0,
					currentSong = shuffledSongs.firstOrNull()
				)
			}
		}
	}

	override fun pause() {
		if (externalPlaybackManager.state.value.activeTarget !is PlaybackTarget.Local) {
			viewModelScope.launch { externalPlaybackManager.pause(); _uiState.update { it.copy(isPaused = true) } }; return
		}
		viewModelScope.launch(Dispatchers.Main.immediate) {
			controller?.pause()
		}
	}

	override fun resume() {
		if (externalPlaybackManager.state.value.activeTarget !is PlaybackTarget.Local) {
			viewModelScope.launch { externalPlaybackManager.play(); _uiState.update { it.copy(isPaused = false) } }; return
		}
		viewModelScope.launch(Dispatchers.Main.immediate) {
			controller?.play()
		}
	}

	override fun next() {
		if (externalPlaybackManager.state.value.activeTarget !is PlaybackTarget.Local) { viewModelScope.launch { externalPlaybackManager.next() }; return }
		viewModelScope.launch(Dispatchers.Main.immediate) {
			if (controller?.hasNextMediaItem() == true) controller?.seekToNextMediaItem()
		}
	}

	override fun previous() {
		if (externalPlaybackManager.state.value.activeTarget !is PlaybackTarget.Local) { viewModelScope.launch { externalPlaybackManager.previous() }; return }
		viewModelScope.launch(Dispatchers.Main.immediate) {
			val controller = controller ?: return@launch
			logPlaybackTimelineSnapshot("previous-command")
			when (previousPlaybackAction(controller.hasPreviousMediaItem())) {
				PreviousPlaybackAction.PreviousItem -> controller.seekToPreviousMediaItem()
				PreviousPlaybackAction.RestartCurrent -> controller.seekTo(0)
			}
		}
	}

	override fun toggleShuffle() {
		viewModelScope.launch {
			controller?.let { player ->
				player.shuffleModeEnabled = !player.shuffleModeEnabled
			}
		}
	}

	override fun toggleRepeat() {
		viewModelScope.launch {
			controller?.let { player ->
				player.repeatMode = when (player.repeatMode) {
					Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
					Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
					else -> Player.REPEAT_MODE_OFF
				}
			}
		}
	}

	override fun seek(normalized: Float) {
		if (externalPlaybackManager.state.value.activeTarget !is PlaybackTarget.Local) {
			val duration = uiState.value.currentSong?.duration?.inWholeMilliseconds ?: return
			viewModelScope.launch { externalPlaybackManager.seekTo((duration * normalized).toLong()); _uiState.update { it.copy(progress = normalized) } }; return
		}
		viewModelScope.launch(Dispatchers.Main.immediate) {
			controller?.let {
				val duration = uiState.value.currentSong?.duration?.inWholeMilliseconds
					?.takeIf { value -> value > 0L }
					?: it.duration
				val target = (duration * normalized).toLong()
				it.seekTo(target)
				_uiState.update { state ->
					state.copy(progress = normalized, currentPositionMs = target.coerceAtLeast(0L))
				}
			}
		}
	}

	override fun seekTo(position: Duration) {
		val requestedMs = position.inWholeMilliseconds.coerceAtLeast(0L)
		if (externalPlaybackManager.state.value.activeTarget !is PlaybackTarget.Local) {
			viewModelScope.launch { externalPlaybackManager.seekTo(requestedMs) }
			return
		}
		viewModelScope.launch(Dispatchers.Main.immediate) {
			val player = controller ?: return@launch
			val actualDuration = player.duration
			val targetMs = if (actualDuration != C.TIME_UNSET && actualDuration > 0L) {
				requestedMs.coerceAtMost(actualDuration)
			} else requestedMs
			if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
				Logger.i(
					"LyricsSeek",
					"command mediaId=${player.currentMediaItem?.mediaId}, requestedMs=$requestedMs, " +
						"targetMs=$targetMs, beforeMs=${player.currentPosition}, durationMs=$actualDuration, " +
						"seekable=${player.isCurrentMediaItemSeekable}, commandAvailable=${player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)}, " +
						"playbackState=${player.playbackState}"
				)
			}
			player.seekTo(targetMs)
			if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
				delay(300)
				Logger.i("LyricsSeek", "result mediaId=${player.currentMediaItem?.mediaId}, afterMs=${player.currentPosition}")
			}
		}
	}

	override fun debugLyricsTap(songId: String, lineIndex: Int, text: String, timestamp: Duration?, metadataDuration: Duration) {
		if (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
		val player = controller
		Logger.i(
			"LyricsTap",
			"handlerReached=true songId=$songId mediaId=${player?.currentMediaItem?.mediaId} lineIndex=$lineIndex " +
				"timestampMs=${timestamp?.inWholeMilliseconds} metadataDurationMs=${metadataDuration.inWholeMilliseconds} " +
				"text=${text.take(80)}"
		)
	}

	override fun onCleared() {
		viewModelScope.launch {
			super.onCleared()
			controllerFuture?.let { MediaController.releaseFuture(it) }
		}
	}

	override fun setPlaybackSpeed(value: Float) {
		viewModelScope.launch {
			controller?.setPlaybackSpeed(value)
		}
		_uiState.update { it.copy(playbackSpeed = value) }
	}

	private fun DomainSong.toMediaItem(): MediaItem {
		val metadataBuilder = MediaMetadata.Builder()
			.setTitle(title)
			.setSubtitle(artistName)
			.setArtist(artistName)
			.setAlbumTitle(albumTitle)
			.setDurationMs(duration.inWholeMilliseconds)
			.setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)

		// Do not embed artwork bytes in queue items. Media3 serializes MediaItems across
		// the session boundary and clones every artwork byte array while doing so. A
		// large queue can therefore retain several copies of every cover and exhaust
		// the app heap. A URI keeps queue metadata small and lets Media3 load only the
		// artwork it currently needs.
		metadataBuilder.setArtworkUri(
			coverArtId?.let { sessionManager.getCoverArtUrl(it).toUri() }
		)

		val metadata = metadataBuilder.build()

		val uri = when {
			id.startsWith("radio_") && !filePath.isNullOrEmpty() -> {
				filePath.toUri()
			}

			else -> {
				val localPath = downloadManager.getDownloadedFilePath(id)
				if (localPath != null) {
					File(localPath).toUri()
				} else {
					getStreamUrl(id)
				}
			}
		}

		val builder = MediaItem.Builder()
			.setUri(uri)
			.setMediaId(id)
			.setMediaMetadata(metadata)

		if (!id.startsWith("radio_") && uri.scheme != "file") {
			builder.setCustomCacheKey(playbackCacheKey(serverNamespace(sessionManager.instanceUrl, sessionManager.username), id, uri))
		}

		if (id.startsWith("radio_")) {
			builder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
		}

		return builder.build()
	}
}

internal fun playbackCacheKey(namespace: String, id: String, uri: Uri): String =
	playbackCacheKey(namespace, id, uri.getQueryParameter("maxBitRate"), uri.getQueryParameter("format"))

internal fun playbackCacheKey(namespace: String, id: String, maxBitRate: String?, format: String?): String = buildString {
	append("v2:")
	append(namespace)
	append(':')
	append(id)
	append(':')
	append(maxBitRate ?: "original")
	append(':')
	append(format ?: "original")
}

internal fun serverNamespace(serverUrl: String, username: String): String {
	val origin = normalizedOrigin(serverUrl)?.let { "${it.scheme}://${it.host}:${it.port}" }.orEmpty()
	return MessageDigest.getInstance("SHA-256")
		.digest("$origin\n$username".encodeToByteArray())
		.joinToString("") { "%02x".format(it) }
}
