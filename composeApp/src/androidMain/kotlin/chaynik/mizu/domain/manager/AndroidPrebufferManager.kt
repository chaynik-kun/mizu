package chaynik.mizu.domain.manager

import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AndroidPrebufferManager(
	scope: CoroutineScope,
	cache: Cache,
	upstreamFactory: DataSource.Factory,
	policy: PrebufferPolicy = PrebufferPolicy()
) : PrebufferManager by DefaultPrebufferManager(scope, Media3PrebufferLoader(cache, upstreamFactory), policy)

private class Media3PrebufferLoader(
	private val cache: Cache,
	private val upstreamFactory: DataSource.Factory
) : PrebufferLoader {
	@Volatile private var writer: CacheWriter? = null

	override fun cachedBytes(candidate: PrebufferCandidate, targetBytes: Long): Long {
		val length = cache.getCachedLength(candidate.cacheKey, 0, targetBytes)
		return if (length > 0) length.coerceAtMost(targetBytes) else 0
	}

	override suspend fun load(candidate: PrebufferCandidate, targetBytes: Long, onProgress: (Long) -> Unit) = withContext(Dispatchers.IO) {
		val dataSpec = DataSpec.Builder().setUri(Uri.parse(candidate.uri)).setKey(candidate.cacheKey)
			// This describes the complete resource. Limiting DataSpec.length to the
			// preload target made the first FLAC MediaPeriod appear unseekable.
			.setPosition(0).build()
		val dataSource = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstreamFactory).createDataSource()
		var targetReached = false
		lateinit var current: CacheWriter
		current = CacheWriter(dataSource, dataSpec, null) { _, bytesCached, _ ->
			onProgress(bytesCached.coerceAtMost(targetBytes))
			if (prebufferTargetReached(bytesCached, targetBytes)) {
				targetReached = true
				current.cancel()
			}
		}
		writer = current
		try {
			current.cache()
		} catch (error: IOException) {
			if (!targetReached) throw error
		} finally {
			if (writer === current) writer = null
		}
	}

	override fun cancel() { writer?.cancel() }
}
