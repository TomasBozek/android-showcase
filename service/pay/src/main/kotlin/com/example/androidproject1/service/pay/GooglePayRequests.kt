package com.example.androidproject1.service.pay

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.math.BigDecimal
import java.util.Currency

/**
 * What this app asks of Google Pay, and what it is allowed to take.
 *
 * The defaults are Google's own **test** gateway (`example`): a token is made, no money moves, and
 * no merchant account is involved. A real one is a gateway and a merchant id from a payment
 * provider, and a merchant account with Google — both out of scope here.
 */
public data class PayConfig(
    val merchantName: String = "Example Merchant",
    val gateway: String = "example",
    val gatewayMerchantId: String = "exampleGatewayMerchantId",
    val cardNetworks: List<String> = listOf("MASTERCARD", "VISA"),
    /** `PAN_ONLY` is a card saved in the Google account, `CRYPTOGRAM_3DS` a device token. */
    val authMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS"),
)

/** The two Google Pay API requests as JSON, and the token read back out of an answer. */
internal object GooglePayRequests {

    private const val API_VERSION = 2

    /** `IsReadyToPayRequest`: can *this* user pay with a card they already have on Google Pay? */
    public fun isReadyToPay(config: PayConfig = PayConfig()): String = buildJsonObject {
        apiVersion()
        putJsonArray("allowedPaymentMethods") { add(cardMethod(config, tokenization = false)) }
        put("existingPaymentMethodRequired", true)
    }.toString()

    /** `PaymentDataRequest` for [amount] minor units of [currency]. */
    public fun paymentData(amount: Long, currency: String, config: PayConfig = PayConfig()): String = buildJsonObject {
        apiVersion()
        putJsonArray("allowedPaymentMethods") { add(cardMethod(config, tokenization = true)) }
        putJsonObject("transactionInfo") {
            put("totalPrice", price(amount, currency))
            put("totalPriceStatus", "FINAL")
            put("currencyCode", currency)
        }
        putJsonObject("merchantInfo") { put("merchantName", config.merchantName) }
    }.toString()

    /**
     * `12.34` for 1234 `EUR`, `1500` for 1500 `JPY`: the currency's own number of decimals, as text
     * — Google Pay takes a string, and a float would put `0.1 + 0.2` on a receipt.
     */
    public fun price(amount: Long, currency: String): String {
        val digits = Currency.getInstance(currency).defaultFractionDigits.coerceAtLeast(0)
        return BigDecimal.valueOf(amount, digits).toPlainString()
    }

    /**
     * The payment token in a `PaymentData` JSON answer, or `null` if it is not there. A JSON `null`
     * is not a token: `JsonNull` is a primitive whose text is `"null"`, which would be handed to a
     * gateway as one.
     */
    public fun tokenFrom(paymentDataJson: String): String? = runCatching {
        val token = Json.parseToJsonElement(paymentDataJson).jsonObject
            .getValue("paymentMethodData").jsonObject
            .getValue("tokenizationData").jsonObject
            .getValue("token").jsonPrimitive
        token.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun JsonObjectBuilderScope.apiVersion() {
        put("apiVersion", API_VERSION)
        put("apiVersionMinor", 0)
    }

    private fun cardMethod(config: PayConfig, tokenization: Boolean): JsonObject = buildJsonObject {
        put("type", "CARD")
        putJsonObject("parameters") {
            put("allowedAuthMethods", strings(config.authMethods))
            put("allowedCardNetworks", strings(config.cardNetworks))
        }
        if (tokenization) {
            putJsonObject("tokenizationSpecification") {
                put("type", "PAYMENT_GATEWAY")
                putJsonObject("parameters") {
                    put("gateway", config.gateway)
                    put("gatewayMerchantId", config.gatewayMerchantId)
                }
            }
        }
    }

    private fun strings(values: List<String>): JsonArray = buildJsonArray { values.forEach { add(JsonPrimitive(it)) } }
}

private typealias JsonObjectBuilderScope = kotlinx.serialization.json.JsonObjectBuilder
