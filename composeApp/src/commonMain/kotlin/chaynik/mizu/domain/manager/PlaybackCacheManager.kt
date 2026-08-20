package chaynik.mizu.domain.manager

import kotlinx.coroutines.flow.StateFlow

interface PlaybackCacheManager {
	val sizeBytes: Long
	val cachedSongIds: StateFlow<Set<String>>
	val fullyCachedSongIds: StateFlow<Set<String>>
	fun useNamespace(namespace: String)
	fun markCompleted(cacheKey: String)
	fun clear()
}
