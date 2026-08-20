package chaynik.mizu.ui.screens.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_composers
import mizu.composeapp.generated.resources.title_track_artists
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.data.database.dao.SongDao
import chaynik.mizu.data.database.dao.ArtistDao
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.navigation.Screen

private data class LibraryPerson(val id: String, val name: String, val hasArtistPage: Boolean = false)

@Composable
fun LibraryPeopleScreen(type: Screen.LibraryPeopleType) {
	val backStack = LocalNavStack.current
	val songDao = koinInject<SongDao>()
	val artistDao = koinInject<ArtistDao>()
	val people by produceState(emptyList<LibraryPerson>(), type) {
		val songs = songDao.getAllSongs()
		val artistIds = artistDao.getAllArtistIds().toSet()
		value = when (type) {
			Screen.LibraryPeopleType.TRACK_ARTISTS -> songs
				.map { LibraryPerson(it.artistId, it.artistName, it.artistId in artistIds) }
			Screen.LibraryPeopleType.COMPOSERS -> songs
				.flatMap { song -> song.contributors
					.filter { it.role.contains("composer", ignoreCase = true) }
					.map { LibraryPerson(it.artistId, it.artistName, it.artistId in artistIds) }
			}
		}.distinctBy { it.id to it.name }.sortedBy { it.name.lowercase() }
	}
	val title = if (type == Screen.LibraryPeopleType.COMPOSERS)
		Res.string.title_composers else Res.string.title_track_artists
	Scaffold(topBar = { NestedTopBar({ Text(stringResource(title)) }) }) { padding ->
		LazyColumn(Modifier.fillMaxSize().padding(padding)) {
			items(people, key = { "${it.id}:${it.name}" }) { person ->
				ListItem(
					headlineContent = { Text(person.name) },
					modifier = if (person.hasArtistPage) Modifier.clickable(onClick = dropUnlessResumed {
						backStack.add(Screen.ArtistDetail(person.id))
					}) else Modifier,
				)
			}
		}
	}
}
