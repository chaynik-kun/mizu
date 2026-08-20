package chaynik.mizu.ui.screens.song

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.screens.song.components.SongDetailScreenInfoRow
import chaynik.mizu.ui.screens.song.viewmodels.SongDetailViewModel
import chaynik.mizu.ui.theme.MizuTheme
import chaynik.mizu.util.ui.rememberColorSchemeFromCoverArt

@Composable
fun SongDetailScreen(
	songId: String,
	initialCoverArtId: String? = null
) {
	val viewModel = koinViewModel<SongDetailViewModel>(
		key = songId,
		parameters = { parametersOf(songId) }
	)

	val songState by viewModel.songState.collectAsStateWithLifecycle()
	val song = songState.data
	val info by viewModel.info.collectAsStateWithLifecycle()

	val activeCoverArtId = if (initialCoverArtId.isNullOrEmpty()) song?.coverArtId else initialCoverArtId
	val colorScheme = rememberColorSchemeFromCoverArt(activeCoverArtId)

	MizuTheme(colorScheme) {
		Scaffold(
			topBar = { NestedTopBar({ Text(song?.title.orEmpty()) }) }
		) { contentPadding ->
			Column(
				Modifier
					.verticalScroll(rememberScrollState())
					.padding(
						top = contentPadding.calculateTopPadding() + 12.dp,
						start = 12.dp,
						end = 12.dp
					)
			) {
				Form(bottomPadding = 0.dp) {
					info.forEach { (key, value) ->
						SongDetailScreenInfoRow(key, value)
					}
				}

				Spacer(
					Modifier.padding(
						WindowInsets.systemBars
							.only(WindowInsetsSides.Bottom)
							.asPaddingValues()
					)
				)
			}
		}
	}
}
