package chaynik.mizu.ui.screens.settings.viewmodels

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import chaynik.mizu.domain.models.settings.NavbarConfig
import chaynik.mizu.domain.models.settings.NavbarTab
import chaynik.mizu.ui.core.UiState

class NavtabsViewModel(
	private val settings: Settings
) : ViewModel() {
	private val json = Json

	val state: StateFlow<UiState<NavbarConfig>>
		field = MutableStateFlow<UiState<NavbarConfig>>(UiState.Loading())

	init {
		try {
			state.value = UiState.Success(loadConfig())
		} catch (e: Exception) {
			state.value = UiState.Error(e)
		}
	}

	private fun loadConfig(): NavbarConfig {
		val raw = settings.getStringOrNull(NavbarConfig.KEY)
			?: return NavbarConfig.default
		val config: NavbarConfig = json.decodeFromString(raw)
		return (config.takeIf { it.version == NavbarConfig.VERSION }
			?: NavbarConfig.default).withRegularTabLimit()
	}

	private fun NavbarConfig.withRegularTabLimit(): NavbarConfig {
		var visibleRegularTabs = 0
		return copy(tabs = tabs.map { tab ->
			if (!tab.visible || tab.id == NavbarTab.Id.SEARCH) return@map tab
			visibleRegularTabs++
			if (visibleRegularTabs <= 4) tab else tab.copy(visible = false)
		})
	}

	private fun setConfig(newConfig: NavbarConfig) {
		state.value = UiState.Success(newConfig)
		settings[NavbarConfig.KEY] = json.encodeToString(newConfig)
	}

	fun move(from: Int, to: Int) {
		val config = (state.value as UiState.Success).data
		setConfig(
			config.copy(
				tabs = config.tabs.toMutableList().apply {
					add(to, removeAt(from))
				}
			))
	}

	fun toggleVisibility(id: NavbarTab.Id) {
		val config = (state.value as UiState.Success).data
		val tab = config.tabs.firstOrNull { it.id == id } ?: return
		if (!tab.visible && id != NavbarTab.Id.SEARCH) {
			val visibleRegularTabs = config.tabs.count {
				it.visible && it.id != NavbarTab.Id.SEARCH
			}
			if (visibleRegularTabs >= 4) return
		}
		setConfig(
			config.copy(
				tabs = config.tabs.map {
					if (it.id == id) it.copy(visible = !it.visible) else it
				}
			)
		)
	}
}
