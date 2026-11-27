package com.example.androidproject1.service.core.ui.test

import com.example.androidproject1.service.core.ui.viewmodel.Reachability
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Scripts whether the device is online for a view model under test: set [online] and every
 * `BaseViewModel` sees it. A rule, because [Reachability] is the process's: it is installed before
 * each test and put back to always-online after, so no other test inherits a dead network.
 */
class FakeReachability(initial: Boolean = true) : TestWatcher() {

    val online = MutableStateFlow(initial)

    override fun starting(description: Description) = Reachability.install(online)

    override fun finished(description: Description) = Reachability.reset()
}
