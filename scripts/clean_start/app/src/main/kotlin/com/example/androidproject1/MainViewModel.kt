package com.example.androidproject1

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.config.AppConfigRepository
import com.example.androidproject1.config.AppHold
import com.example.androidproject1.config.holdFor
import com.example.androidproject1.message.MessageRepository
import com.example.androidproject1.message.ServerMessage
import com.example.androidproject1.service.core.ui.event.AppMessage
import com.example.androidproject1.service.core.ui.event.AppMessages
import com.example.androidproject1.service.core.ui.event.MessageAction
import com.example.androidproject1.service.core.ui.event.MessageDuration
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.device.DeviceInfo
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.navigation.AppNavigation
import com.example.androidproject1.service.navigation.AppNavigatorChannel
import com.example.androidproject1.service.network.ConnectivityMonitor
import com.example.androidproject1.service.play.AppUpdates
import com.example.androidproject1.service.play.UpdateKind
import com.example.androidproject1.service.telemetry.CrashLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * What the whole app shows around its screens: whether the startup config holds it, the
 * offer of a newer version, a server message, the offer to send last run's crash record
 *, whether the device is online, and where a deep link lands.
 */
class MainViewModel(
    logger: Logger,
    connectivity: ConnectivityMonitor,
    private val messageRepository: MessageRepository,
    private val deviceInfo: DeviceInfo,
    private val clock: Clock,
    private val appConfigRepository: AppConfigRepository,
    private val appMessages: AppMessages,
    private val appNavigator: AppNavigatorChannel,
    private val appUpdates: AppUpdates,
    private val crashLog: CrashLog,
) : ViewModel() {

    private val logger = logger.withTag(TAG)

    /** The whole app is held — an update it must have, or maintenance — until the config says not. */
    val hold: StateFlow<AppHold?> = appConfigRepository.config
        .map { it?.holdFor(deviceInfo.versionCode) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val dismissedUpdate = MutableStateFlow<Int?>(null)

    /** A newer version the user has not dismissed yet, or `null`. */
    val updateOffer: StateFlow<Int?> = combine(appConfigRepository.config, dismissedUpdate) { config, dismissed ->
        config?.latestVersionCode?.takeIf { dismissed != null && it > deviceInfo.versionCode && it > dismissed }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableMessage = MutableStateFlow<ServerMessage?>(null)
    val message: StateFlow<ServerMessage?> = mutableMessage.asStateFlow()

    private val mutableDeepLink = MutableStateFlow<List<NavKey>>(emptyList())

    /** The keys a deep link lands on, until `MainActivity` has put them on the back stack. */
    val deepLink: StateFlow<List<NavKey>> = mutableDeepLink.asStateFlow()

    val online: StateFlow<Boolean> = connectivity.online

    private var flexibleAsked = false

    init {
        viewModelScope.launch {
            dismissedUpdate.value = appConfigRepository.dismissedUpdate()
            appConfigRepository.refresh()
        }
        observeFlexibleUpdate()
        offerCrashReport()
        followAppNavigation()
        fetchMessage()
    }

    /** Asks Play for a flexible update once a newer version is out, once per process. */
    fun checkForUpdate() {
        viewModelScope.launch {
            val config = appConfigRepository.config.value ?: return@launch
            if (flexibleAsked || config.latestVersionCode <= deviceInfo.versionCode) return@launch
            if (config.holdFor(deviceInfo.versionCode) != null) return@launch
            val offer = appUpdates.check() ?: return@launch
            if (!offer.flexibleAllowed) return@launch
            flexibleAsked = true
            appUpdates.start(UpdateKind.Flexible)
        }
    }

    fun dismissUpdate() {
        val offered = updateOffer.value ?: return
        dismissedUpdate.value = offered
        viewModelScope.launch { appConfigRepository.dismissUpdate(offered) }
    }

    fun dismissMessage() {
        val id = mutableMessage.value?.id ?: return
        mutableMessage.value = null
        viewModelScope.launch { messageRepository.dismiss(id) }
    }

    fun followMessage() {
        mutableMessage.value?.action?.let(appNavigator::open)
        dismissMessage()
    }

    fun onDeepLink(uri: String?) {
        val target = DeepLinks.parse(uri) ?: return
        mutableDeepLink.value = target.keys
    }

    fun onDeepLinkApplied() {
        mutableDeepLink.value = emptyList()
    }

    private fun offerCrashReport() {
        viewModelScope.launch {
            crashLog.collect()
            crashLog.takeNew() ?: return@launch
            appMessages.post(
                AppMessage(
                    key = CRASHED_MESSAGE,
                    text = R.string.main_crashed.toUiText(),
                    action = MessageAction.Handler(
                        label = R.string.main_crashed_send.toUiText(),
                        handlerKey = CrashReportHandler.KEY,
                        argument = "",
                    ),
                    duration = MessageDuration.Indefinite,
                ),
            )
        }
    }

    private fun observeFlexibleUpdate() {
        viewModelScope.launch {
            appConfigRepository.config.filterNotNull().first { it.latestVersionCode > deviceInfo.versionCode }
            checkForUpdate()
        }
        viewModelScope.launch {
            appUpdates.downloaded.collect {
                appMessages.post(
                    AppMessage(
                        key = UPDATE_DOWNLOADED_MESSAGE,
                        text = R.string.main_update_ready.toUiText(),
                        action = MessageAction.Handler(
                            label = R.string.main_update_restart.toUiText(),
                            handlerKey = UpdateRestartHandler.KEY,
                            argument = "",
                        ),
                        priority = AppMessage.Priority.High,
                        duration = MessageDuration.Indefinite,
                    ),
                )
            }
        }
    }

    /** A feature asks to go somewhere by its keys or by a link, through [AppNavigatorChannel]. */
    private fun followAppNavigation() {
        viewModelScope.launch {
            appNavigator.requests.collect { request ->
                when (request) {
                    is AppNavigation.Keys -> if (request.keys.isNotEmpty()) mutableDeepLink.value = request.keys
                    is AppNavigation.Link -> onDeepLink(request.deepLink)
                }
            }
        }
    }

    private fun fetchMessage() {
        viewModelScope.launch {
            val dismissed = runCatching { messageRepository.dismissed() }.getOrDefault(emptySet())
            val now = clock.instant()
            mutableMessage.value = runCatching { messageRepository.messages() }
                .onFailure { logger.w { "Messages unavailable ($it); showing none" } }
                .getOrDefault(emptyList())
                .firstOrNull { it.id !in dismissed && it.isLive(now) }
        }
    }

    private companion object {

        const val TAG = "MainViewModel"

        const val CRASHED_MESSAGE = "main.crashed"

        const val UPDATE_DOWNLOADED_MESSAGE = "main.update_downloaded"
    }
}
