package com.example.androidproject1

import com.example.androidproject1.config.AppConfigRepository
import com.example.androidproject1.config.DefaultAppConfigRepository
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.debug.TestNotification
import com.example.androidproject1.feature.devmenu.presentation.MessageReset
import com.example.androidproject1.lifecycle.ProcessForeground
import com.example.androidproject1.message.DefaultMessageRepository
import com.example.androidproject1.message.MessageRepository
import com.example.androidproject1.network.HttpCacheEraser
import com.example.androidproject1.network.connectivityMonitor
import com.example.androidproject1.network.networkEngine
import com.example.androidproject1.service.core.domain.LocalDataEraser
import com.example.androidproject1.service.core.domain.lifecycle.Foreground
import com.example.androidproject1.service.core.ui.event.AppMessages
import com.example.androidproject1.service.core.ui.event.MessageActionHandler
import com.example.androidproject1.service.core.ui.event.MessageOutlet
import com.example.androidproject1.service.core.ui.viewmodel.Reachability
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.devtools.ApiSwitch
import com.example.androidproject1.service.devtools.BuildInfo
import com.example.androidproject1.service.devtools.NotificationTester
import com.example.androidproject1.service.devtools.OfflineSwitch
import com.example.androidproject1.service.locale.AppCompatLanguageRepository
import com.example.androidproject1.service.locale.LanguageRepository
import com.example.androidproject1.service.navigation.AppNavigator
import com.example.androidproject1.service.navigation.AppNavigatorChannel
import com.example.androidproject1.service.network.ConnectivityMonitor
import com.example.androidproject1.service.network.NetworkConfig
import com.example.androidproject1.service.notification.NotificationConfig
import com.example.androidproject1.service.pay.GooglePaySheet
import com.example.androidproject1.service.pay.PaymentSheet
import com.example.androidproject1.service.play.PlayConfig
import com.example.androidproject1.service.telemetry.CrashReports
import io.ktor.client.engine.HttpClientEngine
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Bindings only the application module can provide. */
object ApplicationModule {
    val module: Module = module {
        viewModelOf(::MainViewModel)

        // BuildConfig lives here, so the config is built here: :core:di assembles the client but
        // must not know which flavor it is in.
        single {
            NetworkConfig(
                baseUrl = BuildConfig.BASE_URL,
                // Headers and bodies carry the bearer token and whatever the user typed.
                logBodies = BuildConfig.DEBUG,
            )
        }

        // The one thing about a notification only :app knows: its status-bar icon.
        single { NotificationConfig(smallIcon = R.drawable.ic_notification) }

        // `dev` resolves this to the MockEngine over res/raw fixtures; the other flavors to
        // OkHttp. Different source sets, so the wrong one is not in the build at all.
        single<HttpClientEngine> { networkEngine(androidContext(), get<NetworkConfig>().httpCacheSizeBytes) }

        // Whether a request has a route at all is the platform's to say — and on `dev` the fixture
        // switch's, since the engine is in-process and airplane mode means nothing to it. Per
        // flavor source set, beside the engine.
        single<ConnectivityMonitor> { connectivityMonitor(androidContext(), get()) }

        // What a view model's offline failure waits on: the monitor above, handed to core
        // ui's port at start, since `:service:core:ui` cannot name `:service:network`.
        single(createdAtStart = true) { Reachability.apply { install(get<ConnectivityMonitor>().online) } }

        // The per-app language, stored by AppCompat and read by the Settings feature. Here
        // for the same reason as the monitor: the class it wraps belongs to the activity stack.
        singleOf(::AppCompatLanguageRepository) bind LanguageRepository::class

        // The outlet: the application's own queue, the one every `Screen()` renders, with
        // the handlers the features bind looked up when an action is pressed. At start, so an
        // action pressed before anything injected the outlet still finds its handler.
        single<AppMessages>(createdAtStart = true) {
            MessageOutlet.of(androidContext()).apply {
                bindHandlers(get()) { key -> getAll<MessageActionHandler>().firstOrNull { it.key == key } }
            }
        }

        // The Restart on a downloaded update, found by key when the snackbar's action is pressed.
        singleOf(::UpdateRestartHandler) bind MessageActionHandler::class

        // The crash records shared from the debug menu or sent to support after a crash:
        // here, because the support address and the version are this build's.
        single<CrashReports> {
            CrashReportSender(
                crashLog = get(),
                files = get(),
                intents = get(),
                supportEmail = BuildConfig.SUPPORT_EMAIL,
                version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.FLAVOR})",
            )
        }
        singleOf(::CrashReportHandler) bind MessageActionHandler::class

        // Google Pay's test environment: no merchant account, no money. `dev` wraps it in one
        // the debug menu can pretend is ready; the others hand it back as it is.
        single<PaymentSheet> { DebugMenu.paymentSheet(GooglePaySheet(androidContext())) }

        // Whether the app is in front. Made at start, on the main thread, where the
        // process lifecycle takes its observer.
        single<Foreground>(createdAtStart = true) { ProcessForeground() }

        // The server's messages and the ids dismissed on this install.
        singleOf(::DefaultMessageRepository) bind MessageRepository::class

        // The startup config: asked once per start, the last answer kept, failing open.
        singleOf(::DefaultAppConfigRepository) bind AppConfigRepository::class

        // How code outside a screen moves the user: one channel, which MainViewModel
        // collects and anything else injects as the interface.
        single { AppNavigatorChannel() } bind AppNavigator::class

        // Which flavors talk to Play: `dev` has no store account, so Google's own fakes stand in
        // for the review sheet and the update flow.
        single { PlayConfig(testDoubles = BuildConfig.FLAVOR == "dev") }

        // The HTTP cache is the engine's, and the engine is this module's — so its share of
        // "delete my account" is bound here beside the seven the features bind.
        singleOf(::HttpCacheEraser) bind LocalDataEraser::class

        // Only where there is a debug menu to show them. A binding is a reference, and a `prod`
        // build that named `BuildInfo` would keep it; `DebugMenu.ENABLED` is a const, so the
        // branch folds away there. `KoinGraphTest` skips the same block for the same reason, so
        // the graph it verifies is still the graph that starts.
        if (DebugMenu.ENABLED) debugMenuBindings()
    }
}

/** What the debug menu reads: this build's identity, and the fixture engine's offline switch. */
private fun Module.debugMenuBindings() {
    single {
        val device = get<DeviceInfo>()
        BuildInfo(
            applicationId = BuildConfig.APPLICATION_ID,
            flavor = BuildConfig.FLAVOR,
            buildType = BuildConfig.BUILD_TYPE,
            versionName = device.versionName,
            versionCode = device.versionCode,
            baseUrl = BuildConfig.BASE_URL,
            // LeakCanary installs itself and nothing in :app references it, so asking the
            // classloader is the only honest way to say whether this build has it.
            leakDetection = runCatching { Class.forName(LEAK_CANARY) }.isSuccess,
        )
    }
    single<OfflineSwitch> { DebugMenu.offlineSwitch(androidContext()) }
    single<ApiSwitch> { DebugMenu.apiSwitch(androidContext()) }
    single<NotificationTester> { TestNotification(notifier = get()) }
    single<MessageReset> {
        val messages = get<MessageRepository>()
        object : MessageReset {
            override suspend fun reset() = messages.resetDismissed()
        }
    }
}

/** `debugImplementation` only, so it is absent from a release build by construction. */
private const val LEAK_CANARY = "leakcanary.LeakCanary"
