package com.example.androidproject1.service.navigation.test

import androidx.navigation3.runtime.NavKey
import com.example.androidproject1.service.navigation.Navigator

/**
 * A [Navigator] that records what it was asked, for a destination's test to assert the call
 * rather than a back-stack diff.
 *
 * Moves nothing. [back] and [selectTab] answer what [backAnswer] and [reselect] say, so a test of
 * the "at the start, leave" or the re-select path sets one and reads [calls].
 */
class FakeNavigator(
    override var currentTab: NavKey? = null,
    var backAnswer: Boolean = true,
    var reselect: Boolean = false,
) : Navigator {

    /** One entry per call, in order. */
    sealed interface Call {
        data class Push(val route: NavKey) : Call
        data class Replace(val route: NavKey) : Call
        data object Back : Call
        data class SelectTab(val root: NavKey) : Call
        data class SetResult(val key: String, val value: Any?) : Call
    }

    val calls = mutableListOf<Call>()

    override fun push(route: NavKey) {
        calls += Call.Push(route)
    }

    override fun replace(route: NavKey) {
        calls += Call.Replace(route)
    }

    override fun back(): Boolean {
        calls += Call.Back
        return backAnswer
    }

    override fun selectTab(root: NavKey): Boolean {
        calls += Call.SelectTab(root)
        return reselect
    }

    override fun setResult(key: String, value: Any?) {
        calls += Call.SetResult(key, value)
    }
}
