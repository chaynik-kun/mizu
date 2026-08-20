package chaynik.mizu.domain.manager

enum class DlnaTransportState { PLAYING, PAUSED_PLAYBACK, STOPPED, TRANSITIONING, NO_MEDIA_PRESENT, UNKNOWN }

class DlnaQueueAdvanceGuard {
	private var previous = DlnaTransportState.UNKNOWN
	private var advancing = false
	private var manualStop = false

	fun onManualStop() { manualStop = true }
	fun onLoadStarted() { advancing = true; manualStop = false; previous = DlnaTransportState.TRANSITIONING }
	fun onLoadReady() { advancing = false; previous = DlnaTransportState.PLAYING }

	fun onState(state: DlnaTransportState, hasNext: Boolean): Boolean {
		val naturalEnd = !manualStop && !advancing && hasNext && previous == DlnaTransportState.PLAYING &&
			(state == DlnaTransportState.STOPPED || state == DlnaTransportState.NO_MEDIA_PRESENT)
		if (naturalEnd) advancing = true
		if (state == DlnaTransportState.PLAYING) { manualStop = false; advancing = false }
		previous = state
		return naturalEnd
	}
}
