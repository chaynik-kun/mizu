package chaynik.mizu.ui.screens.login

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import chaynik.mizu.ui.screens.login.pages.LoginScreenContent

@Composable
fun LoginScreen() {
	Scaffold { innerPadding ->
		LoginScreenContent(
			innerPadding = innerPadding
		)
	}
}
