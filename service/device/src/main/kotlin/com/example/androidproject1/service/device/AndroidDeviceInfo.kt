package com.example.androidproject1.service.device

import android.content.Context
import android.os.Build
import java.util.Locale

/** [DeviceInfo] from `PackageManager` and `Build`, read when this is made and never again. */
public class AndroidDeviceInfo(context: Context) : DeviceInfo {

    override val applicationId: String = context.packageName

    override val versionName: String
    override val versionCode: Int

    init {
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        versionName = info?.versionName ?: UNKNOWN
        // `longVersionCode` since API 28; below it the int is all there is, and minSdk is 29.
        versionCode = info?.longVersionCode?.toInt() ?: 0
    }

    override val osVersion: String = "Android ${Build.VERSION.RELEASE}"

    override val sdkInt: Int = Build.VERSION.SDK_INT

    override val model: String = modelOf(Build.MANUFACTURER, Build.MODEL)

    override val locale: String get() = Locale.getDefault().toLanguageTag()

    internal companion object {

        const val UNKNOWN = "unknown"

        /**
         * A model usually already starts with its maker (`Pixel 9` doesn't, `Samsung SM-S928B`
         * sometimes does), so the maker is added only where it is not there — capitalised, because
         * the platform gives it in whatever case the vendor chose.
         */
        fun modelOf(manufacturer: String?, model: String?): String {
            val maker = manufacturer.orEmpty().trim()
            val name = model.orEmpty().trim()
            return when {
                name.isEmpty() -> maker.ifEmpty { UNKNOWN }.replaceFirstChar { it.uppercase() }
                maker.isEmpty() || name.startsWith(maker, ignoreCase = true) -> name
                else -> "${maker.replaceFirstChar { it.uppercase() }} $name"
            }
        }
    }
}
