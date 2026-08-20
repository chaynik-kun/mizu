package chaynik.mizu.data.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import chaynik.mizu.data.database.dao.AlbumDao
import chaynik.mizu.data.database.dao.ArtistDao
import chaynik.mizu.data.database.dao.DownloadDao
import chaynik.mizu.data.database.dao.GenreDao
import chaynik.mizu.data.database.dao.LyricDao
import chaynik.mizu.data.database.dao.PlaylistDao
import chaynik.mizu.data.database.dao.RadioDao
import chaynik.mizu.data.database.dao.SongDao
import chaynik.mizu.data.database.dao.SyncActionDao
import chaynik.mizu.data.database.entities.AlbumEntity
import chaynik.mizu.data.database.entities.ArtistEntity
import chaynik.mizu.data.database.entities.DownloadEntity
import chaynik.mizu.data.database.entities.GenreEntity
import chaynik.mizu.data.database.entities.LyricEntity
import chaynik.mizu.data.database.entities.PlaylistEntity
import chaynik.mizu.data.database.entities.PlaylistSongCrossRef
import chaynik.mizu.data.database.entities.RadioEntity
import chaynik.mizu.data.database.entities.SongEntity
import chaynik.mizu.data.database.entities.SyncActionEntity

@Database(
	version = 16,
	entities = [
		AlbumEntity::class,
		GenreEntity::class,
		PlaylistEntity::class,
		PlaylistSongCrossRef::class,
		SongEntity::class,
		ArtistEntity::class,
		RadioEntity::class,
		LyricEntity::class,
		SyncActionEntity::class,
		DownloadEntity::class
	]
)
@ColumnTypeConverters(Converters::class)
@ConstructedBy(CacheDatabaseConstructor::class)
abstract class CacheDatabase : RoomDatabase() {
	abstract fun albumDao(): AlbumDao
	abstract fun genreDao(): GenreDao
	abstract fun downloadDao(): DownloadDao
	abstract fun playlistDao(): PlaylistDao
	abstract fun songDao(): SongDao
	abstract fun artistDao(): ArtistDao
	abstract fun radioDao(): RadioDao
	abstract fun lyricDao(): LyricDao
	abstract fun syncActionDao(): SyncActionDao
}

@Suppress("KotlinNoActualForExpect")
expect object CacheDatabaseConstructor : RoomDatabaseConstructor<CacheDatabase> {
	override fun initialize(): CacheDatabase
}
