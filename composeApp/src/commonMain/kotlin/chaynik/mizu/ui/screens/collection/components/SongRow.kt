package chaynik.mizu.ui.screens.collection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_add_to_queue
import mizu.composeapp.generated.resources.action_play_next
import mizu.composeapp.generated.resources.info_download_failed
import mizu.composeapp.generated.resources.info_downloaded
import mizu.composeapp.generated.resources.info_cached
import mizu.composeapp.generated.resources.info_explicit
import mizu.composeapp.generated.resources.info_not_available_offline
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
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
import chaynik.mizu.icons.outlined.Queue
import chaynik.mizu.icons.outlined.QueuePlayNext
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.CoverArt
import chaynik.mizu.ui.components.common.MarqueeText
import chaynik.mizu.ui.components.common.Waveform
import chaynik.mizu.ui.components.dialogs.QueueDuplicateDialog
import chaynik.mizu.util.core.InlineExplicitIcon
import chaynik.mizu.util.core.toHoursMinutesSeconds
import chaynik.mizu.util.ui.segmentedShapes

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionDetailScreenSongRow(
	song: DomainSong,
	index: Int,
	count: Int,
	isPlaylist: Boolean = false,
	onClick: (() -> Unit),
	onLongClick: (() -> Unit),
	onPlayNext: (() -> Unit),
	onAddToQueue: (() -> Unit),
	isStarred: Boolean,
	download: DownloadEntity? = null,
	isOffline: Boolean = false
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val playbackCacheManager = koinInject<PlaybackCacheManager>()
	val cachedSongIds by playbackCacheManager.cachedSongIds.collectAsStateWithLifecycle()

	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsStateWithLifecycle()

	val isDownloaded = download?.status == DownloadStatus.DOWNLOADED
	val isCached = !isDownloaded && song.id in cachedSongIds
	val isCurrentTrack = playerState.currentSong?.id == song.id
	val isExplicit = song.explicitStatus == DomainExplicitStatus.Explicit
		&& preferenceManager.explicitContentPlayback != ExplicitContentPlayback.Allowed
	val maybeUnavailable = isOffline && !isDownloaded && !isCached

	val dismissState = rememberSwipeToDismissBoxState()
	val scope = rememberCoroutineScope()

	var isPlayNextPending by rememberSaveable { mutableStateOf<Boolean?>(null) }

	val itemShape = segmentedShapes(
		index = index,
		count = count,
		dismissDirection = dismissState.dismissDirection
	)

	SwipeToDismissBox(
		modifier = Modifier.padding(horizontal = 16.dp, vertical = 1.5.dp).clip(itemShape.shape),
		state = dismissState,
		gesturesEnabled = !isExplicit,
		onDismiss = {
			if (it == SwipeToDismissBoxValue.StartToEnd) {
				if (playerState.queue.any { item -> item.id == song.id }) {
					isPlayNextPending = false
				} else {
					onAddToQueue()
				}
			}
			if (it == SwipeToDismissBoxValue.EndToStart) {
				if (playerState.queue.any { item -> item.id == song.id }) {
					isPlayNextPending = true
				} else {
					onPlayNext()
				}
			}
			scope.launch { dismissState.reset() }
		},
		backgroundContent = {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clip(itemShape.shape)
					.background(MaterialTheme.colorScheme.primaryContainer)
					.padding(horizontal = 20.dp)
			) {
				when (dismissState.dismissDirection) {
					SwipeToDismissBoxValue.StartToEnd -> {
						Icon(
							imageVector = Icons.Outlined.Queue,
							contentDescription = stringResource(Res.string.action_add_to_queue),
							tint = MaterialTheme.colorScheme.onPrimaryContainer,
							modifier = Modifier.align(Alignment.CenterStart)
						)
					}

					SwipeToDismissBoxValue.EndToStart -> {
						Icon(
							imageVector = Icons.Outlined.QueuePlayNext,
							contentDescription = stringResource(Res.string.action_play_next),
							tint = MaterialTheme.colorScheme.onPrimaryContainer,
							modifier = Modifier.align(Alignment.CenterEnd)
						)
					}

					else -> {}
				}
			}
		}
	) {
		SegmentedListItem(
			enabled = !isExplicit,
			contentPadding = PaddingValues(14.dp),
			onClick = onClick,
			onLongClick = onLongClick,
			shapes = itemShape,
			colors = ListItemDefaults.segmentedColors(
				containerColor = MaterialTheme.colorScheme.surfaceContainer
			),
			leadingContent = {
				if (isPlaylist)
					CoverArt(
						modifier = Modifier.size(48.dp),
						coverArtId = song.coverArtId,
						shape = MaterialTheme.shapes.small
					)
				else
					Text(
						text = "${index + 1}",
						modifier = Modifier.width(25.dp),
						style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
						fontWeight = FontWeight(400),
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 1,
						textAlign = TextAlign.Center,
						autoSize = TextAutoSize.StepBased(6.sp, 13.sp)
					)
			},
			content = {
				Column {
					MarqueeText(
						text = buildAnnotatedString {
							append(song.title)
							if (song.explicitStatus == DomainExplicitStatus.Explicit) {
								append(" ")
								appendInlineContent("InlineExplicitIcon")
							}
						},
						inlineContent = InlineExplicitIcon
					)
					Text(
						song.artistName,
						style = MaterialTheme.typography.bodySmall,
						maxLines = 1
					)
				}
			},
			trailingContent = {
				Row(verticalAlignment = Alignment.CenterVertically) {
					if (isCached && !isCurrentTrack) {
						Icon(
							Icons.Outlined.Check,
							contentDescription = stringResource(Res.string.info_cached),
							modifier = Modifier.size(16.dp),
							tint = MaterialTheme.colorScheme.primary
						)
						Spacer(Modifier.width(8.dp))
					}
					if (isStarred) {
						Icon(
							Icons.Filled.Star,
							null,
							modifier = Modifier.size(16.dp)
						)
						Spacer(Modifier.width(6.dp))
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
					song.duration.toHoursMinutesSeconds().let {
						Text(
							text = it,
							style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
							fontWeight = FontWeight(400),
							fontSize = 13.sp,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
							maxLines = 1
						)
					}
				}
			}
		)
	}

	if (isPlayNextPending != null) {
		QueueDuplicateDialog(
			onDismissRequest = { isPlayNextPending = null },
			onConfirm = {
				if (isPlayNextPending == true) onPlayNext() else onAddToQueue()
				isPlayNextPending = null
			}
		)
	}
}
