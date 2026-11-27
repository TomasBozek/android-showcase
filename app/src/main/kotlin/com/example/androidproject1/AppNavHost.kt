package com.example.androidproject1

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.navigation.AppBottomNav
import com.example.androidproject1.core.ui.component.navigation.AppDrawer
import com.example.androidproject1.core.ui.component.navigation.AppNavRail
import com.example.androidproject1.core.ui.component.navigation.DrawerItem
import com.example.androidproject1.core.ui.component.navigation.DrawerSection
import com.example.androidproject1.core.ui.component.navigation.NavItem
import com.example.androidproject1.core.ui.component.status.AppAvatar
import com.example.androidproject1.core.ui.layout.LocalMenuOpener
import com.example.androidproject1.core.ui.layout.LocalSharedElementVisibility
import com.example.androidproject1.core.ui.layout.LocalSharedTransitionScope
import com.example.androidproject1.core.ui.layout.LocalTabReselect
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.shell.ShellDoor
import com.example.androidproject1.core.ui.shell.ShellDoors
import com.example.androidproject1.core.ui.shell.ShellPlacement
import com.example.androidproject1.core.ui.shell.ShellSection
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.core.ui.theme.SizeClass
import com.example.androidproject1.debug.DebugMenu
import com.example.androidproject1.feature.apphold.api.AppHoldDestination
import com.example.androidproject1.feature.apphold.presentation.apphold.appHoldDestination
import com.example.androidproject1.feature.auth.api.AppLockDestination
import com.example.androidproject1.feature.auth.domain.AuthService
import com.example.androidproject1.feature.auth.domain.Session
import com.example.androidproject1.feature.auth.presentation.applock.appLockDestination
import com.example.androidproject1.feature.auth.presentation.forgotpassword.forgotPasswordDestination
import com.example.androidproject1.feature.auth.presentation.login.loginDestination
import com.example.androidproject1.feature.auth.presentation.resetpassword.resetPasswordDestination
import com.example.androidproject1.feature.auth.presentation.signup.signUpDestination
import com.example.androidproject1.feature.auth.presentation.verifyemail.verifyEmailDestination
import com.example.androidproject1.feature.cart.domain.CartItem
import com.example.androidproject1.feature.cart.presentation.cart.cartDestination
import com.example.androidproject1.feature.catalog.domain.CatalogRepository
import com.example.androidproject1.feature.catalog.presentation.categories.categoriesDestination
import com.example.androidproject1.feature.catalog.presentation.productdetail.productDetailDestination
import com.example.androidproject1.feature.catalog.presentation.productpicker.productPickerDestination
import com.example.androidproject1.feature.catalog.presentation.products.productsDestination
import com.example.androidproject1.feature.catalog.presentation.search.productSearchDestination
import com.example.androidproject1.feature.checkout.presentation.checkoutaddress.checkoutAddressDestination
import com.example.androidproject1.feature.checkout.presentation.checkoutdelivery.checkoutDeliveryDestination
import com.example.androidproject1.feature.checkout.presentation.checkoutpayment.checkoutPaymentDestination
import com.example.androidproject1.feature.checkout.presentation.checkoutreview.checkoutReviewDestination
import com.example.androidproject1.feature.devmenu.api.DevMenuDestination
import com.example.androidproject1.feature.devmenu.presentation.devmenu.devMenuDestination
import com.example.androidproject1.feature.devmenu.presentation.networklog.networkLogDestination
import com.example.androidproject1.feature.devmenu.presentation.playground.devMenuPlaygroundDestination
import com.example.androidproject1.feature.gallery.presentation.gallery.galleryDestination
import com.example.androidproject1.feature.gallery.presentation.gallerydetail.galleryDetailDestination
import com.example.androidproject1.feature.home.presentation.home.homeDestination
import com.example.androidproject1.feature.inventory.presentation.inventory.inventoryDestination
import com.example.androidproject1.feature.inventory.presentation.inventorydetail.inventoryDetailDestination
import com.example.androidproject1.feature.inventory.presentation.inventoryeditor.inventoryEditorDestination
import com.example.androidproject1.feature.movies.presentation.moviedetail.movieDetailDestination
import com.example.androidproject1.feature.movies.presentation.moviefavourites.movieFavouritesDestination
import com.example.androidproject1.feature.movies.presentation.movies.moviesDestination
import com.example.androidproject1.feature.onboarding.presentation.onboarding.onboardingDestination
import com.example.androidproject1.feature.onboarding.presentation.tour.TourStep
import com.example.androidproject1.feature.orders.presentation.orderdetail.orderDetailDestination
import com.example.androidproject1.feature.orders.presentation.orders.ordersDestination
import com.example.androidproject1.feature.profile.api.ProfileDestination
import com.example.androidproject1.feature.profile.presentation.avatarcrop.avatarCropDestination
import com.example.androidproject1.feature.profile.presentation.profile.profileDestination
import com.example.androidproject1.feature.settings.presentation.about.aboutDestination
import com.example.androidproject1.feature.settings.presentation.language.settingsLanguageDestination
import com.example.androidproject1.feature.settings.presentation.permissions.settingsPermissionsDestination
import com.example.androidproject1.feature.settings.presentation.settings.settingsDestination
import com.example.androidproject1.feature.settings.presentation.settingsstorage.settingsStorageDestination
import com.example.androidproject1.feature.trips.presentation.destinationpicker.destinationPickerDestination
import com.example.androidproject1.feature.trips.presentation.tripdetail.tripDetailDestination
import com.example.androidproject1.feature.trips.presentation.trips.tripsDestination
import com.example.androidproject1.feature.trips.presentation.tripslist.tripsListDestination
import com.example.androidproject1.feature.trips.presentation.tripwizard.tripWizardDestination
import com.example.androidproject1.feature.weather.presentation.forecast.forecastDestination
import com.example.androidproject1.feature.weather.presentation.weather.weatherDestination
import com.example.androidproject1.service.core.domain.result.Outcome
import com.example.androidproject1.service.navigation.LocalNavResultStore
import com.example.androidproject1.service.navigation.Navigator
import com.example.androidproject1.service.navigation.ProvideNavResultStore
import com.example.androidproject1.service.navigation.TabbedNavigator
import com.example.androidproject1.service.telemetry.ProvideAnalytics
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/**
 * Where every feature's entries are registered. A feature reaches another by pushing that feature's
 * api route through the [Navigator]; the lambdas left here are the ones whose route is
 * not in an api module yet. The drawer, Home's doors and dial, and the dev menu's jumps are what the
 * features contribute, so nothing here names a place to list.
 *
 * Navigation 3 has no nested graphs: [backStack] is a plain list of keys and every entry is
 * registered in one provider. [authEntries] and [mainEntries] — and inside the latter, one block
 * per tab — group them the way the old `navigation<AuthNavGraph>` blocks did. The grouping is what
 * `create_feature.py --graph` writes into, and what makes the file readable, but nothing enforces
 * it at runtime: any entry can appear on any stack. Which flow the user is in is decided by what
 * MainActivity puts on the back stack, and which tab by [currentTab].
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    /**
     * What the drawer's *Show the tour again* row does. Passed in rather than resolved
     * here: the tour's view model is the activity's, and a host that reached into Koin for it
     * would be depending on something its signature does not say.
     */
    onReplayTour: () -> Unit = {},
    /** What Home shows above its doors: a server message's banner, from `MainActivity`. */
    homeHeader: @Composable () -> Unit = {},
) {
    // Around the display, not inside an entry: a nav result has to outlive the screen that
    // produced it being popped, and the analytics has to be in scope for every screen's
    // `AppScaffold` to report its view through. See core.ui.navigation.NavResultStore and
    // service.telemetry.ScreenViewEffect.
    ProvideAnalytics(koinInject()) {
        ProvideNavResultStore {
            // Built once, beside the back stack, over the same list: the host's own doors,
            // the bar and back go through it, and it holds nothing a recomposition could lose.
            val results = LocalNavResultStore.current
            val navigator = remember(backStack, results) {
                TabbedNavigator(backStack, TopLevelDestination.roots, results)
            }
            val currentTab = navigator.currentTab?.let(TopLevelDestination::of)
            CompositionLocalProvider(LocalHomeHeader provides homeHeader) {
                AppNavContent(
                    backStack = backStack,
                    navigator = navigator,
                    currentTab = currentTab,
                    onReplayTour = onReplayTour,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun AppNavContent(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    currentTab: TopLevelDestination?,
    onReplayTour: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The auth flow has no tabs, so it is not wrapped: an empty bar would still reserve its
    // height.
    if (currentTab == null) {
        AppNavDisplay(backStack = backStack, navigator = navigator, modifier = modifier)
        return
    }

    // Every door the features contribute: the drawer lists the ones placed in the menu, and a
    // tab wears the badge of the door with its route. Collected here rather than in a screen, so a
    // count updates while the user is on another tab, which is the whole point of it.
    val doors = koinInject<ShellDoors>().all
    val badges = rememberBadges(doors)
    val tabBadge = { tab: TopLevelDestination -> doors.firstOrNull { it.route == tab.key }?.let { badges[it.id] } ?: 0 }

    // The design system's own items: the label from :app's strings, the count from the
    // tab's door, the id carried on the entry so a flow finds the tab without its label.
    val items = TopLevelDestination.entries.map { tab ->
        NavItem(
            label = stringResource(tab.label),
            icon = tab.icon,
            badge = tabBadge(tab),
            testTag = tab.testTag,
        )
    }
    // A second tap on the tab on screen: the stack pops to the root and the root is told, so it
    // scrolls to the top. A buffer of one, so a tap before the root collects is not lost.
    val reselect = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val onSelect = { index: Int ->
        if (navigator.selectTab(TopLevelDestination.entries[index].key)) reselect.tryEmit(Unit)
    }

    // The main menu: every door, around the bar and the display, so it opens over either
    // shape of the shell and from any screen — the icon on a tab root, the edge swipe anywhere.
    // Saved, because the menu being open is exactly the state a rotation should not lose.
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val menu =
        mainMenu(
            navigator = navigator,
            doors = doors,
            badges = badges,
            tabBadge = tabBadge,
            onReplayTour = onReplayTour,
        )
    val authService: AuthService = koinInject()
    val session by remember(authService) {
        authService.observeSession().map { (it as? Outcome.Success)?.data }
    }.collectAsStateWithLifecycle(initialValue = null)

    AppDrawer(
        open = menuOpen,
        onOpenChange = { menuOpen = it },
        sections = menu.map { section -> DrawerSection(section.title, section.doors.map { it.item }) },
        selectedId = currentTab.name,
        onSelect = { id -> menu.flatMap { it.doors }.first { it.item.id == id }.open() },
        // The shell sits above every screen's own AppScaffold, which is where this is switched on
        // for the content — without it here a flow cannot find a tab by id at all.
        modifier = modifier.semantics { testTagsAsResourceId = true },
        header = { MenuHeader(session = session) },
    ) {
        CompositionLocalProvider(
            LocalMenuOpener provides { menuOpen = true },
            LocalTabReselect provides reselect,
        ) {
            AppShell(
                backStack = backStack,
                navigator = navigator,
                items = items,
                selectedIndex = currentTab.ordinal,
                onSelect = onSelect,
            )
        }
    }
}

/**
 * A bar on a phone, a rail once there is width for one — the theme's breakpoint, the split
 * InventoryDetailScreen already makes. The inset the shell's own chrome pads is consumed
 * before the display, or every tab root's `safeDrawingPadding()` pads it a second time and opens
 * a gap between the screen and the bar.
 */
@Composable
private fun AppShell(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    if (AppTheme.density.sizeClass == SizeClass.Compact) {
        Column {
            AppNavDisplay(
                backStack = backStack,
                navigator = navigator,
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(WindowInsets.navigationBars),
            )
            AppBottomNav(
                items = items,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                // The tour's third stop: the bar itself, whatever tabs are in it.
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottomNavGroup")
                    .appCoachTarget("main_bottomNavGroup"),
            )
        }
    } else {
        Row {
            // The rail is on the start edge, so it pads that side and the two system bars itself.
            val railSides = WindowInsetsSides.Vertical + WindowInsetsSides.Start
            AppNavRail(
                items = items,
                selectedIndex = selectedIndex,
                onSelect = onSelect,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars.only(railSides))
                    .testTag("main_navRailGroup"),
            )
            AppNavDisplay(
                backStack = backStack,
                navigator = navigator,
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start)),
            )
        }
    }
}

/**
 * Every door's badge by its id, collected once. A badge that fails wears none, the way a figure that
 * fails draws nothing new on Home.
 */
@Composable
private fun rememberBadges(doors: List<ShellDoor>): Map<String, Int> {
    // remembered: building the flow inside composition would make a new one on every
    // recomposition, resubscribing the badges each time the bar redraws.
    val flow = remember(doors) {
        combine(doors.map { door -> door.badge.catch { emit(0) }.map { door.id to it } }) { it.toMap() }
    }
    val badges by flow.collectAsStateWithLifecycle(initialValue = emptyMap())
    return badges
}

/** Who is signed in, above the menu: the avatar the email seeds, and the email. */
@Composable
private fun MenuHeader(session: Session?) {
    if (session == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.inset.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md),
    ) {
        AppAvatar(name = session.email)
        AppText(
            text = session.email,
            role = TextRole.Title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Home's header from the host, carried to the entry provider without threading it through
 * the three layouts in between, none of which does anything with it.
 */
private val LocalHomeHeader = staticCompositionLocalOf<@Composable () -> Unit> { {} }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    // Resolved here rather than inside the entry: `mainEntries` is a plain function, and the
    // cart needs the catalog to turn a picked id into a line it can hold.
    val catalogRepository: CatalogRepository = koinInject()
    // Play services draws the licences list in an activity of its own, and only `:app` has it.
    val context = LocalContext.current
    val openLicences =
        remember(context) { { context.startActivity(Intent(context, OssLicensesMenuActivity::class.java)) } }

    val homeHeader = LocalHomeHeader.current

    val entries = entryProvider<NavKey> {
        onboardingEntries(navigator)
        authEntries(navigator)
        mainEntries(navigator, catalogRepository, openLicences, homeHeader)
    }

    // Two panes where there is width for them, one where there is not. The decision is the
    // strategy's, from the window size — nothing here or in a screen asks how wide anything is,
    // and no screen behaves differently: which entries can share a scene is metadata on the two
    // catalog destinations, and every other entry falls through to the single-pane default.
    val listDetail = rememberListDetailSceneStrategy<NavKey>()

    // The product row's name and price travel to the detail: the scope is provided
    // once here, and each entry re-provides its own visibility below, so a screen only ever
    // asks `Modifier.appSharedElement(key)`. On a wider window the catalog's two panes are one
    // scene with both elements visible at once, and nothing travels — so the scope is compact-only.
    val compact = AppTheme.density.sizeClass == SizeClass.Compact
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this.takeIf { compact }) {
            AppNavDisplayContent(
                backStack = backStack,
                navigator = navigator,
                entries = entries,
                listDetail = listDetail,
            )
        }
    }
}

@Composable
private fun AppNavDisplayContent(
    backStack: NavBackStack<NavKey>,
    navigator: Navigator,
    entries: (NavKey) -> NavEntry<NavKey>,
    listDetail: SceneStrategy<NavKey>,
) {
    // Back goes Home, then out: a pushed screen pops, a tab root returns to Home, and Home's
    // own root — with more than one key on the stack, so the display's handler is still enabled —
    // finishes the activity. With one key the handler is off and the platform leaves by itself.
    val activity = LocalActivity.current
    // The push and the pop slide on `screenMillis` — the token a shared element's bounds travel
    // on too, so the two end together — and a tab root cross-fades on `fadeMillis`. Both
    // are built here, because `entryProvider` is not composable and cannot read the theme.
    val motion = AppTheme.motion
    val slide = remember(motion) { tween<IntOffset>(durationMillis = motion.screenMillis) }
    val tabRootTransitions = remember(motion) {
        val fade = tween<Float>(durationMillis = motion.fadeMillis)
        NavDisplay.transitionSpec { fadeIn(fade) togetherWith fadeOut(fade) } +
            NavDisplay.popTransitionSpec { fadeIn(fade) togetherWith fadeOut(fade) }
    }
    NavDisplay(
        backStack = backStack,
        onBack = { if (!navigator.back()) activity?.finish() },
        sceneStrategies = listOf(listDetail),
        // NavDisplay adds its own scene-setup decorator; these two are the ones a screen needs.
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            // Gives every entry its own ViewModelStore, which is what koinViewModel() resolves
            // against — without it two screens would share one ViewModel.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        // Going deeper slides; the library's default is a cross-fade, which reads as unfinished.
        transitionSpec = {
            slideIntoContainer(SlideDirection.Start, slide) togetherWith
                slideOutOfContainer(SlideDirection.Start, slide)
        },
        popTransitionSpec = {
            slideIntoContainer(SlideDirection.End, slide) togetherWith
                slideOutOfContainer(SlideDirection.End, slide)
        },
        // predictivePopTransitionSpec is left at the library's default: it is the platform's own
        // back gesture, and a screen that scales away under the user's finger is what that looks
        // like everywhere else on the device.
        entryProvider = { key ->
            entries(key).withTabRootTransitions(key, tabRootTransitions).withSharedElementVisibility(key)
        },
    )
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

/**
 * Makes a tab root fade instead of slide.
 *
 * Attached to the entry rather than decided in the host's spec because `NavDisplay` resolves entry
 * metadata against the screen *arriving* on a push and the one *leaving* on a pop — which is exactly
 * the rule wanted here and not one the host's spec can express. Arriving at a tab root is a tab
 * switch, so it fades; arriving at a tab root by popping the screen above it off is not, and the
 * leaving screen has no metadata, so it slides back.
 */
private fun NavEntry<NavKey>.withTabRootTransitions(key: NavKey, transitions: Map<String, Any>): NavEntry<NavKey> =
    if (TopLevelDestination.of(key) == null) {
        this
    } else {
        NavEntry(
            key = key,
            contentKey = contentKey,
            metadata = metadata + transitions,
            content = { Content() },
        )
    }

/**
 * Shown until the first-run tour has been finished, whether or not a session is stored.
 *
 * A third flow beside [authEntries] and [mainEntries], and like them it is a grouping rather
 * than a graph: which flow the user is in is decided by what `MainActivity` puts on the stack.
 */
private fun EntryProviderScope<NavKey>.onboardingEntries(navigator: Navigator) {
    onboardingDestination(navigator = navigator)
}

/** Shown while signed out. */
private fun EntryProviderScope<NavKey>.authEntries(navigator: Navigator) {
    loginDestination(navigator = navigator)
    signUpDestination(navigator = navigator)
    forgotPasswordDestination(navigator = navigator)
    resetPasswordDestination(navigator = navigator)
    verifyEmailDestination(navigator = navigator)
}

/** Shown while signed in, grouped by the tab whose stack the screen is pushed onto. */
private fun EntryProviderScope<NavKey>.mainEntries(
    navigator: Navigator,
    catalogRepository: CatalogRepository,
    openLicences: () -> Unit,
    homeHeader: @Composable () -> Unit,
) {
    homeEntries(navigator, homeHeader)
    catalogEntries(navigator)
    settingsEntries(navigator, openLicences)
    cartDestination(
        navigator = navigator,
        onProductPicked = { productId ->
            // The lookup is the catalog's business, so it happens here rather than in the cart.
            (catalogRepository.getProduct(productId) as? Outcome.Success)?.data?.let { product ->
                CartItem(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = 1,
                )
            }
        },
    )
    productPickerDestination(navigator = navigator)
    tripsEntries(navigator)
    inventoryEntries(navigator)
    movieDetailDestination(navigator = navigator)
    movieFavouritesDestination(navigator = navigator)
    forecastDestination(navigator = navigator)
    ordersDestination(navigator = navigator)
    checkoutAddressDestination(navigator = navigator)
    checkoutDeliveryDestination(navigator = navigator)
    checkoutPaymentDestination(navigator = navigator)
    checkoutReviewDestination(navigator = navigator)
    orderDetailDestination(navigator = navigator)
    settingsStorageDestination(navigator = navigator)
    avatarCropDestination(navigator = navigator)
}

private fun EntryProviderScope<NavKey>.homeEntries(
    navigator: Navigator,
    homeHeader: @Composable () -> Unit,
) {
    homeDestination(
        navigator = navigator,
        // Home is a door to every place, and this is the one place that knows what each one
        // is: a tab door selects its tab, the rest push. The developer doors exist only where the
        // debug entries do.
        onOpen = { route -> navigator.open(route) },
        developerDoors = DebugMenu.ENABLED,
        headerContent = homeHeader,
    )
    moviesDestination(navigator = navigator)
    weatherDestination(navigator = navigator)
}

private fun EntryProviderScope<NavKey>.catalogEntries(navigator: Navigator) {
    categoriesDestination(navigator = navigator)
    productsDestination(navigator = navigator)
    productDetailDestination(navigator = navigator)
    productSearchDestination(navigator = navigator)
}

/** Not a tab: Inventory's door is the card on Home, so its entries sit beside the tabs'. */
private fun EntryProviderScope<NavKey>.inventoryEntries(navigator: Navigator) {
    inventoryDestination(navigator = navigator)
    inventoryEditorDestination(navigator = navigator)
    inventoryDetailDestination(navigator = navigator)
}

private fun EntryProviderScope<NavKey>.tripsEntries(navigator: Navigator) {
    tripsDestination(navigator = navigator)
    tripsListDestination(navigator = navigator)
    tripWizardDestination(navigator = navigator)
    tripDetailDestination(navigator = navigator)
    destinationPickerDestination(navigator = navigator)
}

private fun EntryProviderScope<NavKey>.settingsEntries(
    navigator: Navigator,
    openLicences: () -> Unit,
) {
    settingsDestination(
        navigator = navigator,
        navigateToProfile = { navigator.push(ProfileDestination) },
        // Null in `prod`, where DebugMenu.ENABLED is a `const false`: the branch folds away at
        // compile time, so nothing outside it names a debug-menu class and Settings draws no
        // entry. Passing the lambda *is* how the screen knows this build has one.
        navigateToDebugMenu = if (DebugMenu.ENABLED) {
            { navigator.push(DevMenuDestination) }
        } else {
            null
        },
    )
    settingsPermissionsDestination(navigator = navigator)
    profileDestination(navigator = navigator)
    if (DebugMenu.ENABLED) debugEntries(navigator)
    settingsLanguageDestination(navigator = navigator)
    aboutDestination(navigator = navigator, openLicences = openLicences)
}

/**
 * The screens a tester gets and a customer does not.
 *
 * Registered behind `DebugMenu.ENABLED`, which is `const` per flavor source set — so in a `prod`
 * build this call is dead code, R8 removes it, and every class it reaches goes with it: the debug
 * menu and the whole component gallery.
 */
private fun EntryProviderScope<NavKey>.debugEntries(navigator: Navigator) {
    devMenuDestination(navigator = navigator)
    galleryDestination(navigator = navigator)
    galleryDetailDestination(navigator = navigator)
    // The component playground: dev and staging only, like the gallery.
    devMenuPlaygroundDestination(navigator = navigator)
    // The client's exchanges since start, fed only where there is a menu to read them.
    networkLogDestination(navigator = navigator)
}

/**
 * The tour's four stops, each on the frame — none names a feature a copy may delete.
 *
 * Each names an element's own test tag — the string the element registers with
 * `Modifier.appCoachTarget` and a flow finds it by — so a step points at something that exists
 * rather than at a second copy of a name. They run in the order someone meets the app: the doors on
 * Home, the menu everything else is behind, the bar, then the tab worth knowing about. A step whose
 * target is not on screen — the bar where a rail is drawn — is skipped by the coach mark itself.
 */
fun tourSteps(strings: TourStrings): List<TourStep> = listOf(
    TourStep("home_doorsGroup", strings.doorsTitle, strings.doorsBody),
    TourStep("menu_openButton", strings.menuTitle, strings.menuBody),
    TourStep("main_bottomNavGroup", strings.tabsTitle, strings.tabsBody),
    TourStep("tabs_settingsTab", strings.settingsTitle, strings.settingsBody),
)

/**
 * The tour's copy, resolved where a composable can reach `stringResource` and handed down.
 *
 * A data class rather than eight parameters: [tourSteps] is not a composable — it is a list the
 * activity hands to a view model — and the strings have to be read in composition.
 */
data class TourStrings(
    val doorsTitle: String,
    val doorsBody: String,
    val menuTitle: String,
    val menuBody: String,
    val tabsTitle: String,
    val tabsBody: String,
    val settingsTitle: String,
    val settingsBody: String,
)

/** Reads the tour's copy from resources. */
@Composable
fun rememberTourStrings(): TourStrings = TourStrings(
    doorsTitle = stringResource(R.string.tour_doors_title),
    doorsBody = stringResource(R.string.tour_doors_body),
    menuTitle = stringResource(R.string.tour_menu_title),
    menuBody = stringResource(R.string.tour_menu_body),
    tabsTitle = stringResource(R.string.tour_tabs_title),
    tabsBody = stringResource(R.string.tour_tabs_body),
    settingsTitle = stringResource(R.string.tour_settings_title),
    settingsBody = stringResource(R.string.tour_settings_body),
)

/**
 * Where a door on Home leads: a contributed door's route selects its tab when it is a
 * tab root, and is pushed otherwise. A developer door is only drawn where the debug entries exist.
 */
private fun Navigator.open(route: NavKey) {
    if (TopLevelDestination.of(route) != null) selectTab(route) else push(route)
}

/** One door of the main menu: the row the drawer draws, and where it goes. */
private class Door(val item: DrawerItem, val open: () -> Unit)

/** A titled run of doors — a `DrawerSection` with its destinations still attached. */
private class MenuSection(val title: String, val doors: List<Door>)

/**
 * Every place in the app. Places are the five tabs, selected the way the bar selects them;
 * Yours are the doors the features place in the menu, and the tour row; Developer is the
 * doors in that section, which exist only where the debug menu's modules are bound. No Log out row —
 * Settings holds it. The tab rows reuse the `tab_*` labels; a door's row is its own title and
 * `menu_<id>Item`.
 */
@Composable
private fun mainMenu(
    navigator: Navigator,
    doors: List<ShellDoor>,
    badges: Map<String, Int>,
    tabBadge: (TopLevelDestination) -> Int,
    onReplayTour: () -> Unit,
): List<MenuSection> {
    val places = MenuSection(
        title = stringResource(R.string.menu_section_places),
        doors = TopLevelDestination.entries.map { tab ->
            Door(
                item = DrawerItem(
                    id = tab.name,
                    label = stringResource(tab.label),
                    icon = tab.icon,
                    badge = tabBadge(tab),
                    testTag = "menu_${tab.name.replaceFirstChar(Char::lowercase)}Item",
                ),
                open = { navigator.selectTab(tab.key) },
            )
        },
    )
    val (developer, yours) = doors.inMenu().partition { it.section == ShellSection.Developer }
    val tour = Door(
        // The only row that opens nothing: it puts the tour's flag back, and the tour starts on the
        // next Home — which is where its first step points.
        item = DrawerItem(
            id = "tour",
            label = stringResource(R.string.menu_tour),
            icon = Icons.Filled.Info,
            testTag = "menu_tourItem",
        ),
        open = onReplayTour,
    )
    return listOfNotNull(
        places,
        MenuSection(stringResource(R.string.menu_section_yours), yours.map { menuDoor(navigator, it, badges) } + tour),
        developer.takeIf { it.isNotEmpty() }?.let { run ->
            MenuSection(stringResource(R.string.menu_section_developer), run.map { menuDoor(navigator, it, badges) })
        },
    )
}

/** The doors placed in the menu, by section and then their own order. */
private fun List<ShellDoor>.inMenu(): List<ShellDoor> =
    filter { ShellPlacement.Menu in it.placements }.sortedWith(compareBy({ it.section.ordinal }, { it.order }))

/** A contributed door's row: its own title, icon and badge; opened the way Home opens it. */
@Composable
private fun menuDoor(navigator: Navigator, door: ShellDoor, badges: Map<String, Int>) = Door(
    item = DrawerItem(
        id = door.id,
        label = stringResource(door.title),
        icon = door.icon,
        badge = badges[door.id] ?: 0,
        testTag = "menu_${door.id}Item",
    ),
    open = { navigator.open(door.route) },
)

/**
 * The app lock's screen, drawn by `MainActivity` over the main flow while the lock holds it.
 * One entry with a back stack of its own, so the main flow's stays exactly as it was underneath;
 * it takes every touch and the back gesture, so nothing behind it moves — back leaves the app, the
 * way back on a root screen does.
 */
@Composable
fun AppLockHost(modifier: Modifier = Modifier) {
    val backStack = remember { NavBackStack<NavKey>(AppLockDestination) }
    val navigator = remember(backStack) { TabbedNavigator(backStack, TopLevelDestination.roots) }
    val activity = LocalActivity.current
    BackHandler { activity?.moveTaskToBack(true) }
    NavDisplay(
        backStack = backStack,
        onBack = {},
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider { appLockDestination(navigator = navigator) },
        // An empty pointer handler is enough: the topmost node that handles a touch keeps it.
        modifier = modifier.pointerInput(Unit) {},
    )
}

/**
 * The whole app while the startup config holds it: one entry, drawn instead of [AppNavHost]
 * by `MainActivity`, with nothing under it to go back to. Registered here, beside every other
 * entry, because this is the file that knows them all.
 */
@Composable
fun AppHoldHost(hold: AppHoldDestination, modifier: Modifier = Modifier) {
    val backStack = remember(hold) { NavBackStack<NavKey>(hold) }
    val navigator = remember(backStack) { TabbedNavigator(backStack, TopLevelDestination.roots) }
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
