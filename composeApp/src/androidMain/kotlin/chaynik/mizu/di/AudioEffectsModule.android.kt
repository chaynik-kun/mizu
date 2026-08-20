package chaynik.mizu.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module
import chaynik.mizu.domain.manager.*

actual val audioEffectsModule = module {
	single<EqualizerSettingsPersistence> { EqualizerSettingsStore(get()) }
	single<EqualizerEffectFactory> { EqualizerEffectFactory(::AndroidEqualizerEffect) }
	single<EqualizerController> {
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
		DefaultEqualizerController(get(), get(), scope).also { controller ->
			scope.launch { get<ExternalPlaybackManager>().state.collect { controller.setPlaybackTarget(it.activeTarget) } }
		}
	}
}
