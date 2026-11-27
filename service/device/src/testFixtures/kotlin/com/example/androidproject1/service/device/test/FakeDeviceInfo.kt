package com.example.androidproject1.service.device.test

import com.example.androidproject1.service.device.DeviceInfo

/** A [DeviceInfo] a test names the facts of. Defaults to a plausible dev handset. */
class FakeDeviceInfo(
    override val applicationId: String = "com.example.androidproject1.dev",
    override val versionName: String = "1.6.0",
    override val versionCode: Int = 412,
    override val osVersion: String = "Android 15",
    override val sdkInt: Int = 35,
    override val model: String = "Google Pixel 9",
    override var locale: String = "en-US",
) : DeviceInfo
