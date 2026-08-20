package chaynik.mizu.domain.manager

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AndroidPlaybackCacheManager(context: Context) : PlaybackCacheManager, Cache.Listener {
	companion object {
		const val MAX_CACHE_BYTES = 2L * 1024L * 1024L * 1024L
		private const val COMPLETE = "mizu.complete"
		private const val PREFIX = "v2:"
	}

	val cache = SimpleCache(
		File(context.cacheDir, "playback_cache"),
		LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
		StandaloneDatabaseProvider(context)
	)
	private var namespace = ""
	private val mutableCachedSongIds = MutableStateFlow<Set<String>>(emptySet())
	private val mutableFullyCachedSongIds = MutableStateFlow<Set<String>>(emptySet())
	override val cachedSongIds: StateFlow<Set<String>> = mutableCachedSongIds
	override val fullyCachedSongIds: StateFlow<Set<String>> = mutableFullyCachedSongIds

	init {
		// Old keys have no server identity and cannot be migrated without risking cross-server audio.
		cache.keys.filterNot { it.startsWith(PREFIX) }.toList().forEach(cache::removeResource)
		cache.keys.forEach { cache.addListener(it, this) }
		refresh()
	}

	override val sizeBytes: Long get() = cache.cacheSpace

	@Synchronized
	override fun useNamespace(namespace: String) {
		this.namespace = namespace
		refresh()
	}

	@Synchronized
	override fun markCompleted(cacheKey: String) {
		if (cache.getCachedSpans(cacheKey).isEmpty()) return
		val mutations = ContentMetadataMutations().set(COMPLETE, 1L)
		cache.applyContentMetadataMutations(cacheKey, mutations)
		refreshKey(cacheKey)
	}

	@Synchronized
	override fun clear() {
		cache.keys.toList().forEach(cache::removeResource)
		refresh()
	}

	override fun onSpanAdded(cache: Cache, span: CacheSpan) {
		cache.addListener(span.key, this)
		refreshKey(span.key)
	}
	override fun onSpanRemoved(cache: Cache, span: CacheSpan) = refreshKey(span.key)
	override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit

	private fun songId(key: String): String? {
		val parts = key.split(':', limit = 6)
		return if (parts.size == 5 && parts[0] == "v2" && parts[1] == namespace) parts[2] else null
	}

	private fun isComplete(key: String): Boolean {
		val metadata = cache.getContentMetadata(key)
		if (metadata.get(COMPLETE, 0L) == 1L) return true
		val length = metadata.get(ContentMetadata.KEY_CONTENT_LENGTH, -1L)
		return length > 0 && cache.isCached(key, 0, length)
	}

	@Synchronized
	private fun refreshKey(key: String) {
		val id = songId(key) ?: return
		val hasAny = cache.getCachedSpans(key).isNotEmpty()
		mutableCachedSongIds.value = if (hasAny) mutableCachedSongIds.value + id else {
			if (cache.keys.none { songId(it) == id && cache.getCachedSpans(it).isNotEmpty() }) mutableCachedSongIds.value - id else mutableCachedSongIds.value
		}
		mutableFullyCachedSongIds.value = if (hasAny && isComplete(key)) mutableFullyCachedSongIds.value + id else {
			if (cache.keys.none { songId(it) == id && isComplete(it) }) mutableFullyCachedSongIds.value - id else mutableFullyCachedSongIds.value
		}
	}

	@Synchronized
	private fun refresh() {
		mutableCachedSongIds.value = cache.keys.mapNotNull { key -> songId(key)?.takeIf { cache.getCachedSpans(key).isNotEmpty() } }.toSet()
		mutableFullyCachedSongIds.value = cache.keys.mapNotNull { key -> songId(key)?.takeIf { isComplete(key) } }.toSet()
	}
}
