package chaynik.mizu.ui.theme

enum class PlaybackThemeSource { ParentMaterialYou, Artwork, Mizu }

fun selectPlaybackThemeSource(materialYou: Boolean, artworkThemeEnabled: Boolean) = when {
	materialYou -> PlaybackThemeSource.ParentMaterialYou
	artworkThemeEnabled -> PlaybackThemeSource.Artwork
	else -> PlaybackThemeSource.Mizu
}
