package chaynik.mizu.di

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import okio.FileSystem
import coil3.PlatformContext as CoilPlatformContext

private var sharedMemoryCache: MemoryCache? = null
private var sharedDiskCache: DiskCache? = null

private fun getMemoryCache(context: CoilPlatformContext): MemoryCache {
	return sharedMemoryCache ?: MemoryCache.Builder()
		.maxSizePercent(context, 0.12)
		.build().also { sharedMemoryCache = it }
}

private fun getDiskCache(): DiskCache {
	return sharedDiskCache ?: DiskCache.Builder()
		.directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
		.maxSizeBytes(384L * 1024L * 1024L)
		.build().also { sharedDiskCache = it }
}

fun initializeSingletonImageLoader(context: CoilPlatformContext): ImageLoader {
	return ImageLoader.Builder(context)
		.components {
			add(KtorNetworkFetcherFactory())
		}
		.memoryCache { getMemoryCache(context) }
		.diskCache { getDiskCache() }
		.crossfade(true)
		.build()
}
