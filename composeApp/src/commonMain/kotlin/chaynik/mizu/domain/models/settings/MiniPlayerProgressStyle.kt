package chaynik.mizu.domain.models.settings

import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.option_mini_player_progress_style_bottom_bar
import mizu.composeapp.generated.resources.option_mini_player_progress_style_full_background
import org.jetbrains.compose.resources.StringResource

enum class MiniPlayerProgressStyle(val displayName: StringResource) {
	FullBackground(Res.string.option_mini_player_progress_style_full_background),
	BottomBar(Res.string.option_mini_player_progress_style_bottom_bar)
}

fun miniPlayerProgressFraction(positionMs: Long, durationMs: Long): Float {
	if (durationMs <= 0L || positionMs <= 0L) return 0f
	return (positionMs.toDouble() / durationMs.toDouble()).toFloat().coerceIn(0f, 1f)
}

fun sanitizedMiniPlayerProgress(progress: Float): Float =
	if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
