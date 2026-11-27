package com.example.androidproject1.debug

import android.content.Context
import com.example.androidproject1.network.FixtureNetwork
import com.example.androidproject1.service.devtools.ApiSwitch
import com.example.androidproject1.service.devtools.ConfigAnswer
import com.example.androidproject1.service.devtools.OfflineSwitch
import com.example.androidproject1.service.pay.PaymentSheet

/**
 * Whether this build has a debug menu, and what its switches are wired to.
 *
 * One of these per flavor source set, the same split `networkEngine` uses. [ENABLED] is a
 * `const`, so `prod`'s `false` folds the branch in `AppNavHost` away at compile time and R8 drops
 * the screens behind it — the gallery's component catalogue included.
 */
object DebugMenu {

    const val ENABLED = true

    fun offlineSwitch(context: Context): OfflineSwitch = FixtureOfflineSwitch(context)

    fun apiSwitch(context: Context): ApiSwitch = FixtureApiSwitch(context)

    /** The sheet the checkout pays with: [real], unless the debug menu pretends Google Pay is ready. */
    fun paymentSheet(real: PaymentSheet): PaymentSheet = PretendPaymentSheet(real)
}

/**
 * The `dev` flavor talks to fixtures, and [FixtureNetwork] is what makes them fail on demand. It
 * is backed by a file so it survives a process death, which is also why the state is read back
 * rather than remembered.
 */
private class FixtureOfflineSwitch(private val context: Context) : OfflineSwitch {

    override val isSupported: Boolean = true

    override fun isOffline(): Boolean = FixtureNetwork.failing

    override fun setOffline(offline: Boolean) = FixtureNetwork.setFailing(context, offline)

    override fun latencyMs(): Int = FixtureNetwork.latencyMs

    override fun setLatencyMs(latencyMs: Int) = FixtureNetwork.setLatencyMs(context, latencyMs)

    override fun failureRate(): Int = FixtureNetwork.failureRate

    override fun setFailureRate(failureRate: Int) = FixtureNetwork.setFailureRate(context, failureRate)

    override fun configAnswer(answer: ConfigAnswer): Boolean = when (answer) {
        ConfigAnswer.BelowMinimum -> FixtureNetwork.belowMinimum
        ConfigAnswer.Maintenance -> FixtureNetwork.maintenance
        ConfigAnswer.NewerAvailable -> FixtureNetwork.newerAvailable
    }

    override fun setConfigAnswer(answer: ConfigAnswer, on: Boolean) = when (answer) {
        ConfigAnswer.BelowMinimum -> FixtureNetwork.setBelowMinimum(context, on)
        ConfigAnswer.Maintenance -> FixtureNetwork.setMaintenance(context, on)
        ConfigAnswer.NewerAvailable -> FixtureNetwork.setNewerAvailable(context, on)
    }

    override fun fastOrderClock(): Boolean = FixtureNetwork.fastOrderClock

    override fun setFastOrderClock(on: Boolean) = FixtureNetwork.setFastOrderClock(context, on)

    override fun pretendGooglePay(): Boolean = FixtureNetwork.pretendGooglePay

    override fun setPretendGooglePay(on: Boolean) = FixtureNetwork.setPretendGooglePay(context, on)

    override fun isSessionExpiryArmed(): Boolean = FixtureNetwork.sessionExpiryArmed

    override fun setSessionExpiryArmed(armed: Boolean) {
        FixtureNetwork.sessionExpiryArmed = armed
    }
}

/** The other switch on the same engine: the forwarded hosts leave the process while it is on. */
private class FixtureApiSwitch(private val context: Context) : ApiSwitch {

    override val isSupported: Boolean = true

    override fun isRealApi(): Boolean = FixtureNetwork.realApi

    override fun setRealApi(realApi: Boolean) = FixtureNetwork.setRealApi(context, realApi)
}
