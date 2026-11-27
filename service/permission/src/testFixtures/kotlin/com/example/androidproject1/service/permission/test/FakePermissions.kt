package com.example.androidproject1.service.permission.test

import com.example.androidproject1.service.permission.DeclaredPermission
import com.example.androidproject1.service.permission.PermissionChecker
import com.example.androidproject1.service.permission.PermissionStatus

/**
 * A [PermissionChecker] a test scripts: set a permission's status in [statuses] — a refusal, a
 * rationale — and every read answers it. One not scripted is what its [DeclaredPermission] says:
 * granted, or not asked yet.
 */
class FakePermissions(vararg scripted: Pair<String, PermissionStatus>) : PermissionChecker {

    val statuses = mutableMapOf(*scripted)

    override fun status(permission: DeclaredPermission): PermissionStatus = statuses[permission.name]
        ?: if (permission.isGranted) PermissionStatus.Granted else PermissionStatus.NotRequested
}
