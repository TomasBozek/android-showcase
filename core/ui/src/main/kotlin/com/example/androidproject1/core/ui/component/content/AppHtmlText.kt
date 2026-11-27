package com.example.androidproject1.core.ui.component.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview

/**
 * Text that arrives as HTML — a CMS's description, a server's overview — read as markup
 * rather than shown with its tags: bold, italics, a list, a link.
 *
 * The role is [AppText]'s, so a description looks like the text around it. A link is underlined
 * and set semibold in the text's own colour: this system has no link colour, and colour alone
 * would be invisible to too many people anyway. A tap on one hands its URL to [onLink] and
 * nothing more — the caller turns it into `UiCommand.OpenBrowser`, which is what keeps the
 * reader in a Custom Tab rather than the browser proper.
 */
@Composable
fun AppHtmlText(
    html: String,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Body,
    onLink: (url: String) -> Unit = {},
) {
    val text = remember(html, onLink) {
        AnnotatedString.fromHtml(
            htmlString = html,
            linkStyles = TextLinkStyles(
                style = SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold),
            ),
            linkInteractionListener = { link -> (link as? LinkAnnotation.Url)?.url?.let(onLink) },
        )
    }
    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        style = role.textStyle(),
        color = role.textColor(),
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AppHtmlText(
        html = "Freshly <b>ground</b>, brewed to <i>order</i>.<ul><li>Arabica</li><li>Light roast</li></ul>" +
            "Read <a href=\"https://example.com/origin\">where it grows</a>.",
    )
}
