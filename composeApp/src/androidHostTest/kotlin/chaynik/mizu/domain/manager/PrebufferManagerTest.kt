package chaynik.mizu.domain.manager

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PrebufferManagerTest {
	private val a = candidate("a")
	private val b = candidate("b")
	private val c = candidate("c")

	@Test fun normalNextItemUsesEngineIndex() = assertEquals(b, selectPrebufferCandidate(0, 1, listOf(a,b,c)))
	@Test fun shuffleNextItemUsesEngineIndex() = assertEquals(c, selectPrebufferCandidate(0, 2, listOf(a,b,c)))
	@Test fun repeatOneDoesNotPreloadCurrent() = assertNull(selectPrebufferCandidate(1, 1, listOf(a,b,c)))
	@Test fun repeatAllUsesWrappedEngineIndex() = assertEquals(a, selectPrebufferCandidate(2, 0, listOf(a,b,c)))
	@Test fun nextItemRemovalSelectsNewEngineNext() = assertEquals(c, selectPrebufferCandidate(0, 1, listOf(a,c)))
	@Test fun queueReplacementUsesReplacement() = assertEquals(c, selectPrebufferCandidate(0, 1, listOf(a,c)))
	@Test fun prebufferStopsAtTargetWithoutTreatingTargetAsResourceLength() {
		assertFalse(prebufferTargetReached(TARGET - 1, TARGET))
		assertTrue(prebufferTargetReached(TARGET, TARGET))
		assertTrue(prebufferTargetReached(TARGET + 1, TARGET))
	}

	@Test fun alreadyCachedDoesNotDownload() = runTest {
		val loader = FakeLoader(cached = TARGET); val manager = manager(backgroundScope, loader); manager.updatePlaybackContext(context(b)); runCurrent()
		assertEquals(0, loader.loads); assertIs<PrebufferState.Ready>(manager.state.value)
	}
	@Test fun partialCacheLoadsMissingRange() = runTest {
		val loader = FakeLoader(cached = 100, completeImmediately = true); val manager = manager(backgroundScope, loader); manager.updatePlaybackContext(context(b)); runCurrent()
		assertEquals(1, loader.loads); assertEquals(TARGET, loader.requestedTarget); assertIs<PrebufferState.Ready>(manager.state.value)
	}
	@Test fun playbackCacheKeyIsUsedUnchanged() = runTest {
		val loader = FakeLoader(); val manager = manager(backgroundScope, loader); manager.updatePlaybackContext(context(b)); runCurrent()
		assertEquals(b.cacheKey, loader.candidate?.cacheKey)
	}
	@Test fun transcodingProfileComesFromCandidateUri() = runTest {
		val mp3 = b.copy(uri="https://s/stream?id=b&format=mp3&maxBitRate=192", cacheKey="v2:s:b:192:mp3")
		val loader=FakeLoader(); manager(backgroundScope, loader).updatePlaybackContext(context(mp3)); runCurrent(); assertEquals(mp3.uri, loader.candidate?.uri)
	}
	@Test fun queueReorderCancelsOldPreload() = replacementCancels(b,c)
	@Test fun profileChangeCancelsOldPreload() = replacementCancels(b,b.copy(uri="https://s/b?format=mp3",cacheKey="v2:s:b:192:mp3"))
	@Test fun serverChangeCancelsOldPreload() = replacementCancels(b,b.copy(serverNamespace="other",uri="https://other/b"))
	@Test fun networkLossStopsPreload() = runTest { val l=FakeLoader(); val m=manager(backgroundScope,l); m.updatePlaybackContext(context(b)); runCurrent(); m.updatePlaybackContext(context(b,online=false)); runCurrent(); assertTrue(l.cancels>0); assertIs<PrebufferState.Idle>(m.state.value) }
	@Test fun networkRestoreStartsCurrentNext() = runTest { val l=FakeLoader(); val m=manager(backgroundScope,l); m.updatePlaybackContext(context(b,online=false)); m.updatePlaybackContext(context(b)); runCurrent(); assertEquals(1,l.loads) }
	@Test fun localToDlnaDisables() = remoteDisables()
	@Test fun dlnaToLocalEnables() = runTest { val l=FakeLoader(); val m=manager(backgroundScope,l); m.updatePlaybackContext(context(b,local=false)); m.updatePlaybackContext(context(b)); runCurrent(); assertEquals(1,l.loads) }
	@Test fun onlyOnePreloadJobActive() = runTest { val l=FakeLoader(); val m=manager(backgroundScope,l); m.updatePlaybackContext(context(b)); m.updatePlaybackContext(context(b)); runCurrent(); assertEquals(1,l.loads); assertEquals(1,l.maxActive) }

	private fun replacementCancels(first: PrebufferCandidate, second: PrebufferCandidate) = runTest {
		val l=FakeLoader(); val m=manager(backgroundScope,l); m.updatePlaybackContext(context(first)); runCurrent(); m.updatePlaybackContext(context(second)); runCurrent()
		assertTrue(l.cancels>0); assertEquals(second,l.candidate); assertEquals(1,l.maxActive)
	}
	private fun remoteDisables() = runTest { val l=FakeLoader(); val m=manager(backgroundScope,l); m.updatePlaybackContext(context(b)); runCurrent(); m.updatePlaybackContext(context(b,local=false)); runCurrent(); assertTrue(l.cancels>0); assertIs<PrebufferState.Disabled>(m.state.value) }
	private fun manager(scope: kotlinx.coroutines.CoroutineScope, loader: FakeLoader) = DefaultPrebufferManager(scope, loader, PrebufferPolicy(targetBytes=TARGET, maximumBytes=TARGET))
	private fun context(candidate: PrebufferCandidate, online:Boolean=true, local:Boolean=true) = PrebufferContext(candidate,true,online,local,true)
	private fun candidate(id:String)=PrebufferCandidate(id,"https://s/$id?format=raw","v2:s:$id:0:raw","s")

	private class FakeLoader(var cached:Long=0, val completeImmediately:Boolean=false):PrebufferLoader {
		var loads=0; var cancels=0; var active=0; var maxActive=0; var requestedTarget=0L; var candidate:PrebufferCandidate?=null
		override fun cachedBytes(candidate:PrebufferCandidate,targetBytes:Long)=cached
		override suspend fun load(candidate:PrebufferCandidate,targetBytes:Long,onProgress:(Long)->Unit){ loads++; this.candidate=candidate; requestedTarget=targetBytes; active++; maxActive=maxOf(maxActive,active); try { if(completeImmediately){cached=targetBytes;onProgress(targetBytes)} else awaitCancellation() } finally {active--} }
		override fun cancel(){cancels++}
	}
	companion object { const val TARGET=1024L }
}
