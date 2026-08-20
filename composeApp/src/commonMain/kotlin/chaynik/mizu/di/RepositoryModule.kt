package chaynik.mizu.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import chaynik.mizu.domain.repositories.AlbumRepository
import chaynik.mizu.domain.repositories.ArtistRepository
import chaynik.mizu.domain.repositories.CollectionRepository
import chaynik.mizu.domain.repositories.DbRepository
import chaynik.mizu.domain.repositories.GenreRepository
import chaynik.mizu.domain.repositories.LyricsRepository
import chaynik.mizu.domain.repositories.PlaylistRepository
import chaynik.mizu.domain.repositories.RadioRepository
import chaynik.mizu.domain.repositories.SearchRepository
import chaynik.mizu.domain.repositories.ShareRepository
import chaynik.mizu.domain.repositories.SongRepository

val repositoryModule = module {
	singleOf(::AlbumRepository)
	singleOf(::ArtistRepository)
	singleOf(::DbRepository)
	singleOf(::GenreRepository)
	singleOf(::LyricsRepository)
	singleOf(::SearchRepository)
	singleOf(::ShareRepository)
	singleOf(::CollectionRepository)
	singleOf(::PlaylistRepository)
	singleOf(::SongRepository)
	singleOf(::RadioRepository)
}
