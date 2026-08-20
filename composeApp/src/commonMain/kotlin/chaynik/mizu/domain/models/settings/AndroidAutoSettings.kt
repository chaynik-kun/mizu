package chaynik.mizu.domain.models.settings

data class AndroidAutoSettings(
	val showAlbums: Boolean = true,
	val showArtists: Boolean = true,
	val showPlaylists: Boolean = true,
	val showSongs: Boolean = true,
	val showRecentlyAdded: Boolean = true,
	val showRandomTracks: Boolean = true,
	val itemsPerSection: Int = 100
)

enum class AndroidAutoSection {
	ALBUMS, ARTISTS, PLAYLISTS, SONGS, RECENTLY_ADDED, RANDOM_TRACKS
}

fun AndroidAutoSettings.enabledSections(): List<AndroidAutoSection> = buildList {
	if (showAlbums) add(AndroidAutoSection.ALBUMS)
	if (showArtists) add(AndroidAutoSection.ARTISTS)
	if (showPlaylists) add(AndroidAutoSection.PLAYLISTS)
	if (showSongs) add(AndroidAutoSection.SONGS)
	if (showRecentlyAdded) add(AndroidAutoSection.RECENTLY_ADDED)
	if (showRandomTracks) add(AndroidAutoSection.RANDOM_TRACKS)
}

fun Int.validAndroidAutoLimit(): Int = if (this in setOf(25, 50, 100)) this else 100
