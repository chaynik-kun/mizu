package chaynik.mizu.ui.screens.song.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_add_to_queue
import mizu.composeapp.generated.resources.action_play_next
import mizu.composeapp.generated.resources.info_download_failed
import mizu.composeapp.generated.resources.info_downloaded
import mizu.composeapp.generated.resources.info_cached
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
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Star
import chaynik.mizu.icons.outlined.Check
import chaynik.mizu.icons.outlined.Download
import chaynik.mizu.icons.outlined.DownloadOff
import chaynik.mizu.icons.outlined.Queue
import chaynik.mizu.icons.outlined.QueuePlayNext
import chaynik.mizu.ui.components.common.CoverArt
import chaynik.mizu.ui.components.common.MarqueeText
import chaynik.mizu.ui.components.sheets.SongSheet
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.playlist.dialogs.PlaylistUpdateDialog
import chaynik.mizu.util.core.InlineExplicitIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongListScreenItem(
	modifier: Modifier,
	song: DomainSong,
	selected: Boolean,
	starred: Boolean,
	rating: Int,
	onSelect: () -> Unit,
	onDeselect: () -> Unit,
	onSetStarred: (starred: Boolean) -> Unit,
	onSetShareId: (String) -> Unit,
	onPlayNext: () -> Unit,
	onAddToQueue: () -> Unit,
	onClick: () -> Unit,
	onSetRating: (Int) -> Unit,
	download: DownloadEntity?,
	onDownload: () -> Unit,
	onCancelDownload: () -> Unit,
	onDeleteDownload: () -> Unit,
) {
	val backStack = LocalNavStack.current
	val dismissState = rememberSwipeToDismissBoxState()
	val scope = rememberCoroutineScope()
	var playlistDialogShown by rememberSaveable { mutableStateOf(false) }
	val preferenceManager = koinInject<PreferenceManager>()
	val playbackCacheManager = koinInject<PlaybackCacheManager>()
	val cachedSongIds by playbackCacheManager.cachedSongIds.collectAsStateWithLifecycle()
	val isDownloaded = download?.status == DownloadStatus.DOWNLOADED
	val isCached = !isDownloaded && song.id in cachedSongIds

	SwipeToDismissBox(
		modifier = modifier,
		state = dismissState,
		onDismiss = {
			if (it == SwipeToDismissBoxValue.StartToEnd) onAddToQueue()
			if (it == SwipeToDismissBoxValue.EndToStart) onPlayNext()
			scope.launch { dismissState.reset() }
		},
		backgroundContent = {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clip(MaterialTheme.shapes.extraSmall)
					.background(MaterialTheme.colorScheme.primaryContainer)
					.padding(horizontal = 20.dp),
				contentAlignment = Alignment.CenterEnd
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
		Box {
			ListItem(
				onClick = onClick,
				onLongClick = onSelect,
				content = {
					MarqueeText(
						text = buildAnnotatedString {
							append(song.title)
							if (song.explicitStatus == DomainExplicitStatus.Explicit) {
								append(" ")
								appendInlineContent("InlineExplicitIcon")
							}
						},
						inlineContent = InlineExplicitIcon,
					)
				},
				supportingContent = {
					Text(
						buildString {
							append(song.albumTitle ?: stringResource(Res.string.info_unknown_album))
							append(" • ")
							append(song.artistName)
							append(" • ")
							append(song.year ?: stringResource(Res.string.info_unknown_year))
						},
						maxLines = 1
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
					if (isCached) {
						Icon(
							Icons.Outlined.Check,
							contentDescription = stringResource(Res.string.info_cached),
							modifier = Modifier.size(16.dp),
							tint = MaterialTheme.colorScheme.primary
						)
					}
					if (starred) {
						Icon(
							Icons.Filled.Star,
							null,
							modifier = Modifier.size(16.dp)
						)
					}
					if (download != null) {
						when (download.status) {
							DownloadStatus.DOWNLOADING -> {
								CircularProgressIndicator(
									progress = { download.progress },
									modifier = Modifier.size(16.dp),
									strokeWidth = 2.dp
								)
							}

							DownloadStatus.DOWNLOADED -> {
								Icon(
									Icons.Outlined.Download,
									contentDescription = stringResource(Res.string.info_downloaded),
									modifier = Modifier.size(16.dp),
									tint = MaterialTheme.colorScheme.primary
								)
							}

							DownloadStatus.FAILED -> {
								Icon(
									Icons.Outlined.DownloadOff,
									contentDescription = stringResource(Res.string.info_download_failed),
									modifier = Modifier.size(16.dp),
									tint = MaterialTheme.colorScheme.error
								)
							}

							else -> {}
						}
					}
				}
			)
			if (selected) {
				SongSheet(
					onDismissRequest = onDeselect,
					song = song,
					starred = starred,
					rating = rating,
					onSetStarred = onSetStarred,
					onShare = { onSetShareId(song.id) },
					onPlayNext = onPlayNext,
					onAddToQueue = onAddToQueue,
					onTrackInfo = dropUnlessResumed {
						backStack.add(Screen.SongDetailScreen(song.id, song.coverArtId))
					},
					onViewAlbum = song.albumId?.let { albumId ->
						dropUnlessResumed {
							backStack.add(
								Screen.CollectionDetail(
									collectionId = albumId,
									tab = "library"
								)
							)
						}
					},
					onAddToPlaylist = {
						playlistDialogShown = true
					},
					onSetRating = onSetRating,
					downloadStatus = download?.status ?: DownloadStatus.NOT_DOWNLOADED,
					onDownload = onDownload,
					onCancelDownload = onCancelDownload,
					onDeleteDownload = onDeleteDownload
				)
			}
		}
	}

	if (playlistDialogShown) {
		PlaylistUpdateDialog(
			songs = persistentListOf(song),
			onDismissRequest = { playlistDialogShown = false }
		)
	}
}
