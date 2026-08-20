package chaynik.mizu.ui.screens.nowPlaying.components.rows

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import chaynik.mizu.ui.screens.nowPlaying.components.controls.NowPlayingProgressBar
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import org.koin.compose.koinInject
import androidx.compose.runtime.collectAsState
import chaynik.mizu.domain.manager.ExternalPlaybackManager
import chaynik.mizu.domain.models.PlaybackTarget
import mizu.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun NowPlayingControlsRow(
	modifier: Modifier = Modifier,
	isLandscape: Boolean,
	songIsStarred: Boolean,
	onSetSongIsStarred: (Boolean) -> Unit,
	songRating: Int,
	onSetSongRating: (Int) -> Unit,
	isLyricsActive: Boolean,
	onToggleLyrics: () -> Unit
) {
	val targetState by koinInject<ExternalPlaybackManager>().state.collectAsState()
	var visible by rememberSaveable { mutableStateOf(false) }
	val scale by animateFloatAsState(if (visible) 1f else 0f)
	val offset by animateDpAsState(if (visible) 0.dp else 200.dp)

	LaunchedEffect(Unit) {
		delay(200.milliseconds)
		visible = true
	}

	Column(
		modifier = modifier.scale(scale).offset {
			IntOffset(x = 0, y = offset.roundToPx())
		},
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Column {
			NowPlayingInfoRow(
				songIsStarred = songIsStarred,
				onSetSongIsStarred = onSetSongIsStarred,
				songRating = songRating,
				onSetSongRating = onSetSongRating
			)
			NowPlayingProgressBar()
			NowPlayingDurationsRow()
		}
		if (targetState.activeTarget !is PlaybackTarget.Local) {
			val name = when (val target = targetState.activeTarget) { is PlaybackTarget.Dlna -> target.name; else -> "" }
			Text(stringResource(Res.string.info_playing_on, name), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
		}
		Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 30.dp))
		NowPlayingButtonsRow()
		Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 20.dp))
		NowPlayingActionsRow(
			isLyricsActive = isLyricsActive,
			onToggleLyrics = onToggleLyrics
		)
	}
}
