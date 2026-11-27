package com.example.androidproject1.service.play

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager

/**
 * Whether this build talks to Play. [testDoubles] is `dev`: it has no store account to rate or
 * update with, so Google's own fakes complete the flows and draw nothing. `staging` and
 * `prod` use the real managers. Built by `:app`, which knows the flavor.
 */
public data class PlayConfig(
    /**
     * Defaulted, and it has to be: Koin's `verify()` reads the primary constructor even of a type
     * bound by a lambda, and a `Boolean` without a default is reported as a missing binding.
     */
    val testDoubles: Boolean = false,
)

/** The manager a build uses: Play's own, or Google's fake. */
public fun PlayConfig.reviewManager(context: Context): ReviewManager =
    if (testDoubles) FakeReviewManager(context) else ReviewManagerFactory.create(context)

public fun PlayConfig.appUpdateManager(context: Context): AppUpdateManager =
    if (testDoubles) FakeAppUpdateManager(context) else AppUpdateManagerFactory.create(context)
