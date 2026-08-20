package chaynik.mizu.domain.models

import kotlinx.serialization.Serializable

@JvmInline value class EqualizerBandId(val value: Int)

data class EqualizerBand(
	val id: EqualizerBandId,
	val centerFrequencyHz: Int,
	val minGainDb: Float,
	val maxGainDb: Float,
	val gainDb: Float
)

data class EqualizerPreset(val name: String, val systemIndex: Int)
enum class EqualizerMode { PRESET, CUSTOM }
enum class EqualizerUnavailableReason { NO_AUDIO_SESSION, REMOTE_PLAYBACK, NOT_SUPPORTED, INITIALIZATION_FAILED, NO_CONTROL }

data class EqualizerState(
	val supported: Boolean = true,
	val available: Boolean = false,
	val enabled: Boolean = false,
	val active: Boolean = false,
	val bands: List<EqualizerBand> = emptyList(),
	val presets: List<EqualizerPreset> = emptyList(),
	val selectedPreset: EqualizerPreset? = null,
	val mode: EqualizerMode = EqualizerMode.CUSTOM,
	val unavailableReason: EqualizerUnavailableReason? = EqualizerUnavailableReason.NO_AUDIO_SESSION
)

@Serializable data class SavedEqualizerBand(val centerFrequencyHz: Int, val gainDb: Float)
@Serializable data class EqualizerSettings(
	val enabled: Boolean = false,
	val selectedPresetName: String? = null,
	val customBands: List<SavedEqualizerBand> = emptyList(),
	val mode: EqualizerMode = EqualizerMode.CUSTOM
)
