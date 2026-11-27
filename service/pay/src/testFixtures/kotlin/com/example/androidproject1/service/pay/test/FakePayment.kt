package com.example.androidproject1.service.pay.test

import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.pay.PaymentCancelledError
import com.example.androidproject1.service.pay.PaymentSheet

/**
 * A [PaymentSheet] a test scripts: [ready] is what [isReady] says (a dev-menu "Pretend Google Pay
 * is ready" is this set to `true`), [result] is how every [pay] ends — a fake token by default — and
 * [payments] is what was asked, in order, as amount and currency.
 */
class FakePayment(
    var ready: Boolean = true,
    var result: Outcome<String> = Outcome.Success(TOKEN),
) : PaymentSheet {

    val payments = mutableListOf<Pair<Long, String>>()

    override suspend fun isReady(): Boolean = ready

    override suspend fun pay(amount: Long, currency: String): Outcome<String> {
        payments += amount to currency
        // Nothing to pay with is nothing shown: the sheet never opens on a device that is not ready.
        return if (ready) result else Outcome.Failure(PaymentCancelledError())
    }

    companion object {

        const val TOKEN = "fake-google-pay-token"
    }
}
