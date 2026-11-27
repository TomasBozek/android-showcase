package com.example.androidproject1.service.notification.test

import com.example.androidproject1.service.notification.NotificationKind
import com.example.androidproject1.service.notification.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A [NotificationPreferences] in memory: every kind on until a test switches it off. */
class FakeNotificationPreferences : NotificationPreferences {

    private val off = MutableStateFlow(emptySet<NotificationKind>())

    override fun observe(kind: NotificationKind): Flow<Boolean> = off.map { kind !in it }

    override fun isOn(kind: NotificationKind): Boolean = kind !in off.value

    override suspend fun set(kind: NotificationKind, on: Boolean) {
        off.value = if (on) off.value - kind else off.value + kind
    }
}
