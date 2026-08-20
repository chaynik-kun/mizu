package chaynik.mizu.domain.manager

import kotlin.test.*

class PermissionRequestPolicyTest {
	@Test fun unavailableLauncherDoesNotStartRequest() = assertFalse(canStartPermissionRequest(false))
	@Test fun registeredLauncherCanStartRequest() = assertTrue(canStartPermissionRequest(true))
}
