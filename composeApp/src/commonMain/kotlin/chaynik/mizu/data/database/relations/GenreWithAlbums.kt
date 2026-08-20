package chaynik.mizu.data.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import chaynik.mizu.data.database.entities.AlbumEntity
import chaynik.mizu.data.database.entities.GenreEntity

data class GenreWithAlbums(
	@Embedded val genre: GenreEntity,
	@Relation(
		entity = AlbumEntity::class,
		parentColumns = ["genreName"],
		entityColumns = ["genre"]
	)
	val albums: List<AlbumWithSongs>
)
