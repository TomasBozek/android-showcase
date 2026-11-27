package com.example.androidproject1.service.play

import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType

/**
 * What the debug menu drives on `dev`, where Google's `FakeAppUpdateManager` stands in for Play: put
 * an update on offer, and finish the download a flexible one started. On a build with the real
 * manager there is nothing to drive and [NoUpdateSimulator] does nothing.
 */
public interface UpdateSimulator {

    /** Play has [kind] for this device now, at a version code above any this build has. */
    public fun makeAvailable(kind: UpdateKind)

    /** Accepts and completes the download of the flexible update that was started, as a user and a network would. */
    public fun finishDownload()
}

internal object NoUpdateSimulator : UpdateSimulator {

    override fun makeAvailable(kind: UpdateKind): Unit = Unit

    override fun finishDownload(): Unit = Unit
}

internal class FakeUpdateSimulator(private val manager: FakeAppUpdateManager) : UpdateSimulator {

    override fun makeAvailable(kind: UpdateKind) {
        manager.setUpdateAvailable(
            OFFERED_VERSION_CODE,
            if (kind == UpdateKind.Immediate) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE,
        )
    }

    override fun finishDownload() {
        // The fake is a state machine and refuses a step out of order; a tap before the flow started is not an error.
        runCatching {
            manager.userAcceptsUpdate()
            manager.downloadStarts()
            manager.downloadCompletes()
        }
    }

    private companion object {

        const val OFFERED_VERSION_CODE = Int.MAX_VALUE - 1
    }
}

/** The simulator a build gets: the fake's when [PlayConfig] says `dev`, none otherwise. */
public fun simulatorFor(manager: com.google.android.play.core.appupdate.AppUpdateManager): UpdateSimulator =
    if (manager is FakeAppUpdateManager) FakeUpdateSimulator(manager) else NoUpdateSimulator
