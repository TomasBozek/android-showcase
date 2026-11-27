package com.example.androidproject1

import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.config.AppConfig
import com.example.androidproject1.config.AppConfigRepository
import com.example.androidproject1.config.AppHold
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.message.MessageRepository
import com.example.androidproject1.message.ServerMessage
import com.example.androidproject1.service.core.ui.event.MessageAction
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.test.FakeAppMessages
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.device.test.FakeDeviceInfo
import com.example.androidproject1.service.log.domain.test.FakeLogger
import com.example.androidproject1.service.navigation.AppNavigatorChannel
import com.example.androidproject1.service.network.ConnectivityMonitor
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.play.UpdateOffer
import com.example.androidproject1.service.play.test.FakeAppUpdates
import com.example.androidproject1.service.telemetry.CrashKind
import com.example.androidproject1.service.telemetry.CrashRecord
import com.example.androidproject1.service.telemetry.test.FakeCrashLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val logger = FakeLogger()

    private val messages = object : MessageRepository {
        var served = listOf(message("autumn", ServerMessage.Placement.Banner))
        val dismissedIds = mutableSetOf<String>()
        var fetches = 0

        override suspend fun messages(): List<ServerMessage> {
            fetches++
            return served
        }

        override suspend fun dismissed(): Set<String> = dismissedIds

        override suspend fun dismiss(id: String) {
            dismissedIds += id
        }

        override suspend fun resetDismissed() = dismissedIds.clear()
    }

    private fun message(
        id: String,
        placement: ServerMessage.Placement,
        from: String = "2026-01-01T00:00:00Z",
        until: String = "2027-01-01T00:00:00Z",
    ) = ServerMessage(
        id = id,
        placement = placement,
        title = "Title",
        body = "Body",
        action = "app://devmenu",
        from = from,
        until = until,
    )

    private val crashLog = FakeCrashLog()

    private fun viewModel() = MainViewModel(
        logger = logger,
        connectivity = ConnectivityMonitor.AlwaysOnline,
        messageRepository = messages,
        deviceInfo = FakeDeviceInfo(versionName = "1.6.0", versionCode = CURRENT_VERSION_CODE),
        clock = Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC),
        appConfigRepository = appConfig,
        appMessages = appMessages,
        appNavigator = appNavigator,
        appUpdates = appUpdates,
        crashLog = crashLog,
    )

    private val appUpdates = FakeAppUpdates()

    private val appMessages = FakeAppMessages()
    private val appNavigator = AppNavigatorChannel()

    /**
     * The config seam: a refresh applies what the server says, or — offline — the
     * last answer this install kept, or nothing at all.
     */
    private val appConfig = object : AppConfigRepository {
        /** What the server answers; `null` is offline. */
        var served: AppConfig? = null

        /** What an earlier run kept. */
        var cached: AppConfig? = null

        override val config = MutableStateFlow<AppConfig?>(null)

        var dismissed = 0

        override suspend fun dismissedUpdate(): Int = dismissed

        override suspend fun dismissUpdate(versionCode: Int) {
            dismissed = versionCode
        }

        override suspend fun refresh() {
            config.value = served?.also { cached = it } ?: cached
        }
    }

    @Test
    fun `the launch after a crash offers, once, to send the report`() = runTest {
        crashLog.records += CrashRecord(Instant.parse("2026-09-23T11:00:00Z"), CrashKind.Crash, "boom", "trace")

        viewModel()
        viewModel()

        val message = appMessages.posted.single { it.key == "main.crashed" }
        assertEquals(R.string.main_crashed.toUiText(), message.text)
        assertEquals(CrashReportHandler.KEY, (message.action as MessageAction.Handler).handlerKey)
    }

    @Test
    fun `a launch with no crash behind it says nothing about one`() = runTest {
        viewModel()

        assertTrue(appMessages.posted.none { it.key == "main.crashed" })
    }

    @Test
    fun `a build below the server's minimum is held for an update, signed out or not`() = runTest {
        appConfig.served =
            AppConfig(minVersionCode = CURRENT_VERSION_CODE + 1, latestVersionCode = CURRENT_VERSION_CODE + 1)

        val viewModel = viewModel()

        assertEquals(AppHold.UpdateRequired, viewModel.hold.value)
    }

    @Test
    fun `maintenance holds the app, and an update outranks it`() = runTest {
        appConfig.served = AppConfig(maintenance = true)
        assertEquals(AppHold.Maintenance, viewModel().hold.value)

        appConfig.served = AppConfig(minVersionCode = CURRENT_VERSION_CODE + 1, maintenance = true)
        assertEquals(AppHold.UpdateRequired, viewModel().hold.value)
    }

    @Test
    fun `offline, a hold the last answer set still holds`() = runTest {
        appConfig.cached = AppConfig(maintenance = true)

        assertEquals(AppHold.Maintenance, viewModel().hold.value)
    }

    @Test
    fun `offline with nothing cached, the app runs unheld`() = runTest {
        assertNull(viewModel().hold.value)
    }

    @Test
    fun `a newer build is offered, and dismissing it keeps that version from being offered again`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        val viewModel = viewModel()
        assertEquals(CURRENT_VERSION_CODE + 1, viewModel.updateOffer.value)

        viewModel.dismissUpdate()

        assertNull(viewModel.updateOffer.value)
        assertEquals(CURRENT_VERSION_CODE + 1, appConfig.dismissed)
        assertNull("a later start does not offer it either", viewModel().updateOffer.value)
    }

    @Test
    fun `a newer build than the one dismissed is offered again`() = runTest {
        appConfig.dismissed = CURRENT_VERSION_CODE + 1
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 2)

        assertEquals(CURRENT_VERSION_CODE + 2, viewModel().updateOffer.value)
    }

    @Test
    fun `this build being the latest offers nothing`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE)

        assertNull(viewModel().updateOffer.value)
    }

    @Test
    fun `a build at the minimum, with a newer one out, is not held`() = runTest {
        appConfig.served =
            AppConfig(minVersionCode = CURRENT_VERSION_CODE, latestVersionCode = CURRENT_VERSION_CODE + 5)

        assertNull(viewModel().hold.value)
    }

    @Test
    fun `a class that is not a screen moves the user through the app navigator`() = runTest {
        val viewModel = viewModel()

        // Anything with the navigator injected — a worker, a repository — asks the same way.
        appNavigator.navigate(listOf(DevMenuDestination))

        assertEquals(listOf<NavKey>(DevMenuDestination), viewModel.deepLink.value)
    }

    @Test
    fun `a live banner is shown, and fetched once per start`() = runTest {
        val viewModel = viewModel()

        assertEquals("autumn", viewModel.message.value?.id)
        assertEquals(1, messages.fetches)
    }

    @Test
    fun `a dialog message is shown the same way, as a dialog`() = runTest {
        messages.served = listOf(message("outage", ServerMessage.Placement.Dialog))

        assertEquals(ServerMessage.Placement.Dialog, viewModel().message.value?.placement)
    }

    @Test
    fun `a dismissed message is never shown again`() = runTest {
        val viewModel = viewModel()

        viewModel.dismissMessage()

        assertNull(viewModel.message.value)
        assertEquals(setOf("autumn"), messages.dismissedIds)
        assertNull(viewModel().message.value)
    }

    @Test
    fun `a message outside its window is not shown`() = runTest {
        messages.served = listOf(
            message(
                "spring",
                ServerMessage.Placement.Banner,
                from = "2027-03-01T00:00:00Z",
                until = "2027-06-01T00:00:00Z",
            ),
        )

        assertNull(viewModel().message.value)
    }

    @Test
    fun `the action follows its link and puts the message away`() = runTest {
        val viewModel = viewModel()

        viewModel.followMessage()

        assertEquals(listOf<NavKey>(DevMenuDestination), viewModel.deepLink.value)
        assertNull(viewModel.message.value)
        assertEquals(setOf("autumn"), messages.dismissedIds)
    }

    @Test
    fun `a newer build the server names starts Play's flexible update in the background`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        appUpdates.offer = flexibleOffer()

        viewModel()

        assertEquals(listOf(UpdateKind.Flexible), appUpdates.started)
    }

    @Test
    fun `an offer that appears later is taken on the next resume, and asked for once`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        val viewModel = viewModel()
        assertTrue("Play had nothing at start", appUpdates.started.isEmpty())

        appUpdates.offer = flexibleOffer()
        viewModel.checkForUpdate()
        viewModel.checkForUpdate()

        assertEquals(listOf(UpdateKind.Flexible), appUpdates.started)
    }

    @Test
    fun `no newer build, no Play offer, or a held app starts nothing`() = runTest {
        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE)
        appUpdates.offer = flexibleOffer()
        viewModel()
        assertTrue(appUpdates.started.isEmpty())

        appConfig.served = AppConfig(latestVersionCode = CURRENT_VERSION_CODE + 1)
        appUpdates.offer = null
        viewModel()
        assertTrue(appUpdates.started.isEmpty())

        appUpdates.offer = flexibleOffer()
        appConfig.served =
            AppConfig(minVersionCode = CURRENT_VERSION_CODE + 1, latestVersionCode = CURRENT_VERSION_CODE + 1)
        viewModel()
        assertTrue("a held app is the hold screen's to update", appUpdates.started.isEmpty())
    }

    @Test
    fun `a downloaded flexible update posts a snackbar whose action completes it`() = runTest {
        viewModel()

        appUpdates.finishDownload()

        val message = appMessages.posted.single { it.key == "main.update_downloaded" }
        val action = message.action as MessageAction.Handler
        assertEquals(UpdateRestartHandler.KEY, action.handlerKey)
        assertEquals(MessageDuration.Indefinite, message.duration)
        assertEquals(0, appUpdates.completed)

        UpdateRestartHandler(appUpdates).handle(action.argument)

        assertEquals(1, appUpdates.completed)
    }

    private fun flexibleOffer() = UpdateOffer(
        versionCode = CURRENT_VERSION_CODE + 1,
        staleDays = 1,
        priority = 0,
        immediateAllowed = false,
        flexibleAllowed = true,
    )
    private companion object {

        const val CURRENT_VERSION_CODE = 412
    }
}
