package com.example.androidproject1.core.ui.screenshot

import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

/**
 * A golden per `@ComponentPreview` in this module — the one module that records any. The
 * reasoning and the record/verify commands are [PreviewScreenshotSpec].
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class PreviewScreenshotTest(
    preview: ComposablePreview<AndroidPreviewInfo>,
) : PreviewScreenshotSpec(preview) {

    companion object {

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun previews(): List<ComposablePreview<AndroidPreviewInfo>> = previewsIn("com.example.androidproject1.core.ui")
    }
}
