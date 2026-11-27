package com.example.androidproject1.feature.movies.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieFavouritesDao {

    /**
     * The join is why a favourite is an id and nothing more: one query answers "which movies are
     * hearted" from the rows the pages cached — or the page-0 row `storeDetail` keeps for a movie
     * no page listed — without a copy of the movie in the favourites table going stale.
     *
     * The second sort key makes the order total: `favouritedAt` is a millisecond, and two hearts
     * inside one would otherwise be ordered by whatever SQLite feels like.
     */
    @Query(
        """
        SELECT movies.* FROM movies
        INNER JOIN movie_favourites ON movie_favourites.id = movies.id
        ORDER BY movie_favourites.favouritedAt DESC, movies.id ASC
        """,
    )
    fun observeFavourites(): Flow<List<MovieEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM movie_favourites WHERE id = :movieId)")
    fun observeIsFavourite(movieId: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favourite: MovieFavouriteEntity)

    @Query("DELETE FROM movie_favourites WHERE id = :movieId")
    suspend fun remove(movieId: Int)
}
