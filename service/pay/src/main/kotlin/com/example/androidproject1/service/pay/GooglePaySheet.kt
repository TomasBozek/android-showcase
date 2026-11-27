package com.example.androidproject1.service.pay

import android.content.Context
import com.example.androidproject1.service.core.domain.result.Outcome
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** The Google Pay client, in the **test** environment: no merchant account, no money. */
internal fun paymentsClient(context: Context): PaymentsClient = Wallet.getPaymentsClient(
    context,
    Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build(),
)

/**
 * [PaymentSheet] over Google Pay. Asking whether it is ready needs only a context; showing the
 * sheet needs an activity to resolve its result into, which [PayRequestActivity] hosts.
 */
public class GooglePaySheet(
    private val context: Context,
    private val config: PayConfig = PayConfig(),
) : PaymentSheet {

    override suspend fun isReady(): Boolean = runCatching {
        val request = IsReadyToPayRequest.fromJson(GooglePayRequests.isReadyToPay(config))
        paymentsClient(context).isReadyToPay(request).await()
    }.getOrDefault(false)

    override suspend fun pay(amount: Long, currency: String): Outcome<String> {
        val (id, answer) = PendingPayments.open()
        return try {
            context.startActivity(
                PayRequestActivity.intent(context, id, GooglePayRequests.paymentData(amount, currency, config)),
            )
            answer.await()
        } catch (failure: Exception) {
            if (failure is kotlin.coroutines.cancellation.CancellationException) throw failure
            Outcome.Failure(PaymentUnavailableError("Google Pay could not be shown", failure))
        } finally {
            PendingPayments.close(id)
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWith(Result.failure(it)) }
    }
}
