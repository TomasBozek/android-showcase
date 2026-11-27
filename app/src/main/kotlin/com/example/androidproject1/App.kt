package com.example.androidproject1

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkerFactory
import com.example.androidproject1.core.di.debugMenuModules
import com.example.androidproject1.core.di.initKoin
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.debug.installDebugTooling
import com.example.androidproject1.service.notification.NotificationChannels
import com.example.androidproject1.service.telemetry.FileCrashLog
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class App : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // First, so StrictMode sees the graph being built — a disk read behind a Koin single is
        // exactly what it is here to catch. A no-op in a release build.
        installDebugTooling()

        initKoin(
            ApplicationModule.module,
            // Registered from here rather than from `appModules`, so a `prod` build never names
            // the debug menu or the gallery and R8 can drop both — see `debugMenuModules`.
            *debugModules().toTypedArray(),
            isDebug = BuildConfig.DEBUG,
        ) {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@App)
        }

        // Right after the graph, so a crash from here on leaves a record. The handler chains
        // in front of the system's, which still shows its dialog and ends the process.
        get<FileCrashLog>().install()

        // Channels are what the user sees in system settings, and creating them is idempotent —
        // so here, before anything can post to one, rather than at the first notification.
        NotificationChannels.create(this)
    }

    /**
     * WorkManager on demand: its worker factory is built from the handlers the features
     * bound, so it cannot start before Koin does — which is why the manifest removes its default
     * initializer. Asked for the first time when something is scheduled.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(get<WorkerFactory>()).build()

    /** `DebugMenu.ENABLED` is a `const` per flavor, so in `prod` this folds to an empty list. */
    private fun debugModules() = if (DebugMenu.ENABLED) debugMenuModules() else emptyList()
}
