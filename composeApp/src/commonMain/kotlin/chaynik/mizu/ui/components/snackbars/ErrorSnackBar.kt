package chaynik.mizu.ui.components.snackbars

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_ok
import mizu.composeapp.generated.resources.info_error
import mizu.composeapp.generated.resources.info_error_show
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.LocalSnackBarState
import chaynik.mizu.ui.components.common.ErrorCodeBlock
import chaynik.mizu.ui.components.common.FormButton
import chaynik.mizu.ui.components.dialogs.FormDialog
import chaynik.mizu.util.core.Logger

@Composable
fun ErrorSnackBar(
	error: Throwable?,
	onClearError: () -> Unit
) {
	if (error == null) return

	val snackBarState = LocalSnackBarState.current
	var visible by rememberSaveable { mutableStateOf(false) }

	LaunchedEffect(error) {
		val result = snackBarState.showSnackbar(
			message = getString(Res.string.info_error),
			actionLabel = getString(Res.string.info_error_show),
			duration = SnackbarDuration.Long
		)
		if (result == SnackbarResult.ActionPerformed) {
			visible = true
			Logger.e("ErrorSnackBar", "Printing stack trace for error", error)
		} else {
			onClearError()
		}
	}

	if (!visible) return

	FormDialog(
		onDismissRequest = {
			visible = false
			onClearError()
		},
		buttons = {
			FormButton(onClick = {
				visible = false
				onClearError()
			}) {
				Text(stringResource(Res.string.action_ok))
			}
		}
	) {
		ErrorCodeBlock(error)
	}
}
