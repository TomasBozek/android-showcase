package com.example.androidproject1.feature.orders.data.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

/**
 * An order as it was placed. The lines are one JSON column, the way a forecast is: nothing
 * queries inside them, and they never change after the order is placed. The address is spread
 * into columns because a detail screen reads each part.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    /** Epoch milliseconds, UTC. */
    val placedAt: Long,
    val lines: String,
    val name: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val phone: String,
    val deliveryName: String,
    val deliveryPrice: Long,
    val discount: Long,
    val total: Long,
)

/**
 * One status an order reached. The key is the order and the status, so a refresh that brings a
 * change already kept inserts nothing, and a status is reached once.
 */
@Entity(
    tableName = "status_changes",
    primaryKeys = ["orderId", "status"],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class StatusChangeEntity(
    val orderId: String,
    /** `OrderStatus.name`. */
    val status: String,
    /** Epoch milliseconds, UTC. */
    val at: Long,
)

data class OrderWithHistory(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId")
    val history: List<StatusChangeEntity>,
)

@Dao
interface OrdersDao {

    /** Newest first; the id breaks a tie so two orders placed in one millisecond keep their places. */
    @Transaction
    @Query("SELECT * FROM orders ORDER BY placedAt DESC, id DESC")
    fun observeOrders(): Flow<List<OrderWithHistory>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    fun observeOrder(id: String): Flow<OrderWithHistory?>

    /** An order already kept is left as it was: an order does not change after it is placed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrder(order: OrderEntity)

    /** A status already reached is left with the time it was first kept at. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChanges(changes: List<StatusChangeEntity>)

    @Transaction
    suspend fun keep(order: OrderEntity, changes: List<StatusChangeEntity>) {
        insertOrder(order)
        insertChanges(changes)
    }
}

@Database(entities = [OrderEntity::class, StatusChangeEntity::class], version = 1, exportSchema = true)
abstract class OrdersDatabase : RoomDatabase() {

    abstract fun ordersDao(): OrdersDao

    companion object {

        const val NAME = "orders.db"

        /**
         * Every migration this database has, read by the builder in `OrdersModule` and by
         * `OrdersDatabaseMigrationTest`. A `version` bump ships its migration here in the same commit.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
