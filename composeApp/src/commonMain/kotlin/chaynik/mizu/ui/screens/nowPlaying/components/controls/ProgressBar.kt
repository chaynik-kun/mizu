package chaynik.mizu.ui.screens.nowPlaying.components.controls

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.WaveVelocity
import ir.mahozad.multiplatform.wavyslider.material3.Track
import ir.mahozad.multiplatform.wavyslider.material3.WaveAnimationSpecs
import ir.mahozad.multiplatform.wavyslider.material3.WaveVelocity
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.NowPlayingSliderStyle
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.SlimSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingProgressBar() {
	val preferenceManager = koinInject<PreferenceManager>()
	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsState()
	val enabled = playerState.currentSong?.id?.startsWith("radio_") == false
	val progress = if (enabled) playerState.progress else 0f
	val waveHeight by animateDpAsState(
		if (enabled && !playerState.isPaused)
			6.dp
		else 0.dp
	)

	when (preferenceManager.nowPlayingSliderStyle) {
		NowPlayingSliderStyle.Flat -> {
			Slider(
				value = progress,
				onValueChange = { player.seek(it) },
				modifier = Modifier.padding(horizontal = 16.dp),
				enabled = enabled
			)
		}

		NowPlayingSliderStyle.Squiggly, NowPlayingSliderStyle.Yoyo -> {
			val isYoyo = preferenceManager.nowPlayingSliderStyle == NowPlayingSliderStyle.Yoyo
			WavySlider(
				value = progress,
				onValueChange = { player.seek(it) },
				modifier = Modifier.padding(
					horizontal = if (isYoyo) 7.dp else 14.dp
				),
				waveHeight = waveHeight,
				thumb = {
					SliderDefaults.Thumb(
						enabled = enabled,
						thumbSize = if (isYoyo)
							DpSize(20.dp, 20.dp)
						else DpSize(4.dp, 32.dp),
						interactionSource = remember { MutableInteractionSource() }
					)
				},
				track = { sliderState ->
					SliderDefaults.Track(
						sliderState = sliderState,
						thumbTrackGapSize = if (isYoyo) 0.dp else 6.dp,
						waveLength = if (isYoyo) 32.dp else 26.dp,
						waveHeight = waveHeight,
						animationSpecs = SliderDefaults.WaveAnimationSpecs.copy(
							waveAppearanceAnimationSpec = snap()
						),
						waveVelocity = if (isYoyo)
							WaveVelocity(14.dp, WaveDirection.TAIL)
						else SliderDefaults.WaveVelocity
					)
				},
				enabled = enabled
			)
		}

		NowPlayingSliderStyle.Slim -> {
			SlimSlider(
				value = progress,
				onValueChange = { player.seek(it) },
				modifier = Modifier.padding(horizontal = 16.dp),
				enabled = enabled
			)
		}
	}
}
