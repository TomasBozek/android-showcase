package com.example.androidproject1.service.play.test

import com.example.androidproject1.service.play.AppUpdates
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.play.UpdateOffer
import com.example.androidproject1.service.play.UpdateStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * An [AppUpdates] a test scripts: [offer] is what [check] finds (`null` for none), [startResult]
 * how every [start] ends, and [started] and [completed] are what was done, in order.
 */
class FakeAppUpdates(
    var offer: UpdateOffer? = null,
    var startResult: UpdateStart = UpdateStart.Started,
) : AppUpdates {

    val started = mutableListOf<UpdateKind>()
    var completed = 0
        private set

    override suspend fun check(): UpdateOffer? = offer

    override suspend fun start(kind: UpdateKind): UpdateStart {
        started += kind
        return if (offer == null) UpdateStart.NotStarted else startResult
    }

    override fun completeFlexible() {
        completed++
    }

    private val ready = MutableSharedFlow<Unit>(replay = 1)

    override val downloaded: Flow<Unit> = ready

    /** A flexible update finishes downloading, as Play would tell it. */
    fun finishDownload() {
        ready.tryEmit(Unit)
    }
}
