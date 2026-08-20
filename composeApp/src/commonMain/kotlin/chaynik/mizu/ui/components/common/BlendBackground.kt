package chaynik.mizu.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp

/**
 * A calm Now Playing background derived from the cover-art colour scheme.
 * The surrounding [MaterialTheme] is seeded from the current cover, so this
 * avoids decoding and animating another full-screen copy of the artwork.
 */
@Composable
fun BlendBackground(modifier: Modifier = Modifier) {
	val colors = MaterialTheme.colorScheme
	val surface = colors.surface
	// Keep one hue family throughout the screen. Mixing all three Material
	// containers produced muddy grey/purple bands for neutral cover art.
	val gradient = Brush.verticalGradient(
		colorStops = arrayOf(
			0f to lerp(surface, colors.primaryContainer, 0.72f),
			0.36f to lerp(surface, colors.primaryContainer, 0.56f),
			0.72f to lerp(surface, colors.primaryContainer, 0.30f),
			1f to surface
		)
	)
	Box(modifier.fillMaxSize().background(gradient))
}
