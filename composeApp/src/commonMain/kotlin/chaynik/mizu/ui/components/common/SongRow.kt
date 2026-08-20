package chaynik.mizu.ui.components.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.persistentListOf
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_download_failed
import mizu.composeapp.generated.resources.info_downloaded
import mizu.composeapp.generated.resources.info_cached
import mizu.composeapp.generated.resources.info_explicit
import mizu.composeapp.generated.resources.info_not_available_offline
import mizu.composeapp.generated.resources.info_unknown_album
import mizu.composeapp.generated.resources.info_unknown_year
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.data.database.entities.DownloadEntity
import chaynik.mizu.data.database.entities.DownloadStatus
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.PlaybackCacheManager
import chaynik.mizu.domain.models.DomainExplicitStatus
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.settings.ExplicitContentPlayback
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Star
import chaynik.mizu.icons.outlined.Check
import chaynik.mizu.icons.outlined.Download
import chaynik.mizu.icons.outlined.DownloadOff
import chaynik.mizu.icons.outlined.Lock
import chaynik.mizu.icons.outlined.Offline
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.dialogs.QueueDuplicateDialog
import chaynik.mizu.ui.components.sheets.SongSheet
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.playlist.dialogs.PlaylistUpdateDialog
import chaynik.mizu.util.core.InlineExplicitIcon

@Composable
fun SongRow(
	modifier: Modifier = Modifier,
	song: DomainSong,
	selected: Boolean = false,
	onClick: (() -> Unit),
	onLongClick: (() -> Unit),
	isOnline: Boolean = false,
	onDismissRequest: () -> Unit,
	onRemoveStar: () -> Unit,
	onAddStar: () -> Unit,
	onShare: () -> Unit,
	starredState: Boolean,
	download: DownloadEntity? = null,
	onDownload: () -> Unit,
	onCancelDownload: () -> Unit,
	onDeleteDownload: () -> Unit,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
	rating: Int,
	onSetRating: (Int) -> Unit
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val playbackCacheManager = koinInject<PlaybackCacheManager>()
	val cachedSongIds by playbackCacheManager.cachedSongIds.collectAsStateWithLifecycle()
	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsStateWithLifecycle()

	val backStack = LocalNavStack.current
	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }
	var duplicateQueueDialogShown by rememberSaveable { mutableStateOf(false) }
	var duplicateQueueDialogShownPlayNext by rememberSaveable { mutableStateOf(false) }

	val isDownloaded = download?.status == DownloadStatus.DOWNLOADED
	val isCached = !isDownloaded && song.id in cachedSongIds
	val isCurrentTrack = playerState.currentSong?.id == song.id
	val isExplicit = song.explicitStatus == DomainExplicitStatus.Explicit
		&& preferenceManager.explicitContentPlayback != ExplicitContentPlayback.Allowed
	val maybeUnavailable = !isOnline && !isDownloaded && !isCached

	ListItem(
		modifier = modifier
			.width(400.dp)
			.alpha(if (isExplicit) .5f else 1f)
			.combinedClickable(
				onClick = onClick,
				onLongClick = onLongClick,
				enabled = !isExplicit
			),
		headlineContent = {
			Text(
				text = buildAnnotatedString {
					append(song.title)
					if (song.explicitStatus == DomainExplicitStatus.Explicit) {
						append(" ")
						appendInlineContent("InlineExplicitIcon")
					}
				},
				inlineContent = InlineExplicitIcon,
				maxLines = 2
			)
		},
		supportingContent = {
			MarqueeText(
				text = buildString {
					append(song.albumTitle ?: stringResource(Res.string.info_unknown_album))
					append(" • ")
					append(song.artistName)
					append(" • ")
					append(song.year ?: stringResource(Res.string.info_unknown_year))
				}
			)
		},
		leadingContent = {
			CoverArt(
				coverArtId = song.coverArtId,
				modifier = Modifier.size(50.dp),
				shape = preferenceManager.coverArtShape.decreasedShape
			)
		},
		trailingContent = {
			Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(83.dp)) {
				if (isCached && !isCurrentTrack) {
					Icon(
						Icons.Outlined.Check,
						contentDescription = stringResource(Res.string.info_cached),
						modifier = Modifier.size(16.dp),
						tint = MaterialTheme.colorScheme.primary
					)
					Spacer(Modifier.width(8.dp))
				}
				if (starredState) {
					Icon(
						Icons.Filled.Star,
						null,
						modifier = Modifier.size(16.dp)
					)
					Spacer(Modifier.width(8.dp))
				}
				if (isExplicit) {
					Icon(
						Icons.Outlined.Lock,
						stringResource(Res.string.info_explicit),
						modifier = Modifier.size(20.dp)
					)
					Spacer(Modifier.width(6.dp))
				}
				if (maybeUnavailable) {
					Icon(
						Icons.Outlined.Offline,
						stringResource(Res.string.info_not_available_offline),
						modifier = Modifier.size(20.dp)
					)
					Spacer(Modifier.width(6.dp))
				}
				if (download != null && !isCurrentTrack) {
					when (download.status) {
						DownloadStatus.DOWNLOADING -> {
							CircularProgressIndicator(
								progress = { download.progress },
								modifier = Modifier.size(16.dp),
								strokeWidth = 2.dp
							)
							Spacer(Modifier.width(8.dp))
						}

						DownloadStatus.DOWNLOADED -> {
							Icon(
								Icons.Outlined.Download,
								contentDescription = stringResource(Res.string.info_downloaded),
								modifier = Modifier.size(16.dp),
								tint = MaterialTheme.colorScheme.primary
							)
							Spacer(Modifier.width(8.dp))
						}

						DownloadStatus.FAILED -> {
							Icon(
								Icons.Outlined.DownloadOff,
								contentDescription = stringResource(Res.string.info_download_failed),
								modifier = Modifier.size(16.dp),
								tint = MaterialTheme.colorScheme.error
							)
							Spacer(Modifier.width(8.dp))
						}

						else -> {}
					}
				}
				if (isCurrentTrack) {
					Waveform(
						modifier = Modifier.padding(end = 12.dp),
						isPlaying = !playerState.isPaused
					)
				}
			}
		}
	)

	if (selected) {
		SongSheet(
			onDismissRequest = onDismissRequest,
			song = song,
			starred = starredState,
			rating = rating,
			onSetStarred = { starred ->
				if (starred) onAddStar() else onRemoveStar()
			},
			onShare = onShare,
			onPlayNext = {
				if (player.uiState.value.queue.any { it.id == song.id }) {
					duplicateQueueDialogShown = true
					duplicateQueueDialogShownPlayNext = true
				} else {
					onPlayNext()
				}
			},
			onAddToQueue = {
				if (player.uiState.value.queue.any { it.id == song.id }) {
					duplicateQueueDialogShown = true
					duplicateQueueDialogShownPlayNext = false
				} else {
					onAddToQueue()
				}
			},
			onTrackInfo = dropUnlessResumed {
				backStack.add(Screen.SongDetailScreen(song.id, song.coverArtId))
			},
			onViewAlbum = dropUnlessResumed {
				backStack.add(
					Screen.CollectionDetail(
						collectionId = song.albumId as String,
						tab = "library"
					)
				)
			},
			onAddToPlaylist = {
				playlistDialogShown = true
			},
			downloadStatus = download?.status,
			onDownload = onDownload,
			onCancelDownload = onCancelDownload,
			onDeleteDownload = onDeleteDownload,
			onSetRating = onSetRating
		)
	}

	if (playlistDialogShown) {
		PlaylistUpdateDialog(
			songs = persistentListOf(song),
			onDismissRequest = { playlistDialogShown = false }
		)
	}

	if (duplicateQueueDialogShown) {
		QueueDuplicateDialog(
			onDismissRequest = {
				duplicateQueueDialogShown = false
				onDismissRequest()
			},
			onConfirm = {
				if (duplicateQueueDialogShownPlayNext) onPlayNext() else onAddToQueue()
			}
		)
	}
}
