package com.example.androidproject1.service.permission

/**
 * Where a runtime permission stands, as five states rather than a boolean and a flag: each
 * "not granted" state wants different UI, so each is a case the caller names rather than a branch
 * it has to remember.
 */
public sealed interface PermissionStatus {

    /** Nothing has been asked for yet, and nothing was already held: ask. */
    public data object NotRequested : PermissionStatus

    /** Every permission asked for is held. */
    public data object Granted : PermissionStatus

    /**
     * Some of them are, some are not — only a multi-permission request can land here.
     *
     * Deliberately **not** a subtype of [Granted]: `status is Granted` would then be true for a
     * partial grant, which is the footgun this type exists to remove. A caller that is happy with
     * a subset says so by naming this case.
     */
    public data class PartiallyGranted(
        val granted: Set<String>,
        val denied: Set<String>,
    ) : PermissionStatus

    /**
     * Refused once, and the system will ask again: explain why before the second ask — Android
     * says so through `shouldShowRequestPermissionRationale`.
     */
    public data object NeedsRationale : PermissionStatus

    /**
     * Refused for good — twice, or "don't ask again" — so the system dialog will not show. The
     * only way forward is the app's page in system settings.
     */
    public data object RefusedForGood : PermissionStatus
}
