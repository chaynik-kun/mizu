package chaynik.mizu.ui.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.ExternalPlaybackManager
import chaynik.mizu.domain.models.*
import mizu.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackTargetSheet(onDismissRequest: () -> Unit) {
	val manager = koinInject<ExternalPlaybackManager>()
	val state by manager.state.collectAsState()
	val scope = rememberCoroutineScope()
	DisposableEffect(Unit) { manager.setDiscoveryActive(true); onDispose { manager.setDiscoveryActive(false) } }
	ModalBottomSheet(onDismissRequest = onDismissRequest) {
		Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
			Text(stringResource(Res.string.title_play_on), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(24.dp, 8.dp, 24.dp, 12.dp))
			TargetRow(PlaybackTarget.Local, state) { scope.launch { manager.connect(PlaybackTarget.Local); onDismissRequest() } }
			TargetGroup("DLNA / UPnP", state.availableTargets.filterIsInstance<PlaybackTarget.Dlna>(), state) { scope.launch { manager.connect(it); onDismissRequest() } }
			if (state.availableTargets.isEmpty()) Text(stringResource(Res.string.info_no_devices_found), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp, 16.dp))
			state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp, 8.dp)) }
		}
	}
}

@Composable private fun TargetGroup(title: String, targets: List<PlaybackTarget>, state: PlaybackTargetState, click: (PlaybackTarget) -> Unit) {
	if (targets.isEmpty()) return
	Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 4.dp))
	targets.forEach { TargetRow(it, state) { click(it) } }
}

@Composable private fun TargetRow(target: PlaybackTarget, state: PlaybackTargetState, click: () -> Unit) {
	val active = state.activeTarget == target
	val name = when (target) { PlaybackTarget.Local -> stringResource(Res.string.info_this_device); is PlaybackTarget.Dlna -> target.name }
	val detail = when { active && state.connectionState == TargetConnectionState.CONNECTING -> stringResource(Res.string.info_connecting); active && state.connectionState == TargetConnectionState.CONNECTED -> stringResource(Res.string.info_connected); target is PlaybackTarget.Dlna -> target.model; else -> null }
	ListItem(
		headlineContent = { Text(name) },
		supportingContent = detail?.let { { Text(it) } },
		leadingContent = { RadioButton(selected = active, onClick = null) },
		modifier = Modifier.clickable(onClick = click)
	)
}
