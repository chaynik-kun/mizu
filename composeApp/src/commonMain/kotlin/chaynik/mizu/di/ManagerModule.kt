package chaynik.mizu.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import chaynik.mizu.domain.manager.DownloadManager
import chaynik.mizu.domain.manager.LoginManager
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.manager.SleepTimerManager
import chaynik.mizu.domain.manager.SnackBarManager
import chaynik.mizu.domain.manager.SyncManager
import chaynik.mizu.domain.manager.ServerHttpClientFactory

val managerModule = module {
	singleOf(::SleepTimerManager)
	single(createdAtStart = true) {
		SyncManager(get(), get(), get(), get(), get(), get()).apply {
			startPeriodicSync()
		}
	}
	singleOf(::DownloadManager)
	singleOf(::SessionManager)
	singleOf(::PreferenceManager)
	singleOf(::SnackBarManager)
	singleOf(::LoginManager)
	singleOf(::ServerHttpClientFactory)
}
