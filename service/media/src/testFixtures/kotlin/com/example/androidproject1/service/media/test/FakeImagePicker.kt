package com.example.androidproject1.service.media.test

import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.media.ImagePicker
import com.example.androidproject1.service.media.ImageSource
import java.io.File

/**
 * An [ImagePicker] a test scripts: [result] is what every pick answers — a fixture file by
 * default, `Outcome.Success(null)` for a user who backed out, a failure for a picture that could
 * not be read — and [asked] is the sources it was asked for, in order.
 */
class FakeImagePicker(var result: Outcome<File?>) : ImagePicker {

    constructor(file: File?) : this(Outcome.Success(file))

    val asked = mutableListOf<ImageSource>()

    override suspend fun pick(source: ImageSource): Outcome<File?> {
        asked += source
        return result
    }
}
