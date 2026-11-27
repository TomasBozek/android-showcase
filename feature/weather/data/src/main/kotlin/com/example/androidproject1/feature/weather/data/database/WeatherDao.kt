package com.example.androidproject1.feature.weather.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    /** The order the user arranged; `id` only to break a tie a migration cannot produce. */
    @Query("SELECT * FROM places ORDER BY position ASC, id ASC")
    fun observePlaces(): Flow<List<PlaceEntity>>

    @Query("SELECT * FROM places WHERE id = :placeId")
    suspend fun place(placeId: Int): PlaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlace(place: PlaceEntity)

    /**
     * A new place lands at the end, where the user last looked; one already saved keeps the place
     * it was put in, because saving a place twice is a tap on a search result, not a request to
     * move it.
     */
    @Transaction
    suspend fun savePlace(place: PlaceEntity) {
        val position = positionOf(place.id) ?: ((maxPosition() ?: -1) + 1)
        insertPlace(place.copy(position = position))
    }

    /**
     * Moves the place to [toIndex], closing the gap it left and pushing the rest aside — the
     * whole list renumbered from 0, in one transaction, so no two rows ever share a position.
     *
     * By id rather than by a `from` index: a drag knows the row it grabbed, and an index read off
     * a list that has moved on points at a different place. An id that is not saved, or an
     * index outside the list, leaves the order alone.
     */
    @Transaction
    suspend fun movePlace(placeId: Int, toIndex: Int) {
        val ids = orderedIds()
        val from = ids.indexOf(placeId)
        if (from < 0 || toIndex !in ids.indices || from == toIndex) return

        val reordered = ids.toMutableList().apply { add(toIndex, removeAt(from)) }
        reordered.forEachIndexed { index, id -> setPosition(id, index) }
    }

    @Query("SELECT id FROM places ORDER BY position ASC, id ASC")
    suspend fun orderedIds(): List<Int>

    @Query("SELECT position FROM places WHERE id = :placeId")
    suspend fun positionOf(placeId: Int): Int?

    @Query("SELECT MAX(position) FROM places")
    suspend fun maxPosition(): Int?

    @Query("UPDATE places SET position = :position WHERE id = :placeId")
    suspend fun setPosition(placeId: Int, position: Int)

    /** The place and its forecast go together: a removed place leaves no orphan behind. */
    @Query("DELETE FROM places WHERE id = :placeId")
    suspend fun deletePlace(placeId: Int)

    @Query("SELECT * FROM forecasts WHERE placeId = :placeId")
    fun observeForecast(placeId: Int): Flow<ForecastEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: ForecastEntity)

    @Query("DELETE FROM forecasts WHERE placeId = :placeId")
    suspend fun deleteForecast(placeId: Int)
}
