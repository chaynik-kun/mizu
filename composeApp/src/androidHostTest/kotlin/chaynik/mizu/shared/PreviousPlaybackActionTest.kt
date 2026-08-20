package chaynik.mizu.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class PreviousPlaybackActionTest {
	@Test
	fun media3PreviousItemWinsRegardlessOfPlaybackPosition() {
		assertEquals(
			PreviousPlaybackAction.PreviousItem,
			previousPlaybackAction(hasPreviousMediaItem = true)
		)
	}

	@Test
	fun firstTimelineItemRestartsCurrentTrack() {
		assertEquals(
			PreviousPlaybackAction.RestartCurrent,
			previousPlaybackAction(hasPreviousMediaItem = false)
		)
	}

	@Test
	fun transitionUsesMedia3TimelineIndexOnlyWhenMediaIdMatchesQueue() {
		val queueIds = listOf("A", "B", "C")

		assertEquals(true, timelineItemMatches(2, "C", queueIds))
		assertEquals(false, timelineItemMatches(2, "B", queueIds))
	}
}
