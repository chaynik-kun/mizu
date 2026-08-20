package chaynik.mizu.ui.screens.nowPlaying.components.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.CoverArtTapAction
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.screens.nowPlaying.components.NowPlayingArtwork
import kotlin.time.Duration.Companion.milliseconds

private data class ArtworkPagerPlayerState(
	val queue: List<chaynik.mizu.domain.models.DomainSong>,
	val currentIndex: Int,
	val isPaused: Boolean
)

@Composable
fun NowPlayingArtworkPager(
	modifier: Modifier = Modifier,
	isLandscape: Boolean,
	onShowLyrics: () -> Unit
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val player = koinInject<MediaPlayerViewModel>()
	val pagerPlayerState = remember(player) {
		player.uiState.map {
			ArtworkPagerPlayerState(it.queue, it.currentIndex, it.isPaused)
		}.distinctUntilChanged()
	}
	val playerState by pagerPlayerState.collectAsState(
		player.uiState.value.let {
			ArtworkPagerPlayerState(it.queue, it.currentIndex, it.isPaused)
		}
	)

	val pagerState = rememberPagerState(
		initialPage = playerState.currentIndex.coerceAtLeast(0),
		pageCount = { playerState.queue.size }
	)
	val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
	var userSwiped by remember { mutableStateOf(false) }

	LaunchedEffect(isDragged) {
		if (isDragged) userSwiped = true
	}

	var visible by rememberSaveable { mutableStateOf(false) }
	val scale by animateFloatAsState(if (visible) 1f else 0f)
	val offset by animateDpAsState(if (visible) 0.dp else 200.dp)

	LaunchedEffect(Unit) {
		delay(50.milliseconds)
		visible = true
	}

	LaunchedEffect(playerState.currentIndex, playerState.queue.size, isDragged) {
		val targetPage = playerState.currentIndex
		if (
			!isDragged &&
			targetPage in playerState.queue.indices &&
			targetPage != pagerState.currentPage
		) {
			// Pager may receive the non-empty queue before its first page has been
			// measured. animateScrollToPage() assumes a visible item exists and throws
			// when called in that short window. Wait for layout and jump directly;
			// user-driven swipes remain animated by the pager itself.
			snapshotFlow { pagerState.layoutInfo.visiblePagesInfo.isNotEmpty() }
				.first { it }
			if (targetPage in 0 until pagerState.pageCount) {
				pagerState.scrollToPage(targetPage)
			}
		}
	}

	LaunchedEffect(pagerState) {
		snapshotFlow { pagerState.settledPage }.collect { page ->
			if (userSwiped && page != playerState.currentIndex && page in playerState.queue.indices) {
				val wasPaused = playerState.isPaused
				player.playAt(page)
				if (wasPaused) {
					player.pause()
				}
			}
			userSwiped = false
		}
	}

	HorizontalPager(
		modifier = modifier.scale(scale).offset {
			IntOffset(x = 0, y = offset.roundToPx())
		},
		state = pagerState,
		contentPadding = PaddingValues(horizontal = if (isLandscape) 0.dp else 8.dp),
		userScrollEnabled = preferenceManager.swipeToSkip,
		overscrollEffect = null
	) { page ->
		val song = playerState.queue.getOrNull(page) ?: return@HorizontalPager
		val tapAction = preferenceManager.nowPlayingCoverArtAction
		val enabled = pagerState.settledPage == page
			&& tapAction != CoverArtTapAction.Disabled
		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.Center
		) {
			NowPlayingArtwork(
				song = song,
				isLandscape = isLandscape,
				isCurrent = playerState.currentIndex == page,
				onClick = if (enabled) dropUnlessResumed {
					when (tapAction) {
						CoverArtTapAction.ShowLyrics -> onShowLyrics()
						CoverArtTapAction.Disabled -> {}
					}
				} else null
			)
		}
	}
}
