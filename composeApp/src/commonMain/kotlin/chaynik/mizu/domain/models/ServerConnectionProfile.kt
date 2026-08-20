package chaynik.mizu.domain.models

data class TimeoutPolicy(
	val connectMillis: Long = 10_000,
	val requestMillis: Long = 30_000,
	val socketMillis: Long = 30_000
)

data class RetryPolicy(val maxDelayMillis: Long = 60_000)
data class NetworkPolicy(val allowMetered: Boolean = true)

/** Shared declaration consumed by API, artwork and playback transports.
 * TLS identity, proxy and fallback endpoints deliberately belong here when implemented.
 */
data class ServerConnectionProfile(
	val serverId: String,
	val baseUrl: String,
	val username: String,
	val customHeaders: Map<String, String>,
	val timeoutPolicy: TimeoutPolicy = TimeoutPolicy(),
	val retryPolicy: RetryPolicy = RetryPolicy(),
	val networkPolicy: NetworkPolicy = NetworkPolicy()
)
