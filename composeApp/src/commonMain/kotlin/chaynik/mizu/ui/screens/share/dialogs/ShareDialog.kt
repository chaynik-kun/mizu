package chaynik.mizu.ui.screens.share.dialogs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_cancel
import mizu.composeapp.generated.resources.action_share
import mizu.composeapp.generated.resources.notice_copied
import mizu.composeapp.generated.resources.notice_expiry
import mizu.composeapp.generated.resources.option_share_expires
import mizu.composeapp.generated.resources.title_create_share
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.LocalSnackBarState
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Share
import chaynik.mizu.ui.components.common.DurationPicker
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormButton
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.dialogs.FormDialog
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.screens.settings.components.SettingSwitchRow
import chaynik.mizu.ui.screens.share.viewmodels.ShareDialogViewModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareDialog(
	id: String?,
	onIdClear: () -> Unit,
	expiry: Duration?,
	onExpiryChange: (expiry: Duration?) -> Unit
) {

	val viewModel = koinViewModel<ShareDialogViewModel>()

	// There is not an elegant cross-platform way of making a ClipEntry yet this is deprecated lmao
	@Suppress("DEPRECATION")
	val clipboard = LocalClipboardManager.current

	val snackBarState = LocalSnackBarState.current
	val state by viewModel.state.collectAsState()

	LaunchedEffect(state) {
		if (state is UiState.Success && id != null) {
			viewModel.viewModelScope.launch {
				val link = (state as? UiState.Success<String?>)?.data
					?: return@launch
				onIdClear()
				clipboard.setText(AnnotatedString(link))
				snackBarState.showSnackbar(
					message = buildString {
						append(getString(Res.string.notice_copied))
						expiry?.let {
							append(
								"\n" + getString(
									Res.string.notice_expiry, expiry.toString()
								)
							)
						}
					}
				)
			}
		}
	}

	id?.let {
		FormDialog(
			icon = { Icon(Icons.Outlined.Share, null) },
			title = { Text(stringResource(Res.string.title_create_share)) },
			buttons = {
				FormButton(
					onClick = { viewModel.share(id, expiry) },
					color = MaterialTheme.colorScheme.primary
				) {
					if (state is UiState.Loading) {
						CircularProgressIndicator(Modifier.size(20.dp))
					}
					Text(stringResource(Res.string.action_share))
				}
				FormButton(onClick = onIdClear) {
					Text(stringResource(Res.string.action_cancel))
				}
			},
			onDismissRequest = {
				if (state !is UiState.Loading) {
					onIdClear()
				}
			}
		) {
			Spacer(Modifier.height(12.dp))
			(state as? UiState.Error)?.error?.let {
				SelectionContainer {
					Text("$it")
				}
			}
			Form(bottomPadding = 0.dp) {
				SettingSwitchRow(
					title = { Text(stringResource(Res.string.option_share_expires)) },
					enabled = state !is UiState.Loading,
					value = expiry != null,
					onSetValue = {
						onExpiryChange(if (it) 1.hours else null)
					},
					contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
				)
				expiry?.let {
					FormRow {
						DurationPicker(
							duration = expiry,
							onDurationChange = onExpiryChange,
							enabled = state !is UiState.Loading,
						)
					}
				}
			}
		}
	}
}
