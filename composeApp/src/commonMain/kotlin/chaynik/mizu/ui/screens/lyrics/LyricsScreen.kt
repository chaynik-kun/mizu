package chaynik.mizu.ui.screens.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.settings.ToolbarPosition
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.ErrorBox
import chaynik.mizu.ui.components.common.KeepScreenOn
import chaynik.mizu.ui.components.layouts.SheetScaffold
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.PersistentViewModelStoreOwner
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenContent
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenLoadingView
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenPlaceholder
import chaynik.mizu.ui.screens.lyrics.components.LyricsScreenToolbar
import chaynik.mizu.ui.screens.lyrics.dialogs.LyricsShareSheet
import chaynik.mizu.ui.screens.lyrics.viewmodels.LyricsScreenViewModel
import chaynik.mizu.util.ui.LocalSheetState

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
	song: DomainSong?
) {
	val backStack = LocalNavStack.current

	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsStateWithLifecycle()

	val viewModel = koinViewModel<LyricsScreenViewModel>(
		viewModelStoreOwner = koinInject<PersistentViewModelStoreOwner>()
	)
	LaunchedEffect(song?.id) { viewModel.loadLyrics(song) }
	val lyricsLoadState by viewModel.lyricsState.collectAsStateWithLifecycle()
	val lyricsState = if (lyricsLoadState.songId == song?.id) {
		lyricsLoadState.content
	} else {
		UiState.Loading()
	}

	var isSelecting by rememberSaveable(song?.id) { mutableStateOf(false) }
	val selectedIndices = rememberSaveable(song?.id) { mutableStateListOf<Int>() }
	var wasPlayingBeforeSelection by rememberSaveable(song?.id) { mutableStateOf(false) }
	var shareSheetOpen by rememberSaveable { mutableStateOf(false) }

	val song = song ?: return LyricsScreenPlaceholder(
		onRefresh = { viewModel.refreshResults() }
	)
	val duration = song.duration
	val currentDuration = playerState.currentPositionMs.milliseconds

	val spatialSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
	val effectSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

	val toggleIsSelecting = {
		if (isSelecting) {
			isSelecting = false
			selectedIndices.clear()
			if (wasPlayingBeforeSelection) {
				player.resume()
			}
		} else {
			wasPlayingBeforeSelection = !playerState.isPaused
			player.pause()
			isSelecting = true
		}
	}

	val sheetState = LocalSheetState.current
	val closeScope = rememberCoroutineScope()
	val animateToDismiss = {
		closeScope.launch {
			sheetState.hide()
		}.invokeOnCompletion {
			if (!sheetState.isVisible) {
				backStack.remove(Screen.Lyrics)
			}
		}
	}

	val preferenceManager = koinInject<PreferenceManager>()
	if (preferenceManager.lyricsKeepAlive) {
		KeepScreenOn()
	}

	SheetScaffold(
		toolbar = { windowInsets ->
			LyricsScreenToolbar(
				onDismissRequest = { animateToDismiss() },
				onShare = { shareSheetOpen = true },
				isSelecting = isSelecting,
				toggleIsSelecting = toggleIsSelecting,
				windowInsets = windowInsets,
				selectedIndices = selectedIndices.toImmutableList()
			)
		},
		toolbarPosition = ToolbarPosition.Top
	) { contentPadding ->
		AnimatedContent(
			targetState = lyricsState,
			modifier = Modifier.fillMaxSize(),
			transitionSpec = {
				ContentTransform(
					targetContentEnter = fadeIn(effectSpec) + scaleIn(spatialSpec, 0.8f),
					initialContentExit = fadeOut(effectSpec) + scaleOut(spatialSpec)
				)
			},
		) { lyricsState ->
			when (lyricsState) {
				is UiState.Error -> ErrorBox(
					error = lyricsState,
					modifier = Modifier.wrapContentSize(),
					onRetry = { viewModel.refreshResults() }
				)

				is UiState.Loading -> LyricsScreenLoadingView()
				is UiState.Success -> {
					key(song.id) {
						LyricsScreenContent(
							data = lyricsState.data,
							onRefresh = { viewModel.refreshResults() },
							isSelecting = isSelecting,
							songId = song.id,
							selectedIndices = selectedIndices.toImmutableList(),
							onAddSelectedIndex = { idx -> selectedIndices.add(idx) },
							onRemoveSelectedIndex = { idx -> selectedIndices.remove(idx) },
							onRestartAtIndex = { idx ->
								selectedIndices.clear()
								selectedIndices.add(idx)
							},
							duration = duration,
							currentDuration = currentDuration,
							contentPadding = contentPadding
						)
					}
				}
			}
		}

		if (shareSheetOpen) {
			val lyricsList = lyricsState.data?.lines?.map { line ->
				(line.time?.inWholeMilliseconds ?: 0L) to line.text
			}

			if (lyricsList != null) {
				val sortedIndices = selectedIndices.sorted()
				val stringsToShare = sortedIndices.mapNotNull { index ->
					lyricsList.getOrNull(index)?.second
				}.toImmutableList()

				LyricsShareSheet(
					song = song,
					selectedLyrics = stringsToShare,
					onDismiss = { shareSheetOpen = false },
					onShare = {
						shareSheetOpen = false
						isSelecting = false
						selectedIndices.clear()
					}
				)
			}
		}
	}
}
