package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_acknowledgements
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.ui.components.layouts.NestedTopBar

@Composable
fun SettingsAcknowledgementsScreen() {
	val libraries by produceLibraries {
		Res.readBytes("files/acknowledgements.json").decodeToString()
	}
	Scaffold(
		topBar = { NestedTopBar({ Text(stringResource(Res.string.title_acknowledgements)) }) }
	) { innerPadding ->
		LibrariesContainer(
			libraries,
			modifier = Modifier
				.fillMaxSize(),
			contentPadding = innerPadding
		)
	}
}
