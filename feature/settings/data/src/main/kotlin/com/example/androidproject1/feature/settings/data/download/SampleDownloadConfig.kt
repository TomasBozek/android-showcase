package com.example.androidproject1.feature.settings.data.download

/**
 * What the sample download fetches, and what it is called on disk. The default is a public
 * 100 MB test file; the `dev` fixture answers that host itself unless the real-hosts switch is on.
 */
data class SampleDownloadConfig(
    val url: String = "https://speed.cloudflare.com/__down?bytes=104857600",
    val fileName: String = "sample-100mb.bin",
)
