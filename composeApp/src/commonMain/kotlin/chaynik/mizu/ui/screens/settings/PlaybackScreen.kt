package chaynik.mizu.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.collections.immutable.toImmutableList
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.option_audio_offload
import mizu.composeapp.generated.resources.option_equalizer_custom
import mizu.composeapp.generated.resources.option_equalizer_disabled
import mizu.composeapp.generated.resources.option_enable_scrobbling
import mizu.composeapp.generated.resources.option_explicit_playback
import mizu.composeapp.generated.resources.option_gapless_playback
import mizu.composeapp.generated.resources.option_min_duration_to_scrobble
import mizu.composeapp.generated.resources.option_preload_next_track
import mizu.composeapp.generated.resources.option_replay_gain
import mizu.composeapp.generated.resources.option_scrobble_percentage
import mizu.composeapp.generated.resources.subtitle_audio_offload
import mizu.composeapp.generated.resources.subtitle_enable_scrobbling
import mizu.composeapp.generated.resources.subtitle_gapless_playback
import mizu.composeapp.generated.resources.subtitle_streaming_quality
import mizu.composeapp.generated.resources.title_behaviour
import mizu.composeapp.generated.resources.title_equalizer
import mizu.composeapp.generated.resources.title_playback
import mizu.composeapp.generated.resources.title_streaming_quality
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack
import chaynik.mizu.LocalPlatformContext
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.ExplicitContentPlayback
import chaynik.mizu.domain.models.settings.ReplayGainMode
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.outlined.ChevronForward
import chaynik.mizu.ui.components.common.Form
import chaynik.mizu.ui.components.common.FormRow
import chaynik.mizu.ui.components.common.FormTitle
import chaynik.mizu.ui.components.layouts.NestedTopBar
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.settings.components.SettingSelectionRow
import chaynik.mizu.ui.screens.settings.components.SettingSwitchRow
import chaynik.mizu.util.core.PlatformType
import kotlin.math.roundToInt

@Composable
fun SettingsPlaybackScreen() {
	val platformContext = LocalPlatformContext.current
	val backStack = LocalNavStack.current
	val preferenceManager = koinInject<PreferenceManager>()
	var equalizerVisible by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			NestedTopBar(
				{ Text(stringResource(Res.string.title_playback)) },
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
						title = { Text(stringResource(Res.string.option_preload_next_track)) },
						value = preferenceManager.preloadNextTrack,
						onSetValue = { preferenceManager.preloadNextTrack = it }
					)
					FormRow(
						onClick = { equalizerVisible = true },
						horizontalArrangement = Arrangement.Start
					) {
						val equalizerState by koinInject<chaynik.mizu.domain.manager.EqualizerController>().state.collectAsState()
						Column(Modifier.weight(1f)) {
							Text(stringResource(Res.string.title_equalizer))
							Text(if (!equalizerState.enabled) stringResource(Res.string.option_equalizer_disabled) else equalizerState.selectedPreset?.name ?: stringResource(Res.string.option_equalizer_custom), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
						Icon(Icons.Outlined.ChevronForward, null)
					}
					FormRow(
						onClick = dropUnlessResumed { backStack.add(Screen.Settings.StreamingQuality) },
						horizontalArrangement = Arrangement.Start
					) {
						Column(Modifier.weight(1f)) {
							Text(stringResource(Res.string.title_streaming_quality))
							Text(
								text = stringResource(Res.string.subtitle_streaming_quality),
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
						Icon(Icons.Outlined.ChevronForward, null)
					}
					if (platformContext.platformType == PlatformType.Android) {
						SettingSelectionRow(
							title = { Text(stringResource(Res.string.option_replay_gain)) },
							items = ReplayGainMode.entries.toImmutableList(),
							label = { stringResource(it.displayName) },
							selection = preferenceManager.replayGainMode,
							onSelect = { preferenceManager.replayGainMode = it }
						)
						SettingSwitchRow(
							title = { Text(stringResource(Res.string.option_gapless_playback)) },
							subtitle = { Text(stringResource(Res.string.subtitle_gapless_playback)) },
							value = preferenceManager.gaplessPlayback,
							onSetValue = { preferenceManager.gaplessPlayback = it }
						)
						SettingSwitchRow(
							title = { Text(stringResource(Res.string.option_audio_offload)) },
							subtitle = { Text(stringResource(Res.string.subtitle_audio_offload)) },
							value = preferenceManager.audioOffload,
							onSetValue = { preferenceManager.audioOffload = it }
						)
					}
					SettingSelectionRow(
						title = { Text(stringResource(Res.string.option_explicit_playback)) },
						label = { stringResource(it.displayName) },
						items = ExplicitContentPlayback.entries.toImmutableList(),
						selection = preferenceManager.explicitContentPlayback,
						onSelect = { preferenceManager.explicitContentPlayback = it }
					)
				}

				FormTitle(stringResource(Res.string.title_behaviour))
				Form {
					SettingSwitchRow(
						title = { Text(stringResource(Res.string.option_enable_scrobbling)) },
						subtitle = { Text(stringResource(Res.string.subtitle_enable_scrobbling)) },
						value = preferenceManager.enableScrobbling,
						onSetValue = { preferenceManager.enableScrobbling = it }
					)

					FormRow {
						Column(Modifier.fillMaxWidth()) {
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.SpaceBetween
							) {
								Text(stringResource(Res.string.option_scrobble_percentage))
								Text(
									"${(preferenceManager.scrobblePercentage * 100).roundToInt()}%",
									fontFamily = FontFamily.Monospace,
									fontWeight = FontWeight(400),
									fontSize = 13.sp,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							}
							Slider(
								value = preferenceManager.scrobblePercentage,
								onValueChange = {
									preferenceManager.scrobblePercentage = it
								},
								valueRange = 0f..1f,
							)
						}
					}
					FormRow {
						Column(Modifier.fillMaxWidth()) {
							Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.SpaceBetween
							) {
								Text(stringResource(Res.string.option_min_duration_to_scrobble))
								Text(
									"${preferenceManager.minDurationToScrobble.toInt()}s",
									fontFamily = FontFamily.Monospace,
									fontWeight = FontWeight(400),
									fontSize = 13.sp,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							}
							Slider(
								value = preferenceManager.minDurationToScrobble,
								onValueChange = {
									preferenceManager.minDurationToScrobble = it
								},
								valueRange = 0f..400f,
							)
						}
					}
				}
			}
		}
	}
	if (equalizerVisible) EqualizerSheet { equalizerVisible = false }
}
