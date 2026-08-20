package chaynik.mizu.di

import com.russhwolf.settings.Settings
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import chaynik.mizu.ui.navigation.PersistentViewModelStoreOwner

val appModule = module {
	single { Settings() }
	singleOf(::PersistentViewModelStoreOwner)
}
