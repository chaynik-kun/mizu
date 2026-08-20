package chaynik.mizu.ui.screens.nowPlaying.components.controls

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.persistentListOf
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_more
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.MoreHoriz
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.domain.manager.DownloadManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.data.database.entities.DownloadStatus
import chaynik.mizu.ui.components.sheets.SongSheet
import chaynik.mizu.ui.components.sheets.SleepTimerSheet
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.playlist.dialogs.PlaylistUpdateDialog
import chaynik.mizu.ui.screens.share.dialogs.ShareDialog
import chaynik.mizu.ui.theme.MizuTheme
import chaynik.mizu.util.ui.rememberColorSchemeFromCoverArt
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongCollection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

private data class MoreButtonPlayerState(
	val song: DomainSong?,
	val collection: DomainSongCollection?
)

@Composable
fun NowPlayingMoreButton(
	songRating: Int,
	onSetSongRating: (Int) -> Unit,
	enabled: Boolean = true
) {
	val backStack = LocalNavStack.current
	val player = koinInject<MediaPlayerViewModel>()
	val downloadManager = koinInject<DownloadManager>()
	val downloads by downloadManager.allDownloads.collectAsStateWithLifecycle(emptyList())
	val moreButtonStateFlow = remember(player) {
		player.uiState.map { MoreButtonPlayerState(it.currentSong, it.currentCollection) }
			.distinctUntilChanged()
	}
	val playerState by moreButtonStateFlow.collectAsState(MoreButtonPlayerState(null, null))
	val song = playerState.song
	val download = song?.let { current -> downloads.firstOrNull { it.songId == current.id } }
	var expanded by remember { mutableStateOf(false) }
	var sleepTimerSheetShown by rememberSaveable { mutableStateOf(false) }
	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }
	var shareId by remember { mutableStateOf<String?>(null) }
	var shareExpiry by remember { mutableStateOf<Duration?>(null) }
	val colorScheme = rememberColorSchemeFromCoverArt(song?.coverArtId)
	val preferences = koinInject<PreferenceManager>()
	val sheetColorScheme = if (preferences.dynamicTheming) MaterialTheme.colorScheme else colorScheme

	IconButton(
		onClick = {
			expanded = true
		},
		colors = IconButtonDefaults.filledTonalIconButtonColors(),
		modifier = Modifier.size(32.dp),
		enabled = enabled && song != null
	) {
		Icon(
			imageVector = Icons.Outlined.MoreHoriz,
			contentDescription = stringResource(Res.string.action_more)
		)
	}

	if (expanded && song != null) {
		MizuTheme(sheetColorScheme) {
			SongSheet(
				onDismissRequest = { expanded = false },
				song = song,
				collection = playerState.collection,
				onViewAlbum = dropUnlessResumed {
					playerState.collection?.let { collection ->
						backStack.remove(Screen.NowPlaying)
						backStack.add(Screen.CollectionDetail(collection.id, ""))
					}
				},
				onViewArtist = dropUnlessResumed {
					backStack.remove(Screen.NowPlaying)
					backStack.add(Screen.ArtistDetail(song.artistId))
				},
				onShare = {
					shareId = song.id
				},
				onAddToPlaylist = {
					playlistDialogShown = true
				},
				onTrackInfo = dropUnlessResumed {
					expanded = false
					backStack.add(Screen.SongDetailSheet(songId = song.id, coverArtId = song.coverArtId))
				},
				rating = songRating,
				onSetRating = onSetSongRating,
				downloadStatus = download?.status ?: DownloadStatus.NOT_DOWNLOADED,
				onDownload = { downloadManager.downloadSong(song) },
				onCancelDownload = { downloadManager.cancelDownload(song.id) },
				onDeleteDownload = { downloadManager.deleteDownload(song.id) },
				showSleepTimer = true,
				onSleepTimer = {
					expanded = false
					sleepTimerSheetShown = true
				},
				showPlaybackSpeed = true,
				onPlaybackSpeed = {
					expanded = false
					backStack.add(Screen.PlaybackSpeed)
				}
			)
		}
	}

	if (sleepTimerSheetShown) {
		MizuTheme(sheetColorScheme) {
			SleepTimerSheet(
				onDismissRequest = { sleepTimerSheetShown = false }
			)
		}
	}

	if (playlistDialogShown && song != null) {
		MizuTheme(sheetColorScheme) {
			PlaylistUpdateDialog(
				songs = persistentListOf(song),
				onDismissRequest = { playlistDialogShown = false }
			)
		}
	}

	MizuTheme(sheetColorScheme) {
		ShareDialog(
			id = shareId,
			onIdClear = { shareId = null },
			expiry = shareExpiry,
			onExpiryChange = { shareExpiry = it }
		)
	}
}
