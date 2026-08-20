package chaynik.mizu.domain.manager

import io.ktor.http.Url

data class ServerOrigin(val scheme: String, val host: String, val port: Int)

fun normalizedOrigin(url: String): ServerOrigin? = runCatching {
	val parsed = Url(url)
	ServerOrigin(parsed.protocol.name.lowercase(), parsed.host.lowercase(), parsed.port)
}.getOrNull()

fun headersForDestination(
	serverUrl: String,
	destinationUrl: String,
	headers: Map<String, String>
): Map<String, String> = if (normalizedOrigin(serverUrl) == normalizedOrigin(destinationUrl)) headers else emptyMap()
