package chaynik.mizu.ui.screens.share

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_no_shares
import mizu.composeapp.generated.resources.title_shares
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.LocalBottomBarScrollManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.BottomBarVisibilityMode
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.ShareOff
import chaynik.mizu.ui.components.common.ContentUnavailable
import chaynik.mizu.ui.components.dialogs.DeletionDialog
import chaynik.mizu.ui.components.dialogs.DeletionEndpoint
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.components.layouts.PullToRefreshBox
import chaynik.mizu.ui.components.layouts.RootBottomBar
import chaynik.mizu.ui.components.layouts.artGridError
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.screens.share.components.ShareListScreenItem
import chaynik.mizu.ui.screens.share.viewmodels.ShareListViewModel
import chaynik.mizu.util.ui.withoutTop

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareListScreen() {
	val viewModel = koinViewModel<ShareListViewModel>()
	val sharesState by viewModel.sharesState.collectAsState()
	val isRefreshingFlow by viewModel.isRefreshing.collectAsState()
	var deletionId by remember { mutableStateOf<String?>(null) }
	val preferenceManager = koinInject<PreferenceManager>()

	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.title_shares)) }) },
		bottomBar = {
			val scrollManager = LocalBottomBarScrollManager.current
			if (preferenceManager.bottomBarVisibilityMode == BottomBarVisibilityMode.AllScreens) {
				RootBottomBar(scrolled = scrollManager.isTriggered)
			}
		}
	) { contentPadding ->
		PullToRefreshBox(
			modifier = Modifier
				.padding(top = contentPadding.calculateTopPadding())
				.background(MaterialTheme.colorScheme.surface),
			finished = sharesState !is UiState.Loading && !isRefreshingFlow,
			onRefresh = { viewModel.refreshShares() },
			key = listOf(sharesState, isRefreshingFlow)
		) {
			Crossfade(sharesState) { stateValue ->
				LazyVerticalGrid(
					modifier = Modifier.fillMaxSize(),
					columns = GridCells.Fixed(1),
					contentPadding = contentPadding.withoutTop(),
					state = viewModel.gridState,
					verticalArrangement = if ((stateValue as? UiState.Success)?.data?.isEmpty() == true)
						Arrangement.Center
					else Arrangement.Top
				) {
					when (stateValue) {
						is UiState.Loading -> {
							return@LazyVerticalGrid
						}

						is UiState.Error -> artGridError(stateValue)
						is UiState.Success -> {
							items(stateValue.data, { it.id }) { share ->
								ShareListScreenItem(
									modifier = Modifier.animateItem(fadeInSpec = null),
									share = share,
									onSetDeletionId = { newDeletionId ->
										deletionId = newDeletionId
									}
								)
							}
							if (stateValue.data.isEmpty()) {
								item(span = { GridItemSpan(maxLineSpan) }) {
									ContentUnavailable(
										icon = Icons.Filled.ShareOff,
										label = stringResource(Res.string.info_no_shares)
									)
								}
							}
						}
					}
				}
			}
		}
	}

	DeletionDialog(
		endpoint = DeletionEndpoint.SHARE,
		id = deletionId,
		onIdClear = { deletionId = null },
		onRefresh = { viewModel.refreshShares() }
	)
}
