package com.example.androidproject1.service.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private data object Somewhere : NavKey

/**
 * Code with no screen moves the user by asking the app navigator: the ask waits in the
 * channel until the shell collects it, so a request made before anything collects is not lost.
 */
class AppNavigatorChannelTest {

    /** A class that is not a screen, and navigates. */
    private class SessionWatcher(private val navigator: AppNavigator) {
        fun onSomethingHappened() = navigator.navigate(listOf(Somewhere))
    }

    @Test
    fun `a request from a class that is not a screen reaches the shell`() = runTest {
        val channel = AppNavigatorChannel()

        SessionWatcher(channel).onSomethingHappened()

        assertEquals(AppNavigation.Keys(listOf(Somewhere)), channel.requests.first())
    }

    @Test
    fun `a link and keys arrive in the order they were asked`() = runTest {
        val channel = AppNavigatorChannel()

        channel.open("app://cart")
        channel.navigate(listOf(Somewhere))

        assertEquals(
            listOf(AppNavigation.Link("app://cart"), AppNavigation.Keys(listOf(Somewhere))),
            channel.requests.take(2).toList(),
        )
    }
}
