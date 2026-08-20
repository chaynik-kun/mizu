package chaynik.mizu.ui.screens.share.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_delete
import mizu.composeapp.generated.resources.action_share
import mizu.composeapp.generated.resources.info_error
import mizu.composeapp.generated.resources.info_share_expired
import mizu.composeapp.generated.resources.info_share_expires_in
import mizu.composeapp.generated.resources.info_shared_by
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalSnackBarState
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.ShareManager
import chaynik.mizu.domain.models.DomainShare
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Delete
import chaynik.mizu.icons.outlined.Share
import chaynik.mizu.ui.components.common.CoverArt
import chaynik.mizu.ui.components.common.Dropdown
import chaynik.mizu.ui.components.common.DropdownItem
import chaynik.mizu.util.core.toHoursMinutesSeconds
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareListScreenItem(
	modifier: Modifier = Modifier,
	share: DomainShare,
	onSetDeletionId: (newDeletionId: String) -> Unit
) {
	val shareManager = koinInject<ShareManager>()
	val snackBarState = LocalSnackBarState.current
	var expanded by remember { mutableStateOf(false) }
	var currentTime by remember { mutableStateOf(Clock.System.now()) }
	val scope = rememberCoroutineScope()
	val dismissState = rememberSwipeToDismissBoxState()
	val preferenceManager = koinInject<PreferenceManager>()

	LaunchedEffect(share.expiresAt) {
		while (true) {
			delay(1.seconds)
			currentTime = Clock.System.now()
		}
	}

	SwipeToDismissBox(
		state = dismissState,
		onDismiss = {
			if (it != SwipeToDismissBoxValue.Settled) onSetDeletionId(share.id)
			scope.launch { dismissState.reset() }
		},
		backgroundContent = {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.clip(MaterialTheme.shapes.extraSmall)
					.background(MaterialTheme.colorScheme.errorContainer)
					.padding(horizontal = 20.dp)
			) {
				Icon(
					imageVector = Icons.Outlined.Delete,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onErrorContainer,
					modifier = Modifier.align(
						when (dismissState.dismissDirection) {
							SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
							else -> Alignment.CenterEnd
						}
					)
				)
			}
		}
	) {
		Surface {
			Box {
				ListItem(
					modifier = modifier,
					leadingContent = {
						CoverArt(
							coverArtId = share.items.firstOrNull()?.coverArtId,
							modifier = Modifier.size(60.dp),
							shape = preferenceManager.coverArtShape.decreasedShape
						)
					},
					content = {
						Text(share.description)
					},
					supportingContent = {
						Text(stringResource(Res.string.info_shared_by, share.username))
					},
					overlineContent = {
						val expires = share.expiresAt
						val remaining = expires - currentTime
						if (remaining.isPositive()) {
							Text(
								stringResource(
									Res.string.info_share_expires_in,
									remaining.toHoursMinutesSeconds()
								)
							)
						} else {
							Text(stringResource(Res.string.info_share_expired))
						}
					},
					onClick = {
						expanded = true
					},
					onLongClick = {
						expanded = true
					}
				)
				Dropdown(
					expanded = expanded,
					onDismissRequest = { expanded = false }
				) {
					DropdownItem(
						onClick = {
							expanded = false
							scope.launch {
								try {
									shareManager.shareString(share.url)
								} catch (e: Exception) {
									snackBarState.showSnackbar(
										e.message ?: getString(Res.string.info_error)
									)
								}
							}
						},
						leadingIcon = { Icon(Icons.Outlined.Share, null) },
						text = { Text(stringResource(Res.string.action_share)) }
					)
					DropdownItem(
						onClick = {
							expanded = false
							onSetDeletionId(share.id)
						},
						leadingIcon = { Icon(Icons.Outlined.Delete, null) },
						text = { Text(stringResource(Res.string.action_delete)) }
					)
				}
			}
		}
	}
}
