package com.example.androidproject1.debug

import com.example.androidproject1.network.FixtureNetwork
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.pay.PaymentSheet

/**
 * The `dev` flavor's payment sheet: the real one, unless the debug menu's "Pretend Google
 * Pay is ready" is on. Then it is ready whatever the device says, and a payment answers at once
 * with a made-up token — an emulator has no wallet, and the fixture takes any token that is not a
 * published decline card. The switch is read on every call, a marker file, so flipping it needs no restart.
 */
internal class PretendPaymentSheet(private val real: PaymentSheet) : PaymentSheet {

    override suspend fun isReady(): Boolean = FixtureNetwork.pretendGooglePay || real.isReady()

    override suspend fun pay(amount: Long, currency: String): Outcome<String> =
        if (FixtureNetwork.pretendGooglePay) {
            Outcome.Success("dev-google-pay-token-$amount-$currency")
        } else {
            real.pay(amount, currency)
        }
}
