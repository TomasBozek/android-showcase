package com.example.androidproject1.service.core.ui.test

import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.AppMessages

/** Records every post, in order, so a test asserts what was said without a screen to say it on. */
class FakeAppMessages : AppMessages {

    val posted = mutableListOf<AppMessage>()

    override fun post(message: AppMessage) {
        posted += message
    }
}
