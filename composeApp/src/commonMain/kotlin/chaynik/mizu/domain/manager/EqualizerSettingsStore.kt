package chaynik.mizu.domain.manager

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import chaynik.mizu.domain.models.EqualizerSettings

interface EqualizerSettingsPersistence {
	fun load(): EqualizerSettings
	fun save(value: EqualizerSettings)
}

class EqualizerSettingsStore(private val settings: Settings) : EqualizerSettingsPersistence {
	private val key = "equalizerSettingsV1"
	override fun load(): EqualizerSettings = settings.getStringOrNull(key)?.let { runCatching { Json.decodeFromString<EqualizerSettings>(it) }.getOrNull() } ?: EqualizerSettings()
	override fun save(value: EqualizerSettings) = settings.putString(key, Json.encodeToString(value))
}
