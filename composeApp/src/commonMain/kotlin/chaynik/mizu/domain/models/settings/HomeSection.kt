package chaynik.mizu.domain.models.settings

import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_artists
import mizu.composeapp.generated.resources.title_genres
import mizu.composeapp.generated.resources.title_playlists
import mizu.composeapp.generated.resources.title_random_tracks
import mizu.composeapp.generated.resources.title_recently_added
import mizu.composeapp.generated.resources.title_new_releases
import mizu.composeapp.generated.resources.title_recently_played
import mizu.composeapp.generated.resources.title_random_albums
import mizu.composeapp.generated.resources.title_frequently_played
import mizu.composeapp.generated.resources.title_favorite_albums
import mizu.composeapp.generated.resources.title_favorite_artists
import mizu.composeapp.generated.resources.title_radios
import org.jetbrains.compose.resources.StringResource

enum class HomeSection(val title: StringResource) {
	RandomTracks(Res.string.title_random_tracks),
	RecentlyAdded(Res.string.title_recently_added),
	NewReleases(Res.string.title_new_releases),
	RecentlyPlayed(Res.string.title_recently_played),
	RandomAlbums(Res.string.title_random_albums),
	FrequentlyPlayed(Res.string.title_frequently_played),
	FavoriteAlbums(Res.string.title_favorite_albums),
	FavoriteArtists(Res.string.title_favorite_artists),
	Stations(Res.string.title_radios),
	Playlists(Res.string.title_playlists),
	Artists(Res.string.title_artists),
	Genres(Res.string.title_genres);

	companion object {
		fun decode(value: String): List<HomeSection> {
			val saved = value.split(',').mapNotNull { name -> entries.find { it.name == name } }
			return (saved + entries).distinct()
		}

		fun encode(sections: List<HomeSection>) = sections.joinToString(",") { it.name }
	}
}
