package chaynik.mizu.util.core

// This class is a workaround for not being able to access :androidApp's R class inside :composeApp
interface ResourceProvider {
	val icMizu: Int
	val animLibrary: Int
	val animPlaylist: Int
	val animArtist: Int
	val animPause: Int
	val autoAlbums: String
	val autoPlaylists: String
	val autoSongs: String
}
