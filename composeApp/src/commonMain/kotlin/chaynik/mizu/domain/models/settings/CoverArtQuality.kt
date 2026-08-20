package chaynik.mizu.domain.models.settings

import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.option_quality_high
import mizu.composeapp.generated.resources.option_quality_low
import mizu.composeapp.generated.resources.option_quality_medium
import org.jetbrains.compose.resources.StringResource

enum class CoverArtQuality(
	val displayName: StringResource,
	val value: Int
) {
	Low(Res.string.option_quality_low, 512),
	Medium(Res.string.option_quality_medium, 1024),
	High(Res.string.option_quality_high, 4096)
}
