package chaynik.mizu.di

import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import chaynik.mizu.domain.manager.*
import chaynik.mizu.external.DlnaBackend
import chaynik.mizu.external.NavidromeRemoteStreamResolver

actual val externalPlaybackModule = module {
	single<RemotePlaybackBackend>(qualifier = org.koin.core.qualifier.named("dlna")) { DlnaBackend(androidApplication()) }
	single<ExternalPlaybackManager> {
		DefaultExternalPlaybackManager(
			dlna = get(org.koin.core.qualifier.named("dlna")),
			resolver = get()
		)
	}
	single<RemoteStreamResolver> { NavidromeRemoteStreamResolver(get()) }
}
