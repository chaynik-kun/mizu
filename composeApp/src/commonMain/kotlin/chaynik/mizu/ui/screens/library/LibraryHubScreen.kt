package chaynik.mizu.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_album_artists
import mizu.composeapp.generated.resources.title_albums
import mizu.composeapp.generated.resources.title_composers
import mizu.composeapp.generated.resources.title_genres
import mizu.composeapp.generated.resources.title_library
import mizu.composeapp.generated.resources.title_random_albums
import mizu.composeapp.generated.resources.title_random_tracks
import mizu.composeapp.generated.resources.title_songs
import mizu.composeapp.generated.resources.title_track_artists
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.LocalBottomBarScrollManager
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.models.DomainAlbumListType
import chaynik.mizu.domain.models.DomainArtistListType
import chaynik.mizu.domain.models.DomainSongListType
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Album
import chaynik.mizu.icons.filled.Artist
import chaynik.mizu.icons.filled.Genre
import chaynik.mizu.icons.filled.Note
import chaynik.mizu.icons.filled.ShuffleOn
import chaynik.mizu.icons.outlined.Badge
import chaynik.mizu.ui.components.layouts.RootBottomBar
import chaynik.mizu.ui.components.layouts.RootTopBar
import chaynik.mizu.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHubScreen() {
	val backStack = LocalNavStack.current
	val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
	Scaffold(
		topBar = { RootTopBar({ Text(stringResource(Res.string.title_library)) }, scrollBehavior) },
		bottomBar = {
			val scrollManager = LocalBottomBarScrollManager.current
			RootBottomBar(scrolled = scrollManager.isTriggered)
		}
	) { innerPadding ->
		BoxWithConstraints(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = innerPadding.calculateTopPadding())
		) {
			val singleColumn = maxWidth < 400.dp
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState())
					.padding(horizontal = 16.dp, vertical = 12.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				LibraryButtonRow(
					LibraryButtonData(Res.string.title_albums, Icons.Filled.Album, Screen.AlbumList(true)),
					LibraryButtonData(Res.string.title_album_artists, Icons.Filled.Artist, Screen.ArtistList(true)),
					singleColumn
				)
				LibraryButtonRow(
					LibraryButtonData(Res.string.title_songs, Icons.Filled.Note, Screen.SongList(true)),
					LibraryButtonData(Res.string.title_track_artists, Icons.Filled.Artist, Screen.LibraryPeople(Screen.LibraryPeopleType.TRACK_ARTISTS)),
					singleColumn
				)
				LibraryButtonRow(
					LibraryButtonData(Res.string.title_genres, Icons.Filled.Genre, Screen.GenreList(true)),
					LibraryButtonData(Res.string.title_composers, Icons.Outlined.Badge, Screen.LibraryPeople(Screen.LibraryPeopleType.COMPOSERS)),
					singleColumn
				)
				LibraryButtonRow(
					LibraryButtonData(Res.string.title_random_tracks, Icons.Filled.ShuffleOn, Screen.SongList(true, DomainSongListType.Random), accent = true),
					LibraryButtonData(Res.string.title_random_albums, Icons.Filled.ShuffleOn, Screen.AlbumList(true, DomainAlbumListType.Random), accent = true),
					singleColumn
				)
			}
		}
	}
}

private data class LibraryButtonData(
	val title: org.jetbrains.compose.resources.StringResource,
	val icon: ImageVector,
	val destination: Screen,
	val accent: Boolean = false
)

@Composable
private fun LibraryButtonRow(first: LibraryButtonData, second: LibraryButtonData, singleColumn: Boolean) {
	if (singleColumn) {
		Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
			LibraryButton(first, Modifier.fillMaxWidth())
			LibraryButton(second, Modifier.fillMaxWidth())
		}
	} else {
		Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			LibraryButton(first, Modifier.weight(1f))
			LibraryButton(second, Modifier.weight(1f))
		}
	}
}

@Composable
private fun LibraryButton(item: LibraryButtonData, modifier: Modifier) {
	val backStack = LocalNavStack.current
	Surface(
		modifier = modifier.height(104.dp),
		shape = MaterialTheme.shapes.extraLarge,
		color = if (item.accent) MaterialTheme.colorScheme.primaryContainer
		else MaterialTheme.colorScheme.surfaceContainer,
		onClick = dropUnlessResumed { backStack.add(item.destination) }
	) {
		Row(
			modifier = Modifier.padding(PaddingValues(horizontal = 18.dp, vertical = 26.dp)),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(14.dp)
		) {
			Icon(item.icon, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
			Text(
				text = stringResource(item.title),
				style = MaterialTheme.typography.titleMedium,
				maxLines = 2
			)
		}
	}
}
