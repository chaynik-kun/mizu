package chaynik.mizu.ui.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_add_to_another_playlist
import mizu.composeapp.generated.resources.action_add_to_playlist
import mizu.composeapp.generated.resources.action_add_to_queue
import mizu.composeapp.generated.resources.action_cancel_download
import mizu.composeapp.generated.resources.action_delete_download
import mizu.composeapp.generated.resources.action_download
import mizu.composeapp.generated.resources.action_play_next
import mizu.composeapp.generated.resources.action_remove_from_playlist
import mizu.composeapp.generated.resources.action_remove_star
import mizu.composeapp.generated.resources.action_share
import mizu.composeapp.generated.resources.action_sleep_timer
import mizu.composeapp.generated.resources.action_sleep_timer_enabled
import mizu.composeapp.generated.resources.action_star
import mizu.composeapp.generated.resources.action_track_info
import mizu.composeapp.generated.resources.action_view_album
import mizu.composeapp.generated.resources.action_view_artist
import mizu.composeapp.generated.resources.info_click_to_retry
import mizu.composeapp.generated.resources.info_download_failed
import mizu.composeapp.generated.resources.option_playback_speed
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.data.database.entities.DownloadStatus
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.SleepTimerManager
import chaynik.mizu.domain.models.DomainAlbum
import chaynik.mizu.domain.models.DomainExplicitStatus
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Star
import chaynik.mizu.icons.outlined.Album
import chaynik.mizu.icons.outlined.Artist
import chaynik.mizu.icons.outlined.Bedtime
import chaynik.mizu.icons.outlined.Close
import chaynik.mizu.icons.outlined.Delete
import chaynik.mizu.icons.outlined.Download
import chaynik.mizu.icons.outlined.DownloadOff
import chaynik.mizu.icons.outlined.Info
import chaynik.mizu.icons.outlined.PlaylistAdd
import chaynik.mizu.icons.outlined.PlaylistRemove
import chaynik.mizu.icons.outlined.Queue
import chaynik.mizu.icons.outlined.QueuePlayNext
import chaynik.mizu.icons.outlined.Share
import chaynik.mizu.icons.outlined.Speed
import chaynik.mizu.icons.outlined.Star
import chaynik.mizu.ui.components.common.CoverArt
import chaynik.mizu.ui.components.common.MarqueeText
import chaynik.mizu.ui.components.common.RatingRow
import chaynik.mizu.ui.theme.positive
import chaynik.mizu.util.core.InlineExplicitIcon
import chaynik.mizu.util.core.label
import chaynik.mizu.util.ui.rememberColorSchemeFromCoverArt
import chaynik.mizu.ui.theme.MizuTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SongSheet(
	onDismissRequest: () -> Unit,
	song: DomainSong,
	collection: DomainSongCollection? = null,
	starred: Boolean? = null,
	onSetStarred: ((Boolean) -> Unit)? = null,
	onShare: (() -> Unit)? = null,
	onPlayNext: (() -> Unit)? = null,
	onAddToQueue: (() -> Unit)? = null,
	onTrackInfo: (() -> Unit)? = null,
	onViewAlbum: (() -> Unit)? = null,
	onViewArtist: (() -> Unit)? = null,
	onAddToPlaylist: (() -> Unit)? = null,
	onRemoveFromPlaylist: (() -> Unit)? = null,
	downloadStatus: DownloadStatus? = null,
	onDownload: (() -> Unit)? = null,
	onCancelDownload: (() -> Unit)? = null,
	onDeleteDownload: (() -> Unit)? = null,
	rating: Int? = null,
	onSetRating: ((Int) -> Unit)? = null,
	showSleepTimer: Boolean = false,
	onSleepTimer: (() -> Unit)? = null,
	showPlaybackSpeed: Boolean = false,
	onPlaybackSpeed: (() -> Unit)? = null,
	useSongTheme: Boolean = true
) {
	val preferenceManager = koinInject<PreferenceManager>()

	val sleepTimerManager = koinInject<SleepTimerManager>()
	val sleepTimerLeft = sleepTimerManager.timeLeft
	val contentPadding = PaddingValues(horizontal = 16.dp)

	val colorScheme = if (useSongTheme) rememberColorSchemeFromCoverArt(song.coverArtId) else null

	MizuTheme(colorScheme) {
		val colors = ListItemDefaults.colors(
			containerColor = Color.Transparent,
			trailingIconColor = MaterialTheme.colorScheme.onSurface,
			headlineColor = MaterialTheme.colorScheme.onSurface
		)
		ModalBottomSheet(
			onDismissRequest = onDismissRequest,
			dragHandle = null,
			containerColor = MaterialTheme.colorScheme.surface,
			sheetState = rememberBottomSheetState(
				initialValue = SheetValue.Hidden,
				enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
			),
			contentWindowInsets = {
				BottomSheetDefaults.modalWindowInsets.add(
					WindowInsets(
						left = 8.dp,
						right = 8.dp
					)
				)
			}
		) {
			Spacer(Modifier.height(16.dp))

			ListItem(
				headlineContent = {
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
					MarqueeText(
						"${song.albumTitle ?: ""} • ${song.artistName} • ${song.year ?: ""}"
					)
				},
				leadingContent = {
					CoverArt(
						coverArtId = song.coverArtId,
						modifier = Modifier.size(50.dp),
						shape = preferenceManager.coverArtShape.decreasedShape
					)
				},
				colors = colors
			)
			if (rating != null && onSetRating != null) {
				RatingRow(
					rating = rating,
					setRating = onSetRating
				)
				Spacer(Modifier.height(14.dp))
			}

			HorizontalDivider(Modifier.padding(horizontal = 8.dp, vertical = 2.dp))

			Column(Modifier.verticalScroll(rememberScrollState())) {
				if (onShare != null) {
					ListItem(
						content = { Text(stringResource(Res.string.action_share)) },
						leadingContent = { Icon(Icons.Outlined.Share, null) },
						onClick = {
							onShare()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (starred != null && onSetStarred != null) {
					ListItem(
						content = {
							Text(stringResource(if (starred) Res.string.action_remove_star else Res.string.action_star))
						},
						leadingContent = {
							Icon(if (starred) Icons.Filled.Star else Icons.Outlined.Star, null)
						},
						onClick = {
							onSetStarred(!starred)
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (downloadStatus != null) {
					when (downloadStatus) {
						DownloadStatus.DOWNLOADING -> {
							ListItem(
								content = { Text(stringResource(Res.string.action_cancel_download)) },
								leadingContent = { Icon(Icons.Outlined.Close, null) },
								onClick = {
									onCancelDownload?.invoke()
									onDismissRequest()
								},
								colors = colors,
								contentPadding = contentPadding
							)
						}

						DownloadStatus.DOWNLOADED -> {
							ListItem(
								content = { Text(stringResource(Res.string.action_delete_download)) },
								leadingContent = { Icon(Icons.Outlined.Delete, null) },
								onClick = {
									onDeleteDownload?.invoke()
									onDismissRequest()
								},
								colors = colors,
								contentPadding = contentPadding
							)
						}

						DownloadStatus.FAILED -> {
							ListItem(
								content = {
									Text(
										text = stringResource(Res.string.info_download_failed),
										color = MaterialTheme.colorScheme.error
									)
								},
								supportingContent = {
									Text(
										text = stringResource(Res.string.info_click_to_retry),
										color = MaterialTheme.colorScheme.error,
										style = MaterialTheme.typography.labelSmall
									)
								},
								leadingContent = {
									Icon(
										Icons.Outlined.DownloadOff,
										null,
										tint = MaterialTheme.colorScheme.error
									)
								},
								onClick = {
									onDownload?.invoke()
									onDismissRequest()
								},
								colors = colors,
								contentPadding = contentPadding
							)
						}

						else -> {
							ListItem(
								content = { Text(stringResource(Res.string.action_download)) },
								leadingContent = { Icon(Icons.Outlined.Download, null) },
								onClick = {
									onDownload?.invoke()
									onDismissRequest()
								},
								colors = colors,
								contentPadding = contentPadding
							)
						}
					}
				} else if (onDownload != null) {
					ListItem(
						content = { Text(stringResource(Res.string.action_download)) },
						leadingContent = { Icon(Icons.Outlined.Download, null) },
						onClick = {
							onDownload()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onPlayNext != null) {
					ListItem(
						content = { Text(stringResource(Res.string.action_play_next)) },
						leadingContent = { Icon(Icons.Outlined.QueuePlayNext, null) },
						onClick = {
							onPlayNext()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onAddToQueue != null) {
					ListItem(
						content = { Text(stringResource(Res.string.action_add_to_queue)) },
						leadingContent = { Icon(Icons.Outlined.Queue, null) },
						onClick = {
							onAddToQueue()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onAddToPlaylist != null) {
					ListItem(
						content = {
							Text(
								stringResource(
									if (collection != null && collection !is DomainAlbum)
										Res.string.action_add_to_another_playlist
									else Res.string.action_add_to_playlist
								)
							)
						},
						leadingContent = { Icon(Icons.Outlined.PlaylistAdd, null) },
						onClick = {
							onAddToPlaylist()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onRemoveFromPlaylist != null && collection != null && collection !is DomainAlbum) {
					ListItem(
						content = { Text(stringResource(Res.string.action_remove_from_playlist)) },
						leadingContent = { Icon(Icons.Outlined.PlaylistRemove, null) },
						onClick = {
							onRemoveFromPlaylist()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onViewAlbum != null) {
					ListItem(
						content = {
							Text(stringResource(Res.string.action_view_album))
						},
						leadingContent = { Icon(Icons.Outlined.Album, null) },
						onClick = {
							onViewAlbum()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onViewArtist != null) {
					ListItem(
						content = { Text(stringResource(Res.string.action_view_artist)) },
						leadingContent = { Icon(Icons.Outlined.Artist, null) },
						onClick = {
							onViewArtist()
							onDismissRequest()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (showSleepTimer) {
					if (sleepTimerLeft != null) {
						ListItem(
							content = {
								Text(
									stringResource(
										Res.string.action_sleep_timer_enabled,
										sleepTimerLeft.label()
									),
									color = MaterialTheme.colorScheme.positive
								)
							},
							leadingContent = {
								Icon(
									Icons.Outlined.Bedtime,
									null,
									tint = MaterialTheme.colorScheme.positive
								)
							},
							onClick = {
								onSleepTimer?.invoke()
							},
							colors = colors,
							contentPadding = contentPadding
						)
					} else {
						ListItem(
							content = {
								Text(
									stringResource(Res.string.action_sleep_timer)
								)
							},
							leadingContent = {
								Icon(
									Icons.Outlined.Bedtime,
									null
								)
							},
							onClick = {
								onSleepTimer?.invoke()
							},
							colors = colors,
							contentPadding = contentPadding
						)
					}
				}

				if (showPlaybackSpeed) {
					ListItem(
						content = {
							Text(
								stringResource(Res.string.option_playback_speed)
							)
						},
						leadingContent = {
							Icon(
								Icons.Outlined.Speed,
								null
							)
						},
						onClick = dropUnlessResumed {
							onPlaybackSpeed?.invoke()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}

				if (onTrackInfo != null) {
					ListItem(
						content = { Text(stringResource(Res.string.action_track_info)) },
						leadingContent = { Icon(Icons.Outlined.Info, null) },
						onClick = {
							onDismissRequest()
							onTrackInfo()
						},
						colors = colors,
						contentPadding = contentPadding
					)
				}
			}
		}
	}
}
