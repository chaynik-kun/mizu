package chaynik.mizu.ui.screens.nowPlaying.components.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_lyrics
import mizu.composeapp.generated.resources.action_queue
import mizu.composeapp.generated.resources.action_sleep_timer
import mizu.composeapp.generated.resources.title_equalizer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Bedtime
import chaynik.mizu.icons.outlined.List
import chaynik.mizu.icons.outlined.Lyrics
import chaynik.mizu.icons.outlined.OutputDevice
import chaynik.mizu.icons.outlined.Equalizer
import chaynik.mizu.domain.manager.ExternalPlaybackManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.PlaybackTarget
import chaynik.mizu.domain.models.settings.NowPlayingActionVisibility
import chaynik.mizu.ui.components.sheets.PlaybackTargetSheet
import chaynik.mizu.ui.screens.settings.EqualizerSheet
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.sheets.SleepTimerSheet
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.theme.MizuTheme
import chaynik.mizu.util.ui.rememberColorSchemeFromCoverArt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private data class ActionsPlayerState(val songId: String?, val coverArtId: String?, val isRadio: Boolean)

/**
 * Bottom action bar shown under the transport controls on the Now Playing
 * screen: lyrics, equalizer, output device, sleep timer and queue.
 */
@Composable
fun NowPlayingActionsRow(
	modifier: Modifier = Modifier,
	isLyricsActive: Boolean,
	onToggleLyrics: () -> Unit
) {
	val backStack = LocalNavStack.current
	val player = koinInject<MediaPlayerViewModel>()
	val actionsStateFlow = remember(player) {
		player.uiState.map { state ->
			ActionsPlayerState(
				state.currentSong?.id,
				state.currentSong?.coverArtId,
				state.currentSong?.id?.startsWith("radio_") == true
			)
		}.distinctUntilChanged()
	}
	val actionsState by actionsStateFlow.collectAsState(
		ActionsPlayerState(null, null, false)
	)
	val hasSong = actionsState.songId != null
	val isRadio = actionsState.isRadio
	var activeSheet by rememberSaveable { mutableStateOf<NowPlayingSheet?>(null) }
	val externalPlaybackManager = koinInject<ExternalPlaybackManager>()
	val targetState by externalPlaybackManager.state.collectAsState()
	val colorScheme = rememberColorSchemeFromCoverArt(actionsState.coverArtId)
	val preferences = koinInject<PreferenceManager>()
	val sheetColorScheme = if (preferences.dynamicTheming) MaterialTheme.colorScheme else colorScheme
	val visibilityFlow = remember(preferences) {
		combine(
			preferences.showNowPlayingLyricsFlow,
			preferences.showNowPlayingEqualizerFlow,
			preferences.showNowPlayingOutputFlow,
			preferences.showNowPlayingSleepTimerFlow,
			preferences.showNowPlayingQueueFlow
		) { lyrics, equalizer, output, sleep, queue ->
			NowPlayingActionVisibility(lyrics, equalizer, output, sleep, queue)
		}.distinctUntilChanged()
	}
	val visibility by visibilityFlow.collectAsState(NowPlayingActionVisibility())

	if (visibility.anyVisible) Row(
		modifier = modifier.widthIn(max = 400.dp).fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
		verticalAlignment = Alignment.CenterVertically
	) {
		if (visibility.lyrics)
		IconButton(onClick = onToggleLyrics, enabled = hasSong && !isRadio) {
			Icon(
				imageVector = Icons.Outlined.Lyrics,
				contentDescription = stringResource(Res.string.action_lyrics),
				tint = if (isLyricsActive) MaterialTheme.colorScheme.primary
					else LocalContentColor.current,
				modifier = Modifier.size(24.dp)
			)
		}
		if (visibility.equalizer)
		IconButton(
			onClick = { activeSheet = NowPlayingSheet.Equalizer }, enabled = hasSong
		) {
			Icon(Icons.Outlined.Equalizer, contentDescription = stringResource(Res.string.title_equalizer), modifier = Modifier.size(24.dp))
		}
		if (visibility.outputDevices)
		IconButton(
			onClick = { activeSheet = NowPlayingSheet.PlaybackTarget }, enabled = hasSong
		) {
			val deviceName = when (val target = targetState.activeTarget) { is PlaybackTarget.Dlna -> target.name; else -> null }
			Icon(Icons.Outlined.OutputDevice, contentDescription = deviceName?.let { "Воспроизведение на $it" } ?: "Устройства воспроизведения",
				tint = if (targetState.activeTarget !is PlaybackTarget.Local) MaterialTheme.colorScheme.primary else LocalContentColor.current,
				modifier = Modifier.size(24.dp))
		}
		if (visibility.sleepTimer)
		IconButton(
			onClick = { activeSheet = NowPlayingSheet.SleepTimer },
			enabled = hasSong
		) {
			Icon(
				imageVector = Icons.Outlined.Bedtime,
				contentDescription = stringResource(Res.string.action_sleep_timer),
				modifier = Modifier.size(24.dp)
			)
		}
		if (visibility.queue)
		IconButton(
			onClick = dropUnlessResumed { backStack.add(Screen.Queue) },
			enabled = hasSong && !isRadio
		) {
			Icon(
				imageVector = Icons.Outlined.List,
				contentDescription = stringResource(Res.string.action_queue),
				modifier = Modifier.size(24.dp)
			)
		}
	}

	when (activeSheet) {
		NowPlayingSheet.Equalizer -> MizuTheme(sheetColorScheme) {
			EqualizerSheet(onDismissRequest = { activeSheet = null })
		}
		NowPlayingSheet.SleepTimer -> {
		MizuTheme(sheetColorScheme) {
			SleepTimerSheet(
				onDismissRequest = { activeSheet = null }
			)
		}
		}
		NowPlayingSheet.PlaybackTarget -> PlaybackTargetSheet { activeSheet = null }
		null -> Unit
	}
}

enum class NowPlayingSheet { Equalizer, SleepTimer, PlaybackTarget }

data class NowPlayingSheetState(val activeSheet: NowPlayingSheet? = null) {
	fun open(sheet: NowPlayingSheet) = copy(activeSheet = sheet)
	fun dismiss() = copy(activeSheet = null)
}
