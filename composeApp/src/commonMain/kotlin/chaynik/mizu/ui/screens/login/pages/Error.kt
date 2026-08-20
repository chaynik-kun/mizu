package chaynik.mizu.ui.screens.login.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.core.LoginUiState
import chaynik.mizu.domain.manager.LoginException
import chaynik.mizu.domain.manager.LoginErrorCode
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.error_login_unknown_host
import mizu.composeapp.generated.resources.error_login_timeout
import mizu.composeapp.generated.resources.error_login_tls
import mizu.composeapp.generated.resources.error_login_credentials
import mizu.composeapp.generated.resources.error_login_unsupported_server
import mizu.composeapp.generated.resources.error_login_insecure_http
import mizu.composeapp.generated.resources.error_login_generic
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginScreenError(
	loginUiState: LoginUiState
) {
	val spatialSpec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
	val effectSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
	AnimatedContent(
		(loginUiState as? LoginUiState.Error),
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
		transitionSpec = {
			(fadeIn(
				animationSpec = effectSpec
			) + scaleIn(
				initialScale = 0.8f,
				animationSpec = spatialSpec
			)) togetherWith (fadeOut(
				animationSpec = effectSpec
			) + scaleOut(
				animationSpec = spatialSpec
			))
		}
	) {
		if (it != null) {
			val code = (it.error as? LoginException)?.code ?: LoginErrorCode.Generic
			val message = stringResource(when (code) {
				LoginErrorCode.UnknownHost -> Res.string.error_login_unknown_host
				LoginErrorCode.Timeout -> Res.string.error_login_timeout
				LoginErrorCode.Tls -> Res.string.error_login_tls
				LoginErrorCode.InvalidCredentials -> Res.string.error_login_credentials
				LoginErrorCode.UnsupportedServer -> Res.string.error_login_unsupported_server
				LoginErrorCode.InsecureHttp -> Res.string.error_login_insecure_http
				LoginErrorCode.Generic -> Res.string.error_login_generic
			})
			Form(modifier = Modifier.fillMaxWidth(), bottomPadding = 8.dp) {
				FormRow(color = MaterialTheme.colorScheme.errorContainer) {
					Text(message)
				}
			}
		}
	}
}
