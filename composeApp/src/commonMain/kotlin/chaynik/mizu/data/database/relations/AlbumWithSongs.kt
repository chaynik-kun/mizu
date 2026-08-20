package chaynik.mizu.data.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import chaynik.mizu.data.database.entities.AlbumEntity
import chaynik.mizu.data.database.entities.SongEntity

data class AlbumWithSongs(
	@Embedded val album: AlbumEntity,
	@Relation(
		parentColumns = ["albumId"],
		entityColumns = ["belongsToAlbumId"]
	)
	val songs: List<SongEntity>
)
