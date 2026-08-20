package chaynik.mizu.ui.components.layouts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_albums
import mizu.composeapp.generated.resources.title_artists
import mizu.composeapp.generated.resources.title_genres
import mizu.composeapp.generated.resources.title_library
import mizu.composeapp.generated.resources.title_home
import mizu.composeapp.generated.resources.title_playlists
import mizu.composeapp.generated.resources.title_radios
import mizu.composeapp.generated.resources.title_search
import mizu.composeapp.generated.resources.title_songs
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import chaynik.mizu.LocalNavStack
import chaynik.mizu.LocalPlatformContext
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.NavbarConfig
import chaynik.mizu.domain.models.settings.NavbarTab
import chaynik.mizu.domain.models.settings.NavigationBarLabelVisibility
import chaynik.mizu.domain.models.settings.NavigationBarStyle
import chaynik.mizu.icons.Icons
import chaynik.mizu.icons.filled.Album
import chaynik.mizu.icons.filled.Artist
import chaynik.mizu.icons.filled.Genre
import chaynik.mizu.icons.filled.LibraryMusic
import chaynik.mizu.icons.filled.Home
import chaynik.mizu.icons.filled.Radio
import chaynik.mizu.icons.outlined.Album
import chaynik.mizu.icons.outlined.Artist
import chaynik.mizu.icons.outlined.Genre
import chaynik.mizu.icons.outlined.LibraryMusic
import chaynik.mizu.icons.outlined.Home
import chaynik.mizu.icons.outlined.Note
import chaynik.mizu.icons.outlined.PlaylistPlay
import chaynik.mizu.icons.outlined.Radio
import chaynik.mizu.icons.outlined.Search
import chaynik.mizu.ui.components.common.animatedTabIconPainter
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.settings.viewmodels.NavtabsViewModel

private enum class NavItem(
	val destination: Screen,
	val icon: ImageVector,
	val iconUnselected: ImageVector = icon,
	val label: StringResource
) {
	HOME(
		destination = Screen.Home(),
		icon = Icons.Filled.Home,
		iconUnselected = Icons.Outlined.Home,
		label = Res.string.title_home
	),
	LIBRARY(
		destination = Screen.Library(),
		icon = Icons.Filled.LibraryMusic,
		iconUnselected = Icons.Outlined.LibraryMusic,
		label = Res.string.title_library
	),
	ALBUMS(
		destination = Screen.AlbumList(),
		icon = Icons.Filled.Album,
		iconUnselected = Icons.Outlined.Album,
		label = Res.string.title_albums
	),
	PLAYLISTS(
		destination = Screen.PlaylistList(),
		icon = Icons.Outlined.PlaylistPlay,
		label = Res.string.title_playlists
	),
	ARTISTS(
		destination = Screen.ArtistList(),
		icon = Icons.Filled.Artist,
		iconUnselected = Icons.Outlined.Artist,
		label = Res.string.title_artists
	),
	SEARCH(
		destination = Screen.Search(),
		icon = Icons.Outlined.Search,
		iconUnselected = Icons.Outlined.Search,
		label = Res.string.title_search
	),
	GENRES(
		destination = Screen.GenreList(),
		icon = Icons.Filled.Genre,
		iconUnselected = Icons.Outlined.Genre,
		label = Res.string.title_genres
	),
	SONGS(
		destination = Screen.SongList(),
		icon = Icons.Outlined.Note,
		iconUnselected = Icons.Outlined.Note,
		label = Res.string.title_songs
	),
	RADIOS(
		destination = Screen.RadioList(),
		icon = Icons.Filled.Radio,
		iconUnselected = Icons.Outlined.Radio,
		label = Res.string.title_radios
	)
}

private fun navItemFor(id: NavbarTab.Id): NavItem = when (id) {
	NavbarTab.Id.HOME -> NavItem.HOME
	NavbarTab.Id.LIBRARY -> NavItem.LIBRARY
	NavbarTab.Id.ALBUMS -> NavItem.ALBUMS
	NavbarTab.Id.PLAYLISTS -> NavItem.PLAYLISTS
	NavbarTab.Id.ARTISTS -> NavItem.ARTISTS
	NavbarTab.Id.SEARCH -> NavItem.SEARCH
	NavbarTab.Id.GENRES -> NavItem.GENRES
	NavbarTab.Id.SONGS -> NavItem.SONGS
	NavbarTab.Id.RADIOS -> NavItem.RADIOS
}

/**
 * Floating "capsule" navigation bar (Apple Music style): a rounded, elevated
 * pill sized to its tabs and centered horizontally. The active tab sits on a
 * lighter highlight chip that can be tapped, or long-pressed and dragged onto
 * another tab to switch — the chip lifts and follows the finger, then settles
 * onto whichever tab it is released over.
 */
@Composable
private fun CapsuleBottomBar(
	tabs: List<NavbarTab>,
	labelVisibility: NavigationBarLabelVisibility,
	enabled: Boolean,
	windowInsets: WindowInsets,
	// When true (narrow / compact screens) the capsule spans the available
	// width and distributes the tabs evenly so they never overflow and get
	// clipped; otherwise it hugs its content and is centered.
	fillWidth: Boolean,
	modifier: Modifier = Modifier
) {
	val backStack = LocalNavStack.current
	val density = LocalDensity.current
	val scope = rememberCoroutineScope()

	// Apple-Music style: the Search tab is lifted out of the pill and shown as a
	// separate round button to the right; the pill only holds the remaining tabs.
	val searchTab = remember(tabs) { tabs.firstOrNull { it.id == NavbarTab.Id.SEARCH } }
	val pillTabs = remember(tabs) { tabs.filter { it.id != NavbarTab.Id.SEARCH } }

	val selectedIndex = pillTabs.indexOfFirst { navItemFor(it.id).destination == backStack.lastOrNull() }

	// Per-tab geometry (relative to the tab row), filled in as tabs are laid out.
	val tabLefts = remember(pillTabs.size) { mutableStateListOf<Float>().apply { repeat(pillTabs.size) { add(0f) } } }
	val tabWidths = remember(pillTabs.size) { mutableStateListOf<Float>().apply { repeat(pillTabs.size) { add(0f) } } }
	var tabHeight by remember(pillTabs.size) { mutableStateOf(0f) }
	var measured by remember(pillTabs.size) { mutableStateOf(false) }

	// Highlight chip position/size in px; single source of truth for both the
	// resting state and the drag, so releasing always settles smoothly.
	val highlightX = remember { Animatable(0f) }
	val highlightW = remember { Animatable(0f) }

	var dragging by remember { mutableStateOf(false) }
	var dragReady by remember { mutableStateOf(false) }
	var dragActivationJob by remember { mutableStateOf<Job?>(null) }
	var hoveredIndex by remember(pillTabs.size) { mutableStateOf(selectedIndex) }
	val lift by animateFloatAsState(if (dragging) 1.12f else 1f, label = "capsuleLift")

	val indexAt: (Float) -> Int = index@{ x ->
		if (!measured) return@index -1
		for (i in pillTabs.indices) {
			val left = tabLefts[i]
			if (x >= left && x <= left + tabWidths[i]) return@index i
		}
		pillTabs.indices.minByOrNull { i ->
			kotlin.math.abs(x - (tabLefts[i] + tabWidths[i] / 2f))
		} ?: -1
	}

	val navigateTo: (Int) -> Unit = { index ->
		val destination = navItemFor(pillTabs[index].id).destination
		if (backStack.lastOrNull() != destination) {
			backStack.apply {
				clear()
				add(destination)
			}
		}
	}

	// Settle the chip onto the selected tab whenever the selection changes from
	// the outside (tap / navigation) and we are not mid-drag.
	LaunchedEffect(selectedIndex, measured, tabLefts.toList(), tabWidths.toList(), dragging) {
		if (!measured || selectedIndex !in pillTabs.indices || dragging) return@LaunchedEffect
		val targetX = tabLefts[selectedIndex]
		val targetW = tabWidths[selectedIndex]
		if (highlightW.value == 0f) {
			highlightX.snapTo(targetX)
			highlightW.snapTo(targetW)
		} else {
			launch { highlightX.animateTo(targetX, spring()) }
			launch { highlightW.animateTo(targetW, spring()) }
		}
	}

	val chipShape = RoundedCornerShape(20.dp)
	val highlightColor = MaterialTheme.colorScheme.surfaceContainerHighest
	val highlightAlpha by animateFloatAsState(
		if (selectedIndex in pillTabs.indices || dragging) 1f else 0f,
		label = "capsuleHighlightAlpha"
	)

	// Round search button; matches the pill height once tabs are measured.
	val searchButtonSize = if (tabHeight > 0f) with(density) { tabHeight.toDp() } + 16.dp else 64.dp
	val searchDestination = searchTab?.let { navItemFor(it.id).destination }
	val searchActive = searchDestination != null && searchDestination == backStack.lastOrNull()

	Box(
		modifier = modifier
			.fillMaxWidth()
			.windowInsetsPadding(windowInsets)
			.padding(horizontal = 16.dp)
			.padding(bottom = 8.dp),
		contentAlignment = Alignment.Center
	) {
		Row(
			// Compact phones stretch the whole bar so tabs never overflow; on
			// wider screens the pill + search button hug their content and stay
			// together as one centered group (Apple Music style).
			modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Box(
				modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
				contentAlignment = Alignment.Center
			) {
				if (pillTabs.isEmpty()) return@Box
				Surface(
					shape = RoundedCornerShape(percent = 50),
					color = MaterialTheme.colorScheme.surfaceContainer,
					contentColor = MaterialTheme.colorScheme.onSurface,
					tonalElevation = 3.dp,
					shadowElevation = 12.dp
				) {
					Box(
						modifier = Modifier
							.padding(horizontal = 14.dp, vertical = 8.dp)
							.pointerInput(pillTabs, enabled) {
								if (!enabled) return@pointerInput
								detectTapGestures { pos ->
									val i = indexAt(pos.x)
									if (i in pillTabs.indices) navigateTo(i)
								}
							}
							.pointerInput(pillTabs, enabled) {
								if (!enabled) return@pointerInput
								detectDragGesturesAfterLongPress(
								onDragStart = { pos ->
										hoveredIndex = indexAt(pos.x).let {
											if (it in pillTabs.indices) it else 0
										}
									dragReady = false
									dragActivationJob?.cancel()
									dragActivationJob = scope.launch {
										delay(400)
										dragReady = true
										dragging = true
									}
								},
								onDrag = { change, dragAmount ->
									if (!dragReady) return@detectDragGesturesAfterLongPress
										change.consume()
										val width = highlightW.value
										val minX = tabLefts.minOrNull() ?: 0f
										val maxX = (pillTabs.indices.maxOfOrNull { i ->
											tabLefts[i] + tabWidths[i]
										} ?: minX) - width
											.coerceAtLeast(minX)
										val newX = (highlightX.value + dragAmount.x).coerceIn(minX, maxX)
										hoveredIndex = indexAt(newX + width / 2f)
										scope.launch { highlightX.snapTo(newX) }
									},
								onDragEnd = {
									dragActivationJob?.cancel()
									if (!dragReady) {
										dragging = false
										return@detectDragGesturesAfterLongPress
									}
										val target = hoveredIndex.coerceIn(0, pillTabs.lastIndex)
										dragging = false
										scope.launch { highlightX.animateTo(tabLefts[target], spring()) }
										scope.launch { highlightW.animateTo(tabWidths[target], spring()) }
									navigateTo(target)
									dragReady = false
								},
								onDragCancel = {
									dragActivationJob?.cancel()
									dragReady = false
									dragging = false
								}
								)
							}
					) {
						// Moving highlight chip, drawn behind the tabs.
						Box(
							modifier = Modifier
								// Both the anchor and offset must use physical coordinates.
								// TopStart resolves to the right edge in RTL, which left the
								// highlight under Home while Albums was selected.
								.align(AbsoluteAlignment.TopLeft)
								.absoluteOffset { IntOffset(highlightX.value.roundToInt(), 0) }
								.width(with(density) { highlightW.value.toDp() })
								.height(with(density) { tabHeight.toDp() })
								.graphicsLayer {
									scaleX = lift
									scaleY = lift
									alpha = highlightAlpha
								}
								.shadow(if (dragging) 8.dp else 0.dp, chipShape, clip = false)
								.clip(chipShape)
								.background(highlightColor)
						)

						Row(
							modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
							horizontalArrangement = Arrangement.spacedBy(if (fillWidth) 0.dp else 2.dp),
							verticalAlignment = Alignment.CenterVertically
						) {
							pillTabs.forEachIndexed { index, tab ->
								val item = navItemFor(tab.id)
								val active = if (dragging) index == hoveredIndex else index == selectedIndex

								val tint by animateColorAsState(
									if (active) MaterialTheme.colorScheme.primary
									else MaterialTheme.colorScheme.onSurfaceVariant
								)
								val showLabel = labelVisibility == NavigationBarLabelVisibility.Always ||
									(labelVisibility == NavigationBarLabelVisibility.OnlySelected && active)

								Column(
									modifier = (if (fillWidth) Modifier.weight(1f) else Modifier)
										.onGloballyPositioned { coords ->
											val left = coords.boundsInParent().left
											val width = coords.size.width.toFloat()
											if (tabLefts[index] != left) tabLefts[index] = left
											if (tabWidths[index] != width) tabWidths[index] = width
											if (index == 0) tabHeight = coords.size.height.toFloat()
											if (!measured && tabWidths.all { it > 0f }) measured = true
										}
										.padding(horizontal = if (fillWidth) 4.dp else 14.dp, vertical = 8.dp)
										.then(if (fillWidth) Modifier else Modifier.widthIn(min = 44.dp)),
									horizontalAlignment = Alignment.CenterHorizontally,
									verticalArrangement = Arrangement.spacedBy(2.dp)
								) {
									if (active) {
										val painter = animatedTabIconPainter(item.destination)
										if (painter != null) {
											Icon(painter = painter, null, tint = tint, modifier = Modifier.size(26.dp))
										} else {
											Icon(item.icon, null, tint = tint, modifier = Modifier.size(26.dp))
										}
									} else {
										Icon(item.iconUnselected, null, tint = tint, modifier = Modifier.size(26.dp))
									}

									if (showLabel) {
										Text(
											stringResource(item.label),
											color = tint,
											style = MaterialTheme.typography.labelSmall,
											maxLines = 1,
											// Shrink long labels to fit the (possibly narrow) slot
											// instead of clipping/ellipsising them.
											autoSize = TextAutoSize.StepBased(
												minFontSize = 8.sp,
												maxFontSize = MaterialTheme.typography.labelSmall.fontSize
											),
											overflow = TextOverflow.Ellipsis
										)
									}
								}
							}
						}
					}
				}
			}

			if (searchTab != null) {
				val searchTint by animateColorAsState(
					if (searchActive) MaterialTheme.colorScheme.primary
					else MaterialTheme.colorScheme.onSurfaceVariant,
					label = "capsuleSearchTint"
				)
				Surface(
					onClick = {
						if (searchDestination != null && backStack.lastOrNull() != searchDestination) {
							backStack.apply {
								clear()
								add(searchDestination)
							}
						}
					},
					enabled = enabled,
					shape = CircleShape,
					color = MaterialTheme.colorScheme.surfaceContainer,
					contentColor = MaterialTheme.colorScheme.onSurface,
					tonalElevation = 3.dp,
					shadowElevation = 12.dp,
					modifier = Modifier.size(searchButtonSize)
				) {
					Box(contentAlignment = Alignment.Center) {
						Icon(
							Icons.Outlined.Search,
							contentDescription = stringResource(Res.string.title_search),
							tint = searchTint,
							modifier = Modifier.size(26.dp)
						)
					}
				}
			}
		}
	}
}

@Composable
fun BottomBar(
	modifier: Modifier = Modifier,
	containerColor: Color = NavigationBarDefaults.containerColor,
	windowInsets: WindowInsets = NavigationBarDefaults.windowInsets,
	enabled: Boolean = true
) {
	val viewModel = koinViewModel<NavtabsViewModel>()
	val backStack = LocalNavStack.current
	val platformContext = LocalPlatformContext.current
	val state by viewModel.state.collectAsState()
	val tabs = ((state as? UiState.Success)?.data ?: NavbarConfig.default)
		.tabs.filter { tab -> tab.visible }
	val preferenceManager = koinInject<PreferenceManager>()

	if (tabs.size < 2) return

	if (preferenceManager.navigationBarStyle == NavigationBarStyle.Capsule) {
		CapsuleBottomBar(
			tabs = tabs,
			labelVisibility = preferenceManager.navigationBarLabelVisibility,
			enabled = enabled,
			windowInsets = windowInsets,
			fillWidth = platformContext.sizeClass.widthSizeClass <= WindowWidthSizeClass.Compact,
			modifier = modifier
		)
		return
	}

	// Classic Material navigation bar (Normal / Short styles).
	val animatedContainerColor by animateColorAsState(containerColor)
	val scope = rememberCoroutineScope()
	var draggedIndex by remember(tabs) { mutableStateOf<Int?>(null) }
	var classicDragReady by remember { mutableStateOf(false) }
	var classicDragJob by remember { mutableStateOf<Job?>(null) }
	val classicDragModifier = Modifier.pointerInput(tabs, enabled) {
		if (!enabled) return@pointerInput
		fun indexAt(x: Float): Int =
			(x / (size.width.toFloat() / tabs.size)).toInt().coerceIn(0, tabs.lastIndex)
		detectDragGesturesAfterLongPress(
			onDragStart = { position ->
				classicDragReady = false
				classicDragJob?.cancel()
				classicDragJob = scope.launch {
					delay(400)
					classicDragReady = true
					draggedIndex = indexAt(position.x)
				}
			},
			onDrag = { change, _ ->
				if (classicDragReady) {
					draggedIndex = indexAt(change.position.x)
					change.consume()
				}
			},
			onDragEnd = {
				classicDragJob?.cancel()
				val target = draggedIndex
				classicDragReady = false
				draggedIndex = null
				if (target != null) {
					backStack.apply {
						clear()
						add(navItemFor(tabs[target].id).destination)
					}
				}
			},
			onDragCancel = {
				classicDragJob?.cancel()
				classicDragReady = false
				draggedIndex = null
			}
		)
	}

	AnimatedContent(
		platformContext.sizeClass.widthSizeClass <= WindowWidthSizeClass.Compact
			&& tabs.size > 1
	) {
		if (tabs.size < 2) return@AnimatedContent
		if (it) {
			NavigationBar(
				modifier = modifier.then(classicDragModifier),
				containerColor = animatedContainerColor,
				windowInsets = windowInsets
			) {
				tabs.forEachIndexed { index, tab ->
					val item = navItemFor(tab.id)
					val selected = draggedIndex?.let { it == index }
						?: (backStack.lastOrNull() == item.destination)

					NavigationBarItem(
						selected = selected,
						enabled = enabled,
						alwaysShowLabel = preferenceManager.navigationBarLabelVisibility
							== NavigationBarLabelVisibility.Always,
						onClick = {
							backStack.apply {
								clear()
								add(item.destination)
							}
						},
						icon = {
							if (selected) {
								val painter = animatedTabIconPainter(item.destination)
								if (painter != null) {
									Icon(painter = painter, null)
								} else {
									Icon(item.icon, null)
								}
							} else {
								Icon(item.iconUnselected, null)
							}
						},
						label = if (preferenceManager.navigationBarLabelVisibility
							!== NavigationBarLabelVisibility.Never) {
							{
								Text(
									stringResource(item.label),
									maxLines = 1,
									autoSize = TextAutoSize.StepBased(
										minFontSize = 1.sp,
										maxFontSize = MaterialTheme.typography.labelMedium.fontSize
									)
								)
							}
						} else {
							null
						}
					)
				}
			}
		} else {
			ShortNavigationBar(
				modifier = modifier.then(classicDragModifier),
				containerColor = animatedContainerColor
			) {
				tabs.forEachIndexed { index, tab ->
					val item = navItemFor(tab.id)
					val selected = draggedIndex?.let { it == index }
						?: (backStack.last() == item.destination)

					ShortNavigationBarItem(
						iconPosition = if (platformContext.sizeClass.widthSizeClass > WindowWidthSizeClass.Compact)
							NavigationItemIconPosition.Start
						else NavigationItemIconPosition.Top,
						selected = selected,
						enabled = enabled,
						onClick = {
							backStack.apply {
								clear()
								add(item.destination)
							}
						},
						icon = {
							if (selected) {
								val painter = animatedTabIconPainter(item.destination)
								if (painter != null) {
									Icon(painter = painter, null)
								} else {
									Icon(item.icon, null)
								}
							} else {
								Icon(item.iconUnselected, null)
							}
						},
						label = if (
							preferenceManager.navigationBarLabelVisibility == NavigationBarLabelVisibility.Always ||
							(preferenceManager.navigationBarLabelVisibility == NavigationBarLabelVisibility.OnlySelected && selected)
						) {
							{ Text(stringResource(item.label)) }
						} else {
							null
						},
					)
				}
			}
		}
	}
}
