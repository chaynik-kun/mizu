package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalPlatformContext
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.screens.settings.components.SettingSelectionRow
import chaynik.mizu.ui.screens.settings.components.SettingSwitchRow

@Composable
fun SettingsAndroidAutoScreen() {
	val preferences = koinInject<PreferenceManager>()
	val platform = LocalPlatformContext.current
	Scaffold(topBar = {
		NestedTopBar(
			{ Text(stringResource(Res.string.title_android_auto)) },
			hideBack = platform.sizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
		)
	}) { padding ->
		Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
			Form {
				SettingSwitchRow({ Text(stringResource(Res.string.auto_show_albums)) }, value = preferences.androidAutoShowAlbums, onSetValue = { preferences.androidAutoShowAlbums = it })
				SettingSwitchRow({ Text(stringResource(Res.string.auto_show_artists)) }, value = preferences.androidAutoShowArtists, onSetValue = { preferences.androidAutoShowArtists = it })
				SettingSwitchRow({ Text(stringResource(Res.string.auto_show_playlists)) }, value = preferences.androidAutoShowPlaylists, onSetValue = { preferences.androidAutoShowPlaylists = it })
				SettingSwitchRow({ Text(stringResource(Res.string.auto_show_songs)) }, value = preferences.androidAutoShowSongs, onSetValue = { preferences.androidAutoShowSongs = it })
				SettingSwitchRow({ Text(stringResource(Res.string.auto_show_recent)) }, value = preferences.androidAutoShowRecentlyAdded, onSetValue = { preferences.androidAutoShowRecentlyAdded = it })
				SettingSwitchRow({ Text(stringResource(Res.string.auto_show_random)) }, value = preferences.androidAutoShowRandomTracks, onSetValue = { preferences.androidAutoShowRandomTracks = it })
				SettingSelectionRow(
					title = { Text(stringResource(Res.string.auto_items_per_section)) },
					items = listOf(25, 50, 100).toImmutableList(),
					label = { it.toString() }, selection = preferences.androidAutoItemsPerSection,
					onSelect = { preferences.androidAutoItemsPerSection = it }
				)
			}
		}
	}
}
