package chaynik.mizu.androidApp.shared

import android.content.Context
import chaynik.mizu.util.core.ResourceProvider

class AndroidResourceProvider(
	context: Context,
	override val icMizu: Int = chaynik.mizu.androidApp.R.drawable.ic_mizu,
	override val animLibrary: Int = chaynik.mizu.androidApp.R.drawable.anim_library,
	override val animPlaylist: Int = chaynik.mizu.androidApp.R.drawable.anim_playlist,
	override val animArtist: Int = chaynik.mizu.androidApp.R.drawable.anim_artist,
	override val animPause: Int = chaynik.mizu.androidApp.R.drawable.anim_pause,
	override val autoAlbums: String = context.getString(chaynik.mizu.androidApp.R.string.auto_albums),
	override val autoPlaylists: String = context.getString(chaynik.mizu.androidApp.R.string.auto_playlists),
	override val autoSongs: String = context.getString(chaynik.mizu.androidApp.R.string.auto_songs)
) : ResourceProvider
