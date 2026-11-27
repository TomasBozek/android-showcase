package com.example.androidproject1.core.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Draws a field's digits through [format] — `4242 4242`, `08/26`, `777 123 456` — while the value
 * stays the digits alone, so the caret, a paste and a delete all work on what the user typed and
 * never on a separator.
 *
 * [format] may add anything but must keep the value's characters in order; the caret after the
 * n-th of them is drawn after the n-th of them in the output, whatever sits in between.
 */
internal class DigitsTransformation(private val format: (String) -> String) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = format(raw)
        // Where each character of the value landed in the output, found by walking both in order.
        val at = IntArray(raw.length)
        var j = 0
        raw.forEachIndexed { i, char ->
            while (j < out.length && out[j] != char) j++
            at[i] = j.coerceAtMost(out.length - 1).coerceAtLeast(0)
            j++
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset == 0 ||
                    raw.isEmpty()
                ) {
                    0
                } else {
                    (at[offset.coerceAtMost(raw.length) - 1] + 1).coerceAtMost(out.length)
                }

            override fun transformedToOriginal(offset: Int): Int = at.count { it < offset }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }

    override fun equals(other: Any?): Boolean = other is DigitsTransformation && other.format === format

    override fun hashCode(): Int = format.hashCode()
}

/** Digits in groups of [size], a space between: `4242 4242 4242 4242`. */
internal fun grouped(digits: String, size: Int = 4): String = digits.chunked(size).joinToString(" ")
