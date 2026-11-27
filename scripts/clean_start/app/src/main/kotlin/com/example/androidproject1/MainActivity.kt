package com.example.androidproject1

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.androidproject1.config.AppHold
import com.example.androidproject1.config.UpdateBanner
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.api.AppHoldReason
import com.example.androidproject1.message.MessageBanner
import com.example.androidproject1.message.MessageDialog
import com.example.androidproject1.message.ServerMessage
import com.example.androidproject1.service.navigation.AppNavigator
import com.example.androidproject1.service.navigation.LocalAppNavigator
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject

/**
 * The one activity. It draws the app in the system's theme: the offline banner, the startup
 * config's hold or, below a server message and an update offer, [AppNavHost] — which starts
 * on [START_DESTINATION].
 */
class MainActivity : AppCompatActivity() {

    internal val viewModel by viewModel<MainViewModel>()

    override fun onResume() {
        super.onResume()
        viewModel.checkForUpdate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A link that started the process is read once; a rotation must not replay it.
        if (savedInstanceState == null) {
            viewModel.onDeepLink(intent.deepLinkUri())
        }

        setContent {
            AppTheme {
                val backStack = rememberNavBackStack(START_DESTINATION)
                val deepLink by viewModel.deepLink.collectAsStateWithLifecycle()
                LaunchedEffect(deepLink) {
                    if (deepLink.isNotEmpty()) {
                        backStack.applyDeepLink(deepLink)
                        viewModel.onDeepLinkApplied()
                    }
                }
                val online by viewModel.online.collectAsStateWithLifecycle()
                CompositionLocalProvider(LocalAppNavigator provides koinInject<AppNavigator>()) {
                    Column(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                        AppBanner(
                            text = stringResource(R.string.main_offline),
                            visible = !online,
                            modifier = Modifier
                                .statusBarsPadding()
                                .testTag("main_offlineBadge"),
                        )
                        val hold by viewModel.hold.collectAsStateWithLifecycle()
                        val storeUrl = STORE_URL + packageName
                        hold?.let {
                            AppHoldHost(
                                hold = AppHoldDestination(
                                    reason = when (it) {
                                        AppHold.UpdateRequired -> AppHoldReason.UpdateRequired
                                        AppHold.Maintenance -> AppHoldReason.Maintenance
                                    },
                                    storeUrl = storeUrl,
                                ),
                            )
                        }
                        if (hold == null) {
                            Banners(storeUrl)
                            AppNavHost(
                                backStack = backStack,
                                modifier = Modifier.consumeWindowInsets(
                                    if (online) WindowInsets(0, 0, 0, 0) else WindowInsets.statusBars,
                                ),
                            )
                            ServerMessageDialog()
                        }
                    }
                }
            }
        }
    }

    /** A server message meant as a banner, and the offer of a newer version. */
    @Composable
    private fun Banners(storeUrl: String) {
        val message by viewModel.message.collectAsStateWithLifecycle()
        message?.takeIf { it.placement == ServerMessage.Placement.Banner }?.let {
            MessageBanner(it, viewModel::followMessage, viewModel::dismissMessage)
        }
        val updateOffer by viewModel.updateOffer.collectAsStateWithLifecycle()
        val uriHandler = LocalUriHandler.current
        if (updateOffer != null) {
            UpdateBanner(
                onUpdate = { uriHandler.openUri(storeUrl) },
                onDismiss = viewModel::dismissUpdate,
            )
        }
    }

    @Composable
    private fun ServerMessageDialog() {
        val message by viewModel.message.collectAsStateWithLifecycle()
        message?.takeIf { it.placement == ServerMessage.Placement.Dialog }?.let {
            MessageDialog(it, viewModel::followMessage, viewModel::dismissMessage)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onDeepLink(intent.deepLinkUri())
    }

    // Only the app's own scheme: any other VIEW intent that reaches this activity is not ours to parse.
    private fun Intent.deepLinkUri(): String? =
        data?.takeIf { it.scheme == BuildConfig.APPLICATION_ID }?.toString()
}

/** A link's keys on top of the start screen, which stays under them so back returns to it. */
private fun NavBackStack<NavKey>.applyDeepLink(keys: List<NavKey>) {
    if (keys.isEmpty() || lastOrNull() == keys.last()) return
    while (size > 1) removeAt(lastIndex)
    addAll(keys.filter { it != START_DESTINATION })
}

private const val STORE_URL = "https://play.google.com/store/apps/details?id="
