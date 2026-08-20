package chaynik.mizu.external

import chaynik.mizu.domain.manager.*
import chaynik.mizu.domain.models.*
import chaynik.mizu.domain.manager.SessionManager

class NavidromeRemoteStreamResolver(private val sessionManager: SessionManager) : RemoteStreamResolver {
	override suspend fun resolve(track: RemoteTrack, target: PlaybackTarget, capabilities: RemotePlaybackCapabilities): RemotePlaybackItem {
		val kind = when (target) {
			is PlaybackTarget.Dlna -> BackendKind.DLNA
			PlaybackTarget.Local -> error("Local target does not require a remote stream")
		}
		val profile = selectRemoteStreamProfile(kind, track.sourceMimeType, capabilities)
		val uri = sessionManager.api.getStreamUrl(track.id, profile.maxBitRate ?: 0, profile.format)
		validateRemoteUri(uri)
		return RemotePlaybackItem(track, uri, profile.mimeType, profile.transcoded)
	}
}
