package com.example.androidproject1.service.pay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.androidproject1.service.core.domain.result.Outcome
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Payments waiting for the sheet, by id. [GooglePaySheet] parks a caller here and starts
 * [PayRequestActivity]; the activity answers, once, and the caller resumes. In memory: a request
 * does not survive the process, and there is nobody left to answer.
 */
internal object PendingPayments {

    private val next = AtomicInteger()
    private val waiting = ConcurrentHashMap<Int, CompletableDeferred<Outcome<String>>>()

    fun open(): Pair<Int, CompletableDeferred<Outcome<String>>> {
        val id = next.incrementAndGet()
        return id to CompletableDeferred<Outcome<String>>().also { waiting[id] = it }
    }

    fun answer(id: Int, outcome: Outcome<String>) {
        waiting.remove(id)?.complete(outcome)
    }

    fun close(id: Int) {
        waiting.remove(id)
    }
}

/**
 * Shows the Google Pay sheet on behalf of [GooglePaySheet] and reports the token, or that the user
 * closed it. Invisible: it exists to hold the result contract. Finished the moment it has answered;
 * recreated after a process death it has nobody waiting and simply finishes.
 */
internal class PayRequestActivity : ComponentActivity() {

    private var id = 0

    private val sheet = registerForActivityResult(TaskResultContracts.GetPaymentDataResult()) { answer(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        id = intent.getIntExtra(EXTRA_ID, 0)
        // Only the first time: after a recreation the launcher's own result is already on its way.
        if (savedInstanceState != null) return
        val request = PaymentDataRequest.fromJson(requireNotNull(intent.getStringExtra(EXTRA_REQUEST)))
        sheet.launch(paymentsClient(this).loadPaymentData(request))
    }

    private fun answer(result: ApiTaskResult<PaymentData>) {
        val outcome: Outcome<String> = when {
            result.status.isSuccess -> {
                val token = result.result?.toJson()?.let(GooglePayRequests::tokenFrom)
                if (token != null) {
                    Outcome.Success(token)
                } else {
                    Outcome.Failure(PaymentUnavailableError("Google Pay answered no token"))
                }
            }
            result.status.statusCode == CommonStatusCodes.CANCELED -> Outcome.Failure(PaymentCancelledError())
            else -> Outcome.Failure(PaymentUnavailableError("Google Pay failed: ${result.status.statusCode}"))
        }
        PendingPayments.answer(id, outcome)
        finish()
    }

    /** The screen closed before it answered: nothing paid. */
    override fun onDestroy() {
        if (isFinishing) PendingPayments.answer(id, Outcome.Failure(PaymentCancelledError()))
        super.onDestroy()
    }

    companion object {

        private const val EXTRA_ID = "pay.id"
        private const val EXTRA_REQUEST = "pay.request"

        fun intent(context: Context, id: Int, requestJson: String): Intent =
            Intent(context, PayRequestActivity::class.java)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_REQUEST, requestJson)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
