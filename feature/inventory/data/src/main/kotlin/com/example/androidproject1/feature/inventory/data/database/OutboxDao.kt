package com.example.androidproject1.feature.inventory.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Query("SELECT * FROM outbox")
    fun observeAll(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox WHERE itemId = :itemId")
    suspend fun get(itemId: String): OutboxEntity?

    /** What the drain sends, oldest first; a failed one waits for the user. */
    @Query("SELECT * FROM outbox WHERE failed = 0 ORDER BY enqueuedAt ASC")
    suspend fun pending(): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: OutboxEntity)

    @Query("UPDATE outbox SET failed = :failed WHERE itemId = :itemId")
    suspend fun setFailed(itemId: String, failed: Boolean)

    @Query("DELETE FROM outbox WHERE itemId = :itemId")
    suspend fun delete(itemId: String)

    @Query("DELETE FROM outbox")
    suspend fun deleteAll()
}
