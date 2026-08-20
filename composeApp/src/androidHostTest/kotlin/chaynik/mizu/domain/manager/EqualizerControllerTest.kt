package chaynik.mizu.domain.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import chaynik.mizu.domain.models.*
import kotlin.test.*

class EqualizerControllerTest {
	private val fiveBands = listOf(60, 230, 910, 3600, 14000).mapIndexed { index, hz -> EqualizerBand(EqualizerBandId(index), hz, -15f, 15f, 0f) }

	@Test fun conversionsFormattingClampingAndRounding() {
		assertEquals(-15f, millibelsToDb(-1500)); assertEquals(0f, millibelsToDb(0)); assertEquals(15f, millibelsToDb(1500))
		assertEquals(1500, dbToMillibels(20f, -1500, 1500)); assertEquals(-1500, dbToMillibels(-20f, -1500, 1500)); assertEquals(356, dbToMillibels(3.569f, -1500, 1500))
		assertEquals(60, millihertzToHz(60_000)); assertEquals("3.6 kHz", formatEqualizerFrequency(3600)); assertEquals("14 kHz", formatEqualizerFrequency(14000))
	}

	@Test fun enabledPersistsDisabledDoesNotActivateAndSessionAttaches() = runTest {
		val fixture = fixture(); fixture.controller.setEnabled(true); fixture.controller.attachToAudioSession(10); fixture.await { fixture.effects.size == 1 && fixture.controller.state.value.active }
		assertTrue(fixture.persistence.value.enabled); assertTrue(fixture.effects.single().effectEnabled)
		fixture.controller.setEnabled(false); fixture.await { !fixture.controller.state.value.active }; assertFalse(fixture.effects.single().effectEnabled)
	}

	@Test fun sessionReplacementReleasesOldRestoresSettingsAndDetachOnce() = runTest {
		val fixture = fixture(EqualizerSettings(true, null, listOf(SavedEqualizerBand(60, 4f))))
		fixture.controller.attachToAudioSession(10); fixture.await { fixture.effects.size == 1 }
		fixture.controller.attachToAudioSession(11); fixture.await { fixture.effects.size == 2 }
		assertEquals(1, fixture.effects[0].closeCount); assertEquals(4f, fixture.effects[1].bands.first().gainDb)
		fixture.controller.detach(); fixture.await { fixture.effects[1].closeCount == 1 }; fixture.controller.detach(); assertEquals(1, fixture.effects[1].closeCount)
	}

	@Test fun remoteMakesInactiveAndLocalReactivatesWithoutChangingPreference() = runTest {
		val fixture = fixture(EqualizerSettings(enabled = true)); fixture.controller.attachToAudioSession(10); fixture.await { fixture.controller.state.value.active }
		fixture.controller.setPlaybackTarget(PlaybackTarget.Dlna("d", "Receiver")); fixture.await { fixture.controller.state.value.unavailableReason == EqualizerUnavailableReason.REMOTE_PLAYBACK }
		assertTrue(fixture.controller.state.value.enabled); assertFalse(fixture.controller.state.value.active)
		fixture.controller.setPlaybackTarget(PlaybackTarget.Local); fixture.await { fixture.controller.state.value.active }
	}

	@Test fun customGainPresetAndResetUpdateStateAndPersistence() = runTest {
		val fixture = fixture(); fixture.controller.attachToAudioSession(10); fixture.await { fixture.effects.isNotEmpty() }
		fixture.controller.setBandGain(EqualizerBandId(0), 5f); fixture.await { fixture.persistence.value.customBands.firstOrNull()?.gainDb == 5f }
		fixture.controller.applyPreset(EqualizerPreset("Odd / 名", 1)); fixture.await { fixture.controller.state.value.mode == EqualizerMode.PRESET }
		assertEquals("Odd / 名", fixture.persistence.value.selectedPresetName)
		fixture.controller.useCustom(); fixture.await { fixture.controller.state.value.mode == EqualizerMode.CUSTOM }
		assertEquals(5f, fixture.controller.state.value.bands.first().gainDb)
		fixture.controller.setBandGain(EqualizerBandId(0), 4f); fixture.await { fixture.controller.state.value.mode == EqualizerMode.CUSTOM }
		assertNull(fixture.controller.state.value.selectedPreset); assertEquals(4f, fixture.persistence.value.customBands.first().gainDb)
		fixture.controller.reset(); fixture.await { fixture.controller.state.value.bands.all { it.gainDb == 0f } }
	}

	@Test fun presetRestoresByNameAndMissingPresetFallsBackSafely() = runTest {
		val found = fixture(EqualizerSettings(true, "Rock", mode = EqualizerMode.PRESET)); found.controller.attachToAudioSession(1); found.await { found.effects.isNotEmpty() }; assertEquals(0, found.effects.single().usedPreset)
		val missing = fixture(EqualizerSettings(true, "Gone", mode = EqualizerMode.PRESET)); missing.controller.attachToAudioSession(2); missing.await { missing.effects.isNotEmpty() }; assertEquals(EqualizerMode.CUSTOM, missing.controller.state.value.mode); assertNull(missing.effects.single().usedPreset)
	}

	@Test fun settingsSurviveControllerRecreationBeforeAudioSession() = runTest {
		val persistence = FakePersistence(EqualizerSettings())
		val firstEffects = mutableListOf<FakeEffect>(); val first = DefaultEqualizerController(EqualizerEffectFactory { FakeEffect(fiveBands).also(firstEffects::add) }, persistence, CoroutineScope(UnconfinedTestDispatcher()))
		first.attachToAudioSession(1); kotlinx.coroutines.yield(); first.setEnabled(true); first.setBandGain(EqualizerBandId(0), 6f); kotlinx.coroutines.yield()
		val secondEffects = mutableListOf<FakeEffect>(); val secondController = DefaultEqualizerController(EqualizerEffectFactory { FakeEffect(fiveBands).also(secondEffects::add) }, persistence, CoroutineScope(UnconfinedTestDispatcher()))
		assertTrue(secondController.state.value.enabled)
		secondController.attachToAudioSession(2); kotlinx.coroutines.yield(); assertEquals(6f, secondEffects.single().bands.first().gainDb); assertTrue(secondController.state.value.active)
	}

	@Test fun bandMappingHandlesFiveToTenAndTenToFiveByFrequency() {
		val saved5 = fiveBands.map { SavedEqualizerBand(it.centerFrequencyHz, it.id.value.toFloat()) }
		val ten = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000).mapIndexed { i, hz -> EqualizerBand(EqualizerBandId(i), hz, -12f, 12f, 0f) }
		val mapped10 = mapSavedEqualizerBands(saved5, ten); assertEquals(0f, mapped10[EqualizerBandId(1)]); assertEquals(3f, mapped10[EqualizerBandId(7)])
		val saved10 = ten.map { SavedEqualizerBand(it.centerFrequencyHz, it.id.value.toFloat()) }
		val mapped5 = mapSavedEqualizerBands(saved10, fiveBands); assertEquals(1f, mapped5[EqualizerBandId(0)]); assertEquals(9f, mapped5[EqualizerBandId(4)])
	}

	private fun fixture(settings: EqualizerSettings = EqualizerSettings()): Fixture {
		val persistence = FakePersistence(settings); val effects = mutableListOf<FakeEffect>()
		val controller = DefaultEqualizerController(EqualizerEffectFactory { FakeEffect(fiveBands).also(effects::add) }, persistence, CoroutineScope(UnconfinedTestDispatcher()))
		return Fixture(controller, persistence, effects)
	}
	private data class Fixture(val controller: DefaultEqualizerController, val persistence: FakePersistence, val effects: List<FakeEffect>) {
		suspend fun await(condition: () -> Boolean) = kotlinx.coroutines.withTimeout(1000) { while (!condition()) kotlinx.coroutines.yield() }
	}
	private class FakePersistence(var value: EqualizerSettings) : EqualizerSettingsPersistence { override fun load() = value; override fun save(value: EqualizerSettings) { this.value = value } }
	private class FakeEffect(initial: List<EqualizerBand>) : EqualizerEffect {
		private var current = initial; override val bands get() = current; override val presets = listOf(EqualizerPreset("Rock", 0), EqualizerPreset("Odd / 名", 1)); override var hasControl = true
		var effectEnabled = false; var closeCount = 0; var usedPreset: Int? = null
		override fun setEnabled(enabled: Boolean) { this.effectEnabled = enabled }
		override fun setBandGain(id: EqualizerBandId, gainDb: Float) { current = current.map { if (it.id == id) it.copy(gainDb = gainDb) else it } }
		override fun usePreset(systemIndex: Int) { usedPreset = systemIndex; current = current.map { it.copy(gainDb = if (systemIndex == 0) 2f else 3f) } }
		override fun setControlListener(listener: (Boolean) -> Unit) = Unit
		override fun close() { closeCount++ }
	}
}
