package chaynik.mizu.ui.screens.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.title_artists
import mizu.composeapp.generated.resources.title_genres
import mizu.composeapp.generated.resources.title_playlists
import mizu.composeapp.generated.resources.title_random_tracks
import mizu.composeapp.generated.resources.title_recently_added
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.domain.models.DomainAlbum
import chaynik.mizu.domain.models.DomainAlbumListType
import chaynik.mizu.domain.models.DomainArtist
import chaynik.mizu.domain.models.DomainGenre
import chaynik.mizu.domain.models.DomainPlaylist
import chaynik.mizu.domain.models.DomainSong
import chaynik.mizu.domain.models.DomainSongListType
import chaynik.mizu.domain.models.DomainArtistListType
import chaynik.mizu.data.database.dao.RadioDao
import chaynik.mizu.data.database.mappers.toDomainModel
import chaynik.mizu.shared.MediaPlayerViewModel
import chaynik.mizu.ui.screens.radio.components.RadioListScreenCard
import mizu.composeapp.generated.resources.title_radios
import chaynik.mizu.domain.manager.PreferenceManager
import chaynik.mizu.domain.models.settings.HomeSection
import chaynik.mizu.ui.components.layouts.horizontalSection
import chaynik.mizu.ui.components.layouts.header
import chaynik.mizu.ui.components.common.CoverArt
import chaynik.mizu.ui.core.UiState
import chaynik.mizu.ui.navigation.Screen
import chaynik.mizu.ui.screens.album.components.AlbumListScreenItem
import chaynik.mizu.ui.screens.artist.ArtistsScreenItem
import chaynik.mizu.ui.screens.genre.components.GenreListScreenCard
import chaynik.mizu.ui.screens.playlist.components.PlaylistListScreenItem
import chaynik.mizu.util.ui.withoutTop
import org.koin.compose.koinInject
import chaynik.mizu.LocalNavStack

internal data class HomeRandomTracks<T>(val preview: List<T>, val playbackQueue: List<T>)

internal fun <T> homeRandomTracks(items: List<T>, previewLimit: Int = 15) =
	HomeRandomTracks(preview = items.take(previewLimit), playbackQueue = items)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreenContent(
	scrollBehavior: TopAppBarScrollBehavior,
	innerPadding: PaddingValues,
	onSetShareId: (String) -> Unit,
	randomSongsState: UiState<ImmutableList<DomainSong>>,
	onPlayRandomSongs: (List<DomainSong>, Int) -> Unit,
	recentAlbumsState: UiState<ImmutableList<DomainAlbum>>,
	randomAlbumsState: UiState<ImmutableList<DomainAlbum>>,
	frequentAlbumsState: UiState<ImmutableList<DomainAlbum>>,
	favoriteAlbumsState: UiState<ImmutableList<DomainAlbum>>,
	releaseAlbumsState: UiState<ImmutableList<DomainAlbum>>,

	// albums
	albumsState: UiState<ImmutableList<DomainAlbum>>,
	selectedAlbum: DomainAlbum?,
	selectedAlbumIsStarred: Boolean,
	selectedAlbumRating: Int,
	onSelectAlbum: (DomainAlbum) -> Unit,
	onClearAlbumSelection: () -> Unit,
	onStarSelectedAlbum: (Boolean) -> Unit,
	onRateSelectedAlbum: (Int) -> Unit,
	onPlayAlbumNext: () -> Unit,
	onAddAlbumToQueue: () -> Unit,

	// artists
	artistsState: UiState<ImmutableList<DomainArtist>>,
	selectedArtist: DomainArtist?,
	selectedArtistAlbums: ImmutableList<DomainAlbum>?,
	selectedArtistIsStarred: Boolean,
	onSelectArtist: (DomainArtist) -> Unit,
	onClearArtistSelection: () -> Unit,
	onStarSelectedArtist: (Boolean) -> Unit,
	onPlayArtistNext: () -> Unit,
	onAddArtistToQueue: () -> Unit,

	// playlists
	playlistsState: UiState<ImmutableList<DomainPlaylist>>,
	selectedPlaylist: DomainPlaylist?,
	onSelectPlaylist: (DomainPlaylist) -> Unit,
	onClearPlaylistSelection: () -> Unit,
	onDeletePlaylist: (String) -> Unit,
	onPlayPlaylistNext: () -> Unit,
	onAddPlaylistToQueue: () -> Unit,

	// genres
	genresState: UiState<ImmutableList<DomainGenre>>
) {
	val preferences = koinInject<PreferenceManager>()
	val radioDao = koinInject<RadioDao>()
	val player = koinInject<MediaPlayerViewModel>()
	val radioEntities by remember(radioDao) { radioDao.getRadiosFlow() }
		.collectAsState(emptyList())
	val radios = remember(radioEntities) { radioEntities.map { it.toDomainModel() } }
	val hiddenSections = preferences.homeHiddenSections.split(',').toSet()
	val homeSections = HomeSection.decode(preferences.homeSectionOrder)
		.filter { it.name !in hiddenSections }
	val randomTracks = homeRandomTracks(randomSongsState.data.orEmpty())
	LazyVerticalGrid(
		modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
		columns = GridCells.Fixed(2),
		contentPadding = innerPadding.withoutTop() + PaddingValues(top = 8.dp),
		verticalArrangement = Arrangement.spacedBy(5.dp),
		horizontalArrangement = Arrangement.spacedBy(5.dp),
	) {
		homeSections.forEach { section ->
			when (section) {
				HomeSection.RandomTracks -> {
		header(
			title = Res.string.title_random_tracks,
			destination = Screen.SongList(true, DomainSongListType.Random),
			active = true,
			sectionKey = section.name
		)
		item(key = "${section.name}:content", span = { GridItemSpan(maxLineSpan) }) {
			LazyHorizontalGrid(
				rows = GridCells.Fixed(3),
				modifier = Modifier.fillMaxWidth().height(216.dp),
				contentPadding = PaddingValues(horizontal = 8.dp),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				items(
					items = randomTracks.preview,
					key = { it.id }
				) { song ->
						ListItem(
						modifier = Modifier.width(320.dp),
						onClick = {
							onPlayRandomSongs(randomTracks.playbackQueue, randomTracks.playbackQueue.indexOf(song))
						},
						leadingContent = {
							CoverArt(
								coverArtId = song.coverArtId,
								modifier = Modifier.size(54.dp)
							)
						},
						content = { Text(song.title, maxLines = 1) },
						supportingContent = { Text(song.artistName, maxLines = 1) }
					)
				}
			}
		}
				}
				HomeSection.RecentlyAdded -> {
		horizontalSection(
			sectionKey = section.name,
			title = Res.string.title_recently_added,
			destination = Screen.AlbumList(true, DomainAlbumListType.Newest),
			state = albumsState,
			key = { it.id },
			seeAll = true
		) { album ->
			AlbumListScreenItem(
				modifier = Modifier.animateItem().width(150.dp),
				tab = "library",
				album = album,
				selected = album == selectedAlbum,
				starred = selectedAlbumIsStarred,
				onSelect = { onSelectAlbum(album) },
				onDeselect = { onClearAlbumSelection() },
				onSetStarred = { onStarSelectedAlbum(it) },
				onSetShareId = { onSetShareId(it) },
				onPlayNext = onPlayAlbumNext,
				onAddToQueue = onAddAlbumToQueue,
				rating = selectedAlbumRating,
				onSetRating = onRateSelectedAlbum
			)
		}
				}
				HomeSection.NewReleases -> homeAlbumSection(section.title, DomainAlbumListType.Year, releaseAlbumsState)
				HomeSection.RecentlyPlayed -> homeAlbumSection(section.title, DomainAlbumListType.Recent, recentAlbumsState)
				HomeSection.RandomAlbums -> homeAlbumSection(section.title, DomainAlbumListType.Random, randomAlbumsState)
				HomeSection.FrequentlyPlayed -> homeAlbumSection(section.title, DomainAlbumListType.Frequent, frequentAlbumsState)
				HomeSection.FavoriteAlbums -> homeAlbumSection(section.title, DomainAlbumListType.Starred, favoriteAlbumsState)
				HomeSection.FavoriteArtists -> homeDestinationRow(
					section.title,
					Screen.ArtistList(true, DomainArtistListType.Starred)
				)
				HomeSection.Stations -> {
					header(
						title = Res.string.title_radios,
						destination = Screen.RadioList(true),
						active = true,
						sectionKey = section.name
					)
					item(key = "${section.name}:content", span = { GridItemSpan(maxLineSpan) }) {
						LazyRow(
							modifier = Modifier.fillMaxWidth(),
							contentPadding = PaddingValues(horizontal = 8.dp),
							horizontalArrangement = Arrangement.spacedBy(12.dp)
						) {
							items(radios, key = { it.id }) { radio ->
								RadioListScreenCard(
									modifier = Modifier.width(170.dp),
									radio = radio,
									onPlayClick = { player.playRadio(radio) }
								)
							}
						}
					}
				}
				HomeSection.Playlists -> {
		horizontalSection(
			sectionKey = section.name,
			title = Res.string.title_playlists,
			destination = Screen.PlaylistList(true),
			state = playlistsState,
			key = { it.id },
			seeAll = true
		) { playlist ->
			PlaylistListScreenItem(
				modifier = Modifier.animateItem().width(150.dp),
				tab = "library",
				playlist = playlist,
				selected = playlist == selectedPlaylist,
				onSelect = { onSelectPlaylist(playlist) },
				onDeselect = { onClearPlaylistSelection() },
				onSetDeletionId = { onDeletePlaylist(it) },
				onSetShareId = { onSetShareId(it) },
				onPlayNext = onPlayPlaylistNext,
				onAddToQueue = onAddPlaylistToQueue
			)
		}
				}
				HomeSection.Artists -> {
		horizontalSection(
			sectionKey = section.name,
			title = Res.string.title_artists,
			destination = Screen.ArtistList(true),
			state = artistsState,
			key = { it.id },
			seeAll = true
		) { artist ->
			ArtistsScreenItem(
				modifier = Modifier.animateItem().width(150.dp),
				tab = "library",
				artist = artist,
				selected = artist == selectedArtist,
				selectedArtistAlbums = selectedArtistAlbums,
				starred = selectedArtistIsStarred,
				onSelect = { onSelectArtist(artist) },
				onDeselect = { onClearArtistSelection() },
				onSetStarred = { onStarSelectedArtist(it) },
				onPlayNext = onPlayArtistNext,
				onAddToQueue = onAddArtistToQueue
			)
		}
				}
				HomeSection.Genres -> {
		horizontalSection(
			sectionKey = section.name,
			title = Res.string.title_genres,
			destination = Screen.GenreList(true),
			state = genresState,
			key = { it.name },
			seeAll = true
		) { genreWithAlbums ->
			GenreListScreenCard(genre = genreWithAlbums)
		}
				}
			}
		}
	}
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.homeAlbumSection(
	title: StringResource,
	listType: DomainAlbumListType,
	state: UiState<ImmutableList<DomainAlbum>>
) {
	header(title, destination = Screen.AlbumList(true, listType), active = true, sectionKey = listType)
	item(key = "$listType:content", span = { GridItemSpan(maxLineSpan) }) {
		val backStack = LocalNavStack.current
		LazyRow(
			contentPadding = PaddingValues(horizontal = 16.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			items(state.data.orEmpty().take(10), key = { it.id }) { album ->
				Column(
					modifier = Modifier.width(150.dp).clickable {
						backStack.add(Screen.CollectionDetail(album.id, "home"))
					}
				) {
					CoverArt(
						modifier = Modifier.fillMaxWidth().height(150.dp),
						coverArtId = album.coverArtId
					)
					Text(
						text = album.name,
						style = MaterialTheme.typography.titleSmallEmphasized,
						maxLines = 2
					)
					Text(
						text = album.artistName,
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						maxLines = 2
					)
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.homeDestinationRow(
	title: StringResource,
	destination: Screen
) {
	item(key = "${destination}:content", span = { GridItemSpan(maxLineSpan) }) {
		val backStack = LocalNavStack.current
		ListItem(
			modifier = Modifier.padding(horizontal = 8.dp),
			onClick = {
				if (backStack.lastOrNull() != destination) backStack.add(destination)
			},
			content = { Text(stringResource(title)) }
		)
	}
}
