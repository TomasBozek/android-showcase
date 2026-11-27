package com.example.androidproject1.feature.profile.presentation.avatarcrop

/** One-off navigation intents, turned into back-stack calls in AvatarCropDestination. */
sealed interface AvatarCropNavigation {

    /** Back to Profile — with the new avatar after Done, with the old one after Cancel. */
    data object Back : AvatarCropNavigation
}
