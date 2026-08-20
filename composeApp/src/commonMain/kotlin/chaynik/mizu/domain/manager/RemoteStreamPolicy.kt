package chaynik.mizu.domain.manager

import chaynik.mizu.domain.models.RemotePlaybackCapabilities

data class RemoteStreamProfile(val maxBitRate: Int?, val format: String?, val mimeType: String?, val transcoded: Boolean)

fun selectRemoteStreamProfile(kind: BackendKind, sourceMime: String?, capabilities: RemotePlaybackCapabilities): RemoteStreamProfile {
	if (sourceMime != null && sourceMime in capabilities.mimeTypes) return RemoteStreamProfile(null, null, sourceMime, false)
	return when (kind) {
		BackendKind.DLNA -> RemoteStreamProfile(320, "mp3", "audio/mpeg", true)
	}
}

fun sanitizeRemoteUrlForLog(url: String): String = url.replace(
	Regex("(?i)([?&](?:p|password|token|t|s|salt|auth|apikey|api_key|key)=)[^&#]*"),
	"$1<redacted>"
)

@Deprecated("Use sanitizeRemoteUrlForLog")
fun redactRemoteUrl(url: String) = sanitizeRemoteUrlForLog(url)

fun validateRemoteUri(uri: String) {
	val host = runCatching { io.ktor.http.Url(uri).host.lowercase() }.getOrNull()
	if (host == "localhost" || host == "127.0.0.1" || host == "::1") {
		throw IllegalArgumentException("Renderer cannot reach a loopback Navidrome address")
	}
}
