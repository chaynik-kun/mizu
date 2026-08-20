package chaynik.mizu.ui.screens.radio.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme
import dev.zt64.compose.pipette.HsvColor
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.info_unknown
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.DomainRadio
import chaynik.mizu.domain.models.settings.ThemeMode
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.Radio
import chaynik.mizu.icons.filled.Play
import chaynik.mizu.ui.components.common.CoverArt
import chaynik.mizu.ui.theme.defaultFont
import kotlin.math.abs

@Composable
fun RadioListScreenCard(
	modifier: Modifier = Modifier,
	radio: DomainRadio,
	onPlayClick: () -> Unit
) {
	val inDarkTheme = isSystemInDarkTheme()
	val preferenceManager = koinInject<PreferenceManager>()

	val isDark = remember(preferenceManager.themeMode) {
		when (preferenceManager.themeMode) {
			ThemeMode.System -> inDarkTheme
			ThemeMode.Dark -> true
			ThemeMode.Light -> false
		}
	}

	val seedColor = remember(radio.name) {
		HsvColor(
			hue = abs(radio.name.hashCode() % 360).toFloat(),
			saturation = 0.5f,
			value = 0.6f
		).toColor()
	}

	val colorScheme = rememberDynamicColorScheme(
		seedColor = seedColor,
		isDark = isDark
	)

	Surface(
		modifier = modifier,
		color = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
		shape = MaterialTheme.shapes.medium,
		shadowElevation = 2.dp,
		onClick = onPlayClick
	) {
		Column(Modifier.fillMaxWidth()) {
			Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
				Icon(
					imageVector = Icons.Outlined.Radio,
					contentDescription = null,
					modifier = Modifier.align(Alignment.Center).size(72.dp).alpha(0.35f),
					tint = colorScheme.primary
				)
				CoverArt(
					coverArtId = radio.id,
					contentDescription = radio.name,
					modifier = Modifier.fillMaxSize(),
					shape = MaterialTheme.shapes.medium
				)
			}
			Row(
				modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Icon(Icons.Filled.Play, null, Modifier.size(20.dp))
				Text(
					text = radio.name,
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold,
					fontFamily = defaultFont(round = 100f),
					overflow = TextOverflow.Ellipsis,
					maxLines = 1
				)
			}
		}
	}
}
