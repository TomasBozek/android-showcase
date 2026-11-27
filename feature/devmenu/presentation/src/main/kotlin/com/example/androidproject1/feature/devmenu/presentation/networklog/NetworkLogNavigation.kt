package com.example.androidproject1.feature.devmenu.presentation.networklog

/** One-off navigation intents, turned into back-stack calls in NetworkLogDestination. */
sealed interface NetworkLogNavigation {

    data object NavigateUp : NetworkLogNavigation
}
