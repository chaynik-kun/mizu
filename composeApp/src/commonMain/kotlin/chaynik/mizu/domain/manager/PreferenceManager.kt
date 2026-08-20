package chaynik.mizu.domain.manager

import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import chaynik.mizu.domain.manager.base.BasePreferenceManager
import chaynik.mizu.domain.models.settings.AnimationStyle
import chaynik.mizu.domain.models.settings.AppIconVariant
import chaynik.mizu.domain.models.settings.BottomBarCollapseMode
import chaynik.mizu.domain.models.settings.BottomBarVisibilityMode
import chaynik.mizu.domain.models.settings.CoverArtQuality
import chaynik.mizu.domain.models.settings.CoverArtShape
import chaynik.mizu.domain.models.settings.CoverArtTapAction
import chaynik.mizu.domain.models.settings.ExplicitContentPlayback
import chaynik.mizu.domain.models.settings.FontOption
import chaynik.mizu.domain.models.settings.GridSize
import chaynik.mizu.domain.models.settings.HomeSection
import chaynik.mizu.domain.models.settings.MarqueeSpeed
import chaynik.mizu.domain.models.settings.MiniPlayerProgressStyle
import chaynik.mizu.domain.models.settings.NavigationBarLabelVisibility
import chaynik.mizu.domain.models.settings.NavigationBarStyle
import chaynik.mizu.domain.models.settings.NowPlayingBackgroundStyle
import chaynik.mizu.domain.models.settings.NowPlayingSliderStyle
import chaynik.mizu.domain.models.settings.OfflineMode
import chaynik.mizu.domain.models.settings.ReplayGainMode
import chaynik.mizu.domain.models.settings.StreamingQuality
import chaynik.mizu.domain.models.settings.Theme
import chaynik.mizu.domain.models.settings.ThemeMode
import chaynik.mizu.domain.models.settings.ToolbarPosition
import com.russhwolf.settings.Settings as KmpSettings

class PreferenceManager(
	settings: KmpSettings
) : BasePreferenceManager(settings) {
	var appIconVariant by preference(AppIconVariant.Original)
	var font by preference(FontOption.System)
	var fontPath by preference("")
	var animationStyle by preference(AnimationStyle.Expressive)
	var nowPlayingBackgroundStyle by preference(NowPlayingBackgroundStyle.Static)
	var swipeToSkip by preference(true)
	var gridSize by preference(GridSize.TwoByTwo)
	var coverArtShape by preference(CoverArtShape.Soft)
	var artistImageShape by preference(CoverArtShape.Soft)
	var coverArtQuality by preference(CoverArtQuality.High)
	var artGridItemSize by preference(150f)
	var marqueeSpeed by preference(MarqueeSpeed.Medium)
	var alphabeticalScroll by preference(false)
	var homeSectionOrder by preference("homeSectionOrderV2", HomeSection.encode(HomeSection.entries))
	var homeHiddenSections by preference(
		"homeHiddenSectionsV2",
		listOf(
			HomeSection.RandomAlbums,
			HomeSection.FrequentlyPlayed,
			HomeSection.FavoriteAlbums,
			HomeSection.FavoriteArtists,
			HomeSection.Stations,
			HomeSection.Playlists,
			HomeSection.Artists,
			HomeSection.Genres
		).joinToString(",") { it.name }
	)
	var lyricsAutoscroll by preference(true)
	var lyricsBeatByBeat by preference(false)
	var lyricsKeepAlive by preference(true)
	var lyricsBlur by preference(false)
	var lyricsBrightInactive by preference(false)
	var lyricsFullScreen by preference(false)
	var enableScrobbling by preference(true)
	var scrobblePercentage by preference(.5f)
	var minDurationToScrobble by preference(30f)
	var replayGainMode by preference(ReplayGainMode.Off)
	var gaplessPlayback by preference(true)
	var audioOffload by preference(false)

	// TODO: better names and strings for these transcoding settings
	var streamingQualityWifi by preference(StreamingQuality.Lossless)
	var streamingQualityCellular by preference(StreamingQuality.Lossless)
	var isAdvancedTranscodingActive by preference(false)
	var customMaxBitrateWifi by preference(0)
	var customMaxBitrateCellular by preference(0)
	var customFormatWifi by preference("")
	var customFormatCellular by preference("")

	var downloadQualityWifi by preference(StreamingQuality.Lossless)
	var downloadQualityCellular by preference(StreamingQuality.Lossless)
	var isAdvancedDownloadTranscodingActive by preference(false)
	var customDownloadMaxBitrateWifi by preference(0)
	var customDownloadMaxBitrateCellular by preference(0)
	var customDownloadFormatWifi by preference("")
	var customDownloadFormatCellular by preference("")

	var nowPlayingToolbarPosition by preference(ToolbarPosition.Bottom)
	var nowPlayingSongInfo by preference(true)
	var nowPlayingSliderStyle by preference(NowPlayingSliderStyle.Slim)
	var nowPlayingCoverArtAction by preference(CoverArtTapAction.Disabled)
	private val showNowPlayingLyricsPreference = observablePreference("showNowPlayingLyrics", true)
	var showNowPlayingLyrics by showNowPlayingLyricsPreference
	val showNowPlayingLyricsFlow = showNowPlayingLyricsPreference.flow
	private val showNowPlayingEqualizerPreference = observablePreference("showNowPlayingEqualizer", true)
	var showNowPlayingEqualizer by showNowPlayingEqualizerPreference
	val showNowPlayingEqualizerFlow = showNowPlayingEqualizerPreference.flow
	private val showNowPlayingOutputPreference = observablePreference("showNowPlayingOutput", true)
	var showNowPlayingOutput by showNowPlayingOutputPreference
	val showNowPlayingOutputFlow = showNowPlayingOutputPreference.flow
	private val showNowPlayingSleepTimerPreference = observablePreference("showNowPlayingSleepTimer", true)
	var showNowPlayingSleepTimer by showNowPlayingSleepTimerPreference
	val showNowPlayingSleepTimerFlow = showNowPlayingSleepTimerPreference.flow
	private val showNowPlayingQueuePreference = observablePreference("showNowPlayingQueue", true)
	var showNowPlayingQueue by showNowPlayingQueuePreference
	val showNowPlayingQueueFlow = showNowPlayingQueuePreference.flow
	private val customHeadersPreference = observablePreference("customHeaders", "")
	var customHeaders by customHeadersPreference
	val customHeadersFlow = customHeadersPreference.flow
	var allowInsecureHttp by preference(false)
	private val automaticPlaybackCachePreference = observablePreference("automaticPlaybackCache", true)
	var automaticPlaybackCache by automaticPlaybackCachePreference
	val automaticPlaybackCacheFlow = automaticPlaybackCachePreference.flow
	private val preloadNextTrackPreference = observablePreference("preloadNextTrack", true)
	var preloadNextTrack by preloadNextTrackPreference
	val preloadNextTrackFlow = preloadNextTrackPreference.flow

	private val androidAutoShowAlbumsPreference = observablePreference("androidAutoShowAlbums", true)
	var androidAutoShowAlbums by androidAutoShowAlbumsPreference
	val androidAutoShowAlbumsFlow = androidAutoShowAlbumsPreference.flow
	private val androidAutoShowArtistsPreference = observablePreference("androidAutoShowArtists", true)
	var androidAutoShowArtists by androidAutoShowArtistsPreference
	val androidAutoShowArtistsFlow = androidAutoShowArtistsPreference.flow
	private val androidAutoShowPlaylistsPreference = observablePreference("androidAutoShowPlaylists", true)
	var androidAutoShowPlaylists by androidAutoShowPlaylistsPreference
	val androidAutoShowPlaylistsFlow = androidAutoShowPlaylistsPreference.flow
	private val androidAutoShowSongsPreference = observablePreference("androidAutoShowSongs", true)
	var androidAutoShowSongs by androidAutoShowSongsPreference
	val androidAutoShowSongsFlow = androidAutoShowSongsPreference.flow
	private val androidAutoShowRecentlyAddedPreference = observablePreference("androidAutoShowRecentlyAdded", true)
	var androidAutoShowRecentlyAdded by androidAutoShowRecentlyAddedPreference
	val androidAutoShowRecentlyAddedFlow = androidAutoShowRecentlyAddedPreference.flow
	private val androidAutoShowRandomTracksPreference = observablePreference("androidAutoShowRandomTracks", true)
	var androidAutoShowRandomTracks by androidAutoShowRandomTracksPreference
	val androidAutoShowRandomTracksFlow = androidAutoShowRandomTracksPreference.flow
	private val androidAutoItemsPerSectionPreference = observablePreference("androidAutoItemsPerSection", 100)
	var androidAutoItemsPerSection by androidAutoItemsPerSectionPreference
	val androidAutoItemsPerSectionFlow = androidAutoItemsPerSectionPreference.flow
	var explicitContentPlayback by preference(ExplicitContentPlayback.Allowed)

	// navigation bar settings
	var bottomBarCollapseMode by preference(BottomBarCollapseMode.Never)
	var bottomBarVisibilityMode by preference(BottomBarVisibilityMode.AllScreens)
	var navigationBarStyle by preference(NavigationBarStyle.Capsule)
	var navigationBarLabelVisibility by preference(
		NavigationBarLabelVisibility.Always
	)
	// V2 intentionally uses a new key: old ordinal values represented
	// Hidden/Visible/Seekable and cannot safely be decoded as the new enum.
	private val initialMiniPlayerProgressStyle =
		if (settings.hasKey("miniPlayerProgressStyle")) MiniPlayerProgressStyle.BottomBar
		else MiniPlayerProgressStyle.FullBackground
	var miniPlayerProgressStyle by preference(
		"miniPlayerProgressStyleV2",
		initialMiniPlayerProgressStyle
	)

	// theme related settings
	var theme by preference(Theme.Turquoise)
	var themeMode by preference(ThemeMode.System)
	var dynamicTheming by preference(false)
	var paletteStyle by preference(PaletteStyle.TonalSpot)
	var paletteSpec by preference(ColorSpec.SpecVersion.SPEC_2025)
	// Seeded-theme accent, stored as an ARGB colour. Default is turquoise.
	var paletteAccentColor by preference(0xFF1DE9B6.toInt())

	// sync related settings
	var lastFullSyncTime by preference(0L)

	fun customHeadersMap(): Map<String, String> = parseCustomHeaders(customHeaders)

	private val offlineModePreference = observablePreference("offlineMode", OfflineMode.Auto)
	var offlineMode by offlineModePreference
	val offlineModeFlow = offlineModePreference.flow
}

fun parseCustomHeaders(rawHeaders: String): Map<String, String> = buildMap {
		val forbiddenHeaders = setOf("host", "content-length", "connection", "transfer-encoding")
		for (line in rawHeaders.split('\n')) {
			val parts = line.split(":", limit = 2)
			if (parts.size < 2) continue

			val rawKey = parts[0]
			val rawValue = parts[1]

			val key = rawKey.trim()
			val value = rawValue.trim()
			if (key.isNotEmpty() && value.isNotEmpty() && key.lowercase() !in forbiddenHeaders &&
				'\r' !in key && '\n' !in key && '\r' !in value && '\n' !in value
			) put(key, value)
		}
}
