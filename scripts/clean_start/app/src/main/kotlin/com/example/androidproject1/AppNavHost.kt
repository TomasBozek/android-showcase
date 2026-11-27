package com.example.androidproject1

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.example.androidproject1.core.ui.layout.LocalSharedElementVisibility
import com.example.androidproject1.core.ui.layout.LocalSharedTransitionScope
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.SizeClass
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.presentation.apphold.appHoldDestination
import com.example.androidproject1.feature.devmenu.presentation.devmenu.devMenuDestination
import com.example.androidproject1.feature.devmenu.presentation.networklog.networkLogDestination
import com.example.androidproject1.feature.devmenu.presentation.playground.devMenuPlaygroundDestination
import com.example.androidproject1.feature.gallery.presentation.gallery.galleryDestination
import com.example.androidproject1.feature.gallery.presentation.gallerydetail.galleryDetailDestination
import com.example.androidproject1.feature.home.api.HomeDestination
import com.example.androidproject1.service.navigation.LocalNavResultStore
import com.example.androidproject1.service.navigation.Navigator
import com.example.androidproject1.service.navigation.ProvideNavResultStore
import com.example.androidproject1.service.navigation.TabbedNavigator
import com.example.androidproject1.service.telemetry.ProvideAnalytics
import org.koin.compose.koinInject

/** The screen the app opens on, and the one root the back stack always keeps. */
internal val START_DESTINATION: NavKey = HomeDestination

/**
 * Where every feature's entries are registered. A feature reaches another by pushing that feature's
 * api route through the [Navigator].
 *
 * Navigation 3 has no nested graphs: [backStack] is a plain list of keys and every entry is
 * registered in one provider. `create_feature.py` and `create_screen.py` write each new destination
 * into [mainEntries].
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    ProvideAnalytics(koinInject()) {
        ProvideNavResultStore {
            val results = LocalNavResultStore.current
            // One root until the app has tabs: a tab bar is the list of roots this takes.
            val navigator = remember(backStack, results) {
                TabbedNavigator(backStack, listOf(START_DESTINATION), results)
            }
            AppNavDisplay(backStack = backStack, navigator = navigator, modifier = modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val entries = entryProvider<NavKey> {
        mainEntries(navigator)
        if (DebugMenu.ENABLED) debugEntries(navigator)
    }

    // Two panes where there is width for them, one where there is not: an entry declares itself a
    // list pane with `listPane(...)` metadata, and every other entry falls through to a single pane.
    val listDetail = rememberListDetailSceneStrategy<NavKey>()

    // A shared element travels between two screens on a phone; on a wider window both are one scene.
    val compact = AppTheme.density.sizeClass == SizeClass.Compact
    val activity = LocalActivity.current
    val motion = AppTheme.motion
    val slide = remember(motion) { tween<IntOffset>(durationMillis = motion.screenMillis) }
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this.takeIf { compact }) {
            NavDisplay(
                backStack = backStack,
                onBack = { if (!navigator.back()) activity?.finish() },
                sceneStrategies = listOf(listDetail),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // Gives every entry its own ViewModelStore, which is what koinViewModel() resolves
                    // against — without it two screens would share one ViewModel.
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    slideIntoContainer(SlideDirection.Start, slide) togetherWith
                        slideOutOfContainer(SlideDirection.Start, slide)
                },
                popTransitionSpec = {
                    slideIntoContainer(SlideDirection.End, slide) togetherWith
                        slideOutOfContainer(SlideDirection.End, slide)
                },
                entryProvider = { key -> entries(key).withSharedElementVisibility(key) },
            )
        }
    }
}

/**
 * Re-provides navigation3's entry scope as `:core:ui`'s nullable local, so a screen can read it
 * without knowing navigation3 — and without the throw its own local raises outside an entry.
 */
private fun NavEntry<NavKey>.withSharedElementVisibility(key: NavKey): NavEntry<NavKey> =
    NavEntry(
        key = key,
        contentKey = contentKey,
        metadata = metadata,
        content = {
            CompositionLocalProvider(LocalSharedElementVisibility provides LocalNavAnimatedContentScope.current) {
                Content()
            }
        },
    )

/** The app's screens. The generators add each new destination here. */
private fun EntryProviderScope<NavKey>.mainEntries(navigator: Navigator) {
}

/**
 * The debug menu, the component gallery and the playground: registered behind
 * `DebugMenu.ENABLED`, a `const` per flavor, so a `prod` build drops them all. The launcher's
 * long-press shortcut opens the menu on `dev` and `staging`.
 */
private fun EntryProviderScope<NavKey>.debugEntries(navigator: Navigator) {
    devMenuDestination(navigator = navigator)
    galleryDestination(navigator = navigator)
    galleryDetailDestination(navigator = navigator)
    devMenuPlaygroundDestination(navigator = navigator)
    networkLogDestination(navigator = navigator)
}

/**
 * The whole app while the startup config holds it: one entry, drawn instead of [AppNavHost]
 * by `MainActivity`, with nothing under it to go back to.
 */
@Composable
fun AppHoldHost(hold: AppHoldDestination, modifier: Modifier = Modifier) {
    val backStack = remember(hold) { NavBackStack<NavKey>(hold) }
    val navigator = remember(backStack) { TabbedNavigator(backStack, listOf(START_DESTINATION)) }
    NavDisplay(
        backStack = backStack,
        onBack = {},
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider { appHoldDestination(navigator = navigator) },
        modifier = modifier,
    )
}
