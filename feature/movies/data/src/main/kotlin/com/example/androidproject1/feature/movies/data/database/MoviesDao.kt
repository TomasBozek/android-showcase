package com.example.androidproject1.feature.movies.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MoviesDao {

    /**
     * Every listed movie, in the order the server gave them, as a source Paging reads pages of. Page
     * 0 is the rows a detail left behind for a movie no page listed: they are not part of the list.
     * Room invalidates it when the table changes, so a page written by the mediator appears.
     */
    @Query("SELECT * FROM movies WHERE page > 0 ORDER BY page ASC, position ASC")
    fun pagingSource(): PagingSource<Int, MovieEntity>

    @Query("SELECT * FROM movies WHERE page = :page ORDER BY position ASC")
    suspend fun page(page: Int): List<MovieEntity>

    /** `null` until the page has been written at least once. */
    @Query("SELECT * FROM movie_pages WHERE page = :page")
    suspend fun pageMarker(page: Int): MoviePageEntity?

    /** When page 1 was written — the list's age; `null` for an empty cache. */
    @Query("SELECT fetchedAt FROM movie_pages WHERE page = 1")
    fun observeListFetchedAt(): Flow<Long?>

    /** The last page fetched — where the mediator carries on from. `null` for an empty cache. */
    @Query("SELECT * FROM movie_pages ORDER BY page DESC LIMIT 1")
    suspend fun lastPageMarker(): MoviePageEntity?

    @Query("SELECT * FROM movies ORDER BY page ASC, position ASC")
    suspend fun allMovies(): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun movie(id: Int): MovieEntity?

    @Query("SELECT * FROM movie_details WHERE id = :id")
    suspend fun detail(id: Int): MovieDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageMarker(marker: MoviePageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: MovieDetailEntity)

    @Query("DELETE FROM movies WHERE page = :page")
    suspend fun deletePage(page: Int)

    @Query("DELETE FROM movies")
    suspend fun deleteMovies()

    @Query("DELETE FROM movie_pages")
    suspend fun deletePageMarkers()

    /**
     * One page, replaced as a unit and stamped. A movie that moved from page 2 to page 1 between
     * two fetches is one row with a primary key, so the insert's REPLACE moves it rather than
     * leaving a copy behind on the page it left.
     */
    @Transaction
    suspend fun replacePage(page: Int, movies: List<MovieEntity>, marker: MoviePageEntity) {
        deletePage(page)
        insertMovies(movies)
        insertPageMarker(marker)
    }

    /** The refresh: every row and every marker gone, then page 1 and its marker — one transaction. */
    @Transaction
    suspend fun replaceAll(movies: List<MovieEntity>, marker: MoviePageEntity) {
        deleteMovies()
        deletePageMarkers()
        insertMovies(movies)
        insertPageMarker(marker)
    }
}
