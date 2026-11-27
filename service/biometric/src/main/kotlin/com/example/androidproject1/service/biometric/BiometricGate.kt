package com.example.androidproject1.service.biometric

import com.example.androidproject1.service.core.ui.text.UiText

/** What this device can do right now. Read it when it matters: a fingerprint is enrolled, or a lock removed, in settings. */
public enum class Availability {

    /** A biometric or a device credential is set up: [BiometricGate.confirm] will show a prompt. */
    Ready,

    /** No biometric hardware *and* no way to set a screen lock the platform reports. */
    NoHardware,

    /** Hardware, but nothing enrolled and no screen lock: there is nothing to prove it with. */
    NotEnrolled,

    /** Temporarily or permanently unusable — a security update pending, locked out, an old platform. */
    Unavailable,
}

/** How a [BiometricGate.confirm] ended. */
public enum class GateResult {

    /** The owner proved it. */
    Confirmed,

    /** Tried and refused: too many wrong attempts, or a lockout. Not the same as backing out. */
    Denied,

    /** Backed out — the back gesture, the cancel button, another app in front. Not an error. */
    Cancelled,

    /** There was nothing to ask with (see [Availability]) or nowhere to ask from. The caller keeps its own confirmation. */
    Unavailable,
}

/**
 * One prompt that asks the user to prove they own the device: a biometric, or the device's PIN,
 * pattern or password — whichever the user has. No biometric of our own, no PIN of our own.
 *
 * A device with no screen lock at all answers [GateResult.Unavailable]; the caller falls back to
 * whatever it asked for before (a confirmation dialog). The gate never blocks what it cannot
 * check.
 */
public interface BiometricGate {

    public fun availability(): Availability

    /** [title] is the prompt's headline; it is resolved into the user's language when the prompt is shown. */
    public suspend fun confirm(title: UiText): GateResult
}
