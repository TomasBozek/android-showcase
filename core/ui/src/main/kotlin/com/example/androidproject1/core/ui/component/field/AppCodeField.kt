package com.example.androidproject1.core.ui.component.field

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.theme.AppTheme

/**
 * A one-time code: [length] boxes, one digit each, over **one** input.
 *
 * One input rather than six fields, because six fields are six focus stops, six places a paste
 * lands in the first box only, and six nodes a screen reader reads as six unnamed edit boxes. Here
 * the keyboard, the paste, the SMS autofill ([ContentType.SmsOtpCode]) and the screen reader all
 * meet a single field; the boxes are its decoration. Anything that is not a digit is dropped and
 * nothing past [length] is kept, so pasting "Your code is 123456" fills it.
 *
 * The key is `Done`, and [onDone] is what it runs — a code field is the last field of its form.
 * The box the next digit goes into wears the focus ring while the field is focused, which is the
 * only cursor a code field needs.
 */
@Composable
fun AppCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = DEFAULT_CODE_LENGTH,
    label: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = errorText != null

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
    ) {
        if (label != null) {
            // For the eye; the same string is on the input below, so it is not read twice.
            AppText(
                text = label,
                role = TextRole.Label,
                color = colors.textSecondary,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
        BasicTextField(
            value = value,
            onValueChange = { typed -> onValueChange(typed.filter(Char::isDigit).take(length)) },
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = onDone?.let { { it() } }),
            // The digits are drawn in the boxes; the input's own glyphs and caret would be a
            // second copy on top of them.
            textStyle = AppTheme.typography.numericMd.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentType = ContentType.SmsOtpCode
                    if (label != null) text = AnnotatedString(label)
                },
            decorationBox = { inner ->
                Box {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.sm),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(length) { index ->
                            val active = focused && index == value.length.coerceAtMost(length - 1)
                            val border = when {
                                !enabled -> colors.border
                                isError -> colors.destructive.bg
                                active -> colors.focusRing
                                else -> colors.borderStrong
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(AppTheme.density.minTouchTarget)
                                    .clip(AppTheme.shapes.md)
                                    .background(if (enabled) colors.surfaceSunken else colors.surfaceBase)
                                    .border(if (active || isError) 3.dp else 1.dp, border, AppTheme.shapes.md),
                            ) {
                                AppText(
                                    text = value.getOrNull(index)?.toString().orEmpty(),
                                    role = TextRole.Numeric,
                                    color = if (enabled) colors.textPrimary else colors.textDisabled,
                                )
                            }
                        }
                    }
                    // Over the boxes and invisible: it is what takes the taps, the keys and the paste.
                    Box(modifier = Modifier.matchParentSize()) { inner() }
                }
            },
        )
        if (errorText != null) {
            AppText(text = errorText, role = TextRole.Label, color = colors.destructive.bg)
        }
    }
}

/** Six, the length every code a sign-up or a reset mails is. */
const val DEFAULT_CODE_LENGTH = 6

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppCodeField(value = "", onValueChange = {}, label = "Code")
    AppCodeField(value = "4209", onValueChange = {}, label = "Code")
    AppCodeField(value = "123456", onValueChange = {}, label = "Code", errorText = "That code is not right")
    AppCodeField(value = "123456", onValueChange = {}, label = "Code", enabled = false)
}
