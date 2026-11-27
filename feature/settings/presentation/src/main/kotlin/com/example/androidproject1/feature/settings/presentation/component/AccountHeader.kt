package com.example.androidproject1.feature.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ComponentPreview
import com.example.androidproject1.core.ui.common.ThemedComponentPreview
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.status.AppAvatarPhoto
import com.example.androidproject1.core.ui.component.status.AppAvatarSizes
import com.example.androidproject1.core.ui.theme.AppGlyphs
import com.example.androidproject1.feature.settings.presentation.R

/**
 * The account at the top of Settings: the avatar, the name and the address, as one row that opens
 * the profile.
 *
 * A row rather than a heading so the account reads like every other entry and is reached the same
 * way. The name is what a person recognises, so it is the headline and the address the supporting
 * line; with no name saved yet the address moves up, and with no session at all the row still
 * draws, since the profile is where the name is put in.
 */
@Composable
fun AccountHeader(
    name: String?,
    email: String?,
    avatarUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCopyEmail: (() -> Unit)? = null,
) {
    // What the avatar's initials and colour come from: the name, else the address, else nothing
    // — a blank disc, since "Not signed in" is a message and not a person to draw initials for.
    val identity = name?.takeIf { it.isNotBlank() } ?: email?.takeIf { it.isNotBlank() }
    val headline = identity ?: stringResource(R.string.settings_account_none)
    AppListItem(
        headline = headline,
        supporting = email?.takeIf { it.isNotBlank() && it != headline },
        onClick = onClick,
        leading = {
            AppAvatarPhoto(
                name = identity.orEmpty(),
                photo = avatarUri,
                size = AppAvatarSizes.row,
            )
        },
        // A key of its own at the row's end: the row's tap still opens the profile.
        trailing = email?.takeIf { it.isNotBlank() && onCopyEmail != null }?.let {
            {
                AppIconButton(
                    icon = AppGlyphs.Copy,
                    contentDescription = stringResource(R.string.settings_copy_email),
                    onClick = { onCopyEmail?.invoke() },
                    modifier = Modifier.testTag("settings_copyButton"),
                )
            }
        },
        modifier = modifier.testTag("settings_profileButton"),
    )
}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {
    AccountHeader(name = "Ada Lovelace", email = "ada@example.com", avatarUri = null, onClick = {}, onCopyEmail = {})
    AccountHeader(name = "", email = "ada@example.com", avatarUri = null, onClick = {})
    AccountHeader(name = null, email = null, avatarUri = null, onClick = {})
}
