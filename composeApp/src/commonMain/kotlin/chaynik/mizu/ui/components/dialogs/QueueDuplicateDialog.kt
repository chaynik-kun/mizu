package chaynik.mizu.ui.components.dialogs

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_cancel
import mizu.composeapp.generated.resources.action_ok
import mizu.composeapp.generated.resources.notice_queue_duplicate
import mizu.composeapp.generated.resources.title_confirm
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.PlaylistAdd
import chaynik.mizu.ui.components.common.FormButton

@Composable
fun QueueDuplicateDialog(
	onDismissRequest: () -> Unit,
	onConfirm: () -> Unit
) {
	FormDialog(
		onDismissRequest = onDismissRequest,
		icon = { Icon(Icons.Outlined.PlaylistAdd, contentDescription = null) },
		title = { Text(stringResource(Res.string.title_confirm)) },
		content = { Text(stringResource(Res.string.notice_queue_duplicate)) },
		buttons = {
			FormButton(
				onClick = {
					onConfirm()
					onDismissRequest()
				}
			) {
				Text(stringResource(Res.string.action_ok))
			}
			FormButton(
				onClick = onDismissRequest
			) {
				Text(stringResource(Res.string.action_cancel))
			}
		},
	)
}
