package com.example.androidproject1.service.core.ui.accessibility

import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Dp

/**
 * The smallest touch target the theme in force allows, published into the semantics tree by the
 * theme's root so a test can hold every clickable to it without naming a number.
 *
 * A theme sets it once — `Modifier.semantics { minTouchTarget = density.minTouchTarget }` on the
 * node that wraps its content — and `assertAccessible()` in this module's test fixtures reads it
 * back: a pointer density that allows 40 dp is then held to 40 dp, a phone to 48 dp, a tablet to
 * 56 dp, from the same tokens the components drew with. Nothing at runtime reads it.
 */
internal val MinTouchTargetKey: SemanticsPropertyKey<Dp> = SemanticsPropertyKey("MinTouchTarget")

public var SemanticsPropertyReceiver.minTouchTarget: Dp by MinTouchTargetKey
