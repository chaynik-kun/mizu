package chaynik.mizu.domain.manager

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class PrebufferPolicy(
	val enabled: Boolean = true,
	val targetBytes: Long = 3L * 1024L * 1024L,
	val maximumBytes: Long = 4L * 1024L * 1024L
)

data class PrebufferCandidate(
	val mediaId: String,
	val uri: String,
	val cacheKey: String,
	val serverNamespace: String
)

fun selectPrebufferCandidate(currentIndex: Int, actualNextIndex: Int, queue: List<PrebufferCandidate>): PrebufferCandidate? =
	actualNextIndex.takeIf { it >= 0 && it != currentIndex }?.let(queue::getOrNull)

data class PrebufferContext(
	val candidate: PrebufferCandidate?,
	val enabled: Boolean,
	val online: Boolean,
	val localPlayback: Boolean,
	val currentPlaybackHealthy: Boolean
)

sealed interface PrebufferState {
	data object Disabled : PrebufferState
	data object Idle : PrebufferState
	data class Loading(val mediaId: String, val cachedBytes: Long, val targetBytes: Long) : PrebufferState
	data class Ready(val mediaId: String, val cachedBytes: Long, val targetBytes: Long) : PrebufferState
	data class Error(val mediaId: String?, val category: String) : PrebufferState
}

interface PrebufferManager {
	val state: StateFlow<PrebufferState>
	fun updatePlaybackContext(context: PrebufferContext)
	fun cancel()
}

interface PrebufferLoader {
	fun cachedBytes(candidate: PrebufferCandidate, targetBytes: Long): Long
	suspend fun load(candidate: PrebufferCandidate, targetBytes: Long, onProgress: (Long) -> Unit)
	fun cancel()
}

internal fun prebufferTargetReached(bytesCached: Long, targetBytes: Long): Boolean =
	bytesCached >= targetBytes

class DefaultPrebufferManager(
	private val scope: CoroutineScope,
	private val loader: PrebufferLoader,
	private val policy: PrebufferPolicy = PrebufferPolicy()
) : PrebufferManager {
	private val mutableState = MutableStateFlow<PrebufferState>(PrebufferState.Idle)
	override val state: StateFlow<PrebufferState> = mutableState
	private var job: Job? = null
	private var activeSignature: String? = null

	@Synchronized
	override fun updatePlaybackContext(context: PrebufferContext) {
		val candidate = context.candidate
		val allowed = policy.enabled && context.enabled && context.online && context.localPlayback && context.currentPlaybackHealthy
		if (!allowed || candidate == null) {
			stop(if (!policy.enabled || !context.enabled || !context.localPlayback) PrebufferState.Disabled else PrebufferState.Idle)
			return
		}
		val signature = candidate.signature()
		if (signature == activeSignature && (job?.isActive == true || state.value is PrebufferState.Ready)) return
		val previous = job
		activeSignature = null
		loader.cancel()
		previous?.cancel()
		job = null
		mutableState.value = PrebufferState.Idle
		activeSignature = signature
		val target = policy.targetBytes.coerceAtMost(policy.maximumBytes).coerceAtLeast(1)
		val cached = loader.cachedBytes(candidate, target)
		if (cached >= target) {
			mutableState.value = PrebufferState.Ready(candidate.mediaId, cached, target)
			return
		}
		job = scope.launch {
			previous?.join()
			try {
				mutableState.value = PrebufferState.Loading(candidate.mediaId, cached, target)
				loader.load(candidate, target) { bytes ->
					if (activeSignature == signature) mutableState.value = PrebufferState.Loading(candidate.mediaId, bytes.coerceAtLeast(cached), target)
				}
				if (activeSignature == signature) mutableState.value = PrebufferState.Ready(candidate.mediaId, loader.cachedBytes(candidate, target), target)
			} catch (_: CancellationException) {
				// Superseded work is expected.
			} catch (error: Throwable) {
				if (activeSignature == signature) mutableState.value = PrebufferState.Error(candidate.mediaId, error::class.simpleName ?: "IO")
			}
		}
	}

	@Synchronized override fun cancel() = stop(PrebufferState.Idle)

	@Synchronized private fun stop(next: PrebufferState) {
		activeSignature = null
		loader.cancel()
		job?.cancel()
		job = null
		mutableState.value = next
	}

	private fun PrebufferCandidate.signature() = "$serverNamespace|$mediaId|$uri|$cacheKey"
}
