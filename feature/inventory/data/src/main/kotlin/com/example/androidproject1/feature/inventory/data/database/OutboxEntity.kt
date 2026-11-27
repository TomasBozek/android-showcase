package com.example.androidproject1.feature.inventory.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One item's write that the server has not accepted yet. One row per item: a newer write
 * replaces the one waiting, so the server is sent the item's last state, not every step to it.
 *
 * @property op `put` or `delete`, as `OutboxOp`'s name.
 * @property failed refused by the server; it waits for the user rather than for the network.
 */
@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val itemId: String,
    val op: String,
    val failed: Boolean,
    val enqueuedAt: Long,
)
