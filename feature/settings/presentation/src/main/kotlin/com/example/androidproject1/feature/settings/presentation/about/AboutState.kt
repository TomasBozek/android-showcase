package com.example.androidproject1.feature.settings.presentation.about

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.settings.domain.AppInfo
import com.example.androidproject1.service.device.DeviceInfo

@Immutable
data class AboutState(
    val appInfo: AppInfo,
    val device: DeviceInfo,
) {

    companion object {

        val PREVIEW = AboutState(
            device = PreviewDevice(),
            appInfo = AppInfo(
                flavor = "dev",
                buildType = "debug",
                privacyUrl = "https://example.com/privacy",
                termsUrl = "https://example.com/terms",
                supportEmail = "support@example.com",
                helpUrl = "https://example.com/help",
            ),
        )
    }
}

/** A development build, the release a user has, and a version long enough to wrap. */
class AboutStatePreviews : PreviewParameterProvider<AboutState> {

    override val values = sequenceOf(
        AboutState.PREVIEW,
        AboutState.PREVIEW.copy(appInfo = AboutState.PREVIEW.appInfo.copy(flavor = "prod", buildType = "release")),
        AboutState.PREVIEW.copy(device = PreviewDevice(versionName = "1.6.0-rc.12+build.20260923.abcdef0123456789")),
    )
}

/** A handset for the preview and the tests: not read from anywhere, so a golden does not depend on the machine. */
internal data class PreviewDevice(
    override val versionName: String = "1.6.0",
    override val versionCode: Int = 412,
    override val applicationId: String = "com.example.androidproject1.dev",
    override val osVersion: String = "Android 15",
    override val sdkInt: Int = 35,
    override val model: String = "Google Pixel 9",
    override val locale: String = "en-US",
) : DeviceInfo
