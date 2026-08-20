package chaynik.mizu.ui.screens.nowPlaying.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Note
import chaynik.mizu.icons.outlined.Radio
import chaynik.mizu.ui.components.common.CoverArt

@Composable
fun NowPlayingArtwork(
	modifier: Modifier = Modifier,
	onClick: (() -> Unit)?,
	isLandscape: Boolean,
	isCurrent: Boolean,
	song: DomainSong
) {
	val isRadio = song.id.startsWith("radio_")

	val padding = if (!isCurrent) 40.dp else 8.dp
	Box(
		contentAlignment = Alignment.Center,
		modifier = modifier
	) {
		CoverArt(
			coverArtId = song.coverArtId,
			modifier = Modifier
				.aspectRatio(1f)
				.then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.fillMaxSize())
				.padding(padding)
				.offset(y = if (isLandscape) 0.dp else 12.dp),
			crossfadeMs = 150,
			shadowElevation = 4.dp,
			onClick = onClick
		)
		if (song.coverArtId.isNullOrEmpty()) {
			Icon(
				imageVector = if (isRadio) Icons.Outlined.Radio else Icons.Filled.Note,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f),
				modifier = Modifier.size(96.dp)
			)
		}
	}
}
