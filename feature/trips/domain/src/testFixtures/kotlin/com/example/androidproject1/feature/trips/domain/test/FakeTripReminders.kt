package com.example.androidproject1.feature.trips.domain.test

import com.example.androidproject1.feature.trips.domain.TripReminders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [TripReminders]: [set] is recorded in [calls] and reflected by [isOn], and [reminded]
 * is what [remindNow] was asked for — answering [canRemind].
 */
class FakeTripReminders(var canRemind: Boolean = true) : TripReminders {

    private val on = MutableStateFlow<Set<String>>(emptySet())

    /** Every `set`, in order: the trip and whether it was turned on. */
    val calls = mutableListOf<Pair<String, Boolean>>()
    val reminded = mutableListOf<String>()

    override suspend fun set(tripId: String, on: Boolean) {
        calls += tripId to on
        this.on.value = if (on) this.on.value + tripId else this.on.value - tripId
    }

    override fun isOn(tripId: String): Flow<Boolean> = on.map { tripId in it }

    override suspend fun remindNow(tripId: String): Boolean {
        reminded += tripId
        return canRemind
    }
}
