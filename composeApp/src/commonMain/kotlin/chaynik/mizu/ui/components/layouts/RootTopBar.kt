package chaynik.mizu.ui.components.layouts

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_account
import mizu.composeapp.generated.resources.title_search
import mizu.composeapp.generated.resources.title_settings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.models.settings.NavbarConfig
import chaynik.mizu.domain.models.settings.NavbarTab
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Settings
import chaynik.mizu.icons.outlined.Globe
import chaynik.mizu.icons.outlined.Offline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import chaynik.mizu.domain.models.ConnectionDisplayState
import chaynik.mizu.domain.models.connectionDisplayState
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.icons.outlined.Search
import chaynik.mizu.ui.components.common.TooltipBox
import chaynik.mizu.ui.components.sheets.AccountSheet
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.settings.viewmodels.NavtabsViewModel

@OptIn(
	ExperimentalMaterial3Api::class,
	ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun RootTopBar(
	title: @Composable () -> Unit,
	scrollBehavior: TopAppBarScrollBehavior,
	actions: @Composable RowScope.() -> Unit = {},
) {
	val navViewModel = koinViewModel<NavtabsViewModel>()
	val navState by navViewModel.state.collectAsState()
	val navConfig = (navState as? UiState.Success)?.data

	MediumFlexibleTopAppBar(
		title = {
			CompositionLocalProvider(
				LocalTextStyle provides when (LocalTextStyle.current) {
					MaterialTheme.typography.headlineMedium -> MaterialTheme.typography.headlineSmall
					else -> MaterialTheme.typography.titleLarge
				}
			) {
				title()
			}
		},
		actions = {
			actions()
			Actions(navConfig = navConfig)
		},
		scrollBehavior = scrollBehavior,
		colors = TopAppBarDefaults.topAppBarColors(
			scrolledContainerColor = MaterialTheme.colorScheme.surface
		),
	)
}

@Composable
private fun Actions(
	navConfig: NavbarConfig?,
) {
	val backStack = LocalNavStack.current
	val connectivityManager = koinInject<ConnectivityManager>()
	val networkState by connectivityManager.networkState.collectAsStateWithLifecycle()
	val serverState by connectivityManager.serverState.collectAsStateWithLifecycle()
	val connectionState = connectionDisplayState(networkState, serverState)

	val isSearchEnabled = navConfig?.tabs?.any {
		it.id == NavbarTab.Id.SEARCH && it.visible
	} == true

	var accountSheetOpen by rememberSaveable { mutableStateOf(false) }

	if (!isSearchEnabled) {
		TooltipBox(stringResource(Res.string.title_search)) {
			IconButton(
				onClick = dropUnlessResumed {
					backStack.add(Screen.Search(nested = true))
				}
			) {
				Icon(
					imageVector = Icons.Outlined.Search,
					contentDescription = stringResource(Res.string.title_search)
				)
			}
		}
	}

	TooltipBox(stringResource(Res.string.title_settings)) {
		IconButton(onClick = dropUnlessResumed {
			backStack.add(Screen.Settings.Root)
		}) {
			Icon(
				imageVector = Icons.Filled.Settings,
				contentDescription = stringResource(Res.string.title_settings)
			)
		}
	}

	TooltipBox(stringResource(Res.string.title_account)) {
		IconButton(onClick = {
			accountSheetOpen = true
		}) {
			if (connectionState == ConnectionDisplayState.CHECKING) {
				CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
			} else Icon(
				imageVector = if (connectionState == ConnectionDisplayState.ONLINE) Icons.Outlined.Globe else Icons.Outlined.Offline,
				contentDescription = when (connectionState) {
					ConnectionDisplayState.ONLINE -> "Сервер доступен"
					ConnectionDisplayState.OFFLINE -> "Сервер недоступен"
					ConnectionDisplayState.ERROR -> "Ошибка подключения"
					ConnectionDisplayState.CHECKING -> "Проверка подключения"
				}
			)
		}
	}

	if (accountSheetOpen) {
		AccountSheet(onDismissRequest = { accountSheetOpen = false })
	}
}
