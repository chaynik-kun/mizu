package chaynik.mizu.ui.screens.radio.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_cancel
import mizu.composeapp.generated.resources.action_ok
import mizu.composeapp.generated.resources.option_radio_homepage_url
import mizu.composeapp.generated.resources.option_radio_name
import mizu.composeapp.generated.resources.option_radio_stream_url
import mizu.composeapp.generated.resources.title_create_radio
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Radio
import chaynik.mizu.ui.components.common.FormButton
import chaynik.mizu.ui.components.dialogs.FormDialog
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.screens.radio.viewmodels.RadioCreateDialogViewModel

@Composable
fun RadioCreateDialog(
	onDismissRequest: () -> Unit,
	onRefresh: () -> Unit
) {
	val viewModel = koinViewModel<RadioCreateDialogViewModel>()
	val state by viewModel.creationState.collectAsState()

	LaunchedEffect(Unit) {
		viewModel.events.collect { event ->
			when (event) {
				is RadioCreateDialogViewModel.Event.Dismiss -> {
					onDismissRequest()
					onRefresh()
				}
			}
		}
	}

	FormDialog(
		onDismissRequest = onDismissRequest,
		icon = { Icon(Icons.Outlined.Radio, null) },
		title = { Text(stringResource(Res.string.title_create_radio)) },
		buttons = {
			FormButton(
				onClick = {
					viewModel.create()
				},
				enabled = state !is UiState.Loading
					&& viewModel.name.text.isNotBlank()
					&& viewModel.streamUrl.text.isNotBlank(),
				color = MaterialTheme.colorScheme.primary
			) {
				if (state !is UiState.Loading) {
					Text(stringResource(Res.string.action_ok))
				} else {
					CircularProgressIndicator(
						modifier = Modifier.size(20.dp)
					)
				}
			}
			FormButton(
				onClick = {
					onDismissRequest()
				},
				enabled = state !is UiState.Loading,
				content = { Text(stringResource(Res.string.action_cancel)) }
			)
		},
		content = {
			(state as? UiState.Error)?.error?.let {
				SelectionContainer {
					Text("$it")
				}
			}
			val fieldModifier = Modifier.height(60.dp)
			val fieldLineLimits = TextFieldLineLimits.SingleLine
			Column(
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				TextField(
					modifier = fieldModifier,
					state = viewModel.name,
					label = { Text("${stringResource(Res.string.option_radio_name)}*") },
					lineLimits = fieldLineLimits
				)
				TextField(
					modifier = fieldModifier,
					state = viewModel.streamUrl,
					label = { Text("${stringResource(Res.string.option_radio_stream_url)}*") },
					lineLimits = fieldLineLimits
				)
				TextField(
					modifier = fieldModifier,
					state = viewModel.homepageUrl,
					label = { Text(stringResource(Res.string.option_radio_homepage_url)) },
					lineLimits = fieldLineLimits
				)
			}
		}
	)
}
