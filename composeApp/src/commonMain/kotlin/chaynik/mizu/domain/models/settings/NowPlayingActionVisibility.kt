package chaynik.mizu.domain.models.settings

data class NowPlayingActionVisibility(
	val lyrics: Boolean = true,
	val equalizer: Boolean = true,
	val outputDevices: Boolean = true,
	val sleepTimer: Boolean = true,
	val queue: Boolean = true
) {
	val anyVisible: Boolean
		get() = lyrics || equalizer || outputDevices || sleepTimer || queue
}
