package com.example.androidproject1.service.biometric.test

import com.example.androidproject1.service.biometric.Availability
import com.example.androidproject1.service.biometric.BiometricGate
import com.example.androidproject1.service.biometric.GateResult
import com.example.androidproject1.service.core.ui.text.UiText

/**
 * A [BiometricGate] a test answers for: [availability] is what the device can do, [answer] is how
 * every [confirm] ends — [GateResult.Confirmed] by default, so the yes path needs no setup — and
 * [titles] is what it was asked, in order.
 */
class FakeBiometric(
    var availability: Availability = Availability.Ready,
    var answer: GateResult = GateResult.Confirmed,
) : BiometricGate {

    val titles = mutableListOf<UiText>()

    override fun availability(): Availability = availability

    override suspend fun confirm(title: UiText): GateResult {
        titles += title
        // A device that cannot ask never shows a prompt, whatever the script says.
        return if (availability == Availability.Ready) answer else GateResult.Unavailable
    }
}
