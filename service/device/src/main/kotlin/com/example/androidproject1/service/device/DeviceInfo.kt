package com.example.androidproject1.service.device

/**
 * What this app, this OS and this handset are — the facts a support request asks for, in one
 * place. About shows them, the debug menu's build line is made from them, and a feedback mail
 * carries them; none reads the platform its own way.
 *
 * Everything but [locale] is fixed for the life of the process and read once. The locale can change
 * while the app runs (the per-app language), so it is asked each time.
 */
public interface DeviceInfo {

    public val applicationId: String

    public val versionName: String

    /** The `versionCode`, which tells two builds of one [versionName] apart. */
    public val versionCode: Int

    /** `Android 15`. */
    public val osVersion: String

    /** The API level, for a report to be exact about which platform behaviours applied. */
    public val sdkInt: Int

    /** `Google Pixel 9`: the maker once, then the model — not `Google Google Pixel 9`. */
    public val model: String

    /** The BCP 47 tag of the language in use now — the app's own, where the user has set one. */
    public val locale: String

    /** The lines of a support report, in the order a reader wants them. */
    public val summary: String
        get() = "$versionName ($versionCode) · $osVersion (API $sdkInt) · $model · $locale"
}
