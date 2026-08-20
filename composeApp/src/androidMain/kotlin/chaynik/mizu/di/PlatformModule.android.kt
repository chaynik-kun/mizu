package chaynik.mizu.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import chaynik.mizu.data.database.CacheDatabase
import chaynik.mizu.data.database.DownloadDatabase
import chaynik.mizu.domain.manager.AppIconManager
import chaynik.mizu.domain.manager.ConnectivityManager
import chaynik.mizu.domain.manager.LogManager
import chaynik.mizu.domain.manager.AndroidCredentialStore
import chaynik.mizu.domain.manager.CredentialStore
import chaynik.mizu.domain.manager.PermissionManager
import chaynik.mizu.domain.manager.AndroidPlaybackCacheManager
import chaynik.mizu.domain.manager.PlaybackCacheManager
import chaynik.mizu.domain.manager.ShareManager
import chaynik.mizu.domain.manager.StorageManager
import chaynik.mizu.domain.repositories.PlayerStateRepository
import chaynik.mizu.shared.AndroidMediaPlayerViewModel
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.util.core.PlatformType

actual val platformModule = module {
	single { PlatformType.Android }
	single<CredentialStore> { AndroidCredentialStore(androidApplication()) }
	single { AndroidPlaybackCacheManager(androidApplication()) }
	single<PlaybackCacheManager> { get<AndroidPlaybackCacheManager>() }
	single<CacheDatabase> {
		val dbPath = androidApplication()
			.getDatabasePath("cache.db")
			.absolutePath
		Room
			.databaseBuilder<CacheDatabase>(get(), dbPath)
			.setDriver(BundledSQLiteDriver())
			.fallbackToDestructiveMigration(true)
			.build()
	}

	single<DownloadDatabase> {
		val dbPath = androidApplication()
			.getDatabasePath("downloads.db")
			.absolutePath
		Room
			.databaseBuilder<DownloadDatabase>(get(), dbPath)
			.setDriver(BundledSQLiteDriver())
			.fallbackToDestructiveMigration(true)
			.build()
	}

	single<PlayerStateRepository> {
		val context = androidApplication()
		val producePath = {
			context.filesDir.resolve(PlayerStateRepository.DATASTORE_FILE_NAME).absolutePath
		}
		PlayerStateRepository(PlayerStateRepository.getInstance(producePath))
	}

	single<MediaPlayerViewModel> {
		AndroidMediaPlayerViewModel(
			application = androidApplication(),
			stateRepository = get(),
			albumDao = get(),
			downloadManager = get(),
			connectivityManager = get(),
			sessionManager = get(),
			platformContext = get(),
			preferenceManager = get(),
			snackBarManager = get(),
			externalPlaybackManager = get()
		)
	}

	singleOf(::ShareManager)
	singleOf(::StorageManager)
	singleOf(::ConnectivityManager)
	singleOf(::LogManager)
	singleOf(::AppIconManager)
	singleOf(::PermissionManager)
}
