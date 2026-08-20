package chaynik.mizu.domain.models.settings

import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.option_navigation_bar_style_capsule
import mizu.composeapp.generated.resources.option_navigation_bar_style_normal
import org.jetbrains.compose.resources.StringResource

enum class NavigationBarStyle(val displayName: StringResource) {
	Capsule(Res.string.option_navigation_bar_style_capsule),
	Normal(Res.string.option_navigation_bar_style_normal)
}
