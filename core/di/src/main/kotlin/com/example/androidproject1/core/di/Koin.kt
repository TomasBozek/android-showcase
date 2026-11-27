package com.example.androidproject1.core.di

import android.util.Log
import androidx.work.WorkerFactory
import com.example.androidproject1.core.ui.shell.ShellDoors
import com.example.androidproject1.feature.apphold.di.AppHoldModule
import com.example.androidproject1.feature.auth.di.AuthModule
import com.example.androidproject1.feature.cart.di.CartModule
import com.example.androidproject1.feature.catalog.di.CatalogModule
import com.example.androidproject1.feature.checkout.di.CheckoutModule
import com.example.androidproject1.feature.devmenu.di.DevMenuModule
import com.example.androidproject1.feature.devmenu.presentation.LogShare
import com.example.androidproject1.feature.gallery.di.GalleryModule
import com.example.androidproject1.feature.home.di.HomeModule
import com.example.androidproject1.feature.inventory.di.InventoryModule
import com.example.androidproject1.feature.movies.di.MoviesModule
import com.example.androidproject1.feature.onboarding.di.OnboardingModule
import com.example.androidproject1.feature.orders.di.OrdersModule
import com.example.androidproject1.feature.profile.di.ProfileModule
import com.example.androidproject1.feature.settings.di.SettingsModule
import com.example.androidproject1.feature.trips.di.TripsModule
import com.example.androidproject1.feature.weather.di.WeatherModule
import com.example.androidproject1.service.biometric.AndroidBiometricGate
import com.example.androidproject1.service.biometric.BiometricGate
import com.example.androidproject1.service.core.data.DataStoreFormDrafts
import com.example.androidproject1.service.core.data.DataStoreProvider
import com.example.androidproject1.service.core.data.EncryptedDataStoreProvider
import com.example.androidproject1.service.core.data.crypto.KeystoreAead
import com.example.androidproject1.service.core.domain.FormDrafts
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.coroutines.DefaultDispatcherProvider
import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider
import com.example.androidproject1.service.core.domain.crypto.Aead
import com.example.androidproject1.service.device.AndroidDeviceInfo
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.devtools.NetworkLogBuffer
import com.example.androidproject1.service.files.AndroidFileStore
import com.example.androidproject1.service.files.Downloader
import com.example.androidproject1.service.files.FilePurpose
import com.example.androidproject1.service.files.FileStore
import com.example.androidproject1.service.files.KtorDownloader
import com.example.androidproject1.service.files.KtorUploader
import com.example.androidproject1.service.files.Uploader
import com.example.androidproject1.service.intents.AndroidSystemIntents
import com.example.androidproject1.service.intents.LaunchResult
import com.example.androidproject1.service.intents.SystemIntents
import com.example.androidproject1.service.location.GeocoderGeocoding
import com.example.androidproject1.service.location.Geocoding
import com.example.androidproject1.service.location.LocationProvider
import com.example.androidproject1.service.location.PlatformLocationProvider
import com.example.androidproject1.service.log.data.AndroidLogger
import com.example.androidproject1.service.log.data.FileLogSink
import com.example.androidproject1.service.log.data.LogSink
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.log.domain.RedactingLogger
import com.example.androidproject1.service.media.AndroidImagePicker
import com.example.androidproject1.service.media.ImagePicker
import com.example.androidproject1.service.network.HttpClientFactory
import com.example.androidproject1.service.network.HttpExchange
import com.example.androidproject1.service.network.NetworkConfig
import com.example.androidproject1.service.network.RequestIdentity
import com.example.androidproject1.service.network.SessionEnder
import com.example.androidproject1.service.network.TokenRefresher
import com.example.androidproject1.service.network.TokenStore
import com.example.androidproject1.service.notification.AndroidNotifier
import com.example.androidproject1.service.notification.DataStoreNotificationPreferences
import com.example.androidproject1.service.notification.NotificationPreferences
import com.example.androidproject1.service.notification.Notifier
import com.example.androidproject1.service.permission.AndroidPermissionChecker
import com.example.androidproject1.service.permission.PermissionChecker
import com.example.androidproject1.service.play.AppUpdates
import com.example.androidproject1.service.play.InAppReview
import com.example.androidproject1.service.play.PlayAppUpdates
import com.example.androidproject1.service.play.PlayConfig
import com.example.androidproject1.service.play.PlayInAppReview
import com.example.androidproject1.service.play.UpdateSimulator
import com.example.androidproject1.service.play.appUpdateManager
import com.example.androidproject1.service.play.reviewManager
import com.example.androidproject1.service.play.simulatorFor
import com.example.androidproject1.service.scanner.CodeScanner
import com.example.androidproject1.service.scanner.FixedCodeScanner
import com.example.androidproject1.service.scanner.PlayCodeScanner
import com.example.androidproject1.service.session.DefaultSessionEnder
import com.example.androidproject1.service.session.DefaultSessionExpiry
import com.example.androidproject1.service.session.DefaultTokenRefresher
import com.example.androidproject1.service.session.EncryptedSessionStore
import com.example.androidproject1.service.session.SessionExpiry
import com.example.androidproject1.service.session.SessionStore
import com.example.androidproject1.service.telemetry.Analytics
import com.example.androidproject1.service.telemetry.AndroidExitReasons
import com.example.androidproject1.service.telemetry.BreadcrumbAnalytics
import com.example.androidproject1.service.telemetry.BreadcrumbErrorTracker
import com.example.androidproject1.service.telemetry.Breadcrumbs
import com.example.androidproject1.service.telemetry.CrashLog
import com.example.androidproject1.service.telemetry.DataStoreInstallId
import com.example.androidproject1.service.telemetry.ErrorTracker
import com.example.androidproject1.service.telemetry.ExitReasons
import com.example.androidproject1.service.telemetry.FileCrashLog
import com.example.androidproject1.service.telemetry.InstallId
import com.example.androidproject1.service.telemetry.LoggingAnalytics
import com.example.androidproject1.service.telemetry.LoggingErrorTracker
import com.example.androidproject1.service.telemetry.TrackingLogger
import com.example.androidproject1.service.work.AndroidScheduler
import com.example.androidproject1.service.work.HandlerWorkerFactory
import com.example.androidproject1.service.work.Scheduler
import com.example.androidproject1.service.work.WorkHandler
import com.google.android.play.core.appupdate.AppUpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import java.io.File
import java.time.Clock

/**
 * Cross-cutting dependencies available to every module.
 *
 * @param isDebug drives what reaches logcat. `:service:core:data` cannot read the app's
 * `BuildConfig`, so the app decides here and a release build stays quiet.
 */
fun coreModule(isDebug: Boolean): Module = module {
    // Every door the features bind, as one constructor argument for Home and the drawer.
    single { ShellDoors(getAll()) }

    val minLogLevel = if (isDebug) Log.DEBUG else Log.WARN

    // A tag comes from `logger.withTag(...)` at the point of use, so one binding is enough. The
    // sink is the log file, bound by `debugMenuModules` and so absent from `prod`.
    // Where defects are counted. The default reports to the log and nowhere else, which is
    // correct in debug and correct in a project that never adds a vendor. To swap in Crashlytics
    // or Sentry, override this one binding in :app. No vendor SDK is in the repo.
    // The vendor binding sits inside the breadcrumb decorator, so a swap keeps the trail.
    single { Breadcrumbs() }
    single<ErrorTracker> {
        BreadcrumbErrorTracker(
            delegate = LoggingErrorTracker(RedactingLogger(AndroidLogger(minLevel = minLogLevel, sink = getOrNull()))),
            breadcrumbs = get(),
            installId = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }

    // Where product events are counted. Same shape and same reasoning as the tracker above: the
    // default reports to the log and nowhere else, and no vendor SDK is in the repo. `AppScaffold`
    // sends the screen view; wire it into composition with `ProvideAnalytics` at the nav host.
    single<Analytics> {
        BreadcrumbAnalytics(
            LoggingAnalytics(RedactingLogger(AndroidLogger(minLevel = minLogLevel, sink = getOrNull()))),
            get(),
        )
    }

    // Every Logger in the app is decorated, so anything logged with a Throwable is reported
    // without BaseViewModel or BaseRepository taking an extra constructor argument. Redaction is
    // outermost: the tracker, logcat and the log file only ever see the masked line —
    // which is also why the two loggers above are wrapped.
    factory<Logger> {
        RedactingLogger(TrackingLogger(AndroidLogger(minLevel = minLogLevel, sink = getOrNull()), get()))
    }

    singleOf(::DefaultDispatcherProvider) bind DispatcherProvider::class
    // Beside the dispatchers, and for the same reason: ambient system state a test has to be able
    // to fix. It is bound rather than defaulted in a constructor because Koin's `*Of` builders
    // resolve every constructor parameter through `get()` and never consult a Kotlin default —
    // a default there compiles, reads as safe, and throws NoDefinitionFoundException on first use.
    single<Clock> { Clock.systemDefaultZone() }
    single { DataStoreProvider(androidContext()) }
    // The one identity telemetry attaches: a UUID in the preferences, never a hardware id,
    // and its own share of "delete my account", so the next install-or-erase gets a new one.
    single { DataStoreInstallId(get<DataStoreProvider>().dataStore) } binds
        arrayOf(InstallId::class, LocalDataEraser::class)
    // A form's unfinished input between launches, in the same preferences file; account
    // data, so a sign-out erases every draft.
    singleOf(::DataStoreFormDrafts) binds arrayOf(FormDrafts::class, LocalDataEraser::class)
    // What a crash or an ANR leaves behind: in the app's files, not its cache, so a record
    // outlives the system clearing space; the install's, so an account deletion takes it.
    single<ExitReasons> { AndroidExitReasons(androidContext()) }
    single {
        FileCrashLog(
            directory = { File(androidContext().filesDir, CRASH_DIRECTORY) },
            exitReasons = get(),
            clock = get(),
            dispatchers = get(),
        )
    } binds arrayOf(CrashLog::class, LocalDataEraser::class)

    // The session, encrypted at rest with a key the Keystore will not hand back. Separate from the
    // preferences store above: encrypting a theme choice costs a cold start and protects nothing.
    single<Aead> { KeystoreAead(alias = SESSION_KEY_ALIAS) }
    single { EncryptedDataStoreProvider(context = androidContext(), aead = get()) }

    // The engine and the config come from :app: the engine because only a flavor knows whether it
    // is talking to a server or to fixtures, the config because BASE_URL lives in :app's
    // BuildConfig and :core:di must not read it. The logger is passed only on debug, which is what
    // keeps request logging out of a release build.
    single {
        HttpClientFactory.create(
            engine = get(),
            config = get(),
            logger = if (isDebug) get<Logger>() else null,
            // The dev menu's network log, on debug only for the same reason: it keeps bodies.
            onExchange = if (isDebug) get<NetworkLogBuffer>().let { log -> { e -> log.record(e) } } else null,
            // Every request says which app, which language and which request it is; the id
            // of one that failed is kept for the dev menu to show.
            identity = get<DeviceInfo>().let { device ->
                RequestIdentity(
                    applicationId = device.applicationId,
                    versionName = device.versionName,
                    versionCode = device.versionCode,
                    osVersion = device.osVersion,
                    model = device.model,
                    language = { device.locale },
                )
            },
            onFailure = get<NetworkLogBuffer>()::recordFailure,
            // Whether there is a route at all is the platform's to say, so it comes from :app
            // as well — per flavor, beside the engine.
            connectivity = get(),
            // The bearer and its refresh: the session's tokens, and the one place a 401 that
            // no refresh answered ends it.
            tokenStore = get(),
            tokenRefresher = get(),
            sessionEnder = get(),
        )
    }

    // The refresh is asked of a client with no bearer of its own, or a refused refresh would ask
    // to be refreshed. Nothing else uses it, so it is not bound as an HttpClient.
    single<TokenRefresher> {
        DefaultTokenRefresher(
            HttpClientFactory.create(engine = get(), config = get<NetworkConfig>().copy(retries = 0)),
        )
    }

    // The session: who is signed in and the tokens, in one encrypted file — the store the client
    // reads its tokens from and the one the auth feature signs in and out through.
    single { EncryptedSessionStore(sessionStoreProvider = get(), dispatcherProvider = get(), logger = get()) } binds
        arrayOf(SessionStore::class, TokenStore::class)
    single<SessionEnder> { DefaultSessionEnder(expiry = get(), logger = get()) }

    single { NetworkLogBuffer() }

    // One instance, or the ender's signal is one the session's owner never hears.
    single<SessionExpiry> { DefaultSessionExpiry() }
}

/**
 * The platform's permission checker (`:service:permission`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val permissionServiceModule: Module = module {
    // A permission's status, for a view model to decide on: the platform's, watching the
    // resumed activity for its rationale answer, so it is one for the process — and made at start,
    // before the first activity resumes; made lazily it misses that resume and reads every refusal
    // as one for good.
    single<PermissionChecker>(createdAtStart = true) { AndroidPermissionChecker(androidApplication()) }
}

/**
 * A code scanner (`:service:scanner`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val scannerServiceModule: Module = module {
    // Read a barcode with Play services' scanner, which draws its own screen — no camera permission.
    // Where there is no store account or camera to point (`dev`), a fixed code.
    single<CodeScanner> {
        if (get<PlayConfig>().testDoubles) FixedCodeScanner() else PlayCodeScanner(androidContext())
    }
}

/**
 * Play's review and in-app update (`:service:play`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val playServiceModule: Module = module {
    // Play's review sheet and in-app update, over Google's fakes where `PlayConfig` says so.
    // The review is made at start: it keeps the resumed activity, and made lazily it misses the one
    // already up.
    single<InAppReview>(createdAtStart = true) {
        PlayInAppReview(androidApplication(), get<PlayConfig>().reviewManager(androidContext()))
    }
    single<AppUpdateManager> { get<PlayConfig>().appUpdateManager(androidContext()) }
    single<AppUpdates>(createdAtStart = true) { PlayAppUpdates(androidApplication(), get()) }
    // What the debug menu drives on `dev`, where the manager is Google's fake; nothing on the real one.
    single<UpdateSimulator> { simulatorFor(get()) }
}

/**
 * The biometric prompt (`:service:biometric`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val biometricServiceModule: Module = module {
    // One prompt that asks the owner to prove it. At start, for the reason the permission
    // checker is: it keeps the resumed activity, and made lazily it misses the one already up.
    single<BiometricGate>(createdAtStart = true) { AndroidBiometricGate(androidApplication()) }
}

/**
 * This app, this OS and this handset (`:service:device`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val deviceServiceModule: Module = module {
    // What this app, this OS and this handset are, read once.
    singleOf(::AndroidDeviceInfo) bind DeviceInfo::class
}

/**
 * Where the device is, and a place's name (`:service:location`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val locationServiceModule: Module = module {
    // Where the device is, and the name of a place from it. The permission is a screen's to ask for.
    single<LocationProvider> { PlatformLocationProvider(androidContext()) }
    single<Geocoding> { GeocoderGeocoding(androidContext()) }
}

/**
 * Files, downloads that resume and uploads with progress (`:service:files`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val filesServiceModule: Module = module {
    // Where files live, how another app gets one, and a download that resumes. The
    // downloader is the app's own client, so a 401 during one ends the session like any other.
    singleOf(::AndroidFileStore) bind FileStore::class
    single<Downloader> { KtorDownloader(get()) }
    // The upload's other half: the same client, so its 401 ends the session too.
    single<Uploader> { KtorUploader(get()) }
}

/**
 * Every command that leaves the app (`:service:intents`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val intentsServiceModule: Module = module {
    // Every command that leaves the app, resolved in one place; `Screen()` builds its own over its activity.
    single<SystemIntents> { AndroidSystemIntents(androidContext(), get()) }
}

/**
 * A picture from the picker or the camera (`:service:media`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val mediaServiceModule: Module = module {
    // A picture from the photo picker or the camera, as one call.
    single<ImagePicker> { AndroidImagePicker(context = androidContext(), files = get(), dispatchers = get()) }
}

/**
 * What the app posts to the shade (`:service:notification`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val notificationServiceModule: Module = module {
    // What the app posts to the shade. `NotificationConfig`, the status-bar icon, is :app's.
    singleOf(::DataStoreNotificationPreferences) bind NotificationPreferences::class
    singleOf(::AndroidNotifier) bind Notifier::class
}

/**
 * Work that runs while the app is closed (`:service:work`).
 * Its own module, so `delete_feature.py --prune-services` takes it whole.
 */
val workServiceModule: Module = module {
    // Work that runs while the app is closed. A feature binds a `WorkHandler` in its own
    // module; the factory finds every one by key, and `App` hands the factory to WorkManager.
    single<Scheduler> { AndroidScheduler(androidContext()) }
    single<WorkerFactory> { HandlerWorkerFactory { getAll<WorkHandler>() } }
}

/**
 * Every module this app's graph is assembled from, in registration order.
 *
 * `scripts/create_feature.py` and `delete_feature.py --prune-services` edit this list, so keep it
 * formatted one module per line: the plumbing in [coreModule], then a `<name>ServiceModule` per
 * service a feature may be the last caller of, then the features.
 * [debugMenuModules] is the one exception to "one list": see the reason there. `KoinGraphTest`
 * verifies both, and each in the builds that actually register it.
 */
fun appModules(isDebug: Boolean): List<Module> = listOf(
    coreModule(isDebug),
    permissionServiceModule,
    scannerServiceModule,
    playServiceModule,
    biometricServiceModule,
    deviceServiceModule,
    locationServiceModule,
    filesServiceModule,
    intentsServiceModule,
    mediaServiceModule,
    notificationServiceModule,
    workServiceModule,
    AuthModule.module,
    HomeModule.module,
    SettingsModule.module,
    CatalogModule.module,
    CartModule.module,
    ProfileModule.module,
    OnboardingModule.module,
    TripsModule.module,
    InventoryModule.module,
    MoviesModule.module,
    WeatherModule.module,
    AppHoldModule.module,
    CheckoutModule.module,
    OrdersModule.module,
)

/**
 * The features only a build with a debug menu registers — the menu itself and the component
 * gallery it leads to.
 *
 * Apart from [appModules] because a Koin module *is* a reference: listing these there would keep
 * `GalleryViewModel` reachable in a `prod` build, and with it `galleryCatalog` and every component
 * demo hanging off it — R8 cannot drop what the DI graph still names. `:app` calls this behind its
 * `DebugMenu.ENABLED` const, which folds to nothing in `prod`, and both features leave the APK.
 */
fun debugMenuModules(): List<Module> = listOf(
    GalleryModule.module,
    DevMenuModule.module,
    fileLogModule,
)

/**
 * The log file a tester shares from the debug menu: kept only where there is a menu to
 * share it from, so `prod` writes nothing to disk. In the cache, which the `FileProvider` already
 * serves, and gone with it when the system needs the space — it is a log, not a record.
 */
private val fileLogModule = module {
    single { FileLogSink(file = { get<FileStore>().file(FilePurpose.Cache, LOG_FILE) }) } bind LogSink::class
    single<LogShare> {
        val sink = get<FileLogSink>()
        val files = get<FileStore>()
        val intents = get<SystemIntents>()
        object : LogShare {
            override suspend fun share(): Boolean = withContext(Dispatchers.IO) {
                val copy = sink.snapshot(files.file(FilePurpose.Cache, LOG_SHARE_FILE))
                copy.length() > 0 && intents.shareFile(copy, "text/plain", null) == LaunchResult.Started
            }
        }
    }
}

/**
 * The single Koin registration point.
 *
 * @param platformModules bindings only the application module can provide, such as `MainViewModel`,
 * and — from a build that has one — [debugMenuModules].
 */
fun initKoin(
    vararg platformModules: Module,
    isDebug: Boolean = false,
    platformActions: KoinApplication.() -> Unit = {},
) {
    startKoin {
        platformActions()
        modules(*platformModules, *appModules(isDebug).toTypedArray())
    }
}

/** Where [FileCrashLog] keeps its records, under the app's files directory. */
private const val CRASH_DIRECTORY = "crashes"

private const val LOG_FILE = "app.log"

/** A copy, so the log can go on while another app reads what was shared. */
private const val LOG_SHARE_FILE = "app-shared.log"

/** The Keystore alias for the session store. Changing it signs everyone out. */
private const val SESSION_KEY_ALIAS = "session"

private fun NetworkLogBuffer.record(exchange: HttpExchange) = record(
    method = exchange.method,
    url = exchange.url,
    status = exchange.status,
    durationMs = exchange.durationMs,
    requestBody = exchange.requestBody,
    responseBody = exchange.responseBody,
)
