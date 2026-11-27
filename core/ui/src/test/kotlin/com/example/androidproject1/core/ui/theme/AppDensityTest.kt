package com.example.androidproject1.core.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What a screen is handed at each size class, and what a pointer changes.
 *
 * This is the half of the size-class work a preview cannot state: a preview shows that a tablet
 * looks different, and this says in what — which typography, which touch target, which row height.
 */
@RunWith(RobolectricTestRunner::class)
class AppDensityTest {

    @get:Rule
    val compose = createComposeRule()

    private var sizeClass by mutableStateOf(SizeClass.Compact)
    private var pointer by mutableStateOf(false)
    private var seen: Tokens? = null
    private var composed = false

    @Test
    fun `the breakpoints are 720 and 1280`() {
        assertEquals(SizeClass.Compact, sizeClassFor(719))
        assertEquals(SizeClass.Regular, sizeClassFor(720))
        assertEquals(SizeClass.Regular, sizeClassFor(1279))
        assertEquals(SizeClass.Expanded, sizeClassFor(1280))
    }

    @Test
    fun `a phone gets the compact scale and a 48 dp target`() {
        val tokens = themeFor(SizeClass.Compact)

        assertEquals(48.dp, tokens.density.minTouchTarget)
        assertEquals(56.dp, tokens.density.listRowHeight)
        assertEquals(compactTypography(), tokens.typography)
    }

    @Test
    fun `a tablet gets the regular scale and a 56 dp target`() {
        val tokens = themeFor(SizeClass.Regular)

        assertEquals(56.dp, tokens.density.minTouchTarget)
        assertEquals(regularTypography(), tokens.typography)
        assertTrue(
            "Regular typography has to be larger than compact, or the size class buys nothing",
            tokens.typography.bodyMd.fontSize.value > compactTypography().bodyMd.fontSize.value,
        )
    }

    @Test
    fun `the reading width is uncapped on a phone and narrows as the window grows`() {
        // A line of text has a width it stops being readable past, and a window wider than that is
        // not an invitation to use all of it. A phone is never wider than the measure, so it has
        // no cap at all rather than a cap it can never reach.
        assertNull(themeFor(SizeClass.Compact).density.contentMaxWidth)
        assertEquals(640.dp, themeFor(SizeClass.Regular).density.contentMaxWidth)
        assertEquals(840.dp, themeFor(SizeClass.Expanded).density.contentMaxWidth)
    }

    @Test
    fun `a pointer does not change the reading width`() {
        // The cap follows the window, and a mouse does not make a line of text longer. It is the
        // one density role a pointer leaves alone.
        for (entry in SizeClass.entries) {
            assertEquals(
                "$entry with a pointer",
                themeFor(entry).density.contentMaxWidth,
                themeFor(entry, pointer = true).density.contentMaxWidth,
            )
        }
    }

    @Test
    fun `a pointer gets 40 dp targets and 48 dp rows whatever the width`() {
        for (entry in SizeClass.entries) {
            val tokens = themeFor(entry, pointer = true)

            assertEquals("$entry with a pointer", 40.dp, tokens.density.minTouchTarget)
            assertEquals("$entry with a pointer", 48.dp, tokens.density.listRowHeight)
            assertTrue("$entry with a pointer", tokens.density.pointer)
        }
    }

    @Test
    fun `a pointer changes the target, not the typography`() {
        val touch = themeFor(SizeClass.Regular).typography
        val mouse = themeFor(SizeClass.Regular, pointer = true).typography

        assertEquals(touch, mouse)
    }

    private class Tokens(val density: AppDensity, val typography: AppTypography)

    /**
     * Composes the theme once and reads back what a screen inside it would see. The inputs are
     * state, so each call recomposes rather than calling `setContent` a second time.
     */
    private fun themeFor(sizeClass: SizeClass, pointer: Boolean = false): Tokens {
        if (!composed) {
            compose.setContent {
                AppTheme(sizeClass = this.sizeClass, pointer = this.pointer) {
                    seen = Tokens(AppTheme.density, AppTheme.typography)
                }
            }
            composed = true
        }
        this.sizeClass = sizeClass
        this.pointer = pointer
        compose.waitForIdle()
        return checkNotNull(seen)
    }
}
