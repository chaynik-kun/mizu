package chaynik.mizu.domain.manager

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.zt64.subsonic.client.SubsonicAuth
import dev.zt64.subsonic.client.SubsonicClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SessionManager(
	private val settings: Settings,
	private val preferenceManager: PreferenceManager,
	private val credentialStore: CredentialStore,
	private val httpClientFactory: ServerHttpClientFactory
) {
	data class ServerIdentity(
		val product: String,
		val apiVersion: String,
		val serverVersion: String?,
		val openSubsonic: Boolean
	)

	val instanceUrl: String
		get() = settings.getString("instanceUrl", "")

	val username: String
		get() = settings.getString("username", "")

	val serverProduct: String
		get() = settings.getString("serverProduct", "Subsonic")

	val isLoggedIn: StateFlow<Boolean>
		field = MutableStateFlow(false)

	var api: SubsonicClient = createClient(
		instanceUrl = settings.getString("instanceUrl", ""),
		username = settings.getString("username", ""),
		password = credentialStore.getPassword(),
	)
		private set

	init {
		val legacyPassword = settings.getString("password", "")
		if (credentialStore.getPassword().isBlank() && legacyPassword.isNotBlank()) {
			credentialStore.setPassword(legacyPassword)
		}
		settings.remove("password")
		isLoggedIn.value = instanceUrl.isNotBlank() && username.isNotBlank() && credentialStore.getPassword().isNotBlank()
	}

	private fun createClient(
		instanceUrl: String,
		username: String,
		password: String,
	) = SubsonicClient.Companion(
		baseUrl = instanceUrl,
		auth = SubsonicAuth.Token(
			username = username,
			password = password,
		),
		client = "Mizu",
		clientConfig = {
			httpClientFactory.configure(this, httpClientFactory.profile(instanceUrl, username))
		}
	)

	private fun createProbeClient(instanceUrl: String, username: String) = httpClientFactory.create(
		httpClientFactory.profile(instanceUrl, username).copy(
			timeoutPolicy = chaynik.mizu.domain.models.TimeoutPolicy(requestMillis = 15_000, socketMillis = 15_000)
		)
	)

	private suspend fun probeServer(
		instanceUrl: String,
		username: String,
		password: String
	): ServerIdentity {
		val auth = SubsonicAuth.Token(username, password)
		val (httpStatus, responseText) = createProbeClient(instanceUrl, username).use { client ->
			val response = client.get("${instanceUrl.trimEnd('/')}/rest/ping.view") {
				parameter("u", auth.username)
				parameter("t", auth.token)
				parameter("s", auth.salt)
				parameter("v", "1.16.1")
				parameter("c", "Mizu")
				parameter("f", "json")
			}
			response.status to response.bodyAsText()
		}
		if (!httpStatus.isSuccess()) {
			throw Exception("Server returned HTTP ${httpStatus.value} instead of a Subsonic response.")
		}
		val root = runCatching { Json.parseToJsonElement(responseText).jsonObject }
			.getOrElse { throw Exception("The address does not return a valid Subsonic JSON response.", it) }
		val payload = root["subsonic-response"]?.jsonObject
			?: throw Exception("No Subsonic server was found at this address.")
		val status = payload["status"]?.jsonPrimitive?.contentOrNull
		if (status != "ok") {
			val message = payload["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
			throw Exception(message ?: "The Subsonic server rejected the login.")
		}
		val apiVersion = payload["version"]?.jsonPrimitive?.contentOrNull
			?: throw Exception("The server response does not contain a Subsonic API version.")
		val rawType = payload["type"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
		val serverVersion = payload["serverVersion"]?.jsonPrimitive?.contentOrNull
		val openSubsonic = payload["openSubsonic"]?.jsonPrimitive?.contentOrNull == "true"
		val product = when {
			rawType.equals("navidrome", ignoreCase = true) -> "Navidrome"
			rawType.isNotBlank() -> rawType.replaceFirstChar { it.uppercase() }
			openSubsonic -> "OpenSubsonic"
			else -> "Subsonic"
		}
		return ServerIdentity(product, apiVersion, serverVersion, openSubsonic)
	}

	suspend fun login(
		instanceUrl: String,
		username: String,
		password: String
	) {
		val identity = try {
			probeServer(instanceUrl, username, password)
		} catch (e: Exception) {
			throw mapNetworkError(e)
		}
		val client = createClient(instanceUrl, username, password)

		settings["instanceUrl"] = instanceUrl
		settings["username"] = username
		credentialStore.setPassword(password)
		settings["serverProduct"] = identity.product
		settings["serverApiVersion"] = identity.apiVersion
		settings["serverVersion"] = identity.serverVersion.orEmpty()
		settings["serverOpenSubsonic"] = identity.openSubsonic

		api = client
		isLoggedIn.value = true
	}

	private fun mapNetworkError(error: Throwable): LoginException {
		val causes = generateSequence(error) { it.cause }.toList()
		val code = when {
			causes.any { it::class.simpleName == "UnknownHostException" } ->
				LoginErrorCode.UnknownHost
			causes.any { it::class.simpleName?.contains("Timeout", ignoreCase = true) == true } ->
				LoginErrorCode.Timeout
			causes.any { it::class.simpleName?.contains("SSL", ignoreCase = true) == true || it::class.simpleName?.contains("Certificate", ignoreCase = true) == true } ->
				LoginErrorCode.Tls
			error.message?.contains("credential", ignoreCase = true) == true ||
				error.message?.contains("authentication", ignoreCase = true) == true ||
				error.message?.contains("password", ignoreCase = true) == true -> LoginErrorCode.InvalidCredentials
			error.message?.contains("Subsonic", ignoreCase = true) == true -> LoginErrorCode.UnsupportedServer
			else -> LoginErrorCode.Generic
		}
		return LoginException(code, error)
	}

	fun logout() {
		settings["username"] = null
		credentialStore.clear()
		isLoggedIn.value = false
	}

	fun refreshClient() {
		api = createClient(
			instanceUrl = settings.getString("instanceUrl", ""),
			username = settings.getString("username", ""),
			password = credentialStore.getPassword(),
		)
	}

	suspend fun getServerVersion(): String? {
		return settings.getString("serverVersion", "").ifBlank { null }
	}

	fun getCoverArtUrl(coverArtId: String) = api.getCoverArtUrl(
		coverArtId,
		auth = true,
		size = "${preferenceManager.coverArtQuality.value}"
	)

	fun isInsecureHttpAllowed() = preferenceManager.allowInsecureHttp
}
