package chaynik.mizu.ui.components.layouts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.dropUnlessResumed
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.kyant.capsule.ContinuousRoundedRectangle
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.action_next_song
import mizu.composeapp.generated.resources.action_pause
import mizu.composeapp.generated.resources.action_play
import mizu.composeapp.generated.resources.action_previous_song
import mizu.composeapp.generated.resources.info_not_playing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.LocalNavStack
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.manager.SessionManager
import chaynik.mizu.domain.models.settings.MiniPlayerProgressStyle
import chaynik.mizu.domain.models.settings.sanitizedMiniPlayerProgress
import chaynik.mizu.domain.models.settings.NavbarConfig
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Note
import chaynik.mizu.icons.filled.Pause
import chaynik.mizu.icons.filled.Play
import chaynik.mizu.icons.filled.SkipNext
import chaynik.mizu.icons.outlined.Radio
import chaynik.mizu.icons.outlined.OutputDevice
import chaynik.mizu.domain.manager.ExternalPlaybackManager
import chaynik.mizu.domain.models.PlaybackTarget
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.components.common.MarqueeText
import chaynik.mizu.ui.components.common.playPauseIconPainter
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.settings.viewmodels.NavtabsViewModel
import coil3.compose.LocalPlatformContext as LocalCoilPlatformContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private data class MiniPlayerContentState(
	val song: DomainSong?,
	val isPaused: Boolean,
	val isLoading: Boolean
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
	modifier: Modifier = Modifier,
	enabled: Boolean = true
) {
	val player = koinInject<MediaPlayerViewModel>()
	val preferenceManager = koinInject<PreferenceManager>()
	val navtabsViewModel = koinViewModel<NavtabsViewModel>()
	val navtabsState by navtabsViewModel.state.collectAsState()
	val tabs = ((navtabsState as? UiState.Success)?.data ?: NavbarConfig.default)
		.tabs.filter { tab -> tab.visible }
	val backStack = LocalNavStack.current
	val haptics = LocalHapticFeedback.current
	val navBarPadding = if (tabs.size < 2)
		with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
	else 0.dp

	val contentStateFlow = remember(player) {
		player.uiState.map { MiniPlayerContentState(it.currentSong, it.isPaused, it.isLoading) }
			.distinctUntilChanged()
	}
	val playerState by contentStateFlow.collectAsState(
		MiniPlayerContentState(null, isPaused = false, isLoading = false)
	)
	val targetState by koinInject<ExternalPlaybackManager>().state.collectAsState()
	val song = playerState.song

	val coilPlatformContext = LocalCoilPlatformContext.current
	val sessionManager = koinInject<SessionManager>()
	val model = remember(song?.coverArtId) {
		ImageRequest.Builder(coilPlatformContext)
			.data(song?.coverArtId?.let { sessionManager.getCoverArtUrl(it) })
			.memoryCacheKey(song?.coverArtId)
			.diskCacheKey(song?.coverArtId)
			.diskCachePolicy(CachePolicy.ENABLED)
			.memoryCachePolicy(CachePolicy.ENABLED)
			.build()
	}

	val outerPadding = 12.dp
	val coverRounding by animateDpAsState(
		if (playerState.isLoading)
			46.dp
		else 8.dp
	)
	val iconSize = 24.dp
	val shape = ContinuousRoundedRectangle(16.dp)

	val onClick = dropUnlessResumed {
		if (!backStack.contains(Screen.NowPlaying)) {
			backStack.add(Screen.NowPlaying)
		}
	}

	val hasSong = song != null
	val isRadio = song?.id?.startsWith("radio_") == true
	val isInteractive = enabled && hasSong

	Swiper(
		onSwipeLeft = {
			if (isInteractive) player.next()
		},
		onSwipeRight = {
			if (isInteractive) player.previous()
		},
		swipeLeftAccessibilityLabel = stringResource(Res.string.action_next_song),
		swipeRightAccessibilityLabel = stringResource(Res.string.action_previous_song),
		modifier = modifier,
		enabled = isInteractive
	) {
		Box(
			modifier = Modifier
				.widthIn(max = 600.dp)
				.padding(
					bottom = outerPadding + navBarPadding,
					start = outerPadding,
					end = outerPadding
					)
					.align(Alignment.Center)
			) {
				Box(
					modifier = Modifier
						.matchParentSize()
						.dropShadow(shape, Shadow(radius = 10.dp, alpha = 0.25f))
						.clip(shape)
						.background(NavigationBarDefaults.containerColor)
				)
				MiniPlayerProgressLayer(
					style = preferenceManager.miniPlayerProgressStyle,
					shape = shape,
					hasSong = hasSong && !isRadio
				)
				ListItem(
					modifier = Modifier
						.pointerInput(isInteractive) {
						if (!isInteractive) return@pointerInput
						var totalDrag = 0f
						detectVerticalDragGestures(
							onVerticalDrag = { _, dragAmount ->
								totalDrag += dragAmount
							},
							onDragEnd = {
								if (totalDrag < -150f) {
									onClick()
								}
								totalDrag = 0f
							}
						)
					},
				contentPadding = PaddingValues(
					start = 10.dp,
					end = 10.dp,
					top = 10.dp,
					bottom = 10.dp
				),
					verticalAlignment = Alignment.CenterVertically,
					colors = ListItemDefaults.colors(
						containerColor = androidx.compose.ui.graphics.Color.Transparent
				),
				shapes = ListItemDefaults.shapes(
					shape = shape,
					selectedShape = shape,
					pressedShape = shape,
					focusedShape = shape,
					hoveredShape = shape,
					draggedShape = shape
				),
				onClick = {
					onClick()
				},
				onLongClick = {
					haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
					onClick()
				},
				leadingContent = {
					Box(contentAlignment = Alignment.Center) {
						AsyncImage(
							model = model,
							contentDescription = null,
							contentScale = ContentScale.Crop,
							modifier = Modifier
								.size(48.dp)
								.padding(if (playerState.isLoading) 8.dp else 0.dp)
								.clip(
									ContinuousRoundedRectangle(coverRounding)
								)
								.background(MaterialTheme.colorScheme.surfaceVariant)
						)
						if (song?.coverArtId.isNullOrEmpty()) {
							Icon(
								imageVector = if (isRadio) Icons.Outlined.Radio else Icons.Filled.Note,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
							)
						}
						AnimatedVisibility(
							playerState.isLoading,
							modifier = Modifier.matchParentSize(),
							enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec())
								+ fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
							exit = scaleOut(MaterialTheme.motionScheme.defaultSpatialSpec())
								+ fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec())
						) {
							CircularProgressIndicator(
								Modifier.matchParentSize(),
								trackColor = MaterialTheme.colorScheme.primaryContainer
							)
						}
					}
				},
				trailingContent = {
					CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(8.dp)
					) {
						if (targetState.activeTarget !is PlaybackTarget.Local) Icon(Icons.Outlined.OutputDevice, "Внешнее устройство", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
						val colors = IconButtonDefaults.iconButtonVibrantColors()
						IconButton(
							onClick = {
								if (playerState.isPaused) {
									player.resume()
								} else {
									player.pause()
								}
							},
							enabled = isInteractive,
							colors = colors
						) {
							val painter = playPauseIconPainter(playerState.isPaused)
							val description = stringResource(
								if (playerState.isPaused)
									Res.string.action_play
								else Res.string.action_pause
							)
							if (painter != null) {
								Icon(
									painter = painter,
									contentDescription = description,
									modifier = Modifier.size(iconSize)
								)
							} else {
								Icon(
									imageVector = if (playerState.isPaused)
										Icons.Filled.Play
									else Icons.Filled.Pause,
									contentDescription = description,
									modifier = Modifier.size(iconSize)
								)
							}
						}
						IconButton(
							onClick = {
								player.next()
							},
							enabled = isInteractive,
							colors = colors
						) {
							Icon(
								imageVector = Icons.Filled.SkipNext,
								contentDescription = stringResource(Res.string.action_next_song),
								modifier = Modifier.size(iconSize)
							)
						}
					} }
				},
				content = {
					song?.title?.let { title ->
						MarqueeText(title)
					}
				},
				supportingContent = {
					if (song != null) {
						val device = when (val target = targetState.activeTarget) { is PlaybackTarget.Dlna -> target.name; else -> null }
						MarqueeText(device?.let { "${song.artistName} • $it" } ?: song.artistName)
					} else {
						MarqueeText(stringResource(Res.string.info_not_playing))
					}
				},
				enabled = enabled
			)
			}
	}
}

@Composable
private fun BoxScope.MiniPlayerProgressLayer(
	style: MiniPlayerProgressStyle,
	shape: androidx.compose.ui.graphics.Shape,
	hasSong: Boolean
) {
	val player = koinInject<MediaPlayerViewModel>()
	val progressFlow = remember(player) {
		player.uiState.map { sanitizedMiniPlayerProgress(it.progress) }.distinctUntilChanged()
	}
	val rawProgress by progressFlow.collectAsState(0f)
	val progress by animateFloatAsState(
		targetValue = if (hasSong) rawProgress else 0f,
		animationSpec = tween(durationMillis = 180, easing = LinearEasing),
		label = "miniPlayerProgress"
	)
	Box(
		modifier = Modifier.matchParentSize().clip(shape),
		contentAlignment = Alignment.BottomStart
	) {
		when (style) {
			MiniPlayerProgressStyle.FullBackground -> Box(
				Modifier.fillMaxWidth(progress).fillMaxHeight()
					.background(MaterialTheme.colorScheme.primary.copy(alpha = .16f))
			)
			MiniPlayerProgressStyle.BottomBar -> Box(
				Modifier.fillMaxWidth(progress).height(3.dp)
					.background(MaterialTheme.colorScheme.primary.copy(alpha = .72f))
			)
		}
	}
}
