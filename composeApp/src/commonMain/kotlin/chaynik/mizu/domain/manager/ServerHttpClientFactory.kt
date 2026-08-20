package chaynik.mizu.domain.manager

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.plugins.api.createClientPlugin
import chaynik.mizu.domain.models.ServerConnectionProfile

private class OriginHeadersConfig {
	var serverUrl: String = ""
	var headers: Map<String, String> = emptyMap()
}

private val OriginHeaders = createClientPlugin("MizuOriginHeaders", ::OriginHeadersConfig) {
	val serverUrl = pluginConfig.serverUrl
	val headers = pluginConfig.headers
	onRequest { request, _ ->
		headersForDestination(serverUrl, request.url.buildString(), headers)
			.forEach { (name, value) -> request.headers.append(name, value) }
	}
}

class ServerHttpClientFactory(private val preferenceManager: PreferenceManager) {
	fun profile(baseUrl: String, username: String, serverId: String = "$baseUrl|$username") = ServerConnectionProfile(
		serverId = serverId,
		baseUrl = baseUrl,
		username = username,
		customHeaders = preferenceManager.customHeadersMap()
	)

	fun create(profile: ServerConnectionProfile) = create(
		connectTimeoutMillis = profile.timeoutPolicy.connectMillis,
		requestTimeoutMillis = profile.timeoutPolicy.requestMillis,
		socketTimeoutMillis = profile.timeoutPolicy.socketMillis,
		baseUrl = profile.baseUrl,
		headers = profile.customHeaders
	)

	fun configure(config: HttpClientConfig<*>, profile: ServerConnectionProfile) = with(config) {
		install(UserAgent) { agent = "Mizu" }
		install(HttpTimeout) {
			connectTimeoutMillis = profile.timeoutPolicy.connectMillis
			requestTimeoutMillis = profile.timeoutPolicy.requestMillis
			socketTimeoutMillis = profile.timeoutPolicy.socketMillis
		}
		if (profile.customHeaders.isNotEmpty()) install(OriginHeaders) {
			serverUrl = profile.baseUrl
			headers = profile.customHeaders
		}
	}

	fun create(
		connectTimeoutMillis: Long = 10_000,
		requestTimeoutMillis: Long = 30_000,
		socketTimeoutMillis: Long = 30_000,
		baseUrl: String? = null,
		headers: Map<String, String> = preferenceManager.customHeadersMap()
	) = HttpClient {
		install(UserAgent) { agent = "Mizu" }
		install(HttpTimeout) {
			this.connectTimeoutMillis = connectTimeoutMillis
			this.requestTimeoutMillis = requestTimeoutMillis
			this.socketTimeoutMillis = socketTimeoutMillis
		}
		if (headers.isNotEmpty() && baseUrl != null) install(OriginHeaders) {
			serverUrl = baseUrl
			this.headers = headers
		}
	}
}
