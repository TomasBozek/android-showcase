package com.example.androidproject1.feature.devmenu.presentation

/**
 * The menu's rows that belong to a feature it cannot name, each `null` in an app without that
 * feature — the row is then not drawn. `DevMenuModule` fills it with `getOrNull()`, so `:app` binds
 * only the seams whose feature it has.
 */
class DevMenuHooks(
    val session: SessionReadout?,
    val reminder: ReminderTrigger?,
    val whatsNew: WhatsNewReset?,
)
