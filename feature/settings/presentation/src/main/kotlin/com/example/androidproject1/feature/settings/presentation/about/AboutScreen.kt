package com.example.androidproject1.feature.settings.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.settings.presentation.R

/**
 * What this build is, and the three documents a store listing links to. A release build shows its
 * version nowhere else — the debug menu has it and `prod` has no debug menu.
 */
@Composable
fun AboutScreen(
    state: AboutState,
    onEvent: (AboutEvent) -> Unit,
) {
    val info = state.appInfo
    AppScaffold(
        screenId = "AboutScreen",
        topBar = {
            AppTopBar(
                title = stringResource(R.string.about_title),
                onNavigateUp = { onEvent(AboutEvent.NavigateUpClicked) },
                navigateUpTestTag = "about_upButton",
            )
        },
        contentPadding = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.xs),
        ) {
            AppSectionHeader(
                title = stringResource(R.string.about_build_section),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            // The version a support request asks for, with the code that tells two builds of one
            // version apart.
            AppListItem(
                headline = stringResource(R.string.about_version),
                supporting = stringResource(
                    R.string.about_version_value,
                    state.device.versionName,
                    state.device.versionCode,
                ),
                modifier = Modifier.testTag("about_versionValue"),
            )
            AppListItem(
                headline = stringResource(R.string.about_build),
                supporting = info.buildType,
                modifier = Modifier.testTag("about_buildValue"),
            )
            AppListItem(
                headline = stringResource(R.string.about_flavor),
                supporting = info.flavor,
                modifier = Modifier.testTag("about_flavorValue"),
            )

            AppSectionHeader(
                title = stringResource(R.string.about_legal_section),
                modifier = Modifier.padding(horizontal = AppTheme.spacing.inset.lg),
            )
            AppListItem(
                headline = stringResource(R.string.about_privacy),
                onClick = { onEvent(AboutEvent.PrivacyClicked) },
                modifier = Modifier.testTag("about_privacyButton"),
            )
            AppListItem(
                headline = stringResource(R.string.about_terms),
                onClick = { onEvent(AboutEvent.TermsClicked) },
                modifier = Modifier.testTag("about_termsButton"),
            )
            AppListItem(
                headline = stringResource(R.string.about_licences),
                onClick = { onEvent(AboutEvent.LicencesClicked) },
                modifier = Modifier.testTag("about_licencesButton"),
            )
        }
    }
}

@ScreenPreview
@Composable
private fun Preview(
    @PreviewParameter(AboutStatePreviews::class) state: AboutState,
) = ThemedScreenPreview {
    AboutScreen(
        state = state,
    ) {}
}
