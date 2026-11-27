package com.example.androidproject1.service.play

import android.app.Application
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager

/**
 * Play's in-app review: the rating sheet, over the app, without a trip to the store.
 *
 * Play decides by quota whether the sheet appears and never says whether the user rated, so
 * [launch] returns when the flow is over, not what happened in it.
 */
public interface InAppReview {

    public suspend fun launch()
}

/**
 * [InAppReview] over whichever activity is in front: nothing in front — the app went to the
 * background between the tap and here — is not worth an error, the next ask will find one.
 */
public class PlayInAppReview(
    application: Application,
    private val manager: ReviewManager,
) : InAppReview {

    private val front = ResumedActivity(application)

    override suspend fun launch() {
        val activity = front.activity ?: return
        val info = manager.requestReview()
        manager.launchReview(activity, info)
    }
}
