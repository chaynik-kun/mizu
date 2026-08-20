package chaynik.mizu.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import chaynik.mizu.domain.models.DomainAlbumListType
import chaynik.mizu.domain.models.DomainArtistListType
import chaynik.mizu.domain.models.DomainSongListType

@Immutable
@Serializable
sealed interface Screen : NavKey {

	// tabs
	@Immutable
	@Serializable
	data class Home(val nested: Boolean = false) : Screen

	@Immutable
	@Serializable
	data class Library(
		val nested: Boolean = false
	) : Screen

	@Immutable
	@Serializable
	data class LibraryPeople(val type: LibraryPeopleType) : Screen

	@Serializable
	enum class LibraryPeopleType { TRACK_ARTISTS, COMPOSERS }

	@Immutable
	@Serializable
	data class Starred(
		val nested: Boolean = false,
		val listType: DomainArtistListType = DomainArtistListType.AlphabeticalByName
	) : Screen

	@Immutable
	@Serializable
	data class PlaylistList(
		val nested: Boolean = false
	) : Screen

	@Immutable
	@Serializable
	data class ArtistList(
		val nested: Boolean = false,
		val listType: DomainArtistListType = DomainArtistListType.AlphabeticalByName
	) : Screen

	@Immutable
	@Serializable
	data class AlbumList(
		val nested: Boolean = false,
		val listType: DomainAlbumListType = DomainAlbumListType.AlphabeticalByArtist
	) : Screen

	@Immutable
	@Serializable
	data class GenreList(
		val nested: Boolean = false
	) : Screen

	@Immutable
	@Serializable
	data class GenreDetail(
		val genreName: String
	) : Screen

	@Immutable
	@Serializable
	data class SongList(
		val nested: Boolean = false,
		val listType: DomainSongListType = DomainSongListType.Alphabetical
	) : Screen

	@Immutable
	@Serializable
	data class RadioList(
		val nested: Boolean = false
	) : Screen

	// misc
	@Immutable
	@Serializable
	data object Login : Screen

	@Immutable
	@Serializable
	data object NowPlaying : Screen

	@Immutable
	@Serializable
	data object Lyrics : Screen

	@Immutable
	@Serializable
	data object Queue : Screen

	@Immutable
	@Serializable
	data object PlaybackSpeed : Screen

	@Immutable
	@Serializable
	data class CollectionDetail(
		val collectionId: String,
		val tab: String
	) : Screen

	@Immutable
	@Serializable
	data class SongDetailSheet(val songId: String, val coverArtId: String? = null) : Screen

	@Immutable
	@Serializable
	data class SongDetailScreen(val songId: String, val coverArtId: String? = null) : Screen

	@Immutable
	@Serializable
	data class Search(
		val nested: Boolean = false
	) : Screen

	@Immutable
	@Serializable
	data object ShareList : Screen

	@Immutable
	@Serializable
	data class ArtistDetail(val artist: String) : Screen

	// settings
	@Immutable
	@Serializable
	sealed interface Settings : Screen {
		@Immutable
		@Serializable
		data object Root : Settings

		@Immutable
		@Serializable
		data object Appearance : Settings

		@Immutable
		@Serializable
		data object Home : Settings

		@Immutable
		@Serializable
		data object Playback : Settings

		@Immutable
		@Serializable
		data object AndroidAuto : Settings

		@Immutable
		@Serializable
		data object Developer : Settings

		@Immutable
		@Serializable
		data object BottomAppBar : Settings

		@Immutable
		@Serializable
		data object NowPlaying : Settings

		@Immutable
		@Serializable
		data object About : Settings

		@Immutable
		@Serializable
		data object Acknowledgements : Settings

		@Immutable
		@Serializable
		data object DataStorage : Settings

		@Immutable
		@Serializable
		data object Fonts : Settings

		@Immutable
		@Serializable
		data object Themes : Settings

		@Immutable
		@Serializable
		data object CustomHeaders : Settings

		@Immutable
		@Serializable
		data object StreamingQuality : Settings

		@Immutable
		@Serializable
		data object DownloadQuality : Settings

		@Immutable
		@Serializable
		data object Logs : Settings

		@Immutable
		@Serializable
		data object AppIcon : Settings
	}
}
