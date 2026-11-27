package com.example.androidproject1

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.core.di.appModules
import com.example.androidproject1.core.di.debugMenuModules
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.feature.devmenu.presentation.LogShare
import com.example.androidproject1.service.log.data.FileLogSink
import com.example.androidproject1.service.log.data.LogSink
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The log file is kept where there is a debug menu to share it from — `dev` and `staging` —
 * and `prod` writes nothing to disk. Unit tests run on `devDebug` only, so the `prod` case is the
 * graph `App` starts when `DebugMenu.ENABLED` is `false`: no [debugMenuModules].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FileLogFlavorTest {

    private fun graph(vararg modules: Module) = koinApplication {
        androidContext(ApplicationProvider.getApplicationContext())
        modules(*modules)
    }.koin

    @Test
    fun `prod's graph binds no file sink and no share`() {
        val koin = graph(ApplicationModule.module, *appModules(isDebug = false).toTypedArray())

        assertNull(koin.getOrNull<LogSink>())
        assertNull(koin.getOrNull<LogShare>())
        koin.close()
    }

    @Test
    fun `this build's graph binds the file sink and its share`() {
        assertTrue("dev is a build with a debug menu", DebugMenu.ENABLED)
        val koin = graph(
            ApplicationModule.module,
            *appModules(isDebug = true).toTypedArray(),
            *debugMenuModules().toTypedArray(),
        )

        assertTrue(koin.get<LogSink>() is FileLogSink)
        koin.get<LogShare>()
        koin.close()
    }
}
