package chaynik.mizu.ui.components.snackbars

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MizuSnackBar(
	snackBarData: SnackbarData,
	modifier: Modifier = Modifier
) {
	Snackbar(
		modifier = modifier,
		snackbarData = snackBarData,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
		contentColor = MaterialTheme.colorScheme.onSurface,
		actionColor = MaterialTheme.colorScheme.primary,
		actionContentColor = MaterialTheme.colorScheme.primary
	)
}
