package com.example.androidproject1.service.core.ui.test

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Every row of a list: a row's tag is `<stem>_item_<id>` — its own domain id after
 * `item`, so a flow can tap the row it means — and a test that wants them all asks by the stem.
 * `stem` is the tag up to and including `item`, e.g. `"cart_item"` or `"weather_placeItem"`; the
 * underscore is added here, so `cart_itemList` and `cart_itemRemoveButton` are not rows.
 */
fun ComposeTestRule.onAllRows(stem: String): SemanticsNodeInteractionCollection = onAllNodes(hasRowTag(stem))

/** A node whose tag names one row of [stem]'s list. */
fun hasRowTag(stem: String): SemanticsMatcher = SemanticsMatcher("TestTag starts with '${stem}_'") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("${stem}_") == true
}
