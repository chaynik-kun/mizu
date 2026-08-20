package chaynik.mizu.ui.screens.nowPlaying.components.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_not_playing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.models.DomainExplicitStatus
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongCollection
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.MarqueeText
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.nowPlaying.components.controls.NowPlayingMoreButton
import chaynik.mizu.ui.screens.nowPlaying.components.controls.NowPlayingStarButton
import chaynik.mizu.util.core.InlineExplicitIconLarge
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class InfoPlayerState(
	val song: DomainSong?,
	val collection: DomainSongCollection?
)

@Composable
fun NowPlayingInfoRow(
	songIsStarred: Boolean,
	onSetSongIsStarred: (Boolean) -> Unit,
	songRating: Int,
	onSetSongRating: (Int) -> Unit
) {
	val backStack = LocalNavStack.current
	val player = koinInject<MediaPlayerViewModel>()
	val infoStateFlow = remember(player) {
		player.uiState.map { InfoPlayerState(it.currentSong, it.currentCollection) }
			.distinctUntilChanged()
	}
	val playerState by infoStateFlow.collectAsState(InfoPlayerState(null, null))
	val song = playerState.song
	val isRadio = song?.id?.startsWith("radio_") == true
	Row(
		modifier = Modifier
			.padding(horizontal = 16.dp)
			.padding(bottom = 6.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Column(Modifier.weight(1f)) {
			song?.let { song ->
				MarqueeText(
					text = buildAnnotatedString {
						append(song.title)
						if (song.explicitStatus == DomainExplicitStatus.Explicit) {
							append(" ")
							appendInlineContent("InlineExplicitIcon")
						}
					},
					inlineContent = InlineExplicitIconLarge,
					modifier = Modifier.clickable(onClick = dropUnlessResumed {
						song.albumId?.let {
							backStack.removeLastOrNull()

							val lastScreen = backStack.lastOrNull()

							val isSameAlbum = if (lastScreen is Screen.CollectionDetail) {
								lastScreen.collectionId == song.albumId
							} else {
								false
							}

							if (!isSameAlbum)
								backStack.add(
									Screen.CollectionDetail(
										playerState.collection?.id
											?: return@dropUnlessResumed,
										""
									)
								)
						}
					}),
					style = MaterialTheme.typography.bodyLarge
						.copy(
							fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.1
						),
				)
			}
			MarqueeText(
				modifier = Modifier.clickable(
					song != null,
					onClick = dropUnlessResumed {
						song?.artistId?.let { id ->
							backStack.remove(Screen.NowPlaying)
							backStack.add(Screen.ArtistDetail(id))
						}
					}
				),
				style = MaterialTheme.typography.bodyMedium
					.copy(
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						fontSize = MaterialTheme.typography.bodyMedium.fontSize * 1.1
					),
				text = song?.artistName ?: stringResource(Res.string.info_not_playing)
			)
		}
		Row(
			horizontalArrangement = Arrangement.spacedBy(10.dp)
		) {
			NowPlayingStarButton(
				songIsStarred = songIsStarred,
				onSetSongIsStarred = onSetSongIsStarred,
				enabled = !isRadio
			)
			NowPlayingMoreButton(
				songRating = songRating,
				onSetSongRating = onSetSongRating,
				enabled = !isRadio
			)
		}
	}
}
