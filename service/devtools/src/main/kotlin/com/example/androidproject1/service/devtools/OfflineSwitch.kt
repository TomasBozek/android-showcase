package com.example.androidproject1.service.devtools

/**
 * The fixture engine's "pretend the server is down" flag, as a switch rather than a file to
 * `touch` over adb.
 *
 * Read and written rather than observed: the flag lives outside this process — `dev`'s
 * `FixtureNetwork` backs it with a file — so there is nothing to collect, only something to
 * re-read. The screen reads it when it opens and after every write.
 *
 * Only the flavor that talks to fixtures has one; the others bind [Unsupported] and the row is not
 * drawn.
 */
public interface OfflineSwitch {

    public val isSupported: Boolean

    public fun isOffline(): Boolean

    public fun setOffline(offline: Boolean)

    /** How long every fixture answer waits, in milliseconds. */
    public fun latencyMs(): Int

    public fun setLatencyMs(latencyMs: Int)

    /** The share of fixture answers that are a 503, in percent, from a seeded draw. */
    public fun failureRate(): Int

    public fun setFailureRate(failureRate: Int)

    /**
     * Whether the next fixture answer is a `401`. One-shot: the engine answers the next
     * request with it and disarms, so the switch reads back off once it has fired.
     */
    public fun isSessionExpiryArmed(): Boolean

    public fun setSessionExpiryArmed(armed: Boolean)

    /**
     * One of the startup config's answers, which the fixture serves the next time the app
     * starts — the config is asked for once per start.
     */
    public fun configAnswer(answer: ConfigAnswer): Boolean

    public fun setConfigAnswer(answer: ConfigAnswer, on: Boolean)

    /** Whether the fixture's orders move thirty times over, so a timeline fills in seconds. */
    public fun fastOrderClock(): Boolean

    public fun setFastOrderClock(on: Boolean)

    /**
     * Whether Google Pay is pretended to be ready: the payment step shows its button and a
     * payment answers with a made-up token, on an emulator that has no wallet.
     */
    public fun pretendGooglePay(): Boolean

    public fun setPretendGooglePay(on: Boolean)

    /** For a build that talks to a real server: there is no fixture to break, slow or fail. */
    public object Unsupported : OfflineSwitch {

        override val isSupported: Boolean = false

        override fun isOffline(): Boolean = false

        override fun setOffline(offline: Boolean): Unit = Unit

        override fun latencyMs(): Int = 0

        override fun setLatencyMs(latencyMs: Int): Unit = Unit

        override fun failureRate(): Int = 0

        override fun setFailureRate(failureRate: Int): Unit = Unit

        override fun isSessionExpiryArmed(): Boolean = false

        override fun setSessionExpiryArmed(armed: Boolean): Unit = Unit

        override fun configAnswer(answer: ConfigAnswer): Boolean = false

        override fun setConfigAnswer(answer: ConfigAnswer, on: Boolean): Unit = Unit

        override fun fastOrderClock(): Boolean = false

        override fun setFastOrderClock(on: Boolean): Unit = Unit

        override fun pretendGooglePay(): Boolean = false

        override fun setPretendGooglePay(on: Boolean): Unit = Unit
    }
}

/** What the fixture's startup config can say: each holds the app, or offers an update. */
public enum class ConfigAnswer {

    /** This build is below the server's minimum: held for an update. */
    BelowMinimum,

    /** The server is down for maintenance: held. */
    Maintenance,

    /** A newer build is out, and this one still works: the soft-update banner. */
    NewerAvailable,
}
