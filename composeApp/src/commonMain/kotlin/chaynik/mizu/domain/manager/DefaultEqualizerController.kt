package chaynik.mizu.domain.manager

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import chaynik.mizu.domain.models.*

class DefaultEqualizerController(
	private val factory: EqualizerEffectFactory,
	private val settingsStore: EqualizerSettingsPersistence,
	private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : EqualizerController {
	private sealed interface Command { data class Attach(val id: Int): Command; data object Detach: Command; data class Enabled(val value: Boolean): Command; data class Gain(val id: EqualizerBandId, val db: Float, val persist: Boolean): Command; data class Preset(val value: EqualizerPreset): Command; data object Custom: Command; data object Reset: Command; data class Target(val value: PlaybackTarget): Command; data class Control(val value: Boolean): Command }
	private val commands = Channel<Command>(Channel.UNLIMITED)
	private var settings = settingsStore.load()
	private var effect: EqualizerEffect? = null
	private var sessionId: Int? = null
	private var local = true
	private val mutableState = MutableStateFlow(EqualizerState(
		enabled = settings.enabled,
		mode = settings.mode,
		selectedPreset = settings.selectedPresetName?.let { EqualizerPreset(it, -1) },
		bands = settings.customBands.mapIndexed { index, saved -> EqualizerBand(EqualizerBandId(index), saved.centerFrequencyHz, -15f, 15f, saved.gainDb.coerceIn(-15f, 15f)) }
	))
	override val state: StateFlow<EqualizerState> = mutableState

	init { scope.launch { for (command in commands) handle(command) } }
	override fun setEnabled(enabled: Boolean) { commands.trySend(Command.Enabled(enabled)) }
	override fun setBandGain(bandId: EqualizerBandId, gainDb: Float, persist: Boolean) { commands.trySend(Command.Gain(bandId, gainDb, persist)) }
	override fun applyPreset(preset: EqualizerPreset) { commands.trySend(Command.Preset(preset)) }
	override fun useCustom() { commands.trySend(Command.Custom) }
	override fun reset() { commands.trySend(Command.Reset) }
	override fun attachToAudioSession(audioSessionId: Int) { if (audioSessionId != 0) commands.trySend(Command.Attach(audioSessionId)) }
	override fun setPlaybackTarget(target: PlaybackTarget) { commands.trySend(Command.Target(target)) }
	override fun detach() { commands.trySend(Command.Detach) }

	private fun handle(command: Command) { when (command) {
		is Command.Attach -> attach(command.id)
		Command.Detach -> release(EqualizerUnavailableReason.NO_AUDIO_SESSION)
		is Command.Enabled -> { settings = settings.copy(enabled = command.value); persist(); applyEnabled() }
		is Command.Target -> { local = command.value is PlaybackTarget.Local; applyEnabled() }
		is Command.Control -> if (command.value) applyEnabled() else updateAvailability(EqualizerUnavailableReason.NO_CONTROL)
		is Command.Gain -> {
			val fx = effect
			val band = (fx?.bands ?: mutableState.value.bands).firstOrNull { it.id == command.id } ?: return
			val gain = command.db.coerceIn(band.minGainDb, band.maxGainDb)
			if (fx != null) runCatching { fx.setBandGain(command.id, gain) }.onFailure { updateAvailability(EqualizerUnavailableReason.NO_CONTROL) }
			val updated = mutableState.value.bands.map { if (it.id == command.id) it.copy(gainDb = gain) else it }
			mutableState.value = mutableState.value.copy(bands = updated, mode = EqualizerMode.CUSTOM, selectedPreset = null)
			if (command.persist) { settings = settings.copy(mode = EqualizerMode.CUSTOM, selectedPresetName = null, customBands = updated.map { SavedEqualizerBand(it.centerFrequencyHz, it.gainDb) }); persist() }
		}
		is Command.Preset -> effect?.let { fx -> runCatching { fx.usePreset(command.value.systemIndex) }.onSuccess {
			settings = settings.copy(mode = EqualizerMode.PRESET, selectedPresetName = command.value.name); persist(); refreshBands(fx, command.value)
		}.onFailure { updateAvailability(EqualizerUnavailableReason.NO_CONTROL) } }
		Command.Custom -> {
			settings = settings.copy(mode = EqualizerMode.CUSTOM, selectedPresetName = null); persist()
			val fx = effect
			if (fx != null) {
				val mapped = mapSavedEqualizerBands(settings.customBands, fx.bands)
				fx.bands.forEach { band -> fx.setBandGain(band.id, mapped[band.id] ?: 0f.coerceIn(band.minGainDb, band.maxGainDb)) }
				refreshBands(fx, null)
			} else mutableState.value = mutableState.value.copy(mode = EqualizerMode.CUSTOM, selectedPreset = null)
		}
		Command.Reset -> {
			val fx = effect
			fx?.bands?.forEach { fx.setBandGain(it.id, 0f.coerceIn(it.minGainDb, it.maxGainDb)) }
			val bands = (fx?.bands ?: mutableState.value.bands).map { it.copy(gainDb = 0f.coerceIn(it.minGainDb, it.maxGainDb)) }
			settings = settings.copy(mode = EqualizerMode.CUSTOM, selectedPresetName = null, customBands = bands.map { SavedEqualizerBand(it.centerFrequencyHz, it.gainDb) }); persist()
			mutableState.value = mutableState.value.copy(bands = bands, mode = EqualizerMode.CUSTOM, selectedPreset = null)
		}
	} }

	private fun attach(id: Int) {
		if (sessionId == id && effect != null) return
		release(null); sessionId = id
		val fx = runCatching { factory.create(id) }.getOrElse { sessionId = null; mutableState.value = mutableState.value.copy(supported = false, available = false, active = false, unavailableReason = EqualizerUnavailableReason.INITIALIZATION_FAILED); return }
		effect = fx; fx.setControlListener { commands.trySend(Command.Control(it)) }
		val preset = settings.selectedPresetName?.let { name -> fx.presets.firstOrNull { it.name == name } }
		if (settings.mode == EqualizerMode.PRESET && preset != null) runCatching { fx.usePreset(preset.systemIndex) } else {
			val mapped = mapSavedEqualizerBands(settings.customBands, fx.bands)
			fx.bands.forEach { band -> runCatching { fx.setBandGain(band.id, mapped[band.id] ?: 0f.coerceIn(band.minGainDb, band.maxGainDb)) } }
			if (settings.mode == EqualizerMode.PRESET) { settings = settings.copy(mode = EqualizerMode.CUSTOM, selectedPresetName = null); persist() }
		}
		refreshBands(fx, preset); applyEnabled()
	}
	private fun refreshBands(fx: EqualizerEffect, preset: EqualizerPreset?) { mutableState.value = mutableState.value.copy(supported = true, bands = fx.bands, presets = fx.presets, selectedPreset = preset, mode = if (preset != null) EqualizerMode.PRESET else EqualizerMode.CUSTOM) }
	private fun applyEnabled() {
		val fx = effect; val active = settings.enabled && local && fx != null && fx.hasControl
		fx?.let { runCatching { it.setEnabled(active) }.onFailure { updateAvailability(EqualizerUnavailableReason.NO_CONTROL); return } }
		mutableState.value = mutableState.value.copy(enabled = settings.enabled, active = active, available = fx != null && local && fx.hasControl, unavailableReason = when { !local -> EqualizerUnavailableReason.REMOTE_PLAYBACK; fx == null -> EqualizerUnavailableReason.NO_AUDIO_SESSION; !fx.hasControl -> EqualizerUnavailableReason.NO_CONTROL; else -> null })
	}
	private fun updateAvailability(reason: EqualizerUnavailableReason?) { mutableState.value = mutableState.value.copy(available = reason == null, active = reason == null && settings.enabled && local, unavailableReason = reason) }
	private fun release(reason: EqualizerUnavailableReason?) { effect?.let { runCatching { it.setEnabled(false) }; runCatching { it.close() } }; effect = null; sessionId = null; if (reason != null) updateAvailability(reason) }
	private fun persist() = settingsStore.save(settings)
}
