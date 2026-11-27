package com.example.androidproject1.service.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * [Scheduler] over WorkManager. The instance is taken on first use, so a graph that binds it costs
 * nothing until something is scheduled — and `App` has configured WorkManager by then.
 */
public class AndroidScheduler(private val context: Context) : Scheduler {

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun schedule(spec: WorkSpec) {
        val constraints = spec.constraints.toConstraints()
        val repeat = spec.repeat
        if (repeat != null) {
            val request = PeriodicWorkRequestBuilder<HandlerWorker>(repeat.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .setInputData(spec.toData())
                .setConstraints(constraints)
                .setInitialDelay(spec.delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .build()
            val policy = if (spec.replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP
            workManager.enqueueUniquePeriodicWork(spec.name, policy, request)
        } else {
            val request = OneTimeWorkRequestBuilder<HandlerWorker>()
                .setInputData(spec.toData())
                .setConstraints(constraints)
                .setInitialDelay(spec.delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .apply {
                    // Out of quota, an expedited job is an ordinary one, not a lost one.
                    if (spec.expedited) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
                .build()
            val policy = if (spec.replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            workManager.enqueueUniqueWork(spec.name, policy, request)
        }
    }

    override fun cancel(name: String) {
        workManager.cancelUniqueWork(name)
    }
}

private fun WorkConstraints.toConstraints(): Constraints = Constraints.Builder()
    .setRequiredNetworkType(
        when (network) {
            NetworkRequirement.None -> NetworkType.NOT_REQUIRED
            NetworkRequirement.Connected -> NetworkType.CONNECTED
            NetworkRequirement.Unmetered -> NetworkType.UNMETERED
        },
    )
    .setRequiresCharging(charging)
    .setRequiresBatteryNotLow(batteryNotLow)
    .build()
