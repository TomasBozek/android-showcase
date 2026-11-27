package com.example.androidproject1

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import com.example.androidproject1.core.di.appModules
import com.example.androidproject1.core.di.debugMenuModules
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.presentation.apphold.AppHoldViewModel
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.feature.devmenu.presentation.ReminderTrigger
import com.example.androidproject1.feature.devmenu.presentation.SessionReadout
import com.example.androidproject1.feature.devmenu.presentation.WhatsNewReset
import com.example.androidproject1.feature.devmenu.presentation.playground.DevMenuPlaygroundViewModel
import com.example.androidproject1.feature.gallery.api.GalleryDetailDestination
import com.example.androidproject1.feature.gallery.presentation.gallerydetail.GalleryDetailViewModel
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.log.domain.RedactingLogger
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Asserts that every dependency every registered class asks for is actually provided.
 *
 * `create_feature.py` assembles part of the graph by editing `Koin.kt` and
 * `core/di/build.gradle.kts`, and `create_screen.py --with-args` adds its screen's route key to
 * [injectedParameters] below. A missing binding is otherwise a crash at app launch, which is the
 * slowest possible way to find out.
 */
@OptIn(KoinExperimentalAPI::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class KoinGraphTest {

    @Test
    fun `every dependency in the graph can be resolved`() {
        val graph = module {
            includes(ApplicationModule.module)
            includes(appModules(isDebug = true))
            if (DebugMenu.ENABLED) includes(debugMenuModules())
        }

        graph.verify(
            // Types Koin cannot see being provided because the platform or a parameter supplies them.
            extraTypes = listOf(
                Context::class,
                SavedStateHandle::class,
                List::class,
                Function0::class,
                DataStore::class,
                String::class,
                Int::class,
                ShellSection::class,
                ImageVector::class,
                NavKey::class,
                Set::class,
                Flow::class,
                // The debug menu's rows for features a clean start has none of: `DevMenuHooks`
                // reads each with `getOrNull()`, which `verify` cannot see.
                SessionReadout::class,
                ReminderTrigger::class,
                WhatsNewReset::class,
            ),
            // A ViewModel handed its route key with `parametersOf(key)`.
            injections = injectedParameters(
                definition<AppHoldViewModel>(AppHoldDestination::class),
                definition<GalleryDetailViewModel>(GalleryDetailDestination::class),
                definition<DevMenuPlaygroundViewModel>(DevMenuPlaygroundDestination::class),
            ),
        )
    }

    @Test
    fun `the logger is redacting, outermost`() {
        val koin = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(ApplicationModule.module, *appModules(isDebug = true).toTypedArray())
        }.koin

        assertTrue(koin.get<Logger>() is RedactingLogger)
        koin.close()
    }
}
