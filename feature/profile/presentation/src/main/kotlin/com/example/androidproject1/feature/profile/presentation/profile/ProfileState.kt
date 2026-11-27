package com.example.androidproject1.feature.profile.presentation.profile

import androidx.compose.runtime.Immutable
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.androidproject1.feature.profile.domain.Profile
import com.example.androidproject1.feature.profile.presentation.ProfileDoorState
import com.example.androidproject1.service.core.ui.form.FieldState
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.email
import com.example.androidproject1.service.core.ui.form.minLength
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.text.UiText
import com.example.androidproject1.service.core.ui.text.toUiText

/**
 * @property cameraOpen whether the camera half of the picture section is showing. Note what this
 * is *not*: whether the camera permission is held. That lives outside the app and changes while
 * the app is backgrounded, so it is read in composition by `PermissionGate` and never kept here.
 */
@Immutable
data class ProfileState(
    val name: FieldState = nameField(),
    val email: FieldState = emailField(),
    val avatarUri: String? = null,
    val cameraOpen: Boolean = false,
    /** How much of the avatar is sent while its upload runs; `null` when none is running. */
    val uploadFraction: Float? = null,
    /** The last upload failed: the local avatar stays and Retry is offered. */
    val uploadFailed: Boolean = false,
    /** The rows other features placed on Profile — the orders, in this app. */
    val doors: List<ProfileDoorState> = emptyList(),
) {

    /** Derived, so "is submit allowed" and "why is it not" cannot disagree. */
    val canSubmit: Boolean get() = Form.canSubmit(name, email)

    /**
     * Whether leaving would lose something.
     *
     * `touched` rather than a comparison against the stored profile: the stored one is not kept
     * after the load, and the flag is already what decides that a load in flight must not overwrite
     * someone's hands. Typing and then typing it back counts as dirty, which errs the safe way.
     */
    val isDirty: Boolean get() = name.touched || email.touched

    /** The first rule broken, for the one line that says what to fix. */
    val firstError: UiText? get() = Form.firstError(name, email)

    companion object {

        val PREVIEW = ProfileState(
            name = nameField("Jana Nováková"),
            email = emailField("jana@example.com"),
            doors = listOf(ProfileDoorState("orders", "Your orders".toUiText(), "3 orders".toUiText())),
        )

        /** A first run: nothing typed, so nothing is wrong yet and nothing is shouted about. */
        val EMPTY = ProfileState()

        /** The avatar on its way to the server. */
        val PREVIEW_UPLOADING = PREVIEW.copy(uploadFraction = 0.4f)

        /** An upload that failed, with its retry. */
        val PREVIEW_UPLOAD_FAILED = PREVIEW.copy(uploadFailed = true)

        /** What the screen looks like after Save was pressed on a form that is not valid. */
        val PREVIEW_INVALID = ProfileState(
            name = nameField("J").touch(),
            email = emailField("jana@").touch(),
        )

        fun of(profile: Profile) = ProfileState(
            name = nameField(profile.name),
            email = emailField(profile.email),
            avatarUri = profile.avatarUri,
        )
    }
}

/**
 * The states this screen is drawn in — for the preview, and so for `ui.2`'s goldens.
 *
 * Several, because one preview only ever shows the state you were thinking about. Empty is where
 * layouts collapse; the invalid one is where the error lines appear and push everything down.
 */
class ProfileStatePreviews : PreviewParameterProvider<ProfileState> {

    override val values = sequenceOf(
        ProfileState.PREVIEW,
        ProfileState.EMPTY,
        ProfileState.PREVIEW_INVALID,
        ProfileState.PREVIEW_UPLOADING,
        ProfileState.PREVIEW_UPLOAD_FAILED,
    )
}

/**
 * The rules, spelled once. A field carries its own validators, so the screen that renders it and
 * the ViewModel that reads it are looking at the same ones.
 */
private fun nameField(value: String = "") =
    FieldState.of(required(), minLength(MIN_NAME_LENGTH), value = value)

private fun emailField(value: String = "") = FieldState.of(required(), email(), value = value)

private const val MIN_NAME_LENGTH = 2
