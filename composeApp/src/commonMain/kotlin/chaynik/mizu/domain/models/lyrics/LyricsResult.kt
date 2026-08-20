package chaynik.mizu.domain.models.lyrics

import androidx.compose.runtime.Immutable

@Immutable
data class LyricsResult(
	val lines: List<LyricsLine>,
	val providerName: String,
	val rawContent: String? = null
) {
	val type: LyricsTimingType = when (lines.count { it.time != null }) {
		0 -> LyricsTimingType.Plain
		lines.size -> LyricsTimingType.LineSynced
		else -> LyricsTimingType.PartiallySynced
	}
	val isSynced: Boolean = type != LyricsTimingType.Plain
}

enum class LyricsTimingType { Plain, LineSynced, PartiallySynced }
