package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_cancel
import mizu.composeapp.generated.resources.action_ok
import mizu.composeapp.generated.resources.action_test_exception_handler
import mizu.composeapp.generated.resources.info_exception_handler
import mizu.composeapp.generated.resources.option_custom_headers
import mizu.composeapp.generated.resources.title_confirm
import mizu.composeapp.generated.resources.title_developer
import mizu.composeapp.generated.resources.title_logs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.LocalPlatformContext
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.ChevronForward
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormButton
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.dialogs.FormDialog
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.util.core.PlatformType

@Composable
fun SettingsDeveloperScreen() {
	val platformContext = LocalPlatformContext.current
	val backStack = LocalNavStack.current
	var exceptionConfirmationShown by rememberSaveable { mutableStateOf(false) }
	val preferenceManager = koinInject<PreferenceManager>()

	Scaffold(
		topBar = {
			NestedTopBar(
				{ Text(stringResource(Res.string.title_developer)) },
				hideBack = platformContext.sizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
			)
		}
	) { innerPadding ->
		CompositionLocalProvider(
			LocalMinimumInteractiveComponentSize provides 0.dp
		) {
			Column(
				Modifier
					.padding(innerPadding)
					.verticalScroll(rememberScrollState())
					.padding(top = 16.dp, end = 16.dp, start = 16.dp)
			) {
				Form {
					FormRow(
						onClick = dropUnlessResumed {
							backStack.lastOrNull()?.let {
								if (it is Screen.Settings.Developer) {
									backStack.add(Screen.Settings.CustomHeaders)
								}
							}
						}
					) {
						Text(stringResource(Res.string.option_custom_headers))
						Icon(Icons.Outlined.ChevronForward, null)
					}
					if (platformContext.platformType == PlatformType.Android) {
						FormRow(
							onClick = dropUnlessResumed {
								backStack.lastOrNull()?.let {
									if (it is Screen.Settings.Developer) {
										backStack.add(Screen.Settings.Logs)
									}
								}
							}
						) {
							Text(stringResource(Res.string.title_logs))
							Icon(Icons.Outlined.ChevronForward, null)
						}
					}
				}
				Form {
					FormRow(onClick = {
						exceptionConfirmationShown = true
					}) {
						Text(
							text = stringResource(Res.string.action_test_exception_handler),
							color = MaterialTheme.colorScheme.error
						)
					}
				}
			}
		}
	}

	if (exceptionConfirmationShown) {
		FormDialog(
			onDismissRequest = { exceptionConfirmationShown = false },
			title = { Text(stringResource(Res.string.title_confirm)) },
			content = { Text(stringResource(Res.string.info_exception_handler)) },
			buttons = {
				FormButton(
					onClick = {
						exceptionConfirmationShown = false
						throw Error("Testing exception handler")
					},
					color = MaterialTheme.colorScheme.error
				) {
					Text(stringResource(Res.string.action_ok))
				}
				FormButton(
					onClick = {
						exceptionConfirmationShown = false
					}
				) {
					Text(stringResource(Res.string.action_cancel))
				}
			},
		)
	}
}
