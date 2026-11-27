package com.example.androidproject1.feature.devmenu.presentation.networklog

import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.devtools.NetworkLogBuffer
import com.example.androidproject1.service.devtools.NetworkLogEntry
import com.example.androidproject1.service.log.domain.Logger
import kotlinx.coroutines.flow.map

/**
 * The client's exchanges since start, as the buffer `:core:di` feeds keeps them. Debug
 * builds only: the buffer is fed there and nowhere else, and the screen is a debug entry.
 */
class NetworkLogViewModel(
    private val networkLog: NetworkLogBuffer,
    logger: Logger,
) : BaseViewModel<NetworkLogState, NetworkLogEvent, NetworkLogNavigation>(
    initialState = NetworkLogState(entries = networkLog.entries.value),
    logger = logger.withTag("NetworkLogViewModel"),
) {

    init {
        observe(flow = { networkLog.entries.map { Outcome.Success(it) } }) { entries ->
            updateData { copy(entries = entries, selected = selected?.takeIf { it in entries }) }
        }
    }

    override fun onUiEvent(event: NetworkLogEvent) = when (event) {
        is NetworkLogEvent.EntryClicked ->
            updateData { copy(selected = entries.firstOrNull { it.id == event.id }) }
        NetworkLogEvent.DetailDismissed -> updateData { copy(selected = null) }
        NetworkLogEvent.CopyClicked -> state.value.data?.selected?.let { sendCommand(UiCommand.Copy(it.asText())) }
            ?: Unit
        NetworkLogEvent.ClearClicked -> networkLog.clear()
        NetworkLogEvent.UpClicked -> navigate(NetworkLogNavigation.NavigateUp)
    }
}

/** What a bug report wants pasted: the request line, the answer, and both bodies. */
private fun NetworkLogEntry.asText(): String = buildString {
    appendLine("$method $url")
    appendLine("${status ?: "no answer"} · $durationMs ms")
    requestBody?.let { appendLine().appendLine(it) }
    responseBody?.let { appendLine().appendLine(it) }
}.trimEnd()
