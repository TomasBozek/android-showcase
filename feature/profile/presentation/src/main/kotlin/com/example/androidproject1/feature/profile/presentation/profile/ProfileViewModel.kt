package com.example.androidproject1.feature.profile.presentation.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.androidproject1.core.ui.shell.ShellDoors
import com.example.androidproject1.feature.profile.api.AvatarCropDestination
import com.example.androidproject1.feature.profile.domain.AvatarUpload
import com.example.androidproject1.feature.profile.domain.Profile
import com.example.androidproject1.feature.profile.domain.ProfileRepository
import com.example.androidproject1.feature.profile.presentation.ProfileDoorState
import com.example.androidproject1.feature.profile.presentation.R
import com.example.androidproject1.feature.profile.presentation.onProfile
import com.example.androidproject1.service.core.ui.event.SystemEvent
import com.example.androidproject1.service.core.ui.event.UiCommand
import com.example.androidproject1.service.core.ui.form.ALERT_ID_DISCARD
import com.example.androidproject1.service.core.ui.form.Form
import com.example.androidproject1.service.core.ui.form.discardAlert
import com.example.androidproject1.service.core.ui.state.clearAlert
import com.example.androidproject1.service.core.ui.state.setAlert
import com.example.androidproject1.service.core.ui.text.toUiText
import com.example.androidproject1.service.core.ui.viewmodel.BaseViewModel
import com.example.androidproject1.service.log.domain.Logger
import com.example.androidproject1.service.media.ImagePicker
import com.example.androidproject1.service.media.ImageSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel(
    logger: Logger,
    private val profileRepository: ProfileRepository,
    private val imagePicker: ImagePicker,
    doors: ShellDoors,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<ProfileState, ProfileEvent, ProfileNavigation>(
    // An empty form is a state the screen can draw, so it renders straight away and fills in when
    // the stored profile arrives. `null` here would put a spinner over a form for the length of
    // one preferences read.
    initialState = ProfileState.EMPTY.copy(doors = doors.all.onProfile().map(ProfileDoorState::of)),
    logger = logger.withTag("ProfileViewModel"),
    savedStateHandle = savedStateHandle,
) {

    private val doors = doors.all.onProfile()

    /** The avatar the stored profile last had, and whether it has been seen at all yet. */
    private var storedAvatar: String? = null
    private var avatarSeen = false
    private var upload: Job? = null

    // A field the user edited survives process death and wins over the stored profile; `null`
    // until it is edited, so an untouched field still fills from the store.
    private var savedName: String? by saved(KEY_NAME, null)
    private var savedEmail: String? by saved(KEY_EMAIL, null)

    init {
        savedName?.let { typed -> updateData { copy(name = name.changed(typed)) } }
        savedEmail?.let { typed -> updateData { copy(email = email.changed(typed)) } }
        observeDoors()
        observeAvatar()
        execute(
            // No overlay for the same reason: it would only flash.
            action = { profileRepository.get() },
            onData = { profile ->
                updateData {
                    // Whoever is typing wins, field by field. The stored profile is where the form
                    // starts, not an update to apply over someone's hands.
                    val loaded = ProfileState.of(profile)
                    loaded.copy(
                        name = if (name.touched) name else loaded.name,
                        email = if (email.touched) email else loaded.email,
                        cameraOpen = cameraOpen,
                        uploadFraction = uploadFraction,
                        uploadFailed = uploadFailed,
                        doors = this.doors,
                    )
                }
            },
        )
    }

    /**
     * The avatar follows the stored profile, the rest of the form does not: the crop screen stores
     * a new one while this screen is under it, and nobody is typing an avatar. A new one
     * is uploaded; the one already stored when the screen opens is not sent again.
     */
    private fun observeAvatar() = observe(flow = { profileRepository.observeProfile() }) { profile ->
        updateData { if (avatarUri == profile.avatarUri) this else copy(avatarUri = profile.avatarUri) }
        val changed = avatarSeen && profile.avatarUri != storedAvatar
        avatarSeen = true
        storedAvatar = profile.avatarUri
        if (changed && profile.avatarUri != null) uploadAvatar()
    }

    /**
     * Sends the stored avatar, its progress on the form. A failure is not an alert: the picture is
     * still on the device and Retry sits under it. A newer avatar replaces an upload in flight.
     */
    private fun uploadAvatar() {
        upload?.cancel()
        updateData { copy(uploadFraction = 0f, uploadFailed = false) }
        upload = viewModelScope.launch {
            profileRepository.uploadAvatar().collect { progress ->
                when (progress) {
                    is AvatarUpload.Sending -> updateData { copy(uploadFraction = progress.fraction) }
                    AvatarUpload.Done -> updateData { copy(uploadFraction = null) }
                    is AvatarUpload.Failed -> {
                        logger.w(progress.error) { "The avatar's upload failed" }
                        updateData { copy(uploadFraction = null, uploadFailed = true) }
                    }
                }
            }
        }
    }

    // A figure that cannot be read leaves its row as it was, not a dialog over the form.
    private fun observeDoors() = doors.forEach { door ->
        viewModelScope.launch {
            door.figure.catch { logger.w(it) { "Door ${door.id}'s figure failed" } }.collect { figure ->
                updateData { copy(doors = doors.map { if (it.id == door.id) it.copy(figure = figure) else it }) }
            }
        }
    }

    override fun onSystemEvent(event: SystemEvent) {
        if (event is SystemEvent.AlertResult.Confirmed && event.id == ALERT_ID_DISCARD) {
            uiState.clearAlert()
            navigate(ProfileNavigation.NavigateUp)
            return
        }
        super.onSystemEvent(event)
    }

    override fun onUiEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.NameChanged -> {
                savedName = event.name
                updateData { copy(name = name.changed(event.name)) }
            }

            // Trimmed as it is typed, not at save. An address never has whitespace around it,
            // pasting one usually brings some, and trimming only at save would validate and store
            // something other than what the field shows.
            is ProfileEvent.EmailChanged -> {
                savedEmail = event.email.trim()
                updateData { copy(email = email.changed(event.email.trim())) }
            }

            ProfileEvent.TakePhotoClicked -> updateData { copy(cameraOpen = true) }

            ProfileEvent.ChoosePhotoClicked -> pickAvatar(ImageSource.Library)

            ProfileEvent.CaptureClicked -> pickAvatar(ImageSource.Camera)

            // The screen has no Context, so "send them to system settings" is a command like any
            // other and `Screen()` is what carries it out.
            ProfileEvent.OpenAppSettingsClicked -> sendCommand(UiCommand.OpenAppSettings)

            ProfileEvent.SaveClicked -> save()

            ProfileEvent.RetryUploadClicked -> uploadAvatar()

            ProfileEvent.NavigateUpClicked -> navigate(ProfileNavigation.NavigateUp)

            is ProfileEvent.DoorClicked -> doors.firstOrNull { it.id == event.id }?.let {
                navigate(ProfileNavigation.Open(it.route))
            }

            ProfileEvent.BackRequested -> uiState.setAlert(discardAlert())
        }
    }

    /**
     * Save, or say why not.
     *
     * The button stays enabled on an invalid form on purpose. A disabled submit is the thing
     * `core.5` was written against: it can say *whether* the form is submittable and never *why*,
     * so someone with a two-letter minimum they have not met is left pressing a dead button.
     * Pressing it here touches every field, which is what reveals the errors, and the first one
     * is repeated in a snackbar for anyone who is looking at the button rather than the field.
     */
    private fun save() {
        val state = uiState.value.data ?: return
        if (!state.canSubmit) {
            val (name, email) = Form.touchAll(state.name, state.email)
            val revealed = state.copy(name = name, email = email)
            updateData { revealed }
            revealed.firstError?.let { showSnackbar(it) }
            return
        }

        execute(
            loading = overlay(),
            action = {
                profileRepository.save(
                    Profile(
                        // A name keeps its spaces while it is being typed — "Jana " is halfway
                        // through "Jana Nováková" — so it is trimmed here instead.
                        name = state.name.value.trim(),
                        email = state.email.value,
                        avatarUri = state.avatarUri,
                    ),
                )
            },
            onData = { showSnackbar(R.string.profile_saved.toUiText()) },
        )
    }

    /**
     * Asks for a picture and, if the user chose one, opens the crop over it: which square is the
     * avatar is the user's to say, and the crop screen stores it. Backing out is `null`,
     * which is not an error and changes nothing. No overlay while another app is in front of this one.
     */
    private fun pickAvatar(source: ImageSource) = execute(
        action = { imagePicker.pick(source) },
        onData = { file ->
            file?.let {
                updateData { copy(cameraOpen = false) }
                navigate(ProfileNavigation.Open(AvatarCropDestination(uri = "file://${it.path}")))
            }
        },
    )

    private companion object {

        const val KEY_NAME = "profile_name"
        const val KEY_EMAIL = "profile_email"
    }
}
