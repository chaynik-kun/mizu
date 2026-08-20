package chaynik.mizu.ui.screens.nowPlaying.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.ErrorBox
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.PersistentViewModelStoreOwner
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenContent
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenLoadingView
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenPlaceholder
import chaynik.mizu.ui.screens.lyrics.viewmodels.LyricsScreenViewModel
import kotlin.time.Duration.Companion.milliseconds

/**
 * Inline lyrics shown in place of the artwork on the Now Playing screen.
 *
 * Reuses [LyricsScreenViewModel] and [LyricsScreenContent] but drops the
 * selection/share affordances, keeping the playback controls visible below.
 * The full-screen lyrics experience still lives in
 * [chaynik.mizu.ui.screens.lyrics.LyricsScreen].
 */
@Composable
fun NowPlayingLyrics(
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues()
) {
	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsStateWithLifecycle()
	val song = playerState.currentSong

	Box(
		modifier
			.fillMaxSize()
			.graphicsLayer {
				compositingStrategy = CompositingStrategy.Offscreen
			}
			.drawWithContent {
				drawContent()
				drawRect(
					brush = Brush.verticalGradient(
						0f to Color.Transparent,
						0.10f to Color.Black,
						0.90f to Color.Black,
						1f to Color.Transparent
					),
					blendMode = BlendMode.DstIn
				)
			},
		contentAlignment = Alignment.Center
	) {
		if (song == null) {
			LyricsScreenPlaceholder(onRefresh = {})
			return@Box
		}

		val viewModel = koinViewModel<LyricsScreenViewModel>(
			viewModelStoreOwner = koinInject<PersistentViewModelStoreOwner>()
		)
		LaunchedEffect(song.id) { viewModel.loadLyrics(song) }
		val lyricsLoadState by viewModel.lyricsState.collectAsStateWithLifecycle()
		val lyricsState = if (lyricsLoadState.songId == song.id) {
			lyricsLoadState.content
		} else {
			UiState.Loading()
		}

		val duration = song.duration
		val currentDuration = playerState.currentPositionMs.milliseconds

		when (val state = lyricsState) {
			is UiState.Error -> ErrorBox(
				error = state,
				modifier = Modifier.wrapContentSize(),
				onRetry = { viewModel.refreshResults() }
			)

			is UiState.Loading -> LyricsScreenLoadingView()
			is UiState.Success -> key(song.id) {
				LyricsScreenContent(
					data = state.data,
					onRefresh = { viewModel.refreshResults() },
					isSelecting = false,
					songId = song.id,
					selectedIndices = persistentListOf(),
					onAddSelectedIndex = {},
					onRemoveSelectedIndex = {},
					onRestartAtIndex = {},
					duration = duration,
					currentDuration = currentDuration,
					contentPadding = contentPadding
				)
			}
		}
	}
}
