package chaynik.mizu.domain.parser

import chaynik.mizu.domain.models.lyrics.LyricsResult
import chaynik.mizu.domain.models.lyrics.LyricsTimingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LyricsContentParserTest {
	@Test fun parsesSupportedTimestampPrecisionsWithoutUnitLoss() {
		val lines = LyricsContentParser.parse("[01:02]a\n[01:02.5]b\n[01:02.50]c\n[01:02.500]d")!!
		assertEquals(listOf(62.seconds, 62.5.seconds, 62.5.seconds, 62.5.seconds), lines.map { it.time })
	}

	@Test fun expandsMultipleTimestampsAndAppliesOffset() {
		val lines = LyricsContentParser.parse("[offset:-250]\n[00:01.00][00:02.000]repeat")!!
		assertEquals(listOf(750.milliseconds, 1750.milliseconds), lines.map { it.time })
		assertEquals(listOf("repeat", "repeat"), lines.map { it.text })
	}

	@Test fun plainAndPartialLyricsKeepUntimedLinesUntimed() {
		val plain = LyricsContentParser.parse("one\ntwo")!!
		assertEquals(listOf(null, null), plain.map { it.time })
		val partial = LyricsContentParser.parse("intro\n[00:10.00]timed")!!
		assertEquals(10.seconds, partial.first().time)
		assertNull(partial.last().time)
		assertEquals(LyricsTimingType.PartiallySynced, LyricsResult(partial, "test").type)
	}

	@Test fun distinguishesPlainAndFullySyncedLyrics() {
		assertEquals(LyricsTimingType.Plain, LyricsResult(LyricsContentParser.parse("one")!!, "test").type)
		assertEquals(LyricsTimingType.LineSynced, LyricsResult(LyricsContentParser.parse("[00:01]one")!!, "test").type)
	}
}
