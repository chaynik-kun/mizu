package chaynik.mizu.ui.screens.genre.components

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.Modifier
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_no_genres
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.domain.models.DomainGenre
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Genre
import chaynik.mizu.ui.components.common.ContentUnavailable
import chaynik.mizu.ui.core.UiState

fun LazyGridScope.genreListScreenContent(
	state: UiState<List<DomainGenre>>
) {
	val data = state.data.orEmpty()
	if (data.isNotEmpty()) {
		items(data, { it.name }) { genre ->
			GenreListScreenCard(
				modifier = Modifier.animateItem(),
				genre = genre
			)
		}
	} else {
		when (state) {
			is UiState.Loading -> items(10) {
				GenreListScreenCardPlaceholder()
			}

			else -> {
				item(span = { GridItemSpan(maxLineSpan) }) {
					ContentUnavailable(
						icon = Icons.Outlined.Genre,
						label = stringResource(Res.string.info_no_genres)
					)
				}
			}
		}
	}
}
