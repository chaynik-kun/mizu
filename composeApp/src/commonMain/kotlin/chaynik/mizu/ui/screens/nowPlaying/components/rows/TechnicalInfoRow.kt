package chaynik.mizu.ui.screens.nowPlaying.components.rows

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.shared.MediaPlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class TechnicalPlayerState(
	val sampleRate: Int?, val bitrate: Int?, val mimeType: String?,
	val songSampleRate: Int?, val songBitrate: Int?, val extension: String?
)

/**
 * Plain-text track technical info (format, sample rate, bitrate) shown centered
 * between the elapsed/total times, matching the Now Playing reference layout.
 */
@Composable
fun NowPlayingTechnicalInfo(
	modifier: Modifier = Modifier,
	style: TextStyle = LocalTextStyle.current,
	color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
	val preferenceManager = koinInject<PreferenceManager>()
	val connectivityManager = koinInject<ConnectivityManager>()
	val player = koinInject<MediaPlayerViewModel>()
	val technicalStateFlow = remember(player) {
		player.uiState.map { state ->
			TechnicalPlayerState(
				state.playbackSampleRate, state.playbackBitrate, state.playbackMimeType,
				state.currentSong?.sampleRate, state.currentSong?.bitRate,
				state.currentSong?.fileExtension
			)
		}.distinctUntilChanged()
	}
	val playerState by technicalStateFlow.collectAsState(
		TechnicalPlayerState(null, null, null, null, null, null)
	)

	val sampleRateFormatted =
		(playerState.sampleRate ?: playerState.songSampleRate)?.let {
			if (it >= 1000) "${it / 1000.0} kHz" else "$it Hz"
		} ?: "-- kHz"

	val isCellular = connectivityManager.isCellular.value
	val requestedBitrate = if (preferenceManager.isAdvancedTranscodingActive) {
		if (isCellular) preferenceManager.customMaxBitrateCellular else preferenceManager.customMaxBitrateWifi
	} else {
		if (isCellular) preferenceManager.streamingQualityCellular.bitrateAndroid else preferenceManager.streamingQualityWifi.bitrateAndroid
	}

	val bitrateFormatted = playerState.bitrate?.let { "${it / 1000} kbps" }
		?: if (requestedBitrate > 0) {
			"$requestedBitrate kbps"
		} else {
			playerState.songBitrate?.let { "$it kbps" }
		} ?: "-- kbps"

	val format =
		playerState.mimeType?.split("/")?.lastOrNull()?.replace("mpeg", "mp3")
			?.uppercase()
			?: playerState.extension?.uppercase()
			?: "--"

	Text(
		modifier = modifier,
		text = "$format • $sampleRateFormatted • $bitrateFormatted",
		style = style.copy(textDirection = TextDirection.Ltr),
		color = color,
		maxLines = 1,
		textAlign = TextAlign.Center,
		overflow = TextOverflow.Ellipsis
	)
}
