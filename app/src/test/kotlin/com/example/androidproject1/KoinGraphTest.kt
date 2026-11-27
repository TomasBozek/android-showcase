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
import com.example.androidproject1.core.ui.shell.ShellCreations
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellJumps
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.presentation.apphold.AppHoldViewModel
import com.example.androidproject1.feature.auth.api.ResetPasswordDestination
import com.example.androidproject1.feature.auth.api.VerifyEmailDestination
import com.example.androidproject1.feature.auth.presentation.resetpassword.ResetPasswordViewModel
import com.example.androidproject1.feature.auth.presentation.verifyemail.VerifyEmailViewModel
import com.example.androidproject1.feature.catalog.api.ProductDetailDestination
import com.example.androidproject1.feature.catalog.api.ProductPickerDestination
import com.example.androidproject1.feature.catalog.api.ProductsDestination
import com.example.androidproject1.feature.catalog.presentation.productdetail.ProductDetailViewModel
import com.example.androidproject1.feature.catalog.presentation.productpicker.ProductPickerViewModel
import com.example.androidproject1.feature.catalog.presentation.products.ProductsViewModel
import com.example.androidproject1.feature.devmenu.api.DevMenuPlaygroundDestination
import com.example.androidproject1.feature.devmenu.presentation.playground.DevMenuPlaygroundViewModel
import com.example.androidproject1.feature.gallery.api.GalleryDetailDestination
import com.example.androidproject1.feature.gallery.presentation.gallerydetail.GalleryDetailViewModel
import com.example.androidproject1.feature.inventory.api.InventoryDetailDestination
import com.example.androidproject1.feature.inventory.api.InventoryEditorDestination
import com.example.androidproject1.feature.inventory.presentation.inventorydetail.InventoryDetailViewModel
import com.example.androidproject1.feature.inventory.presentation.inventoryeditor.InventoryEditorViewModel
import com.example.androidproject1.feature.movies.api.MovieDetailDestination
import com.example.androidproject1.feature.movies.presentation.moviedetail.MovieDetailViewModel
import com.example.androidproject1.feature.orders.api.OrderDetailDestination
import com.example.androidproject1.feature.orders.presentation.orderdetail.OrderDetailViewModel
import com.example.androidproject1.feature.profile.api.AvatarCropDestination
import com.example.androidproject1.feature.profile.presentation.avatarcrop.AvatarCropViewModel
import com.example.androidproject1.feature.trips.api.DestinationPickerDestination
import com.example.androidproject1.feature.trips.api.TripDetailDestination
import com.example.androidproject1.feature.trips.presentation.destinationpicker.DestinationPickerViewModel
import com.example.androidproject1.feature.trips.presentation.tripdetail.TripDetailViewModel
import com.example.androidproject1.feature.weather.api.ForecastDestination
import com.example.androidproject1.feature.weather.presentation.forecast.ForecastViewModel
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.log.domain.RedactingLogger
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
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
 * Worth more here than in most projects: the graph spans 24 modules and `create_feature.py`
 * assembles part of it by editing `Koin.kt` and `core/di/build.gradle.kts`. A missing binding is
 * otherwise a crash at app launch, which is the slowest possible way to find out.
 */
@OptIn(KoinExperimentalAPI::class)
@RunWith(RobolectricTestRunner::class)
// A plain Application, not `App`: `App.onCreate` starts the global Koin, which the next test in
// the sandbox would find already started. The count test below builds a `koinApplication` of its
// own over Robolectric's context and closes it; nothing here needs the global one.
@Config(application = Application::class)
class KoinGraphTest {

    @Test
    fun `every dependency in the graph can be resolved`() {
        // One module including all the others: `verify` follows `includes`, so this checks the
        // graph as it is actually assembled rather than each module in isolation — where every
        // cross-module dependency would look missing. `appModules` is the same list `initKoin`
        // starts, so there is nothing here to keep in step by hand.
        val graph = module {
            includes(ApplicationModule.module)
            includes(appModules(isDebug = true))
            // The debug menu and the gallery are registered by :app, behind a const that is false
            // in `prod` — so this verifies them in exactly the builds that start them.
            if (DebugMenu.ENABLED) includes(debugMenuModules())
        }

        graph.verify(
            // Supplied at resolution time rather than by a module: Context by androidContext(),
            // SavedStateHandle by Koin's own ViewModel factory — see BaseViewModel.saved().
            // List: `DefaultAuthService` takes every LocalDataEraser as `getAll()`, which `verify`
            // cannot see through; the count below is what proves the list is the eight it should be.
            // Function0: `FileLogSink` is handed its file as a lambda, so the disk is not touched
            // until its own thread first writes. DataStore: `DataStoreInstallId` is handed
            // `DataStoreProvider`'s store in its definition, not resolved by type. The rest:
            // a `ShellDoor` is built by a function of its feature, so `verify` reads the data
            // class's fields; the next test counts the doors instead.
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
            ),
            // A screen that takes navigation arguments gets its route key from the destination
            // through `parametersOf(key)`, so the graph does not provide it. One line per such
            // screen; `doctor.py` fails if one is missing, and `create_screen.py --with-args`
            // writes it.
            injections = injectedParameters(
                definition<AvatarCropViewModel>(AvatarCropDestination::class),
                definition<OrderDetailViewModel>(OrderDetailDestination::class),
                definition<VerifyEmailViewModel>(VerifyEmailDestination::class),
                definition<AppHoldViewModel>(AppHoldDestination::class),
                definition<ResetPasswordViewModel>(ResetPasswordDestination::class),
                definition<ForecastViewModel>(ForecastDestination::class),
                definition<MovieDetailViewModel>(MovieDetailDestination::class),
                definition<InventoryDetailViewModel>(InventoryDetailDestination::class),
                definition<InventoryEditorViewModel>(InventoryEditorDestination::class),
                definition<DestinationPickerViewModel>(DestinationPickerDestination::class),
                definition<TripDetailViewModel>(TripDetailDestination::class),
                definition<ProductPickerViewModel>(ProductPickerDestination::class),
                definition<GalleryDetailViewModel>(GalleryDetailDestination::class),
                definition<DevMenuPlaygroundViewModel>(DevMenuPlaygroundDestination::class),
                definition<ProductsViewModel>(ProductsDestination::class),
                definition<ProductDetailViewModel>(ProductDetailDestination::class),
            ),
        )
    }

    /**
     * Every feature with account data binds a `LocalDataEraser`, and `:app` binds the HTTP
     * cache's. Counted rather than named, so a feature that grows a store and forgets its eraser
     * fails here rather than leaving a table behind after "delete my account".
     */
    @Test
    fun `every feature's share of the wipe is in the graph`() {
        val koin = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(ApplicationModule.module, *appModules(isDebug = true).toTypedArray())
        }.koin

        val erasers = koin.getAll<LocalDataEraser>()

        assertEquals(ERASERS, erasers.size)
        assertEquals(ERASERS, erasers.map { it::class }.toSet().size)
        koin.close()
    }

    /**
     * Every place in the shell is a door its feature binds, read with `getAll()`. Counted and
     * keyed, so a feature that loses its door, or two that claim one id, fail here rather than on a
     * Home with a gap in it.
     */
    @Test
    fun `every feature's door is in the graph, once`() {
        val koin = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                ApplicationModule.module,
                *appModules(isDebug = true).toTypedArray(),
                *debugMenuModules().toTypedArray(),
            )
        }.koin

        val doors = koin.getAll<ShellDoor>()

        assertEquals(DOORS, doors.size)
        assertEquals(DOORS, doors.map { it.id }.toSet().size)
        koin.close()
    }

    /**
     * The speed dial's choices and the debug menu's jumps are bound by the features that own
     * them, like the doors — counted and keyed the same way.
     */
    @Test
    fun `every feature's creations and jumps are in the graph, once`() {
        val koin = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(
                ApplicationModule.module,
                *appModules(isDebug = true).toTypedArray(),
                *debugMenuModules().toTypedArray(),
            )
        }.koin

        val creations = koin.get<ShellCreations>().all
        val jumps = koin.get<ShellJumps>().all

        assertEquals(listOf("trip", "item", "place"), creations.sortedBy { it.order }.map { it.id })
        assertEquals(JUMPS, jumps.size)
        assertEquals(JUMPS, jumps.map { it.id }.toSet().size)
        koin.close()
    }

    /** What every class is handed masks personal data before anything under it sees the line. */
    @Test
    fun `the logger is redacting, outermost`() {
        val koin = koinApplication {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(ApplicationModule.module, *appModules(isDebug = true).toTypedArray())
        }.koin

        assertTrue(koin.get<Logger>() is RedactingLogger)
        koin.close()
    }

    private companion object {

        /**
         * cart, catalog, inventory, movies, orders, trips, weather, profile, the checkout draft
         * — :app's HTTP cache, and what `coreModule` binds: the install id, the
         * form drafts and the crash log.
         */
        const val ERASERS = 13

        /** Seven samples' doors and five of the frame's: profile, settings, and — only where the debug menu's modules are — the menu, the gallery and the playground. */
        const val DOORS = 12

        /** Fourteen screens a tester jumps to: two each of the gallery, trips and inventory, three of movies, two of weather, and one each of the catalog, profile and settings. */
        const val JUMPS = 14
    }
}
