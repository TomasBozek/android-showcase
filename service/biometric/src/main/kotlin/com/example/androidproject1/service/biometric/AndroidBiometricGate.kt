package com.example.androidproject1.service.biometric

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.androidproject1.service.core.ui.text.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

/** Asks the platform which authenticators can be used now — a seam so a test scripts every answer. */
public fun interface AuthenticatorCheck {

    /** One of `BiometricManager.BIOMETRIC_*`. */
    public fun canAuthenticate(authenticators: Int): Int
}

/**
 * [BiometricGate] over `androidx.biometric`.
 *
 * The prompt is a `BiometricPrompt`, which needs a `FragmentActivity` to draw over, so this keeps
 * the resumed one (the app's single activity is an `AppCompatActivity`). With none resumed — the app
 * is in the background — the answer is [GateResult.Unavailable], not a prompt nobody sees.
 *
 * **Weak biometrics or the device credential**, not strong: `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`
 * is refused by the platform below API 30, and the minimum here is 29.
 */
public class AndroidBiometricGate(
    application: Application,
    private val check: AuthenticatorCheck = BiometricManager.from(application).let { manager ->
        AuthenticatorCheck { manager.canAuthenticate(it) }
    },
) : BiometricGate {

    @Volatile
    private var resumed: WeakReference<Activity>? = null

    init {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    resumed = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (resumed?.get() === activity) resumed = null
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) = Unit
            },
        )
    }

    override fun availability(): Availability = availabilityOf(check.canAuthenticate(AUTHENTICATORS))

    override suspend fun confirm(title: UiText): GateResult {
        if (availability() != Availability.Ready) return GateResult.Unavailable
        val activity = resumed?.get() as? FragmentActivity ?: return GateResult.Unavailable
        // A prompt is built and shown on the main thread.
        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(GateResult.Confirmed)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (continuation.isActive) continuation.resume(resultOfError(errorCode))
                        }

                        // A finger that did not match: the prompt stays up and the user tries again.
                        override fun onAuthenticationFailed() = Unit
                    },
                )
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
                prompt.authenticate(
                    BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title.resolve(activity))
                        .setAllowedAuthenticators(AUTHENTICATORS)
                        .build(),
                )
            }
        }
    }

    internal companion object {

        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        fun availabilityOf(code: Int): Availability = when (code) {
            BiometricManager.BIOMETRIC_SUCCESS -> Availability.Ready
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Availability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NotEnrolled
            else -> Availability.Unavailable
        }

        fun resultOfError(code: Int): GateResult = when (code) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED,
            -> GateResult.Cancelled

            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            BiometricPrompt.ERROR_TIMEOUT,
            -> GateResult.Denied

            // No hardware, nothing enrolled, no space, a vendor error: nothing to ask with.
            else -> GateResult.Unavailable
        }
    }
}
