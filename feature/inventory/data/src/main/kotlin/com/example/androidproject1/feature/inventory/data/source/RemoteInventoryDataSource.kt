package com.example.androidproject1.feature.inventory.data.source

import com.example.androidproject1.feature.inventory.domain.Item

/**
 * The server's half of the outbox: `PUT` and `DELETE` on `inventory/items/<id>` under the
 * template's own `BASE_URL` — the `dev` fixture answers them. A failure arrives as a `DomainError`.
 */
interface RemoteInventoryDataSource {

    suspend fun put(item: Item)

    suspend fun delete(itemId: String)
}
