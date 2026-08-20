package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_lyrics
import mizu.composeapp.generated.resources.option_cover_art_action
import mizu.composeapp.generated.resources.option_lyrics_autoscroll
import mizu.composeapp.generated.resources.option_lyrics_beat_by_beat
import mizu.composeapp.generated.resources.option_lyrics_blur
import mizu.composeapp.generated.resources.option_lyrics_bright_inactive
import mizu.composeapp.generated.resources.option_lyrics_full_screen
import mizu.composeapp.generated.resources.option_lyrics_keep_alive
import mizu.composeapp.generated.resources.option_lyrics_priority
import mizu.composeapp.generated.resources.option_now_playing_background_style
import mizu.composeapp.generated.resources.option_now_playing_slider_style
import mizu.composeapp.generated.resources.option_now_playing_song_info
import mizu.composeapp.generated.resources.option_now_playing_toolbar_position
import mizu.composeapp.generated.resources.option_swipe_to_skip
import mizu.composeapp.generated.resources.subtitle_lyrics_full_screen
import mizu.composeapp.generated.resources.subtitle_now_playing_background_style
import mizu.composeapp.generated.resources.title_layout
import mizu.composeapp.generated.resources.title_now_playing_actions
import mizu.composeapp.generated.resources.option_show_action_lyrics
import mizu.composeapp.generated.resources.option_show_action_equalizer
import mizu.composeapp.generated.resources.option_show_action_output
import mizu.composeapp.generated.resources.option_show_action_sleep_timer
import mizu.composeapp.generated.resources.option_show_action_queue
import mizu.composeapp.generated.resources.title_now_playing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalPlatformContext
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.CoverArtTapAction
import chaynik.mizu.domain.models.settings.NowPlayingBackgroundStyle
import chaynik.mizu.domain.models.settings.ToolbarPosition
import chaynik.mizu.domain.models.settings.NowPlayingActionVisibility
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.common.FormTitle
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.screens.settings.components.SettingSelectionRow
import chaynik.mizu.ui.screens.settings.components.SettingSwitchRow
import chaynik.mizu.ui.screens.settings.dialogs.LyricsPriorityDialog
import chaynik.mizu.ui.screens.settings.dialogs.NowPlayingSliderStyleDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SettingsNowPlayingScreen() {
	val platformContext = LocalPlatformContext.current
	val preferenceManager = koinInject<PreferenceManager>()
	val actionVisibilityFlow = remember(preferenceManager) {
		combine(
			preferenceManager.showNowPlayingLyricsFlow,
			preferenceManager.showNowPlayingEqualizerFlow,
			preferenceManager.showNowPlayingOutputFlow,
			preferenceManager.showNowPlayingSleepTimerFlow,
			preferenceManager.showNowPlayingQueueFlow
		) { lyrics, equalizer, output, sleep, queue ->
			NowPlayingActionVisibility(lyrics, equalizer, output, sleep, queue)
		}.distinctUntilChanged()
	}
	val actionVisibility by actionVisibilityFlow.collectAsState(NowPlayingActionVisibility())
	var showLyricsPriorityDialog by rememberSaveable { mutableStateOf(false) }

	Scaffold(
		topBar = {
			NestedTopBar(
				{ Text(stringResource(Res.string.title_now_playing)) },
				hideBack = platformContext.sizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
			)
		}
	) { innerPadding ->
		CompositionLocalProvider(
			LocalMinimumInteractiveComponentSize provides 0.dp
		) {
			Column(
				Modifier
					.padding(innerPadding)
					.verticalScroll(rememberScrollState())
					.padding(top = 16.dp, end = 16.dp, start = 16.dp)
			) {
				Form {
					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_swipe_to_skip)) },
						value = preferenceManager.swipeToSkip,
						onSetValue = { preferenceManager.swipeToSkip = it }
					)

					SettingSelectionRow(
						items = CoverArtTapAction.entries.toImmutableList(),
						label = { stringResource(it.displayName) },
						selection = preferenceManager.nowPlayingCoverArtAction,
						onSelect = { preferenceManager.nowPlayingCoverArtAction = it },
						title = { Text(stringResource(Res.string.option_cover_art_action)) }
					)

					SettingSelectionRow(
						items = NowPlayingBackgroundStyle.entries.toImmutableList(),
						label = { stringResource(it.displayName) },
						selection = preferenceManager.nowPlayingBackgroundStyle,
						onSelect = { preferenceManager.nowPlayingBackgroundStyle = it },
						description = stringResource(Res.string.subtitle_now_playing_background_style),
						title = { Text(stringResource(Res.string.option_now_playing_background_style)) }
					)

					var showSliderStyleDialog by rememberSaveable { mutableStateOf(false) }
					FormRow(
						onClick = {
							showSliderStyleDialog = true
						}
					) {
						Column(Modifier.weight(1f)) {
							Text(stringResource(Res.string.option_now_playing_slider_style))
							Text(
								stringResource(preferenceManager.nowPlayingSliderStyle.displayName),
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}

					NowPlayingSliderStyleDialog(
						presented = showSliderStyleDialog,
						onDismissRequest = { showSliderStyleDialog = false }
					)
				}

				FormTitle(stringResource(Res.string.action_lyrics))
				Form {
					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_lyrics_autoscroll)) },
						value = preferenceManager.lyricsAutoscroll,
						onSetValue = { preferenceManager.lyricsAutoscroll = it }
					)

					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_lyrics_beat_by_beat)) },
						value = preferenceManager.lyricsBeatByBeat,
						onSetValue = { preferenceManager.lyricsBeatByBeat = it }
					)

					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_lyrics_keep_alive)) },
						value = preferenceManager.lyricsKeepAlive,
						onSetValue = { preferenceManager.lyricsKeepAlive = it }
					)

					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_lyrics_blur)) },
						value = preferenceManager.lyricsBlur,
						onSetValue = { preferenceManager.lyricsBlur = it }
					)

					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_lyrics_bright_inactive)) },
						value = preferenceManager.lyricsBrightInactive,
						onSetValue = { preferenceManager.lyricsBrightInactive = it }
					)

					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_lyrics_full_screen)) },
						subtitle = { Text(stringResource(Res.string.subtitle_lyrics_full_screen)) },
						value = preferenceManager.lyricsFullScreen,
						onSetValue = { preferenceManager.lyricsFullScreen = it }
					)

					FormRow(
						onClick = { showLyricsPriorityDialog = true }
					) {
						Text(stringResource(Res.string.option_lyrics_priority))
					}
				}

				FormTitle(stringResource(Res.string.title_layout))
				Form {
					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_now_playing_song_info)) },
						value = preferenceManager.nowPlayingSongInfo,
						onSetValue = { preferenceManager.nowPlayingSongInfo = it }
					)

					SettingSelectionRow(
						items = ToolbarPosition.entries.toImmutableList(),
						label = { stringResource(it.displayName) },
						selection = preferenceManager.nowPlayingToolbarPosition,
						onSelect = { preferenceManager.nowPlayingToolbarPosition = it },
						title = { Text(stringResource(Res.string.option_now_playing_toolbar_position)) }
					)
				}

				FormTitle(stringResource(Res.string.title_now_playing_actions))
				Form {
					SettingSwitchRow(title = { Text(stringResource(Res.string.option_show_action_lyrics)) }, value = actionVisibility.lyrics, onSetValue = { preferenceManager.showNowPlayingLyrics = it })
					SettingSwitchRow(title = { Text(stringResource(Res.string.option_show_action_equalizer)) }, value = actionVisibility.equalizer, onSetValue = { preferenceManager.showNowPlayingEqualizer = it })
					SettingSwitchRow(title = { Text(stringResource(Res.string.option_show_action_output)) }, value = actionVisibility.outputDevices, onSetValue = { preferenceManager.showNowPlayingOutput = it })
					SettingSwitchRow(title = { Text(stringResource(Res.string.option_show_action_sleep_timer)) }, value = actionVisibility.sleepTimer, onSetValue = { preferenceManager.showNowPlayingSleepTimer = it })
					SettingSwitchRow(title = { Text(stringResource(Res.string.option_show_action_queue)) }, value = actionVisibility.queue, onSetValue = { preferenceManager.showNowPlayingQueue = it })
				}
			}
		}
		LyricsPriorityDialog(
			presented = showLyricsPriorityDialog,
			onDismissRequest = { showLyricsPriorityDialog = false }
		)
	}
}
