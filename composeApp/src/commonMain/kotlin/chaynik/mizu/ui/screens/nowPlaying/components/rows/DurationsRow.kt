package chaynik.mizu.ui.screens.nowPlaying.components.rows

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.util.core.toHoursMinutesSeconds
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun NowPlayingDurationsRow() {
	val preferenceManager = koinInject<PreferenceManager>()
	val player = koinInject<MediaPlayerViewModel>()
	val playerState by player.uiState.collectAsState()
	val duration = playerState.currentSong?.duration
	val style = MaterialTheme.typography.bodyMedium
		.copy(
			shadow = Shadow(
				color = MaterialTheme.colorScheme.inverseOnSurface,
				offset = Offset(0f, 4f),
				blurRadius = 10f
			)
		)
	val color = MaterialTheme.colorScheme.onSurfaceVariant

	val (start, end) = when {
		duration == kotlin.time.Duration.ZERO -> "LIVE" to "∞"
		duration != null -> playerState.currentPositionMs.milliseconds
			.coerceIn(kotlin.time.Duration.ZERO, duration)
			.toHoursMinutesSeconds() to duration.toHoursMinutesSeconds()

		else -> "--:--" to "--:--"
	}

	Row(
		modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = start,
			modifier = Modifier.width(64.dp),
			color = color,
			style = style.copy(fontFeatureSettings = "tnum"),
			textAlign = TextAlign.Start,
			maxLines = 1
		)
		if (preferenceManager.nowPlayingSongInfo) {
			NowPlayingTechnicalInfo(
				modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
				style = style.copy(
					fontSize = MaterialTheme.typography.bodySmall.fontSize
				),
				color = color
			)
		} else androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
		Text(
			text = end,
			modifier = Modifier.width(64.dp),
			color = color,
			style = style.copy(fontFeatureSettings = "tnum"),
			textAlign = TextAlign.End,
			maxLines = 1
		)
	}
}
