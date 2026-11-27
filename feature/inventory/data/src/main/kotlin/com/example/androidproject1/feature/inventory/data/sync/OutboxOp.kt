package com.example.androidproject1.feature.inventory.data.sync

/** What a write in the outbox asks of the server. */
enum class OutboxOp { Put, Delete }

/** A write waiting in the outbox, as the drain reads it. */
data class OutboxEntry(
    val itemId: String,
    val op: OutboxOp,
    val failed: Boolean,
)
