package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.subtitle_about
import mizu.composeapp.generated.resources.subtitle_appearance
import mizu.composeapp.generated.resources.subtitle_bottom_app_bar
import mizu.composeapp.generated.resources.subtitle_data_storage
import mizu.composeapp.generated.resources.subtitle_developer
import mizu.composeapp.generated.resources.subtitle_home_settings
import mizu.composeapp.generated.resources.subtitle_now_playing
import mizu.composeapp.generated.resources.subtitle_playback
import mizu.composeapp.generated.resources.info_library_albums
import mizu.composeapp.generated.resources.info_library_artists
import mizu.composeapp.generated.resources.info_library_playlists
import mizu.composeapp.generated.resources.info_library_tracks
import mizu.composeapp.generated.resources.title_album_year_histogram
import mizu.composeapp.generated.resources.title_library_stats
import mizu.composeapp.generated.resources.title_server_info
import mizu.composeapp.generated.resources.title_about
import mizu.composeapp.generated.resources.title_appearance
import mizu.composeapp.generated.resources.title_bottom_app_bar
import mizu.composeapp.generated.resources.title_data_storage
import mizu.composeapp.generated.resources.title_developer
import mizu.composeapp.generated.resources.title_home_settings
import mizu.composeapp.generated.resources.title_now_playing
import mizu.composeapp.generated.resources.title_playback
import mizu.composeapp.generated.resources.title_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import mizu.composeapp.generated.resources.title_android_auto
import mizu.composeapp.generated.resources.subtitle_android_auto
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.LocalDeveloperBuild
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.data.database.dao.AlbumDao
import chaynik.mizu.data.database.dao.ArtistDao
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.dao.SongDao
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.BottomNavigation
import chaynik.mizu.icons.filled.Info
import chaynik.mizu.icons.filled.Palette
import chaynik.mizu.icons.filled.Play
import chaynik.mizu.icons.outlined.ChevronForward
import chaynik.mizu.icons.outlined.Code
import chaynik.mizu.icons.outlined.DataTable
import chaynik.mizu.icons.outlined.Note
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.theme.defaultFont

@Composable
fun SettingsScreen() {
	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.title_settings)) }) }
	) { innerPadding ->
		Column(
			modifier = Modifier
				.padding(innerPadding)
				.verticalScroll(rememberScrollState())
				.padding(top = 16.dp, end = 16.dp, start = 16.dp)
		) {
			Form {
				PageRow(
					destination = Screen.Settings.Home,
					icon = Icons.Outlined.Note,
					iconSize = 24.dp,
					title = Res.string.title_home_settings,
					subtitle = Res.string.subtitle_home_settings
				)
				PageRow(
					destination = Screen.Settings.Appearance,
					icon = Icons.Filled.Palette,
					iconSize = 24.dp,
					title = Res.string.title_appearance,
					subtitle = Res.string.subtitle_appearance
				)
				PageRow(
					destination = Screen.Settings.NowPlaying,
					icon = Icons.Filled.Play,
					iconSize = 24.dp,
					title = Res.string.title_now_playing,
					subtitle = Res.string.subtitle_now_playing
				)
				PageRow(
					destination = Screen.Settings.BottomAppBar,
					icon = Icons.Filled.BottomNavigation,
					iconSize = 24.dp,
					title = Res.string.title_bottom_app_bar,
					subtitle = Res.string.subtitle_bottom_app_bar
				)
				PageRow(
					destination = Screen.Settings.Playback,
					icon = Icons.Outlined.Note,
					iconSize = 24.dp,
					title = Res.string.title_playback,
					subtitle = Res.string.subtitle_playback
				)
				PageRow(
					destination = Screen.Settings.AndroidAuto,
					icon = Icons.Filled.Play,
					iconSize = 24.dp,
					title = Res.string.title_android_auto,
					subtitle = Res.string.subtitle_android_auto
				)
				PageRow(
					destination = Screen.Settings.DataStorage,
					icon = Icons.Outlined.DataTable,
					iconSize = 24.dp,
					title = Res.string.title_data_storage,
					subtitle = Res.string.subtitle_data_storage
				)
				if (LocalDeveloperBuild.current) PageRow(
					destination = Screen.Settings.Developer,
					icon = Icons.Outlined.Code,
					iconSize = 24.dp,
					title = Res.string.title_developer,
					subtitle = Res.string.subtitle_developer
				)
			}
			Form {
				PageRow(
					destination = Screen.Settings.About,
					icon = Icons.Filled.Info,
					title = Res.string.title_about,
					subtitle = Res.string.subtitle_about
				)
			}
			SettingsInfoCards()
		}
	}
}

private data class SettingsLibraryInfo(
	val tracks: Int = 0,
	val albums: Int = 0,
	val artists: Int = 0,
	val playlists: Int = 0,
	val years: List<Int> = emptyList(),
	val serverVersion: String? = null
)

@Composable
private fun SettingsInfoCards() {
	val sessionManager = koinInject<SessionManager>()
	val songDao = koinInject<SongDao>()
	val albumDao = koinInject<AlbumDao>()
	val artistDao = koinInject<ArtistDao>()
	val playlistDao = koinInject<PlaylistDao>()
	val info by produceState(SettingsLibraryInfo()) {
		val albums = albumDao.getAllAlbumsList()
		value = SettingsLibraryInfo(
			tracks = songDao.getAllSongs().size,
			albums = albums.size,
			artists = artistDao.getAllArtistsList().size,
			playlists = playlistDao.getPlaylistCount(),
			years = albums.mapNotNull { it.album.year }.filter { it > 0 },
			serverVersion = runCatching { sessionManager.getServerVersion() }.getOrNull()
		)
	}

	BoxWithConstraints(Modifier.fillMaxWidth()) {
		val singleColumn = maxWidth < 600.dp
		val cards: @Composable ColumnScope.() -> Unit = {
		InfoCard(stringResource(Res.string.title_server_info), Modifier.fillMaxWidth()) {
			Text("●  ${sessionManager.username.ifBlank { "—" }}")
			Text("◎  ${sessionManager.instanceUrl.ifBlank { "—" }}", maxLines = 1)
			Text("▤  ${sessionManager.serverProduct}")
			Text("№  ${info.serverVersion ?: "—"}")
		}
		InfoCard(stringResource(Res.string.title_library_stats), Modifier.fillMaxWidth()) {
			Text(stringResource(Res.string.info_library_tracks, info.tracks))
			Text(stringResource(Res.string.info_library_albums, info.albums))
			Text(stringResource(Res.string.info_library_artists, info.artists))
			Text(stringResource(Res.string.info_library_playlists, info.playlists))
		}
		}
		if (singleColumn) {
			Column(content = cards)
		} else {
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
				InfoCard(stringResource(Res.string.title_server_info), Modifier.weight(1f)) {
					Text("●  ${sessionManager.username.ifBlank { "—" }}")
					Text("◎  ${sessionManager.instanceUrl.ifBlank { "—" }}", maxLines = 1)
					Text("▤  ${sessionManager.serverProduct}")
					Text("№  ${info.serverVersion ?: "—"}")
				}
				InfoCard(stringResource(Res.string.title_library_stats), Modifier.weight(1f)) {
					Text(stringResource(Res.string.info_library_tracks, info.tracks))
					Text(stringResource(Res.string.info_library_albums, info.albums))
					Text(stringResource(Res.string.info_library_artists, info.artists))
					Text(stringResource(Res.string.info_library_playlists, info.playlists))
				}
			}
		}
	}

	InfoCard(
		title = stringResource(Res.string.title_album_year_histogram),
		modifier = Modifier.fillMaxWidth()
	) {
		AlbumYearHistogram(info.years)
	}
}

@Composable
private fun InfoCard(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
	Surface(
		modifier = modifier.padding(bottom = 12.dp),
		shape = MaterialTheme.shapes.extraLarge,
		color = MaterialTheme.colorScheme.surfaceContainer
	) {
		Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
			Text(title, style = MaterialTheme.typography.titleSmall)
			Spacer(Modifier.height(2.dp))
			content()
		}
	}
}

@Composable
private fun AlbumYearHistogram(years: List<Int>) {
	if (years.isEmpty()) {
		Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
		return
	}
	val minYear = years.min()
	val maxYear = years.max()
	val bucketCount = 24
	val range = (maxYear - minYear + 1).coerceAtLeast(1)
	val buckets = IntArray(bucketCount)
	years.forEach { year ->
		val index = ((year - minYear) * bucketCount / range).coerceIn(0, bucketCount - 1)
		buckets[index]++
	}
	val peak = buckets.max().coerceAtLeast(1)
	Row(
		modifier = Modifier.fillMaxWidth().height(96.dp),
		horizontalArrangement = Arrangement.spacedBy(2.dp),
		verticalAlignment = Alignment.Bottom
	) {
		buckets.forEach { count ->
			Spacer(
				Modifier
					.weight(1f)
					.height((8 + 72 * count / peak).dp)
					.background(MaterialTheme.colorScheme.primary, CircleShape)
			)
		}
	}
	Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		Text(minYear.toString(), style = MaterialTheme.typography.labelMedium)
		Text(maxYear.toString(), style = MaterialTheme.typography.labelMedium)
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PageRow(
	destination: Screen? = null,
	icon: ImageVector,
	iconSize: Dp = 22.dp,
	title: StringResource,
	subtitle: StringResource
) {
	val backStack = LocalNavStack.current
	val preferenceManager = koinInject<PreferenceManager>()
	FormRow(
		onClick = dropUnlessResumed {
			destination?.let { destination ->
				backStack.lastOrNull()?.let {
					if (it is Screen.Settings) {
						if (it !is Screen.Settings.Root) {
							backStack.removeLastOrNull()
						}
						backStack.add(destination)
					}
				}
			}
		},
		horizontalArrangement = Arrangement.spacedBy(12.dp),
		contentPadding = PaddingValues(if (preferenceManager.theme.isMaterialLike()) 16.dp else 12.dp)
	) {
		if (preferenceManager.theme.isMaterialLike()) {
			Column(
				modifier = Modifier
					.size(40.dp)
					.background(MaterialTheme.colorScheme.primary, CircleShape),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.Center
			) {
				Icon(
					icon,
					contentDescription = null,
					modifier = Modifier.size(iconSize),
					tint = MaterialTheme.colorScheme.onPrimary
				)
			}
		} else {
			Icon(
				icon,
				contentDescription = null,
				modifier = Modifier.padding(start = 8.dp, end = 5.dp).size(22.dp),
				tint = MaterialTheme.colorScheme.primary
			)
		}
		Column(
			Modifier.weight(1f),
			verticalArrangement = Arrangement.spacedBy(1.dp)
		) {
			Text(
				stringResource(title),
				style = MaterialTheme.typography.titleSmall.copy(
					fontFamily = defaultFont(100),
					fontSize = 16.sp,
					lineHeight = 16.sp
				)
			)
			Text(
				stringResource(subtitle),
				style = MaterialTheme.typography.bodyMedium.copy(
					fontFamily = defaultFont(grade = 10),
					lineHeight = 14.sp
				),
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		if (!preferenceManager.theme.isMaterialLike()) {
			Icon(
				Icons.Outlined.ChevronForward,
				null,
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}
