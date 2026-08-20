package chaynik.mizu.util.core

import androidx.compose.runtime.Composable
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import mizu.composeapp.generated.resources.Res
import mizu.composeapp.generated.resources.count_hours
import mizu.composeapp.generated.resources.count_minutes
import mizu.composeapp.generated.resources.option_sort_alphabetical_by_artist
import mizu.composeapp.generated.resources.option_sort_alphabetical_by_name
import mizu.composeapp.generated.resources.option_sort_by_genre
import mizu.composeapp.generated.resources.option_sort_by_year
import mizu.composeapp.generated.resources.option_sort_downloaded
import mizu.composeapp.generated.resources.option_sort_frequent
import mizu.composeapp.generated.resources.option_sort_newest
import mizu.composeapp.generated.resources.option_sort_random
import mizu.composeapp.generated.resources.option_sort_rating
import mizu.composeapp.generated.resources.option_sort_recent
import mizu.composeapp.generated.resources.option_sort_starred
import mizu.composeapp.generated.resources.option_sort_year
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import chaynik.mizu.domain.models.DomainAlbumListType
import chaynik.mizu.domain.models.DomainSongListType
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Composable
fun Duration.label(): String {
	val hours = inWholeHours.toInt()
	val minutes = (this - hours.hours).inWholeMinutes.toInt()

	return when {
		hours > 0 && minutes > 0 ->
			"${pluralStringResource(Res.plurals.count_hours, hours, hours)} ${
				pluralStringResource(
					Res.plurals.count_minutes,
					minutes,
					minutes
				)
			}"

		hours > 0 ->
			pluralStringResource(Res.plurals.count_hours, hours, hours)

		else ->
			pluralStringResource(Res.plurals.count_minutes, max(1, minutes), max(1, minutes))
	}
}

@Composable
fun DomainAlbumListType.label() = when (this) {
	DomainAlbumListType.Random -> stringResource(Res.string.option_sort_random)
	DomainAlbumListType.Newest -> stringResource(Res.string.option_sort_newest)
	DomainAlbumListType.Frequent -> stringResource(Res.string.option_sort_frequent)
	DomainAlbumListType.Recent -> stringResource(Res.string.option_sort_recent)
	DomainAlbumListType.AlphabeticalByName -> stringResource(Res.string.option_sort_alphabetical_by_name)
	DomainAlbumListType.AlphabeticalByArtist -> stringResource(Res.string.option_sort_alphabetical_by_artist)
	DomainAlbumListType.Highest -> stringResource(Res.string.option_sort_rating)
	DomainAlbumListType.Starred -> stringResource(Res.string.option_sort_starred)
	DomainAlbumListType.Downloaded -> stringResource(Res.string.option_sort_downloaded)
	DomainAlbumListType.Year -> stringResource(Res.string.option_sort_year)
	is DomainAlbumListType.ByGenre -> stringResource(Res.string.option_sort_by_genre)
	is DomainAlbumListType.ByYear -> stringResource(Res.string.option_sort_by_year)
}

@Composable
fun DomainSongListType.label() = when (this) {
	DomainSongListType.Alphabetical -> stringResource(Res.string.option_sort_alphabetical_by_name)
	is DomainSongListType.ByArtist -> stringResource(Res.string.option_sort_by_genre)
	is DomainSongListType.ByGenre -> stringResource(Res.string.option_sort_by_genre)
	DomainSongListType.Downloaded -> stringResource(Res.string.option_sort_downloaded)
	DomainSongListType.FrequentlyPlayed -> stringResource(Res.string.option_sort_frequent)
	DomainSongListType.Newest -> stringResource(Res.string.option_sort_newest)
	DomainSongListType.Random -> stringResource(Res.string.option_sort_random)
	DomainSongListType.Rating -> stringResource(Res.string.option_sort_rating)
	DomainSongListType.Starred -> stringResource(Res.string.option_sort_starred)
	DomainSongListType.Year -> stringResource(Res.string.option_sort_by_year)
}

fun PaletteStyle.label(): String = when (this) {
	PaletteStyle.TonalSpot -> "Tonal Spot"
	PaletteStyle.Neutral -> "Neutral"
	PaletteStyle.Vibrant -> "Vibrant"
	PaletteStyle.Expressive -> "Expressive"
	PaletteStyle.Rainbow -> "Rainbow"
	PaletteStyle.FruitSalad -> "Fruit Salad"
	PaletteStyle.Monochrome -> "Monochrome"
	PaletteStyle.Fidelity -> "Fidelity"
	PaletteStyle.Content -> "Content"
}

fun ColorSpec.SpecVersion.label() = when (this) {
	ColorSpec.SpecVersion.SPEC_2021 -> "Material 3 (2021)"
	ColorSpec.SpecVersion.SPEC_2025 -> "Expressive (2025)"
}
