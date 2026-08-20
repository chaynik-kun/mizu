package chaynik.mizu.ui.screens.radio.components

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Modifier
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_no_radios
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.domain.models.DomainRadio
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Radio
import chaynik.mizu.ui.components.common.ContentUnavailable
import chaynik.mizu.ui.core.UiState

fun LazyGridScope.radioListScreenContent(
	state: UiState<List<DomainRadio>>,
	onRadioClick: (DomainRadio) -> Unit
) {
	val data = state.data.orEmpty()

	if (data.isNotEmpty()) {
		items(data, key = { it.id }) { radio ->
			RadioListScreenCard(
				modifier = Modifier.animateItem(),
				radio = radio,
				onPlayClick = { onRadioClick(radio) }
			)
		}
	} else {
		when (state) {
			is UiState.Loading -> {
				items(10) {
					RadioListScreenCardPlaceholder()
				}
			}

			else -> {
				item(span = { GridItemSpan(maxLineSpan) }) {
					ContentUnavailable(
						icon = Icons.Outlined.Radio,
						label = stringResource(Res.string.info_no_radios)
					)
				}
			}
		}
	}
}
