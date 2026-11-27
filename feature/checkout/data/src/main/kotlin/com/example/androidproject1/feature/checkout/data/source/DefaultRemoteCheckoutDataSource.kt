package com.example.androidproject1.feature.checkout.data.source

import com.example.androidproject1.feature.checkout.domain.InvalidPromo
import com.example.androidproject1.feature.checkout.domain.PaymentDeclined
import com.example.androidproject1.feature.checkout.domain.Promo
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.network.HttpErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.withContext

/**
 * Switching to IO is this class's job, and so is mapping a failure: everything above sees a
 * `DomainError`. Two statuses mean something to checkout and are mapped before the shared table —
 * a `402` is a card the bank declined, and a `404` on `promo` is a code that does not exist.
 *
 * `POST orders` is never retried (the client retries idempotent methods only), so a timeout after
 * the server took the order cannot place it twice.
 */
class DefaultRemoteCheckoutDataSource(
    private val client: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) : RemoteCheckoutDataSource {

    override suspend fun getDeliveryOptions(): List<DeliveryOptionDto> = request {
        client.get("delivery").body()
    }

    override suspend fun getPromo(code: String): Promo = request {
        try {
            client.get("promo") { parameter("code", code) }.body<PromoDto>().toDomain()
        } catch (refused: ResponseException) {
            if (refused.response.status == HttpStatusCode.NotFound) throw InvalidPromo(code = code, cause = refused)
            throw refused
        }
    }

    override suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrderDto = request {
        try {
            client.post("orders") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        } catch (refused: ResponseException) {
            if (refused.response.status == HttpStatusCode.PaymentRequired) {
                val decline = runCatching { refused.response.body<DeclineDto>() }.getOrDefault(DeclineDto())
                throw PaymentDeclined(reason = decline.toDomain(), cause = refused)
            }
            throw refused
        }
    }

    private suspend fun <T> request(block: suspend () -> T): T = withContext(dispatcherProvider.io) {
        try {
            block()
        } catch (throwable: Throwable) {
            throw HttpErrorMapper.map(throwable)
        }
    }
}
