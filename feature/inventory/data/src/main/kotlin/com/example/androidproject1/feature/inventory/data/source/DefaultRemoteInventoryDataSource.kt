package com.example.androidproject1.feature.inventory.data.source

import com.example.androidproject1.feature.inventory.domain.Item
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Switching to IO is this class's job, not the repository's: the drain runs on WorkManager's
 * thread, but a source never assumes its caller's. Failures are mapped here, so the handler above
 * sees a `DomainError` and never an HTTP status.
 */
class DefaultRemoteInventoryDataSource(
    private val client: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) : RemoteInventoryDataSource {

    override suspend fun put(item: Item) = request {
        client.put("inventory/items/${item.id}") {
            contentType(ContentType.Application.Json)
            setBody(item.toDto())
        }
        Unit
    }

    override suspend fun delete(itemId: String) = request {
        client.delete("inventory/items/$itemId")
        Unit
    }

    private suspend fun <T> request(block: suspend () -> T): T = withContext(dispatcherProvider.io) {
        try {
            block()
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }
}

/** What the server is sent for an item: its own fields, never its [Item.sync], which is this device's. */
@Serializable
data class ItemDto(
    val id: String,
    val name: String,
    val category: String,
    val condition: String,
    val quantity: Int,
    val priceMinor: Long,
    val acquiredOn: String?,
    val insured: Boolean,
    val tags: List<String>,
    val owner: String,
    val notes: String,
    val barcode: String?,
)

internal fun Item.toDto() = ItemDto(
    id = id,
    name = name,
    category = category.name,
    condition = condition.name,
    quantity = quantity,
    priceMinor = priceMinor,
    acquiredOn = acquiredOn?.toString(),
    insured = insured,
    tags = tags.map { it.name }.sorted(),
    owner = owner,
    notes = notes,
    barcode = barcode,
)
