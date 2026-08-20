package chaynik.mizu.ui.screens.lyrics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.notice_loading_lyrics
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsScreenLoadingView() {
	Column(
		Modifier.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
	) {
		ContainedLoadingIndicator(
			Modifier.size(80.dp)
		)
		Text(
			stringResource(Res.string.notice_loading_lyrics),
			textAlign = TextAlign.Center,
			fontWeight = FontWeight(600),
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}
