package chaynik.mizu.domain.manager

import android.media.audiofx.Equalizer
import chaynik.mizu.domain.models.*

class AndroidEqualizerEffect(audioSessionId: Int) : EqualizerEffect {
	private val effect = Equalizer(0, audioSessionId)
	private val levelRange get() = effect.bandLevelRange
	override val bands: List<EqualizerBand> get() = (0 until effect.numberOfBands.toInt()).map { index ->
		val id = index.toShort()
		EqualizerBand(
			EqualizerBandId(index),
			millihertzToHz(effect.getCenterFreq(id)),
			millibelsToDb(levelRange[0].toInt()),
			millibelsToDb(levelRange[1].toInt()),
			millibelsToDb(effect.getBandLevel(id).toInt())
		)
	}
	override val presets: List<EqualizerPreset> get() = (0 until effect.numberOfPresets.toInt()).map { EqualizerPreset(effect.getPresetName(it.toShort()).orEmpty(), it) }
	override val hasControl: Boolean get() = effect.hasControl()
	override fun setEnabled(enabled: Boolean) { effect.enabled = enabled }
	override fun setBandGain(id: EqualizerBandId, gainDb: Float) { effect.setBandLevel(id.value.toShort(), dbToMillibels(gainDb, levelRange[0].toInt(), levelRange[1].toInt()).toShort()) }
	override fun usePreset(systemIndex: Int) { effect.usePreset(systemIndex.toShort()) }
	override fun setControlListener(listener: (Boolean) -> Unit) { effect.setControlStatusListener { _, control -> listener(control) } }
	override fun close() { effect.release() }
}
