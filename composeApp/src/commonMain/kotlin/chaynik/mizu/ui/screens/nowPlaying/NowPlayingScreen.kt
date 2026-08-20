package chaynik.mizu.ui.screens.nowPlaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.NowPlayingBackgroundStyle
import chaynik.mizu.domain.models.settings.ToolbarPosition
import chaynik.mizu.icons.Icons
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.BlendBackground
import chaynik.mizu.ui.components.layouts.SheetScaffold
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.nowPlaying.components.NowPlayingLyrics
import chaynik.mizu.ui.screens.nowPlaying.components.controls.NowPlayingArtworkPager
import chaynik.mizu.ui.screens.nowPlaying.components.rows.NowPlayingControlsRow
import chaynik.mizu.ui.screens.nowPlaying.viewmodels.NowPlayingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowPlayingScreen() {
	val preferenceManager = koinInject<PreferenceManager>()
	val player = koinInject<MediaPlayerViewModel>()
	val backStack = LocalNavStack.current

	val currentScreen = backStack.lastOrNull()
	val isPlayerCurrent = currentScreen is Screen.NowPlaying
		|| currentScreen is Screen.Queue
		|| currentScreen is Screen.PlaybackSpeed
		|| currentScreen is Screen.SongDetailSheet

	val viewModel = koinViewModel<NowPlayingViewModel> { parametersOf(player) }
	val songIsStarred by viewModel.songIsStarred.collectAsStateWithLifecycle()
	val songRating by viewModel.songRating.collectAsStateWithLifecycle()

	// When full-screen lyrics are disabled, lyrics are shown inline over the
	// artwork; otherwise the standalone lyrics screen is pushed onto the stack.
	val lyricsFullScreen = preferenceManager.lyricsFullScreen
	var showLyrics by rememberSaveable { mutableStateOf(false) }
	val showLyricsAction: () -> Unit = {
		if (lyricsFullScreen) backStack.add(Screen.Lyrics) else showLyrics = true
	}
	val toggleLyricsAction = dropUnlessResumed {
		if (lyricsFullScreen) backStack.add(Screen.Lyrics) else showLyrics = !showLyrics
	}

	SheetScaffold(
		toolbarPosition = ToolbarPosition.Top,
		toolbar = { windowInsets ->
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.windowInsetsPadding(windowInsets)
					.padding(vertical = 12.dp)
					.alpha(if (isPlayerCurrent) 1f else 0f),
				contentAlignment = Alignment.Center
			) {
				Box(
					modifier = Modifier
						.size(width = 32.dp, height = 4.dp)
						.clip(CircleShape)
						.background(
							MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
						)
				)
			}
		}
	) { contentPadding ->
		Box(Modifier.fillMaxSize()) {
			if (preferenceManager.nowPlayingBackgroundStyle
				== NowPlayingBackgroundStyle.Dynamic
			) {
				BlendBackground(
				)
			}
			if (!isPlayerCurrent) return@Box
			BoxWithConstraints(
				modifier = Modifier
					.padding(horizontal = 8.dp)
					.fillMaxSize()
			) {
				val isLandscape = maxWidth > maxHeight
				val padding = contentPadding
				if (isLandscape) {
					Row(
						modifier = Modifier.fillMaxSize().padding(padding),
						horizontalArrangement = Arrangement.SpaceEvenly,
						verticalAlignment = Alignment.CenterVertically
					) {
						if (showLyrics && !lyricsFullScreen) {
							NowPlayingLyrics(
								modifier = Modifier.weight(1f).fillMaxHeight()
							)
						} else {
							NowPlayingArtworkPager(
								modifier = Modifier.weight(1f).fillMaxHeight(),
								isLandscape = true,
								onShowLyrics = showLyricsAction
							)
						}
						NowPlayingControlsRow(
							modifier = Modifier.weight(1f).fillMaxHeight(),
							isLandscape = true,
							songIsStarred = songIsStarred,
							onSetSongIsStarred = { viewModel.starSong(it) },
							songRating = songRating,
							onSetSongRating = { viewModel.rateSong(it) },
							isLyricsActive = showLyrics && !lyricsFullScreen,
							onToggleLyrics = toggleLyricsAction
						)
					}
				} else {
					Column(
						modifier = Modifier.fillMaxSize().padding(padding),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center
					) {
						if (showLyrics && !lyricsFullScreen) {
							NowPlayingLyrics(
								modifier = Modifier.weight(1f).fillMaxWidth()
							)
						} else {
							NowPlayingArtworkPager(
								modifier = Modifier.weight(1f).fillMaxWidth(),
								isLandscape = false,
								onShowLyrics = showLyricsAction
							)
						}
						NowPlayingControlsRow(
							modifier = Modifier.weight(1f),
							isLandscape = false,
							songIsStarred = songIsStarred,
							onSetSongIsStarred = { viewModel.starSong(it) },
							songRating = songRating,
							onSetSongRating = { viewModel.rateSong(it) },
							isLyricsActive = showLyrics && !lyricsFullScreen,
							onToggleLyrics = toggleLyricsAction
						)
					}
				}
			}
		}
	}
}
