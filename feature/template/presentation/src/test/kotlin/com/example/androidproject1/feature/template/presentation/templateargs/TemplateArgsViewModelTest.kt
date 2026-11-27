package com.example.androidproject1.feature.template.presentation.templateargs

import com.example.androidproject1.feature.template.api.TemplateArgsDestination
import com.example.androidproject1.service.core.ui.test.MainDispatcherRule
import com.example.androidproject1.service.log.domain.test.FakeLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TemplateArgsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // The route key the destination hands in. A plain object on Navigation 3 — no Bundle, and so
    // no Robolectric.
    private fun viewModel() = TemplateArgsViewModel(
        logger = FakeLogger(),
        args = TemplateArgsDestination(templateId = "example"),
    )

    @Test
    fun `reads its argument from the route`() = runTest {
        val state = viewModel().state.value

        assertEquals("example", state.data?.templateId)
    }
}
