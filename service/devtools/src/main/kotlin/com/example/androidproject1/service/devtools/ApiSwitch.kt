package com.example.androidproject1.service.devtools

/**
 * The fixture engine's "talk to the real hosts" flag, as a switch rather than a file to
 * `touch` over adb.
 *
 * Read and written rather than observed, like [OfflineSwitch]: the flag lives outside this
 * process — `dev`'s `FixtureNetwork` backs it with a file — so there is nothing to collect, only
 * something to re-read. One switch for every third-party host: it means "leave the process",
 * which is one question — and every forwarded host is public and keyless, so nothing stays
 * behind.
 *
 * Only the flavor that talks to fixtures has one; the others bind [Unsupported] and the row is not
 * drawn — they talk to the real hosts already.
 */
public interface ApiSwitch {

    public val isSupported: Boolean

    public fun isRealApi(): Boolean

    public fun setRealApi(realApi: Boolean)

    /** For a build that talks to a real server: there is nothing to switch. */
    public object Unsupported : ApiSwitch {

        override val isSupported: Boolean = false

        override fun isRealApi(): Boolean = false

        override fun setRealApi(realApi: Boolean): Unit = Unit
    }
}
