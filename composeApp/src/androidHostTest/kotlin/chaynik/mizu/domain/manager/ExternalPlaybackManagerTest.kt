package chaynik.mizu.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import chaynik.mizu.domain.models.*

class ExternalPlaybackManagerTest {
	private val track = RemoteTrack("1", "Song", "Artist", "Album", null, 100_000, "audio/flac")
	private val second = track.copy(id = "2", title = "Next")

	@Test fun localToDlnaUsesDlnaProfile() = runTest {
		val resolver = FakeResolver(); val dlna = FakeBackend(BackendKind.DLNA)
		val manager = manager(dlna, FakeBridge(7_000), resolver)
		manager.connect(PlaybackTarget.Dlna("d", "Receiver"))
		assertIs<PlaybackTarget.Dlna>(resolver.targets.single())
		assertEquals("audio/mpeg", dlna.loaded?.mimeType)
		assertEquals(7_000, dlna.loadedPosition)
	}

	@Test fun unknownDlnaUsesMp3ButFlacCapabilityKeepsOriginal() {
		assertEquals("mp3", selectRemoteStreamProfile(BackendKind.DLNA, "audio/flac", RemotePlaybackCapabilities()).format)
		assertFalse(selectRemoteStreamProfile(BackendKind.DLNA, "audio/flac", RemotePlaybackCapabilities(setOf("audio/flac"))).transcoded)
	}

	@Test fun queueSnapshotRepairsWrongIndexAndMissingCurrentItem() {
		assertEquals(1, LocalPlaybackSnapshot(second, listOf(track, second), 0, 0, true).normalized().index)
		val missing = LocalPlaybackSnapshot(second, listOf(track), 9, 0, true).normalized()
		assertEquals(second.id, missing.queue[missing.index].id)
	}

	@Test fun dlnaToLocalRestoresRemotePositionAndPausedState() = runTest {
		val dlna = FakeBackend(BackendKind.DLNA); val bridge = FakeBridge(12_000)
		val manager = manager(dlna, bridge)
		manager.connect(PlaybackTarget.Dlna("d", "Receiver"))
		dlna.mutableStatus.value = BackendPlaybackStatus(true, RemotePlaybackState.PAUSED, 55_000, currentItemId = "1", queueIndex = 0)
		manager.disconnect(); assertEquals(55_000, bridge.restored); assertFalse(bridge.restoredSnapshot!!.wasPlaying)
	}

	@Test fun failedDlnaConnectionReturnsControlledErrorAndKeepsLocal() = runTest {
		val dlna = FakeBackend(BackendKind.DLNA).apply { failConnect = true }
		val manager = manager(dlna, FakeBridge(0)); manager.connect(PlaybackTarget.Dlna("d", "Receiver"))
		assertIs<PlaybackTarget.Local>(manager.state.value.activeTarget)
		assertEquals(TargetConnectionState.ERROR, manager.state.value.connectionState)
	}

	@Test fun externalDlnaStatusUpdatesManagerTrackAndPlayback() = runTest {
		val dlna = FakeBackend(BackendKind.DLNA); val manager = manager(dlna, FakeBridge(0, listOf(track, second)))
		manager.connect(PlaybackTarget.Dlna("d", "Receiver")); dlna.mutableStatus.value = BackendPlaybackStatus(true, RemotePlaybackState.PAUSED, 20_000, currentItemId = "2", queueIndex = 1)
		await { manager.state.value.currentItemId == "2" }; assertFalse(manager.state.value.isPlaying)
		dlna.mutableStatus.value = dlna.mutableStatus.value.copy(state = RemotePlaybackState.PLAYING); await { manager.state.value.isPlaying }
		dlna.mutableStatus.value = BackendPlaybackStatus(); await { manager.state.value.connectionState == TargetConnectionState.DISCONNECTED }
	}

	@Test fun dlnaCompletionGuardAdvancesExactlyOnceAndNeverAfterManualStop() {
		val guard = DlnaQueueAdvanceGuard(); guard.onLoadReady()
		assertTrue(guard.onState(DlnaTransportState.STOPPED, true)); assertFalse(guard.onState(DlnaTransportState.STOPPED, true))
		val stopped = DlnaQueueAdvanceGuard(); stopped.onLoadReady(); stopped.onManualStop(); assertFalse(stopped.onState(DlnaTransportState.STOPPED, true))
	}

	@Test fun sensitiveStreamUrlIsRedactedAndLoopbackRejected() {
		val value = sanitizeRemoteUrlForLog("https://s/stream?p=pw&t=secret&s=salt&token=abc&apiKey=k&key=x")
		listOf("pw", "secret", "salt", "abc", "=k", "=x").forEach { assertFalse(it in value) }
		assertFails { validateRemoteUri("http://127.0.0.1:4533/stream") }
	}

	private fun manager(d: FakeBackend, bridge: FakeBridge, resolver: FakeResolver = FakeResolver()) =
		DefaultExternalPlaybackManager(d, resolver).apply { attachLocalBridge(bridge) }
	private suspend fun await(condition: () -> Boolean) = kotlinx.coroutines.withTimeout(2_000) { while (!condition()) kotlinx.coroutines.delay(10) }
	private inner class FakeBridge(private val position: Long, private val tracks: List<RemoteTrack> = listOf(track)) : LocalPlaybackBridge {
		var restored: Long? = null; var restoredSnapshot: LocalPlaybackSnapshot? = null
		override fun snapshot() = LocalPlaybackSnapshot(tracks.first(), tracks, 0, position, true)
		override suspend fun pauseLocal() = Unit
		override suspend fun restoreLocal(snapshot: LocalPlaybackSnapshot, positionMs: Long) { restored = positionMs; restoredSnapshot = snapshot }
	}
	private class FakeResolver : RemoteStreamResolver {
		val targets = mutableListOf<PlaybackTarget>()
		override suspend fun resolve(track: RemoteTrack, target: PlaybackTarget, capabilities: RemotePlaybackCapabilities): RemotePlaybackItem {
			targets += target; val dlna = target is PlaybackTarget.Dlna
			return RemotePlaybackItem(track, "https://server/stream?id=${track.id}", if (dlna) "audio/mpeg" else track.sourceMimeType, dlna)
		}
	}
	private class FakeBackend(override val kind: BackendKind) : RemotePlaybackBackend {
		override val targets = MutableStateFlow<List<PlaybackTarget>>(emptyList()); val mutableStatus = MutableStateFlow(BackendPlaybackStatus()); override val status = mutableStatus
		var loaded: RemotePlaybackItem? = null; var loadedPosition = -1L; var failConnect = false; var stops = 0
		override fun setDiscoveryActive(active: Boolean) = Unit
		override suspend fun connect(target: PlaybackTarget) { if (failConnect) error("connect"); mutableStatus.value = BackendPlaybackStatus(true) }
		override suspend fun disconnect() { mutableStatus.value = BackendPlaybackStatus() }
		override suspend fun play() { mutableStatus.value = mutableStatus.value.copy(state = RemotePlaybackState.PLAYING) }
		override suspend fun pause() { mutableStatus.value = mutableStatus.value.copy(state = RemotePlaybackState.PAUSED) }
		override suspend fun stop() { stops++ }
		override suspend fun seekTo(positionMs: Long) = Unit; override suspend fun next() = Unit; override suspend fun previous() = Unit
		override suspend fun load(item: RemotePlaybackItem, queue: List<RemotePlaybackItem>, startIndex: Int, positionMs: Long) { loaded = item; loadedPosition = positionMs }
	}
}
