package com.example.androidproject1.feature.home.presentation.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.androidproject1.core.ui.common.ScreenPreview
import com.example.androidproject1.core.ui.common.ThemedScreenPreview
import com.example.androidproject1.core.ui.component.action.AppFabMenu
import com.example.androidproject1.core.ui.component.action.FabAction
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.navigation.AppScaffold
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.layout.TabReselectEffect
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.layout.appStaggeredEntrance
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.SizeClass
import com.example.androidproject1.feature.home.presentation.R
import com.example.androidproject1.feature.home.presentation.component.HomeCard
import com.example.androidproject1.feature.home.presentation.title
import com.example.androidproject1.service.core.ui.text.resolve

@Composable
fun HomeScreen(
    state: HomeState,
    // Hoisted with a default, so the host's tab re-select and a test can reach it.
    listState: LazyListState = rememberLazyListState(),
    /**
     * Whatever the host puts above the doors — a server message. A slot rather than a
     * state: the message is `:app`'s, and this feature neither knows nor needs its shape.
     */
    headerContent: @Composable () -> Unit = {},
    onEvent: (HomeEvent) -> Unit,
) {
    // Cold start ends here, not at the first frame: the activity is fully drawn once every public
    // door has its figure — the number `am start -W` and a baseline profile move.
    ReportDrawnWhen { state.figuresIn }
    // A second tap on this tab pops to this root and scrolls it to the top.
    TabReselectEffect { listState.animateScrollToItem(0) }
    AppScaffold(
        screenId = "HomeScreen",
        // A tab root: no up arrow, and the menu icon in its place when the host provides one.
        topBar = { AppTopBar(title = stringResource(R.string.home_title)) },
        contentPadding = false,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // One list for the whole screen: the greeting and the doors scroll together, so a third
            // row of cards pushes nothing off the screen for good.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_doorsList"),
                contentPadding = PaddingValues(AppTheme.spacing.inset.xl),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.md),
            ) {
                item(key = "greeting") {
                    AppText(
                        text = state.greeting.resolve(),
                        role = TextRole.Display,
                        modifier = Modifier.testTag("home_greetingValue"),
                    )
                }
                item(key = "header") { headerContent() }
                // A run of doors per section, each a header and rows of cards. The
                // Developer run exists only when the host said this build has the screens.
                val sections = ShellSection.entries.filter { section ->
                    (section != ShellSection.Developer || state.developerDoors) &&
                        state.doors.any { it.section == section }
                }
                sections.forEachIndexed { index, section ->
                    item(key = "section:${section.name}") {
                        AppSectionHeader(title = stringResource(section.title))
                    }
                    item(key = "doors:${section.name}") {
                        val perRow = if (AppTheme.density.sizeClass == SizeClass.Compact) DOORS_COMPACT else DOORS_WIDE
                        val rows = state.doors.filter { it.section == section }.chunked(perRow)
                        // The first run is the tour's first stop: the doors, whichever they are.
                        val group = if (index == 0) {
                            Modifier.testTag("home_doorsGroup").appCoachTarget("home_doorsGroup")
                        } else {
                            Modifier
                        }
                        Column(
                            modifier = group,
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
                        ) {
                            rows.forEachIndexed { row, doors ->
                                // Equal heights across a row, so a two-line figure does not leave its
                                // neighbour's bottom edge higher: the row is as tall as its
                                // tallest card at the width the card is drawn — not `FlowRow`'s
                                // `fillMaxRowHeight`, which measured each card as narrow as its
                                // longest word and left a row with spaces in it lines too tall.
                                Row(
                                    modifier = Modifier.height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
                                ) {
                                    doors.forEachIndexed { column, doorState ->
                                        HomeCard(
                                            title = doorState.title.resolve(),
                                            icon = doorState.icon,
                                            onOpen = { onEvent(HomeEvent.DoorClicked(doorState.id)) },
                                            testTag = "home_${doorState.id}Card",
                                            figure = doorState.figure?.resolve(),
                                            badge = doorState.badge,
                                            // The doors arrive as a run, one after the other.
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .appStaggeredEntrance(row * perRow + column),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // The speed dial: what the features let the user make, over the list where a FAB
            // goes — and no dial at all when none contributes a thing.
            if (state.canCreate) {
                AppFabMenu(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_create),
                    items = state.creations.map { creation ->
                        FabAction(
                            label = creation.label.resolve(),
                            icon = creation.icon,
                            testTag = creation.testTag,
                        ) { onEvent(HomeEvent.CreateChosen(creation.id)) }
                    },
                    expanded = state.createOpen,
                    onExpandedChange = { onEvent(HomeEvent.CreateToggled(it)) },
                    keyTestTag = "home_createButton",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@ScreenPreview
@Composable
private fun Preview() = ThemedScreenPreview {
    HomeScreen(
        state = HomeState.PREVIEW,
    ) {}
}

/** Doors per row: two on a phone, three where the window is wide enough to read three titles. */
private const val DOORS_COMPACT = 2
private const val DOORS_WIDE = 3
