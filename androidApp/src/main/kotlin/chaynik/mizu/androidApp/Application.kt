package chaynik.mizu.androidApp

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.util.Log
import coil3.SingletonImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module
import chaynik.mizu.androidApp.shared.AndroidResourceProvider
import chaynik.mizu.di.initKoin
import chaynik.mizu.di.initializeSingletonImageLoader
import chaynik.mizu.util.core.ResourceProvider
import kotlin.system.exitProcess

class Application : android.app.Application() {
	override fun onCreate() {
		super.onCreate()

		if (isCrashProcess()) {
			return
		}

		SingletonImageLoader.setSafe { context ->
			initializeSingletonImageLoader(context)
		}

		Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
			try {
				val intent = Intent(this, CrashActivity::class.java).apply {
					putExtra(
						"stacktrace",
						if (BuildConfig.DEBUG) Log.getStackTraceString(throwable) else throwable.javaClass.simpleName
					)
					flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
				}
				startActivity(intent)
			} catch (e: Exception) {
				Log.e("Application", "failed to start CrashActivity", e)
			} finally {
				exitProcess(1)
			}
		}

		initKoin {
			modules(module {
				single<ResourceProvider> {
					AndroidResourceProvider(this@Application)
				}
			})
			androidContext(this@Application)
			androidLogger()
		}
	}

	private fun isCrashProcess(): Boolean {
		val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getProcessName()
		} else {
			val pid = android.os.Process.myPid()
			val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
			am.runningAppProcesses?.find { it.pid == pid }?.processName
		}
		return processName?.endsWith(":crash") == true
	}
}
