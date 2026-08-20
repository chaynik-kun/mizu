package chaynik.mizu.androidApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.koin.android.ext.android.inject
import chaynik.mizu.App
import chaynik.mizu.domain.manager.PermissionManager

class MainActivity : ComponentActivity() {
	private val permissionManager: PermissionManager by inject()
	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)
		permissionManager.registerLauncher(this)
		enableEdgeToEdge()
		setContent { App(developerBuild = BuildConfig.DEBUG) }
	}

	override fun onResume() {
		super.onResume()
		requestHighestRefreshRate()
	}

	@Suppress("DEPRECATION")
	private fun requestHighestRefreshRate() {
		val display = windowManager.defaultDisplay
		val currentMode = display.mode
		val bestMode = display.supportedModes
			.asSequence()
			.filter {
				it.physicalWidth == currentMode.physicalWidth &&
					it.physicalHeight == currentMode.physicalHeight
			}
			.maxByOrNull { it.refreshRate }

		bestMode?.let { mode ->
			window.attributes = window.attributes.apply {
				preferredDisplayModeId = mode.modeId
				preferredRefreshRate = mode.refreshRate
			}
		}
	}
}
