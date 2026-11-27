package com.example.androidproject1.service.telemetry

/**
 * What a person does with the [CrashLog]'s records: [share] them from the debug menu, or
 * [send] them to the app's support address from the launch after a crash. Both open another app
 * and send nothing themselves; `false` when there is nothing to send or nothing could take it.
 * Bound by `:app`, which knows the support address.
 */
public interface CrashReports {

    public suspend fun share(): Boolean

    public suspend fun send(): Boolean
}
