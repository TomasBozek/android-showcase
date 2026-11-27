package com.example.androidproject1.service.media.test

import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.media.CropArea
import com.example.androidproject1.service.media.ImageCropper
import java.io.File

/**
 * An [ImageCropper] a test scripts: every crop answers [result], and [asked] is what it was asked
 * to crop — the file, the area and the side — in order.
 */
class FakeImageCropper(var result: Outcome<File>) : ImageCropper {

    constructor(file: File) : this(Outcome.Success(file))

    val asked = mutableListOf<Triple<File, CropArea, Int>>()

    override suspend fun crop(file: File, area: CropArea, sidePx: Int): Outcome<File> {
        asked += Triple(file, area, sidePx)
        return result
    }
}
