package chaynik.mizu.domain.manager

import kotlinx.coroutines.flow.StateFlow
import chaynik.mizu.domain.models.*

interface EqualizerController {
	val state: StateFlow<EqualizerState>
	fun setEnabled(enabled: Boolean)
	fun setBandGain(bandId: EqualizerBandId, gainDb: Float, persist: Boolean = true)
	fun applyPreset(preset: EqualizerPreset)
	fun useCustom()
	fun reset()
	fun attachToAudioSession(audioSessionId: Int)
	fun setPlaybackTarget(target: PlaybackTarget)
	fun detach()
}

interface EqualizerEffect : AutoCloseable {
	val bands: List<EqualizerBand>
	val presets: List<EqualizerPreset>
	val hasControl: Boolean
	fun setEnabled(enabled: Boolean)
	fun setBandGain(id: EqualizerBandId, gainDb: Float)
	fun usePreset(systemIndex: Int)
	fun setControlListener(listener: (Boolean) -> Unit)
	override fun close()
}

fun interface EqualizerEffectFactory { fun create(audioSessionId: Int): EqualizerEffect }

fun millibelsToDb(value: Int): Float = value / 100f
fun dbToMillibels(value: Float, minMb: Int, maxMb: Int): Int = (value * 100f).toInt().coerceIn(minMb, maxMb)
fun millihertzToHz(value: Int): Int = (value / 1000f).toInt()

fun formatEqualizerFrequency(hz: Int): String = when {
	hz < 1000 -> "$hz Hz"
	hz % 1000 == 0 -> "${hz / 1000} kHz"
	else -> "${(hz / 100f).toInt() / 10f} kHz"
}

fun mapSavedEqualizerBands(saved: List<SavedEqualizerBand>, current: List<EqualizerBand>): Map<EqualizerBandId, Float> {
	if (saved.isEmpty()) return emptyMap()
	return current.mapNotNull { band ->
		val nearest = saved.minByOrNull { kotlin.math.abs(kotlin.math.ln((it.centerFrequencyHz.coerceAtLeast(1)).toDouble() / band.centerFrequencyHz.coerceAtLeast(1))) } ?: return@mapNotNull null
		val ratio = maxOf(nearest.centerFrequencyHz, band.centerFrequencyHz).toDouble() / minOf(nearest.centerFrequencyHz, band.centerFrequencyHz).coerceAtLeast(1)
		if (ratio > 1.8) null else band.id to nearest.gainDb.coerceIn(band.minGainDb, band.maxGainDb)
	}.toMap()
}

data class EqualizerDiagnostics(
	val supported: Boolean,
	val enabled: Boolean,
	val active: Boolean,
	val bandCount: Int,
	val preset: String,
	val audioSessionAttached: Boolean,
	val unavailableReason: EqualizerUnavailableReason?
)

fun EqualizerState.diagnostics() = EqualizerDiagnostics(
	supported, enabled, active, bands.size,
	selectedPreset?.name ?: if (mode == EqualizerMode.CUSTOM) "Custom" else "None",
	unavailableReason != EqualizerUnavailableReason.NO_AUDIO_SESSION,
	unavailableReason
)
