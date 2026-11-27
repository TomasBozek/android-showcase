package com.example.androidproject1

import com.example.androidproject1.service.core.ui.event.MessageActionHandler
import com.example.androidproject1.service.play.AppUpdates

/** The Restart on "an update is ready": installs the flexible update Play finished downloading. */
class UpdateRestartHandler(private val appUpdates: AppUpdates) : MessageActionHandler {

    override val key: String = KEY

    override suspend fun handle(argument: String) = appUpdates.completeFlexible()

    companion object {

        const val KEY = "update-restart"
    }
}
