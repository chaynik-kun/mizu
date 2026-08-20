package chaynik.mizu.domain.manager

import kotlin.random.Random
import chaynik.mizu.domain.models.NetworkState
import chaynik.mizu.domain.models.ServerConnectionState

internal class ReachabilityBackoff(
	private val delays: LongArray = longArrayOf(1_000, 2_000, 4_000, 8_000, 15_000, 30_000, 60_000),
	private val jitterFraction: Double = .2,
	private val random: () -> Double = { Random.nextDouble() }
) {
	private var failures = 0

	fun reset() { failures = 0 }

	fun nextDelayMillis(): Long {
		val base = delays[failures.coerceAtMost(delays.lastIndex)]
		failures++
		val factor = 1.0 + ((random() * 2.0 - 1.0) * jitterFraction)
		return (base * factor).toLong().coerceAtLeast(0)
	}
}

internal fun shouldProbeImmediately(previous: NetworkState, current: NetworkState): Boolean =
	current.available && previous != current

internal fun reachabilityRetryDelay(state: ServerConnectionState, backoff: ReachabilityBackoff): Long = when (state) {
	ServerConnectionState.Reachable -> 20_000L
	ServerConnectionState.AuthenticationFailed,
	ServerConnectionState.TlsFailure,
	ServerConnectionState.IncompatibleServer -> 60_000L
	else -> backoff.nextDelayMillis()
}
