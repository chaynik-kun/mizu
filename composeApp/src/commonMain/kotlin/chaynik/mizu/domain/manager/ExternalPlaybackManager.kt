package chaynik.mizu.domain.manager

import kotlinx.coroutines.flow.StateFlow
import chaynik.mizu.domain.models.PlaybackTarget
import chaynik.mizu.domain.models.PlaybackTargetState
import chaynik.mizu.domain.models.RemotePlaybackItem
import chaynik.mizu.domain.models.RemoteTrack
import chaynik.mizu.domain.models.RemotePlaybackCapabilities

interface ExternalPlaybackManager {
	val state: StateFlow<PlaybackTargetState>
	fun setDiscoveryActive(active: Boolean)
	suspend fun connect(target: PlaybackTarget)
	suspend fun disconnect()
	suspend fun play()
	suspend fun pause()
	suspend fun seekTo(positionMs: Long)
	suspend fun next()
	suspend fun previous()
	suspend fun playItem(item: RemotePlaybackItem, queue: List<RemotePlaybackItem>, startIndex: Int, positionMs: Long)
}

interface RemotePlaybackBackend {
	val kind: BackendKind
	val targets: StateFlow<List<PlaybackTarget>>
	val status: StateFlow<BackendPlaybackStatus>
	fun setDiscoveryActive(active: Boolean)
	suspend fun connect(target: PlaybackTarget)
	suspend fun disconnect()
	suspend fun play()
	suspend fun pause()
	suspend fun stop()
	suspend fun seekTo(positionMs: Long)
	suspend fun next()
	suspend fun previous()
	suspend fun load(item: RemotePlaybackItem, queue: List<RemotePlaybackItem>, startIndex: Int, positionMs: Long)
	fun capabilities(target: PlaybackTarget): RemotePlaybackCapabilities = RemotePlaybackCapabilities()
	fun close() = Unit
}

enum class BackendKind { DLNA }
enum class RemotePlaybackState { IDLE, BUFFERING, PLAYING, PAUSED, STOPPED, ERROR }
data class BackendPlaybackStatus(
	val connected: Boolean = false,
	val state: RemotePlaybackState = RemotePlaybackState.IDLE,
	val positionMs: Long = 0,
	val durationMs: Long? = null,
	val currentItemId: String? = null,
	val queueIndex: Int? = null,
	val error: String? = null
) { val playing: Boolean get() = state == RemotePlaybackState.PLAYING }

interface RemoteStreamResolver {
	suspend fun resolve(track: RemoteTrack, target: PlaybackTarget, capabilities: RemotePlaybackCapabilities): RemotePlaybackItem
}

interface LocalPlaybackBridge {
	fun snapshot(): LocalPlaybackSnapshot?
	suspend fun pauseLocal()
	suspend fun restoreLocal(snapshot: LocalPlaybackSnapshot, positionMs: Long)
}

data class LocalPlaybackSnapshot(
	val currentTrack: RemoteTrack,
	val queue: List<RemoteTrack>,
	val index: Int,
	val positionMs: Long,
	val wasPlaying: Boolean
)

fun LocalPlaybackSnapshot.normalized(): LocalPlaybackSnapshot {
	val validIndex = index.takeIf { it in queue.indices && queue[it].id == currentTrack.id }
		?: queue.indexOfFirst { it.id == currentTrack.id }.takeIf { it >= 0 }
	val normalizedQueue = if (validIndex == null) listOf(currentTrack) + queue.filterNot { it.id == currentTrack.id } else queue
	val normalizedIndex = validIndex ?: 0
	return copy(queue = normalizedQueue, index = normalizedIndex)
}
