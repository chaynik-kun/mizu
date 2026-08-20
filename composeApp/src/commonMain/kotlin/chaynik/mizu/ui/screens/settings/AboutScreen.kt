package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_app_version
import mizu.composeapp.generated.resources.mizu_logo
import mizu.composeapp.generated.resources.title_about
import mizu.composeapp.generated.resources.title_acknowledgements
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.LocalNavStack
import chaynik.mizu.LocalPlatformContext
import org.koin.compose.koinInject
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.ChevronForward
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.navigation.Screen

@Composable
fun SettingsAboutScreen() {
	@Suppress("DEPRECATION")
	val clipboard = LocalClipboardManager.current
	val backStack = LocalNavStack.current
	val platformContext = LocalPlatformContext.current
	val hideBack = platformContext.sizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
	Scaffold(
		topBar = {
			NestedTopBar(
				{ Text(stringResource(Res.string.title_about)) },
				hideBack = hideBack
			)
		}
	) { innerPadding ->
		Column(
			Modifier
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(top = 16.dp, end = 16.dp, start = 16.dp)
		) {
			Image(
				painter = painterResource(Res.drawable.mizu_logo),
				contentDescription = null,
				modifier = Modifier
					.align(Alignment.CenterHorizontally)
					.padding(vertical = 24.dp)
					.size(96.dp)
			)
			Form {
				SelectionContainer {
					val text = buildString {
						append(stringResource(Res.string.info_app_version, platformContext.appVersion))
					}
					FormRow(onClick = {
						clipboard.setText(AnnotatedString(text))
					}) {
						Text(text)
					}
				}
			}
			Form {
				FormRow(onClick = dropUnlessResumed {
					backStack.add(Screen.Settings.Acknowledgements)
				}) {
					Text(stringResource(Res.string.title_acknowledgements))
					Icon(Icons.Outlined.ChevronForward, null)
				}
			}
		}
	}
}
