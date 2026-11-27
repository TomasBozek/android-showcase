package com.example.androidproject1.service.play.test

import com.example.androidproject1.service.play.InAppReview

/** An [InAppReview] that counts how many times the flow was launched. */
class FakeInAppReview : InAppReview {

    var launches = 0
        private set

    override suspend fun launch() {
        launches++
    }
}
