package chaynik.mizu.ui.screens.nowPlaying.components.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_next_song
import mizu.composeapp.generated.resources.action_pause
import mizu.composeapp.generated.resources.action_play
import mizu.composeapp.generated.resources.action_previous_song
import mizu.composeapp.generated.resources.action_shuffle
import mizu.composeapp.generated.resources.info_repeat_all
import mizu.composeapp.generated.resources.info_repeat_off
import mizu.composeapp.generated.resources.info_repeat_one
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Pause
import chaynik.mizu.icons.filled.Play
import chaynik.mizu.icons.filled.RepeatOn
import chaynik.mizu.icons.filled.RepeatOneOn
import chaynik.mizu.icons.filled.ShuffleOn
import chaynik.mizu.icons.filled.SkipNext
import chaynik.mizu.icons.filled.SkipPrevious
import chaynik.mizu.icons.outlined.Repeat
import chaynik.mizu.icons.outlined.Shuffle
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.playPauseIconPainter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class ButtonsPlayerState(
	val hasSong: Boolean,
	val isRadio: Boolean,
	val isPaused: Boolean,
	val isShuffleEnabled: Boolean,
	val repeatMode: Int,
	val isLoading: Boolean
)

@Composable
fun NowPlayingButtonsRow() {
	val player = koinInject<MediaPlayerViewModel>()
	val buttonsStateFlow = remember(player) {
		player.uiState.map { state ->
			ButtonsPlayerState(
				state.currentSong != null,
				state.currentSong?.id?.startsWith("radio_") == true,
				state.isPaused,
				state.isShuffleEnabled,
				state.repeatMode,
				state.isLoading
			)
		}.distinctUntilChanged()
	}
	val playerState by buttonsStateFlow.collectAsState(
		ButtonsPlayerState(false, false, false, false, 0, false)
	)
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val scale = remember { Animatable(1f) }
	val enabled = playerState.hasSong
	val isRadio = playerState.isRadio
	val haptic = LocalHapticFeedback.current

	val hapticType = if (playerState.isPaused) {
		HapticFeedbackType.ToggleOn
	} else {
		HapticFeedbackType.ToggleOff
	}

	val repeatStateDescription = stringResource(when (playerState.repeatMode) {
		1 -> Res.string.info_repeat_one
		2 -> Res.string.info_repeat_all
		else -> Res.string.info_repeat_off
	})

	LaunchedEffect(isPressed) {
		if (!isPressed) {
			if (scale.value != 1f) {
				scale.animateTo(
					targetValue = 1.2f,
					animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
				)
				scale.animateTo(
					targetValue = 1f,
					animationSpec = spring(
						dampingRatio = Spring.DampingRatioMediumBouncy,
						stiffness = Spring.StiffnessLow
					)
				)
			}
		} else {
			scale.animateTo(0.95f)
		}
	}

	CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Row(
		modifier = Modifier.widthIn(max = 400.dp),
		horizontalArrangement = Arrangement.spacedBy(16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		IconButton(
			modifier = Modifier
				.weight(1f)
				.aspectRatio(1f)
				.semantics {
					toggleableState = ToggleableState(playerState.isShuffleEnabled)
				},
			onClick = {
				player.toggleShuffle()
			},
			enabled = enabled && !isRadio,
		) {
			Icon(
				imageVector = if (playerState.isShuffleEnabled)
					Icons.Filled.ShuffleOn
				else Icons.Outlined.Shuffle,
				contentDescription = stringResource(Res.string.action_shuffle),
				modifier = Modifier.size(24.dp)
			)
		}
		IconButton(
			modifier = Modifier.weight(1f).aspectRatio(1f),
			onClick = {
				player.previous()
			},
			enabled = enabled && !isRadio
		) {
			Icon(
				imageVector = Icons.Filled.SkipPrevious,
				contentDescription = stringResource(Res.string.action_previous_song),
				modifier = Modifier.size(32.dp)
			)
		}
		IconButton(
			modifier = Modifier
				.weight(1.3f)
				.aspectRatio(1f)
				.scale(scale.value)
				.clip(CircleShape)
				.indication(interactionSource, ripple(color = Color.Black)),
			colors = IconButtonDefaults.filledIconButtonColors(),
			onClick = {
				haptic.performHapticFeedback(hapticType)
				player.togglePlay()
			},
			enabled = enabled,
			interactionSource = interactionSource
		) {
			val painter = playPauseIconPainter(playerState.isPaused)
			val description = stringResource(
				if (playerState.isPaused)
					Res.string.action_play
				else Res.string.action_pause
			)
			AnimatedContent(playerState.isLoading) { isBuffering ->
				if (!isBuffering) {
					if (painter != null) {
						Icon(
							painter = painter,
							contentDescription = description,
							modifier = Modifier.size(40.dp)
						)
					} else {
						Icon(
							imageVector = if (playerState.isPaused)
								Icons.Filled.Play
							else Icons.Filled.Pause,
							contentDescription = description,
							modifier = Modifier.size(40.dp)
						)
					}
				} else {
					CircularProgressIndicator(
						Modifier.size(40.dp),
						color = MaterialTheme.colorScheme.onPrimary,
						trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .5f),
					)
				}
			}
		}
		IconButton(
			modifier = Modifier.weight(1f).aspectRatio(1f),
			onClick = {
				player.next()
			},
			enabled = enabled && !isRadio,
		) {
			Icon(
				imageVector = Icons.Filled.SkipNext,
				contentDescription = stringResource(Res.string.action_next_song),
				modifier = Modifier.size(32.dp)
			)
		}
		IconButton(
			modifier = Modifier
				.weight(1f)
				.aspectRatio(1f)
				.semantics {
					stateDescription = repeatStateDescription
				},
			onClick = {
				player.toggleRepeat()
			},
			enabled = enabled && !isRadio,
		) {
			Icon(
				imageVector = when (playerState.repeatMode) {
					1 -> Icons.Filled.RepeatOneOn
					2 -> Icons.Filled.RepeatOn
					else -> Icons.Outlined.Repeat
				},
				contentDescription = null,
				modifier = Modifier.size(24.dp)
			)
		}
	} }
}
