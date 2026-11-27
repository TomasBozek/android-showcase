package com.example.androidproject1.service.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

/**
 * Finds a job's [WorkHandler] by key. [handlers] is asked each time, so a graph that is still
 * being built when the factory is handed to WorkManager is not a problem.
 */
public class HandlerWorkerFactory(private val handlers: () -> List<WorkHandler>) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        if (workerClassName != HandlerWorker::class.java.name) return null
        val key = workerParameters.inputData.getString(KEY_HANDLER)
        return HandlerWorker(appContext, workerParameters, handlers().firstOrNull { it.key == key })
    }
}

/**
 * The one worker class. A handler nobody registered fails the job: a crash would be retried by the
 * platform on every start, for a job that can never run.
 */
internal class HandlerWorker(
    context: Context,
    params: WorkerParameters,
    private val handler: WorkHandler?,
) : CoroutineWorker(context, params) {

    private val input: Map<String, String>
        get() = inputData.keyValueMap.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap() - KEY_HANDLER

    override suspend fun doWork(): Result = when (handler?.run(input)) {
        WorkResult.Success -> Result.success()
        WorkResult.Retry -> Result.retry()
        WorkResult.Failure, null -> Result.failure()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        handler?.foregroundInfo(input) ?: super.getForegroundInfo()
}

internal const val KEY_HANDLER = "androidproject.work.handler"

internal fun WorkSpec.toData(): Data = Data.Builder()
    .putAll(input)
    .putString(KEY_HANDLER, handler)
    .build()
