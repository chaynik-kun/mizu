package chaynik.mizu.domain.models

sealed interface PlaybackTarget {
	data object Local : PlaybackTarget
	data class Dlna(val id: String, val name: String, val address: String? = null, val model: String? = null) : PlaybackTarget
}

enum class TargetConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

data class PlaybackTargetState(
	val activeTarget: PlaybackTarget = PlaybackTarget.Local,
	val availableTargets: List<PlaybackTarget> = emptyList(),
	val connectionState: TargetConnectionState = TargetConnectionState.DISCONNECTED,
	val errorMessage: String? = null,
	val remotePositionMs: Long = 0,
	val isPlaying: Boolean = false,
	val currentItemId: String? = null,
	val queueIndex: Int? = null
)

data class RemotePlaybackCapabilities(
	val mimeTypes: Set<String> = emptySet(),
	val supportsSeek: Boolean = true,
	val supportsQueue: Boolean = false,
	val supportsVolume: Boolean = false
)

data class RemoteTrack(
	val id: String,
	val title: String,
	val artist: String?,
	val album: String?,
	val artwork: String?,
	val durationMs: Long?,
	val sourceMimeType: String?
)

data class RemotePlaybackItem(
	val track: RemoteTrack,
	val uri: String,
	val mimeType: String?,
	val transcoded: Boolean = false
)
