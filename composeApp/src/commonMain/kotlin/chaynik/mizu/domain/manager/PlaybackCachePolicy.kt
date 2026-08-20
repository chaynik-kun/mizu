package chaynik.mizu.domain.manager

data class PlaybackCachePolicy(val readEnabled: Boolean, val writeEnabled: Boolean)

fun playbackCachePolicy(automaticPlaybackCache: Boolean) = PlaybackCachePolicy(
	readEnabled = true,
	writeEnabled = automaticPlaybackCache
)
