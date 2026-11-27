package com.example.androidproject1.feature.gallery.presentation.gallerydetail

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.gallery.presentation.galleryEntry
import com.example.androidproject1.service.core.ui.test.assertAccessible
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The gallery's demos have to be usable, not only visible.
 *
 * The components are stateless, so a demo that passes a constant looks right and does nothing
 * when tapped. These tests reach the control inside a variant — every variant carries the same
 * `galleryDetail_variantItem` tag, so the control is found by `hasAnyAncestor` rather than by
 * text — and assert that trying it changes it.
 */
@RunWith(RobolectricTestRunner::class)
class GalleryDetailScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<GalleryDetailEvent>()

    private fun render(componentId: String, theme: DetailTheme = DetailTheme.System) {
        val entry = requireNotNull(galleryEntry(componentId)) { "$componentId is not in the catalogue" }
        val state = GalleryDetailState(
            componentId = entry.id,
            name = entry.name,
            group = entry.group,
            summary = entry.summary,
            theme = theme,
        )
        compose.setContent {
            AppTheme {
                GalleryDetailScreen(state = state, onEvent = events::add)
            }
        }
    }

    private fun insideAVariant(matcher: SemanticsMatcher) =
        matcher and hasAnyAncestor(hasTestTag("galleryDetail_variantItem"))

    @Test
    fun `is accessible`() {
        // Every clickable has a name and a touch target the theme allows — the two facts a
        // screen reader and a thumb need, on the state the previews show.
        render("checkbox")

        compose.assertAccessible()
    }

    @Test
    fun `renders the screen and its variants`() {
        render("checkbox")

        compose.onNodeWithTag("GalleryDetailScreen").assertIsDisplayed()
        compose.onAllNodesWithTag("galleryDetail_variantItem").onFirst().assertIsDisplayed()
    }

    @Test
    fun `a checkbox demo toggles when tapped`() {
        render("checkbox")
        // The first variant is the one that starts on.
        val checkbox = compose.onAllNodes(insideAVariant(isToggleable())).onFirst()

        checkbox.assertIsOn()
        checkbox.performClick()
        checkbox.assertIsOff()
    }

    @Test
    fun `the theme strip reports the theme that was picked`() {
        render("checkbox")

        compose.onNodeWithTag("galleryDetail_themeTab").assertIsDisplayed()
        // The options carry no tag of their own — the group does — so one is reached by position,
        // the way the bench's own stage test does it. Never by label: that is copy.
        compose.onAllNodes(
            hasClickAction() and hasAnyAncestor(hasTestTag("galleryDetail_themeTab")),
        )[DetailTheme.Dark.ordinal].performClick()

        assertEquals(listOf(GalleryDetailEvent.ThemePicked(DetailTheme.Dark)), events)
    }

    @Test
    fun `the variants still draw when the strip is on dark`() {
        // The strip is a second AppTheme written here (not the bench's stage), so
        // the one thing to hold is that the variants are still composed inside it.
        render("checkbox", theme = DetailTheme.Dark)

        compose.onAllNodesWithTag("galleryDetail_variantItem").onFirst().assertIsDisplayed()
    }

    @Test
    fun `the bench link reports its action`() {
        render("checkbox")

        compose.onNodeWithTag("galleryDetail_playgroundButton").performClick()

        assertEquals(listOf(GalleryDetailEvent.PlaygroundClicked), events)
    }

    @Test
    fun `a foundation page draws its variants`() {
        // The seven pages are entries like any other, so the detail screen needs nothing of its
        // own to render one.
        render("colours")

        compose.onNodeWithTag("GalleryDetailScreen").assertIsDisplayed()
        compose.onAllNodesWithTag("galleryDetail_variantItem").onFirst().assertIsDisplayed()
    }

    @Test
    fun `a text field demo keeps what is typed`() {
        render("textfield")
        // The first variant is the empty one.
        val field = compose.onAllNodes(insideAVariant(hasSetTextAction())).onFirst()

        field.performTextInput("Pilsner")

        field.assert(hasText("Pilsner"))
    }
}
