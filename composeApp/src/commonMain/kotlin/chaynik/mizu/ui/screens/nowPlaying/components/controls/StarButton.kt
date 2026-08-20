package chaynik.mizu.ui.screens.nowPlaying.components.controls

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_star
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Star
import chaynik.mizu.icons.outlined.Star
import chaynik.mizu.shared.MediaPlayerViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun NowPlayingStarButton(
	songIsStarred: Boolean,
	onSetSongIsStarred: (Boolean) -> Unit,
	enabled: Boolean = true
) {
	val player = koinInject<MediaPlayerViewModel>()
	val hasSongFlow = remember(player) {
		player.uiState.map { it.currentSong != null }.distinctUntilChanged()
	}
	val hasSong by hasSongFlow.collectAsState(false)
	IconButton(
		onClick = {
			onSetSongIsStarred(!songIsStarred)
		},
		colors = IconButtonDefaults.filledTonalIconButtonColors(),
		modifier = Modifier.size(32.dp),
		enabled = enabled && hasSong
	) {
		Icon(
			if (songIsStarred) Icons.Filled.Star else Icons.Outlined.Star,
			contentDescription = stringResource(Res.string.action_star)
		)
	}
}
