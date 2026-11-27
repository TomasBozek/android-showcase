package com.example.androidproject1.feature.inventory.domain

/**
 * Where an item's last write is: accepted by the server, waiting in the outbox to be sent,
 * or refused — a `409` among the reasons — for the user to retry or discard.
 */
enum class SyncState { Synced, Pending, Failed }
