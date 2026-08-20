package chaynik.mizu.domain.manager

import chaynik.mizu.domain.models.settings.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AndroidAutoSettingsTest {
	@Test fun allSectionsEnabledByDefault() = assertEquals(AndroidAutoSection.entries, AndroidAutoSettings().enabledSections())
	@Test fun albumsCanBeDisabled() = disabled(AndroidAutoSection.ALBUMS, AndroidAutoSettings(showAlbums = false))
	@Test fun artistsCanBeDisabled() = disabled(AndroidAutoSection.ARTISTS, AndroidAutoSettings(showArtists = false))
	@Test fun playlistsCanBeDisabled() = disabled(AndroidAutoSection.PLAYLISTS, AndroidAutoSettings(showPlaylists = false))
	@Test fun songsCanBeDisabled() = disabled(AndroidAutoSection.SONGS, AndroidAutoSettings(showSongs = false))
	@Test fun recentlyAddedCanBeDisabled() = disabled(AndroidAutoSection.RECENTLY_ADDED, AndroidAutoSettings(showRecentlyAdded = false))
	@Test fun randomCanBeDisabled() = disabled(AndroidAutoSection.RANDOM_TRACKS, AndroidAutoSettings(showRandomTracks = false))
	@Test fun limit25IsValid() = assertEquals(25, 25.validAndroidAutoLimit())
	@Test fun limit50IsValid() = assertEquals(50, 50.validAndroidAutoLimit())
	@Test fun limit100IsValid() = assertEquals(100, 100.validAndroidAutoLimit())
	private fun disabled(section: AndroidAutoSection, settings: AndroidAutoSettings) = assertFalse(section in settings.enabledSections())
}
