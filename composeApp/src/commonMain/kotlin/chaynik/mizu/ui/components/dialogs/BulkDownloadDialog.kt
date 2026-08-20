package chaynik.mizu.ui.components.dialogs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_cancel
import mizu.composeapp.generated.resources.action_download
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Download
import chaynik.mizu.ui.components.common.FormButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkDownloadDialog(
	title: String,
	message: String,
	showDialog: Boolean,
	onDismissRequest: () -> Unit,
	onConfirm: () -> Unit
) {
	if (showDialog) {
		FormDialog(
			onDismissRequest = onDismissRequest,
			icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
			title = { Text(title) },
			buttons = {
				FormButton(
					onClick = {
						onConfirm()
						onDismissRequest()
					},
					color = MaterialTheme.colorScheme.primary
				) {
					Text(stringResource(Res.string.action_download))
				}
				FormButton(onClick = onDismissRequest) {
					Text(stringResource(Res.string.action_cancel))
				}
			},
			content = {
				Text(text = message)
			}
		)
	}
}
