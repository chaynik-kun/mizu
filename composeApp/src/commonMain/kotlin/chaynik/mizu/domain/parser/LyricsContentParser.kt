package chaynik.mizu.domain.parser

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import chaynik.mizu.domain.models.lyrics.LyricsLine
import chaynik.mizu.domain.models.lyrics.LyricsWord
import chaynik.mizu.util.core.Logger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration

@Serializable
private data class YoulyResponse(
	val lyrics: List<YoulyLine> = emptyList()
)

@Serializable
private data class YoulyLine(
	val time: Long = 0L,
	val text: String = "",
	val syllabus: List<YoulySyllable>? = null
)

@Serializable
private data class YoulySyllable(
	val time: Long = 0L,
	val duration: Long = 0L,
	val text: String = ""
)

object LyricsContentParser {
	private val timestampTag = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")
	private val offsetTag = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)
	private val jsonParser = Json {
		isLenient = true
		explicitNulls = false
		ignoreUnknownKeys = true
	}

	fun parse(content: String): List<LyricsLine>? {
		val text = content.trim()
		if (text.isEmpty()) return null

		return try {
			if (text.startsWith("{")) {
				parseJson(text)
			} else {
				parseLrc(text)
			}
		} catch (e: Exception) {
			Logger.e("LyricRepository", "Lyrics parsing failed!", e)
			null
		}
	}

	private fun parseJson(jsonString: String): List<LyricsLine>? {
		val jsonObject = jsonParser.parseToJsonElement(jsonString).jsonObject

		val syncedStr = jsonObject["syncedLyrics"]?.jsonPrimitive?.contentOrNull
		if (!syncedStr.isNullOrEmpty()) {
			return parseLrc(syncedStr)
		}

		val plainStr = jsonObject["plainLyrics"]?.jsonPrimitive?.contentOrNull
		if (!plainStr.isNullOrEmpty()) {
			return plainStr.lineSequence()
				.map { LyricsLine(text = it.trim()) }
				.toList()
		}

		if (jsonObject.containsKey("lyrics")) {
			val youlyResponse = jsonParser.decodeFromString<YoulyResponse>(jsonString)
			return parseYoulyResponse(youlyResponse)
		}

		return null
	}

	private fun parseYoulyResponse(response: YoulyResponse): List<LyricsLine>? {
		if (response.lyrics.isEmpty()) return null
		return response.lyrics.map { line ->
			LyricsLine(
				time = line.time.milliseconds,
				text = line.text,
				words = line.syllabus?.map { syl ->
					LyricsWord(syl.time.milliseconds, syl.duration.milliseconds, syl.text)
				}
			)
		}.sortedBy { it.time }
	}

	private fun parseLrc(input: String): List<LyricsLine> {
		val lines = input.lineSequence().toList()

		if (!input.contains("[")) {
			return lines.map { LyricsLine(text = it.trim()) }
		}

		val offsetMs = offsetTag.find(input)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
		return lines
			.filter { it.isNotBlank() }
			.flatMap { line ->
				val matches = timestampTag.findAll(line).toList()
				if (matches.isEmpty()) {
					if (line.startsWith("[") && line.contains("]")) emptyList()
					else listOf(LyricsLine(text = line.trim()))
				} else {
					val text = line.substring(matches.last().range.last + 1).trim()
					if (text.isEmpty()) emptyList() else matches.mapNotNull { match ->
						val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
						val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
						if (seconds !in 0..59) return@mapNotNull null
						val fraction = match.groupValues[3]
						val fractionMs = fraction.takeIf { it.isNotEmpty() }
							?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
						val raw = minutes.minutes + seconds.seconds + fractionMs.milliseconds + offsetMs.milliseconds
						LyricsLine(time = raw.coerceAtLeast(Duration.ZERO), text = text)
					}
				}
			}
			.sortedBy { it.time ?: Duration.INFINITE }
	}
}
