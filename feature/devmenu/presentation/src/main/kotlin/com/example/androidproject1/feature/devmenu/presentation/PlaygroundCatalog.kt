package com.example.androidproject1.feature.devmenu.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.androidproject1.core.ui.component.ControlSize
import com.example.androidproject1.core.ui.component.action.AppButton
import com.example.androidproject1.core.ui.component.action.AppFab
import com.example.androidproject1.core.ui.component.action.AppFabMenu
import com.example.androidproject1.core.ui.component.action.AppIconButton
import com.example.androidproject1.core.ui.component.action.ButtonKind
import com.example.androidproject1.core.ui.component.action.FabAction
import com.example.androidproject1.core.ui.component.action.IconButtonKind
import com.example.androidproject1.core.ui.component.choice.AppCheckbox
import com.example.androidproject1.core.ui.component.choice.AppChip
import com.example.androidproject1.core.ui.component.choice.AppChipGroup
import com.example.androidproject1.core.ui.component.choice.AppRadio
import com.example.androidproject1.core.ui.component.choice.AppRadioGroup
import com.example.androidproject1.core.ui.component.choice.AppRangeSlider
import com.example.androidproject1.core.ui.component.choice.AppSegmented
import com.example.androidproject1.core.ui.component.choice.AppSlider
import com.example.androidproject1.core.ui.component.choice.AppStepper
import com.example.androidproject1.core.ui.component.choice.AppSwitch
import com.example.androidproject1.core.ui.component.choice.CheckState
import com.example.androidproject1.core.ui.component.content.AppCounter
import com.example.androidproject1.core.ui.component.content.AppDivider
import com.example.androidproject1.core.ui.component.content.AppEmptyState
import com.example.androidproject1.core.ui.component.content.AppHtmlText
import com.example.androidproject1.core.ui.component.content.AppIcon
import com.example.androidproject1.core.ui.component.content.AppImage
import com.example.androidproject1.core.ui.component.content.AppImageCropper
import com.example.androidproject1.core.ui.component.content.AppLabelledDivider
import com.example.androidproject1.core.ui.component.content.AppReveal
import com.example.androidproject1.core.ui.component.content.AppSparkline
import com.example.androidproject1.core.ui.component.content.AppSwitcher
import com.example.androidproject1.core.ui.component.content.AppText
import com.example.androidproject1.core.ui.component.content.TextRole
import com.example.androidproject1.core.ui.component.field.AppCardField
import com.example.androidproject1.core.ui.component.field.AppCardFieldState
import com.example.androidproject1.core.ui.component.field.AppCodeField
import com.example.androidproject1.core.ui.component.field.AppDateField
import com.example.androidproject1.core.ui.component.field.AppFieldGroup
import com.example.androidproject1.core.ui.component.field.AppFieldGroupRow
import com.example.androidproject1.core.ui.component.field.AppFormField
import com.example.androidproject1.core.ui.component.field.AppPhoneField
import com.example.androidproject1.core.ui.component.field.AppPhoneFieldState
import com.example.androidproject1.core.ui.component.field.AppSearchField
import com.example.androidproject1.core.ui.component.field.AppSelect
import com.example.androidproject1.core.ui.component.field.AppTextField
import com.example.androidproject1.core.ui.component.field.AppTimeField
import com.example.androidproject1.core.ui.component.list.AppAccordion
import com.example.androidproject1.core.ui.component.list.AppCard
import com.example.androidproject1.core.ui.component.list.AppDescriptionList
import com.example.androidproject1.core.ui.component.list.AppListItem
import com.example.androidproject1.core.ui.component.list.AppPullToRefresh
import com.example.androidproject1.core.ui.component.list.AppReorder
import com.example.androidproject1.core.ui.component.list.AppScrollShadow
import com.example.androidproject1.core.ui.component.list.AppSectionHeader
import com.example.androidproject1.core.ui.component.list.DescriptionRow
import com.example.androidproject1.core.ui.component.list.ScrollEdge
import com.example.androidproject1.core.ui.component.list.SurfaceLevel
import com.example.androidproject1.core.ui.component.list.SwipeAction
import com.example.androidproject1.core.ui.component.list.SwipeActions
import com.example.androidproject1.core.ui.component.navigation.AppBottomActionBar
import com.example.androidproject1.core.ui.component.navigation.AppBottomNav
import com.example.androidproject1.core.ui.component.navigation.AppDrawer
import com.example.androidproject1.core.ui.component.navigation.AppNavRail
import com.example.androidproject1.core.ui.component.navigation.AppPager
import com.example.androidproject1.core.ui.component.navigation.AppTabs
import com.example.androidproject1.core.ui.component.navigation.AppToolbar
import com.example.androidproject1.core.ui.component.navigation.AppTopBar
import com.example.androidproject1.core.ui.component.navigation.DrawerItem
import com.example.androidproject1.core.ui.component.navigation.DrawerSection
import com.example.androidproject1.core.ui.component.navigation.NavItem
import com.example.androidproject1.core.ui.component.navigation.TabItem
import com.example.androidproject1.core.ui.component.overlay.AppActionSheet
import com.example.androidproject1.core.ui.component.overlay.AppChoiceDialog
import com.example.androidproject1.core.ui.component.overlay.AppCoachMark
import com.example.androidproject1.core.ui.component.overlay.AppConfirmDialog
import com.example.androidproject1.core.ui.component.overlay.AppImageViewer
import com.example.androidproject1.core.ui.component.overlay.AppMenu
import com.example.androidproject1.core.ui.component.overlay.AppPermissionRationale
import com.example.androidproject1.core.ui.component.overlay.AppPickerSheet
import com.example.androidproject1.core.ui.component.overlay.AppPromptDialog
import com.example.androidproject1.core.ui.component.overlay.AppSheet
import com.example.androidproject1.core.ui.component.overlay.AppToast
import com.example.androidproject1.core.ui.component.overlay.AppTooltip
import com.example.androidproject1.core.ui.component.overlay.ChoiceOption
import com.example.androidproject1.core.ui.component.overlay.CoachStep
import com.example.androidproject1.core.ui.component.overlay.MenuItem
import com.example.androidproject1.core.ui.component.overlay.ToastTone
import com.example.androidproject1.core.ui.component.status.AppAvatar
import com.example.androidproject1.core.ui.component.status.AppAvatarPhoto
import com.example.androidproject1.core.ui.component.status.AppBadge
import com.example.androidproject1.core.ui.component.status.AppBanner
import com.example.androidproject1.core.ui.component.status.AppProgress
import com.example.androidproject1.core.ui.component.status.AppSkeleton
import com.example.androidproject1.core.ui.component.status.AppSpinner
import com.example.androidproject1.core.ui.component.status.AppStatusDot
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.component.status.BannerTone
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.layout.LocalCoachTargets
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.layout.appReorderItem
import com.example.androidproject1.core.ui.layout.rememberCoachTargets
import com.example.androidproject1.core.ui.layout.rememberReorderState
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.service.core.ui.form.required
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * One value a knob holds. Six shapes, one per [Knob] shape; a knob's control writes its own and
 * an entry's `render` reads it back through [KnobValues].
 */
@Immutable
sealed interface KnobValue {

    data class Bool(val value: Boolean) : KnobValue

    /** An index into the knob's options. */
    data class Choice(val index: Int) : KnobValue

    data class Text(val value: String) : KnobValue

    data class Number(val value: Int) : KnobValue

    /** How many times an [Knob.Action] has been fired — a count, so every tap is a new value. */
    data class Ticks(val count: Int) : KnobValue

    /** A share in `0f..1f`. */
    data class Fraction(val value: Float) : KnobValue
}

/**
 * One property of a component the playground lets a tester drive.
 *
 * The shape is the control: a [Toggle] is a switch, a [Choice] a segmented control (a select past
 * four options), a [Text] a text field, a [Number] a stepper, an [Action] a button that counts its
 * taps, a [Fraction] a slider. `PlaygroundKnob` holds the one `when` over these; nothing else in
 * the module inspects a knob's shape, and no entry does.
 *
 * @property key what an entry's `render` asks [KnobValues] for. Unique within one entry, and the
 *   stem of the control's test id — `devMenuPlayground_<key>Switch`.
 * @property label what the control shows. Dev tooling, so a literal rather than a resource, like
 *   the gallery's names.
 */
@Immutable
sealed interface Knob {

    val key: String

    val label: String

    val default: KnobValue

    /**
     * A one-shot: a button whose every tap is one more tick. An entry that wraps its component in
     * `key(v.ticks("replay")) { … }` replays an entrance, a pop or a sweep from the start — the way
     * a bench fires a thing rather than holding it.
     */
    data class Action(
        override val key: String,
        override val label: String,
    ) : Knob {

        override val default: KnobValue get() = KnobValue.Ticks(0)
    }

    /** A share in `0f..1f`, as a slider — a progress bar's fill, a sparkline's highlighted point. */
    data class Fraction(
        override val key: String,
        override val label: String,
        val initial: Float = 0f,
    ) : Knob {

        override val default: KnobValue get() = KnobValue.Fraction(initial)
    }

    data class Toggle(
        override val key: String,
        override val label: String,
        val initial: Boolean,
    ) : Knob {

        override val default: KnobValue get() = KnobValue.Bool(initial)
    }

    data class Choice(
        override val key: String,
        override val label: String,
        val options: List<String>,
        val initial: Int = 0,
    ) : Knob {

        override val default: KnobValue get() = KnobValue.Choice(initial)
    }

    data class Text(
        override val key: String,
        override val label: String,
        val initial: String,
    ) : Knob {

        override val default: KnobValue get() = KnobValue.Text(initial)
    }

    data class Number(
        override val key: String,
        override val label: String,
        val range: IntRange,
        val initial: Int,
    ) : Knob {

        override val default: KnobValue get() = KnobValue.Number(initial)
    }
}

/**
 * What an entry's `render` reads: the current value of each knob, falling back to the knob's
 * default when the state has none yet — so a freshly picked entry renders without a first edit,
 * and a value of the wrong shape (a stale key from another entry) is the default rather than a
 * cast.
 */
@Immutable
class KnobValues(
    private val entry: PlaygroundEntry,
    private val values: Map<String, KnobValue>,
) {

    fun bool(key: String): Boolean = (value(key) as? KnobValue.Bool)?.value ?: false

    fun choice(key: String): Int = (value(key) as? KnobValue.Choice)?.index ?: 0

    fun text(key: String): String = (value(key) as? KnobValue.Text)?.value ?: ""

    fun int(key: String): Int = (value(key) as? KnobValue.Number)?.value ?: 0

    fun ticks(key: String): Int = (value(key) as? KnobValue.Ticks)?.count ?: 0

    fun fraction(key: String): Float = (value(key) as? KnobValue.Fraction)?.value ?: 0f

    private fun value(key: String): KnobValue? = values[key] ?: entry.knobs.firstOrNull { it.key == key }?.default
}

/**
 * One component on the bench: its knobs, and how to draw it from their values.
 *
 * The demos live here rather than in `:core:ui`, the way the gallery's do: a feature composes
 * components, and `:core:ui` ships no sample code. The gallery itself is another feature's
 * `presentation`, which this one may not import — so this is a second, smaller catalog, of the
 * controls with the most states, and adding one is one `entry(...)` here and nothing anywhere else.
 */
@Immutable
data class PlaygroundEntry(
    val id: String,
    val name: String,
    val knobs: List<Knob>,
    val render: @Composable (KnobValues) -> Unit,
)

/** Every knob at its default: what the state holds when an entry is picked. */
fun PlaygroundEntry.defaults(): Map<String, KnobValue> = knobs.associate { it.key to it.default }

private fun entry(
    id: String,
    name: String,
    vararg knobs: Knob,
    render: @Composable (KnobValues) -> Unit,
) = PlaygroundEntry(id, name, knobs.toList(), render)

private fun <E : Enum<E>> names(entries: List<E>): List<String> = entries.map { it.name }

/**
 * The bench. An entry's own callbacks are empty on purpose: the stage is a display, and its
 * state is the knobs' — a switch on the stage is turned by the `checked` knob under it, which is
 * what a bench is for.
 */
val playgroundCatalog: List<PlaygroundEntry> = listOf(
    entry(
        "button",
        "AppButton",
        Knob.Text("label", "Label", "Pay"),
        Knob.Choice("kind", "Kind", names(ButtonKind.entries)),
        Knob.Choice("size", "Size", names(ControlSize.entries), initial = ControlSize.Medium.ordinal),
        Knob.Toggle("enabled", "Enabled", true),
        Knob.Toggle("loading", "Loading", false),
    ) { v ->
        AppButton(
            label = v.text("label"),
            onClick = {},
            kind = ButtonKind.entries[v.choice("kind")],
            size = ControlSize.entries[v.choice("size")],
            enabled = v.bool("enabled"),
            loading = v.bool("loading"),
        )
    },
    entry(
        "switch",
        "AppSwitch",
        Knob.Text("label", "Label", "Print receipt automatically"),
        Knob.Text("supporting", "Supporting", "Every sale"),
        Knob.Toggle("checked", "Checked", true),
        Knob.Toggle("enabled", "Enabled", true),
        Knob.Choice("size", "Size", names(ControlSize.entries), initial = ControlSize.Medium.ordinal),
    ) { v ->
        AppSwitch(
            checked = v.bool("checked"),
            onCheckedChange = {},
            label = v.text("label"),
            supporting = v.text("supporting").ifEmpty { null },
            enabled = v.bool("enabled"),
            size = ControlSize.entries[v.choice("size")],
        )
    },
    entry(
        "textfield",
        "AppTextField",
        Knob.Text("value", "Value", "ada@example.com"),
        Knob.Text("label", "Label", "Email"),
        Knob.Text("helper", "Helper", "Where the receipt goes"),
        Knob.Text("error", "Error", ""),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppTextField(
            value = v.text("value"),
            onValueChange = {},
            label = v.text("label").ifEmpty { null },
            helperText = v.text("helper").ifEmpty { null },
            errorText = v.text("error").ifEmpty { null },
            enabled = v.bool("enabled"),
        )
    },
    entry(
        "tag",
        "AppTag",
        Knob.Text("label", "Label", "Active"),
        Knob.Choice("tone", "Tone", names(TagTone.entries), initial = TagTone.Positive.ordinal),
    ) { v ->
        AppTag(label = v.text("label"), tone = TagTone.entries[v.choice("tone")])
    },
    entry(
        "stepper",
        "AppStepper",
        Knob.Number("value", "Value", 0..STEPPER_CEILING, initial = 2),
        Knob.Number("min", "Min", 0..STEPPER_CEILING, initial = 0),
        Knob.Number("max", "Max", 0..STEPPER_CEILING, initial = STEPPER_CEILING),
    ) { v ->
        AppStepper(value = v.int("value"), onValueChange = {}, min = v.int("min"), max = v.int("max"))
    },
    entry(
        "segmented",
        "AppSegmented",
        Knob.Choice("selected", "Selected", listOf("Day", "Week", "Month")),
        Knob.Choice("size", "Size", names(ControlSize.entries), initial = ControlSize.Medium.ordinal),
    ) { v ->
        AppSegmented(
            options = listOf("Day", "Week", "Month"),
            selectedIndex = v.choice("selected"),
            onSelect = {},
            size = ControlSize.entries[v.choice("size")],
        )
    },
    entry(
        "badge",
        "AppBadge",
        Knob.Number("count", "Count", 0..BADGE_CEILING, initial = 3),
        Knob.Action("replay", "Replay"),
    ) { v ->
        key(v.ticks("replay")) {
            AppBadge(count = v.int("count"))
        }
    },
    entry(
        "progress",
        "AppProgress",
        Knob.Fraction("fraction", "Fraction", initial = 0.4f),
        Knob.Text("label", "Label", "Syncing"),
        Knob.Action("replay", "Replay"),
    ) { v ->
        key(v.ticks("replay")) {
            AppProgress(fraction = v.fraction("fraction"), label = v.text("label").ifEmpty { null })
        }
    },
    // ---- Every other component. In the gallery's order; a knob per parameter that is a
    // Boolean, an enum, a String or a small Int. Overlays get one knob that opens them.
    entry(
        "text",
        "AppText",
        Knob.Text("text", "Text", "The quick brown fox jumps over the lazy dog"),
        Knob.Choice("role", "Role", names(TextRole.entries), initial = TextRole.Body.ordinal),
        Knob.Number("maxLines", "Max lines", 1..MAX_LINES_CEILING, initial = MAX_LINES_CEILING),
    ) { v ->
        AppText(
            text = v.text("text"),
            role = TextRole.entries[v.choice("role")],
            maxLines = v.int("maxLines"),
            overflow = TextOverflow.Ellipsis,
        )
    },
    entry(
        "card",
        "AppCard",
        Knob.Choice("level", "Level", names(SurfaceLevel.entries), initial = SurfaceLevel.Raised.ordinal),
        Knob.Text("title", "Title", "Order #1042"),
    ) { v ->
        AppCard(level = SurfaceLevel.entries[v.choice("level")], modifier = Modifier.fillMaxWidth()) {
            AppText(v.text("title"), role = TextRole.Title)
            AppText("Two items · picked up", role = TextRole.Secondary)
        }
    },
    entry(
        "listitem",
        "AppListItem",
        Knob.Text("headline", "Headline", "Coffee"),
        Knob.Text("supporting", "Supporting", "Freshly ground"),
        Knob.Toggle("leading", "Leading avatar", true),
        Knob.Toggle("trailing", "Trailing tag", true),
        Knob.Toggle("selected", "Selected", false),
        Knob.Toggle("destructive", "Destructive", false),
        Knob.Toggle("swipe", "Swipe action (revealed)", false),
    ) { v ->
        AppListItem(
            headline = v.text("headline"),
            supporting = v.text("supporting").ifEmpty { null },
            onClick = {},
            selected = v.bool("selected"),
            destructive = v.bool("destructive"),
            swipeActions = if (v.bool("swipe")) {
                SwipeActions(
                    end = SwipeAction("Remove", Icons.Filled.Delete, onAction = {}, tone = TagTone.Negative),
                    initiallyRevealed = true,
                )
            } else {
                null
            },
            leading = if (v.bool("leading")) ({ AppAvatar(name = v.text("headline")) }) else null,
            trailing = if (v.bool("trailing")) ({ AppTag(label = "New") }) else null,
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "descriptionlist",
        "AppDescriptionList",
        Knob.Number("rows", "Rows", 1..ROWS_CEILING, initial = 3),
        Knob.Toggle("numeric", "Numeric values", false),
        Knob.Toggle("missing", "A missing value", false),
    ) { v ->
        AppDescriptionList(
            rows = List(v.int("rows")) { index ->
                DescriptionRow(
                    label = "Row ${index + 1}",
                    value = if (v.bool("missing") &&
                        index == 0
                    ) {
                        null
                    } else if (v.bool("numeric")) {
                        "${(index + 1) * ROW_STEP}"
                    } else {
                        "Value"
                    },
                    numeric = v.bool("numeric"),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "accordion",
        "AppAccordion",
        Knob.Text("title", "Title", "Delivery details"),
        Knob.Toggle("expanded", "Expanded", true),
    ) { v ->
        AppAccordion(title = v.text("title"), expanded = v.bool("expanded"), onToggle = {
        }, modifier = Modifier.fillMaxWidth()) {
            AppText("Left with the neighbour.")
        }
    },
    entry(
        "sectionheader",
        "AppSectionHeader",
        Knob.Text("title", "Title", "Favourites"),
        Knob.Toggle("action", "Action", true),
    ) { v ->
        AppSectionHeader(
            title = v.text("title"),
            actionLabel = if (v.bool("action")) "See all" else null,
            onAction = if (v.bool("action")) ({}) else null,
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "divider",
        "AppDivider",
        Knob.Choice("kind", "Kind", listOf("Plain", "Strong", "Labelled")),
    ) { v ->
        when (v.choice("kind")) {
            0 -> AppDivider(modifier = Modifier.fillMaxWidth())
            1 -> AppDivider(modifier = Modifier.fillMaxWidth(), strong = true)
            else -> AppLabelledDivider(label = "or", modifier = Modifier.fillMaxWidth())
        }
    },
    entry(
        "image",
        "AppImage",
        Knob.Text("url", "URL", "https://example.invalid/poster.jpg"),
        Knob.Choice("shape", "Shape", listOf("xs", "sm", "md", "lg")),
    ) { v ->
        AppImage(
            model = v.text("url").ifEmpty { null },
            contentDescription = null,
            shape = shapeFor(v.choice("shape")),
            modifier = Modifier
                .fillMaxWidth(IMAGE_FRACTION)
                .aspectRatio(1f),
        )
    },
    entry(
        "emptystate",
        "AppEmptyState",
        Knob.Text("title", "Title", "No favourites yet"),
        Knob.Text("message", "Message", "Tap the heart on a product to keep it here."),
        Knob.Toggle("action", "Action", true),
    ) { v ->
        AppEmptyState(
            title = v.text("title"),
            message = v.text("message"),
            actionLabel = if (v.bool("action")) "Browse" else null,
            onAction = if (v.bool("action")) ({}) else null,
        )
    },
    entry(
        "scrollshadow",
        "AppScrollShadow",
        Knob.Choice("edge", "Edge", names(ScrollEdge.entries), initial = ScrollEdge.Bottom.ordinal),
    ) { v ->
        val scroll = rememberScrollState()
        Box(modifier = Modifier.height(AppTheme.density.listRowHeight * SHADOW_ROWS)) {
            Column(modifier = Modifier.verticalScroll(scroll)) {
                repeat(SHADOW_ROWS * 3) { AppListItem("Row ${it + 1}") }
            }
            val edge = ScrollEdge.entries[v.choice("edge")]
            AppScrollShadow(
                state = scroll,
                edge = edge,
                modifier = Modifier.align(if (edge == ScrollEdge.Top) Alignment.TopCenter else Alignment.BottomCenter),
            )
        }
    },
    entry(
        "pulltorefresh",
        "AppPullToRefresh",
        Knob.Toggle("refreshing", "Refreshing", true),
    ) { v ->
        AppPullToRefresh(
            refreshing = v.bool("refreshing"),
            onRefresh = {},
            modifier = Modifier.height(AppTheme.density.listRowHeight * SHADOW_ROWS),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                repeat(SHADOW_ROWS) { AppListItem("Row ${it + 1}") }
            }
        }
    },
    entry(
        "pager",
        "AppPager",
        Knob.Number("pages", "Pages", 1..PAGES_CEILING, initial = 3),
    ) { v ->
        AppPager(pageCount = v.int("pages"), modifier = Modifier.fillMaxWidth()) { page ->
            AppText("Page ${page + 1} of ${v.int("pages")}", role = TextRole.Title)
        }
    },
    entry(
        "iconbutton",
        "AppIconButton",
        Knob.Choice("kind", "Kind", names(IconButtonKind.entries)),
        Knob.Choice("size", "Size", names(ControlSize.entries), initial = ControlSize.Medium.ordinal),
        Knob.Toggle("enabled", "Enabled", true),
        Knob.Toggle("square", "Square", false),
    ) { v ->
        AppIconButton(
            icon = Icons.Filled.Favorite,
            contentDescription = "Favourite",
            onClick = {},
            enabled = v.bool("enabled"),
            kind = IconButtonKind.entries[v.choice("kind")],
            size = ControlSize.entries[v.choice("size")],
            square = v.bool("square"),
        )
    },
    entry(
        "fab",
        "AppFab",
        Knob.Text("label", "Label", "New item"),
    ) { v ->
        AppFab(icon = Icons.Filled.Add, contentDescription = "New item", onClick = {
        }, label = v.text("label").ifEmpty { null })
    },
    entry(
        "fabmenu",
        "AppFabMenu",
        Knob.Toggle("expanded", "Expanded", true),
        Knob.Number("count", "Actions", 1..FAB_MENU_ACTIONS_CEILING, initial = 3),
    ) { v ->
        Box(modifier = Modifier.height(AppTheme.density.listRowHeight * DRAWER_STAGE_ROWS)) {
            AppFabMenu(
                icon = Icons.Filled.Add,
                contentDescription = "Create",
                items = List(v.int("count")) { FabAction("Action ${it + 1}", Icons.Filled.Add) {} },
                expanded = v.bool("expanded"),
                onExpandedChange = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    },
    entry(
        "bottomactionbar",
        "AppBottomActionBar",
        Knob.Text("action", "Action", "Pay"),
        Knob.Text("label", "Label", "Total"),
        Knob.Text("value", "Value", "€ 24.50"),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppBottomActionBar(
            actionLabel = v.text("action"),
            onAction = {},
            label = v.text("label").ifEmpty { null },
            value = v.text("value").ifEmpty { null },
            enabled = v.bool("enabled"),
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "checkbox",
        "AppCheckbox",
        Knob.Text("label", "Label", "Send me the receipt"),
        Knob.Choice("state", "State", names(CheckState.entries), initial = CheckState.On.ordinal),
        Knob.Toggle("enabled", "Enabled", true),
        Knob.Text("error", "Error", ""),
        Knob.Choice("size", "Size", names(ControlSize.entries), initial = ControlSize.Medium.ordinal),
    ) { v ->
        AppCheckbox(
            checked = CheckState.entries[v.choice("state")],
            onCheckedChange = {},
            label = v.text("label"),
            enabled = v.bool("enabled"),
            errorText = v.text("error").ifEmpty { null },
            size = ControlSize.entries[v.choice("size")],
        )
    },
    entry(
        "radio",
        "AppRadio",
        Knob.Choice("selected", "Selected", listOf("Card", "Cash", "Later")),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppRadioGroup {
            listOf("Card", "Cash", "Later").forEachIndexed { index, label ->
                AppRadio(selected = v.choice("selected") == index, onSelect = {
                }, label = label, enabled = v.bool("enabled"))
            }
        }
    },
    entry(
        "select",
        "AppSelect",
        Knob.Choice("selected", "Selected", listOf("None", "Small", "Medium", "Large")),
        Knob.Text("placeholder", "Placeholder", "Size"),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppSelect(
            options = listOf("Small", "Medium", "Large"),
            selectedIndex = v.choice("selected") - 1,
            onSelect = {},
            placeholder = v.text("placeholder"),
            enabled = v.bool("enabled"),
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "searchfield",
        "AppSearchField",
        Knob.Text("value", "Value", "coff"),
        Knob.Text("placeholder", "Placeholder", "Search products"),
    ) { v ->
        AppSearchField(value = v.text("value"), onValueChange = {
        }, placeholder = v.text("placeholder"), modifier = Modifier.fillMaxWidth())
    },
    entry(
        "datefield",
        "AppDateField",
        Knob.Toggle("set", "Has a date", true),
        Knob.Text("label", "Label", "Delivery"),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppDateField(
            value = if (v.bool("set")) DEMO_DATE else null,
            onValueChange = {},
            label = v.text("label").ifEmpty { null },
            enabled = v.bool("enabled"),
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "timefield",
        "AppTimeField",
        Knob.Toggle("set", "Has a time", true),
        Knob.Text("label", "Label", "Pick-up"),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppTimeField(
            value = if (v.bool("set")) DEMO_TIME else null,
            onValueChange = {},
            label = v.text("label").ifEmpty { null },
            enabled = v.bool("enabled"),
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "slider",
        "AppSlider",
        Knob.Number("value", "Value", 0..PERCENT, initial = 40),
        Knob.Text("label", "Label", "Volume"),
        Knob.Toggle("range", "Range", false),
    ) { v ->
        val fraction = v.int("value") / PERCENT.toFloat()
        if (v.bool("range")) {
            AppRangeSlider(
                value = 0f..fraction,
                onValueChange = {},
                label = v.text("label").ifEmpty { null },
                valueLabel = "0–${v.int("value")}",
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            AppSlider(
                value = fraction,
                onValueChange = {},
                label = v.text("label").ifEmpty { null },
                valueLabel = "${v.int("value")}",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    },
    entry(
        "icon",
        "AppIcon",
        Knob.Choice("size", "Size", listOf("sm", "md", "lg"), initial = 1),
        Knob.Choice("tint", "Tint", listOf("Secondary", "Primary", "Confirm")),
    ) { v ->
        AppIcon(
            icon = Icons.Filled.Favorite,
            contentDescription = "Favourite",
            size = when (v.choice("size")) {
                0 -> AppTheme.icons.sm
                1 -> AppTheme.icons.md
                else -> AppTheme.icons.lg
            },
            tint = when (v.choice("tint")) {
                0 -> AppTheme.colors.textSecondary
                1 -> AppTheme.colors.textPrimary
                else -> AppTheme.colors.confirm.bg
            },
        )
    },
    entry(
        "sparkline",
        "AppSparkline",
        Knob.Choice("trend", "Trend", listOf("Rising", "Falling", "Flat")),
        Knob.Toggle("highlight", "Highlight", true),
        Knob.Fraction("point", "Highlighted point", initial = 0.6f),
        Knob.Toggle("second", "A second series", false),
        Knob.Toggle("bars", "Rain as columns", false),
    ) { v ->
        val values = when (v.choice("trend")) {
            0 -> RISING_WEEK
            1 -> FALLING_WEEK
            else -> FLAT_WEEK
        }
        AppSparkline(
            values = values,
            values2 = if (v.bool("second")) values.map { it - SECOND_SERIES_GAP } else null,
            bars = if (v.bool("bars")) RAIN_WEEK else null,
            contentDescription = "A week of temperatures",
            highlightIndex = if (v.bool("highlight")) (v.fraction("point") * (values.size - 1)).roundToInt() else null,
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "formfield",
        "AppFormField",
        Knob.Text("label", "Label", "Name"),
        Knob.Toggle("required", "Required", true),
        Knob.Text("helper", "Helper", "As on the card"),
        Knob.Text("error", "Error", ""),
    ) { v ->
        AppFormField(
            label = v.text("label"),
            required = v.bool("required"),
            helperText = v.text("helper").ifEmpty { null },
            errorText = v.text("error").ifEmpty { null },
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppTextField(value = "Ada", onValueChange = {})
        }
    },
    entry(
        "fieldgroup",
        "AppFieldGroup",
        Knob.Text("label", "Label", "Card"),
        Knob.Text("helper", "Helper", "Number, then expiry"),
        Knob.Text("error", "Error", ""),
    ) { v ->
        AppFieldGroup(
            label = v.text("label").ifEmpty { null },
            helperText = v.text("helper").ifEmpty { null },
            errorText = v.text("error").ifEmpty { null },
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppFieldGroupRow { AppText("4242 4242 4242 4242", role = TextRole.Numeric) }
            AppFieldGroupRow(last = true) { AppText("01 / 24", role = TextRole.Numeric) }
        }
    },
    entry(
        "avatar",
        "AppAvatar",
        Knob.Text("name", "Name", "Ada Lovelace"),
        Knob.Choice("status", "Status", listOf("None") + names(TagTone.entries)),
    ) { v ->
        val status = v.choice("status")
        AppAvatar(
            name = v.text("name"),
            status = if (status == 0) null else TagTone.entries[status - 1],
            statusDescription = if (status == 0) null else "Status",
        )
    },
    entry(
        "avatarphoto",
        "AppAvatarPhoto",
        Knob.Text("name", "Name", "Ada Lovelace"),
        Knob.Toggle("photo", "Photo", false),
    ) { v ->
        AppAvatarPhoto(name = v.text("name"), photo = if (v.bool("photo")) "https://example.invalid/ada.jpg" else null)
    },
    entry(
        "statusdot",
        "AppStatusDot",
        Knob.Text("label", "Label", "Online"),
        Knob.Choice("tone", "Tone", names(TagTone.entries), initial = TagTone.Positive.ordinal),
    ) { v ->
        AppStatusDot(label = v.text("label"), tone = TagTone.entries[v.choice("tone")])
    },
    entry(
        "banner",
        "AppBanner",
        Knob.Text("text", "Text", "You are offline. Showing what was loaded last time."),
        Knob.Choice("tone", "Tone", names(BannerTone.entries), initial = BannerTone.Warning.ordinal),
    ) { v ->
        AppBanner(
            text = v.text("text"),
            tone = BannerTone.entries[v.choice("tone")],
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "toast",
        "AppToast",
        Knob.Text("message", "Message", "Added to the cart"),
        Knob.Choice("tone", "Tone", names(ToastTone.entries)),
        Knob.Toggle("action", "Action", true),
        Knob.Action("replay", "Replay"),
    ) { v ->
        key(v.ticks("replay")) {
            AppToast(
                message = v.text("message"),
                tone = ToastTone.entries[v.choice("tone")],
                actionLabel = if (v.bool("action")) "Undo" else null,
                onAction = if (v.bool("action")) ({}) else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    },
    entry(
        "spinner",
        "AppSpinner",
        Knob.Choice("colour", "Colour", listOf("Confirm", "Text", "Destructive")),
    ) { v ->
        AppSpinner(
            color = when (v.choice("colour")) {
                0 -> AppTheme.colors.confirm.bg
                1 -> AppTheme.colors.textPrimary
                else -> AppTheme.colors.destructive.bg
            },
        )
    },
    entry(
        "skeleton",
        "AppSkeleton",
        Knob.Choice("shape", "Shape", listOf("xs", "sm", "md", "lg")),
        Knob.Number("lines", "Lines", 1..ROWS_CEILING, initial = 3),
        Knob.Action("replay", "Replay"),
    ) { v ->
        key(v.ticks("replay")) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm),
            ) {
                repeat(v.int("lines")) {
                    AppSkeleton(shape = shapeFor(v.choice("shape")), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    },
    entry(
        "tooltip",
        "AppTooltip",
        Knob.Text("text", "Text", "Split the bill"),
    ) { v ->
        AppTooltip(text = v.text("text")) {
            AppIconButton(icon = Icons.Filled.Favorite, contentDescription = v.text("text"), onClick = {
            }, tooltip = false)
        }
    },
    entry(
        "tabs",
        "AppTabs",
        Knob.Choice("selected", "Selected", listOf("Overview", "Notes", "History")),
        Knob.Number("badge", "Badge on Notes", 0..BADGE_CEILING, initial = 2),
    ) { v ->
        AppTabs(
            tabs = listOf(TabItem("Overview"), TabItem("Notes", badge = v.int("badge")), TabItem("History")),
            selectedIndex = v.choice("selected"),
            onSelect = {},
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "bottomnav",
        "AppBottomNav",
        Knob.Choice("selected", "Selected", listOf("Home", "Catalog", "Cart")),
        Knob.Number("badge", "Badge on Cart", 0..BADGE_CEILING, initial = 3),
    ) { v ->
        AppBottomNav(items = navItems(v.int("badge")), selectedIndex = v.choice("selected"), onSelect = {
        }, modifier = Modifier.fillMaxWidth())
    },
    entry(
        "drawer",
        "AppDrawer",
        Knob.Toggle("open", "Open", false),
        Knob.Choice("selected", "Selected", listOf("Home", "Catalog", "Cart")),
        Knob.Number("badge", "Badge on Cart", 0..BADGE_CEILING, initial = 3),
    ) { v ->
        val items = navItems(v.int("badge"))
        Box(modifier = Modifier.height(AppTheme.density.listRowHeight * DRAWER_STAGE_ROWS)) {
            AppDrawer(
                open = v.bool("open"),
                onOpenChange = {},
                sections = listOf(
                    DrawerSection("Places", items.map { DrawerItem(it.label, it.label, it.icon, badge = it.badge) }),
                ),
                selectedId = items[v.choice("selected")].label,
                onSelect = {},
            ) {
                AppText("Turn the knob to open the drawer.", role = TextRole.Secondary)
            }
        }
    },
    entry(
        "navrail",
        "AppNavRail",
        Knob.Choice("selected", "Selected", listOf("Home", "Catalog", "Cart")),
        Knob.Number("badge", "Badge on Cart", 0..BADGE_CEILING, initial = 3),
    ) { v ->
        AppNavRail(items = navItems(v.int("badge")), selectedIndex = v.choice("selected"), onSelect = {})
    },
    entry(
        "topbar",
        "AppTopBar",
        Knob.Text("title", "Title", "Settings"),
        Knob.Toggle("up", "Up arrow", true),
        Knob.Toggle("action", "Action", true),
    ) { v ->
        AppTopBar(
            title = v.text("title"),
            onNavigateUp = if (v.bool("up")) ({}) else null,
            actions = {
                if (v.bool(
                        "action",
                    )
                ) {
                    AppIconButton(icon = Icons.Filled.Favorite, contentDescription = "Favourite", onClick = {
                    })
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "toolbar",
        "AppToolbar",
        Knob.Text("title", "Title", "2 selected"),
        Knob.Toggle("action", "Action", true),
    ) { v ->
        AppToolbar(
            title = v.text("title"),
            actions = {
                if (v.bool("action")) {
                    AppIconButton(icon = Icons.Filled.Delete, contentDescription = "Delete", onClick = {
                    }, kind = IconButtonKind.Inverse)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    },
    entry(
        "menu",
        "AppMenu",
        Knob.Toggle("open", "Open", false),
        Knob.Toggle("destructive", "A destructive item", true),
    ) { v ->
        AppMenu(
            expanded = v.bool("open"),
            onDismiss = {},
            items = listOfNotNull(
                MenuItem("Rename") {},
                MenuItem("Duplicate") {},
                if (v.bool("destructive")) MenuItem("Delete", destructive = true) {} else null,
            ),
            anchor = { AppButton(label = "Open the menu", onClick = {}, kind = ButtonKind.Outline) },
        )
    },
    entry(
        "sheet",
        "AppSheet",
        Knob.Toggle("open", "Open", false),
        Knob.Text("title", "Title", "Filter"),
    ) { v ->
        AppText("Turn the knob to open the sheet.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppSheet(onDismiss = {}, title = v.text("title").ifEmpty { null }) {
                AppText("Sheet content.")
            }
        }
    },
    entry(
        "dialog",
        "AppDialog",
        Knob.Toggle("open", "Open", false),
        Knob.Text("title", "Title", "Delete this item?"),
        Knob.Text("message", "Message", "This cannot be undone."),
        Knob.Toggle("destructive", "Destructive", true),
    ) { v ->
        AppText("Turn the knob to open the dialog.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppConfirmDialog(
                title = v.text("title"),
                confirmLabel = "Delete",
                onConfirm = {},
                onDismiss = {},
                message = v.text("message").ifEmpty { null },
                destructive = v.bool("destructive"),
            )
        }
    },
    entry(
        "reveal",
        "AppReveal",
        Knob.Toggle("visible", "Visible", true),
        Knob.Toggle("slide", "Slide", false),
    ) { v ->
        AppReveal(visible = v.bool("visible"), slide = v.bool("slide")) {
            AppButton(label = "Revealed", onClick = {})
        }
    },
    entry(
        "switcher",
        "AppSwitcher",
        Knob.Choice("target", "Target", listOf("First", "Second", "Third")),
        Knob.Toggle("slide", "Slide", true),
    ) { v ->
        AppSwitcher(target = v.choice("target"), slide = v.bool("slide")) { index ->
            AppText(text = "Panel ${index + 1} of 3")
        }
    },
    entry(
        "counter",
        "AppCounter",
        Knob.Number("value", "Value", 0..COUNTER_CEILING, initial = 8),
        Knob.Choice("role", "Role", listOf("Numeric", "DisplayLarge", "Secondary")),
    ) { v ->
        AppCounter(
            text = v.int("value").toString(),
            role = when (v.choice("role")) {
                0 -> TextRole.Numeric
                1 -> TextRole.DisplayLarge
                else -> TextRole.Secondary
            },
        )
    },
    entry(
        "actionsheet",
        "AppActionSheet",
        Knob.Toggle("open", "Open", false),
        Knob.Text("title", "Title", "Fight Club"),
        Knob.Toggle("destructive", "A destructive item", true),
    ) { v ->
        AppText("Turn the knob to open the sheet.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppActionSheet(
                title = v.text("title").ifEmpty { null },
                onDismiss = {},
                items = listOfNotNull(
                    MenuItem("Open") {},
                    MenuItem("Share") {},
                    if (v.bool("destructive")) MenuItem("Remove", destructive = true) {} else null,
                ),
            )
        }
    },
    entry(
        "pickersheet",
        "AppPickerSheet",
        Knob.Toggle("open", "Open", false),
        Knob.Number("selected", "Selected", 0..2, initial = 0),
    ) { v ->
        AppText("Turn the knob to open the sheet.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppPickerSheet(
                title = "Sort by",
                options = listOf("Name", "Price", "Acquired"),
                selectedIndex = v.int("selected"),
                onSelect = {},
                onDismiss = {},
            )
        }
    },
    entry(
        "chip",
        "AppChip",
        Knob.Text("label", "Label", "Fragile"),
        Knob.Toggle("selected", "Selected", true),
        Knob.Toggle("icon", "Leading icon", false),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppChip(
            label = v.text("label"),
            selected = v.bool("selected"),
            onClick = {},
            leadingIcon = if (v.bool("icon")) Icons.Filled.Star else null,
            enabled = v.bool("enabled"),
        )
    },
    entry(
        "chipgroup",
        "AppChipGroup",
        Knob.Number("count", "Chips", 1..CHIP_CEILING, initial = 5),
        Knob.Number("selected", "Selected", 0..CHIP_CEILING, initial = 1),
        Knob.Toggle("single", "Single choice", false),
    ) { v ->
        AppChipGroup(
            options = List(v.int("count")) { "Tag ${it + 1}" },
            selected = setOf(v.int("selected")).filter { it < v.int("count") }.toSet(),
            onSelectionChange = {},
            single = v.bool("single"),
        )
    },
    entry(
        "reorder",
        "AppReorder",
        Knob.Number("rows", "Rows", 2..REORDER_CEILING, initial = 3),
    ) { v ->
        ReorderBench(rowCount = v.int("rows"))
    },
    entry(
        "coachmark",
        "AppCoachMark",
        Knob.Number("step", "Step", 0..COACH_BENCH_CEILING, initial = 0),
        Knob.Number("count", "Of", 1..COACH_BENCH_CEILING + 1, initial = 3),
    ) { v ->
        CoachMarkBench(stepIndex = v.int("step"), stepCount = v.int("count"))
    },
    entry(
        "codefield",
        "AppCodeField",
        Knob.Text("value", "Value", "4209"),
        Knob.Text("label", "Label", "Code"),
        Knob.Text("error", "Error", ""),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppCodeField(
            value = v.text("value"),
            onValueChange = {},
            label = v.text("label").ifEmpty { null },
            errorText = v.text("error").ifEmpty { null },
            enabled = v.bool("enabled"),
        )
    },
    entry(
        "imageviewer",
        "AppImageViewer",
        Knob.Toggle("open", "Open", false),
    ) { v ->
        // A window of its own: the knob opens it over the playground, and its close key shuts it.
        if (v.bool("open")) AppImageViewer(images = listOf(null, null, null), onClose = {})
    },
    entry(
        "htmltext",
        "AppHtmlText",
        Knob.Text(
            "html",
            "HTML",
            "Freshly <b>ground</b>.<ul><li>Arabica</li></ul><a href=\"https://example.com\">More</a>",
        ),
    ) { v ->
        AppHtmlText(html = v.text("html"))
    },
    entry(
        "promptdialog",
        "AppPromptDialog",
        Knob.Toggle("open", "Open", false),
        Knob.Text("title", "Title", "Rename this trip"),
        Knob.Text("label", "Label", "Name"),
        Knob.Text("initial", "Initial value", "Lisbon"),
        Knob.Toggle("required", "Required", true),
    ) { v ->
        AppText("Turn the knob to open the dialog.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppPromptDialog(
                title = v.text("title"),
                confirmLabel = "Rename",
                onConfirm = {},
                onDismiss = {},
                label = v.text("label").ifEmpty { null },
                initialValue = v.text("initial"),
                validator = if (v.bool("required")) required() else null,
            )
        }
    },
    entry(
        "choicedialog",
        "AppChoiceDialog",
        Knob.Toggle("open", "Open", false),
        Knob.Text("title", "Title", "Sort by"),
        Knob.Toggle("multiple", "Several", false),
    ) { v ->
        AppText("Turn the knob to open the dialog.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppChoiceDialog(
                title = v.text("title"),
                options = listOf(
                    ChoiceOption("name", "Name"),
                    ChoiceOption("price", "Price"),
                    ChoiceOption("added", "Date added"),
                ),
                confirmLabel = "Apply",
                onConfirm = {},
                onDismiss = {},
                multiple = v.bool("multiple"),
            )
        }
    },
    entry(
        "permissionrationale",
        "AppPermissionRationale",
        Knob.Toggle("open", "Open", false),
        Knob.Text("title", "Title", "Allow notifications?"),
        Knob.Text("message", "Message", "We tell you when an order ships."),
        Knob.Toggle("refused", "Refused for good", false),
    ) { v ->
        AppText("Turn the knob to open the dialog.", role = TextRole.Secondary)
        if (v.bool("open")) {
            AppPermissionRationale(
                icon = Icons.Filled.Notifications,
                title = v.text("title"),
                message = v.text("message"),
                refusedForGood = v.bool("refused"),
                onContinue = {},
                onNotNow = {},
            )
        }
    },
    entry(
        "cardfield",
        "AppCardField",
        Knob.Text("number", "Number", "4242424242424242"),
        Knob.Text("expiry", "Expiry (MMYY)", "1230"),
        Knob.Text("code", "Code", "123"),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppCardField(
            value = AppCardFieldState(number = v.text("number"), expiry = v.text("expiry"), code = v.text("code")),
            onValueChange = {},
            enabled = v.bool("enabled"),
        )
    },
    entry(
        "phonefield",
        "AppPhoneField",
        Knob.Text("region", "Region (ISO)", "CZ"),
        Knob.Text("number", "Number", "777123456"),
        Knob.Text("error", "Error", ""),
        Knob.Toggle("enabled", "Enabled", true),
    ) { v ->
        AppPhoneField(
            value = AppPhoneFieldState(region = v.text("region").uppercase(), number = v.text("number")),
            onValueChange = {},
            label = "Phone",
            errorText = v.text("error").ifEmpty { null },
            enabled = v.bool("enabled"),
        )
    },
    entry(
        "imagecropper",
        "AppImageCropper",
        Knob.Text("url", "URL", "https://picsum.photos/id/1015/900/600"),
    ) { v ->
        AppImageCropper(model = v.text("url").ifEmpty { null }, onCropChange = {})
    },
    // create_component.py appends a starter entry here; doctor.py fails on a component with none.
)

/** One step over one target: what the bench is for is the card's own shape, not the walking. */
@Composable
private fun CoachMarkBench(stepIndex: Int, stepCount: Int) {
    val targets = rememberCoachTargets()
    CompositionLocalProvider(LocalCoachTargets provides targets) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.density.listRowHeight * COACH_BENCH_ROWS),
        ) {
            AppButton(
                label = "Weather",
                onClick = {},
                modifier = Modifier.appCoachTarget(COACH_BENCH_TARGET),
            )
            AppCoachMark(
                step = CoachStep(COACH_BENCH_TARGET, "Your places", "Everywhere you keep an eye on."),
                stepIndex = stepIndex.coerceAtMost(stepCount - 1),
                stepCount = stepCount,
                onNext = {},
                onSkip = {},
            )
        }
    }
}

private const val COACH_BENCH_TARGET = "devMenu_coachTarget"

/** More steps than this and the progress row is a line rather than a set of marks. */
private const val COACH_BENCH_CEILING = 5

/** Tall enough for the target and the card under it. */
private const val COACH_BENCH_ROWS = 5

/**
 * A list the bench can drag. The rows are the bench's own state, because that is the contract —
 * `onMove` reports, the caller moves.
 */
@Composable
private fun ReorderBench(rowCount: Int) {
    var rows by remember(rowCount) { mutableStateOf(List(rowCount) { "Row ${it + 1}" }) }
    val state = rememberReorderState { from, to ->
        rows = rows.toMutableList().apply { add(to, removeAt(from)) }
    }
    LazyColumn(
        state = state.listState,
        modifier = Modifier.height(AppTheme.density.listRowHeight * rowCount),
    ) {
        items(rows, key = { it }) { row ->
            AppListItem(
                headline = row,
                modifier = appReorderItem(state, row),
                dragHandle = { AppReorder(state = state, key = row) },
            )
        }
    }
}

/** More rows than this and the bench is a scrolling list rather than a demonstration of a drag. */
private const val REORDER_CEILING = 6

/** Three tabs' worth of destinations, with the badge on the last. */
/** Tall enough for the drawer's three doors under its section divider, and the scrim to read. */
private const val DRAWER_STAGE_ROWS = 5

/** More than three and the dial is a menu; the knob stops where the design does. */
private const val FAB_MENU_ACTIONS_CEILING = 4

private fun navItems(badge: Int) = listOf(
    NavItem("Home", Icons.Filled.Home),
    NavItem("Catalog", Icons.Filled.List),
    NavItem("Cart", Icons.Filled.ShoppingCart, badge = badge),
)

/** A `Choice` over the four corner roles, so a shape knob is an index and never a dp. */
@Composable
private fun shapeFor(index: Int) = when (index) {
    0 -> AppTheme.shapes.xs
    1 -> AppTheme.shapes.sm
    2 -> AppTheme.shapes.md
    else -> AppTheme.shapes.lg
}

fun playgroundEntry(id: String): PlaygroundEntry? = playgroundCatalog.firstOrNull { it.id == id }

/** Wide enough to see a stepper stop at its floor and its ceiling, small enough to reach both. */
private const val STEPPER_CEILING = 9

/** Past the two-digit badge, so the "99+" case is one tap away. */
private const val BADGE_CEILING = 120

/** Across 9 → 10 and 99 → 100, where a counter adds a cell. */
private const val COUNTER_CEILING = 120

private const val PERCENT = 100

private const val MAX_LINES_CEILING = 5
private const val ROWS_CEILING = 6
private const val CHIP_CEILING = 8
private const val ROW_STEP = 1_250
private const val PAGES_CEILING = 5

/** Two rows tall, so a scrolling demo always has more than it can show. */
private const val SHADOW_ROWS = 2

private val RISING_WEEK = listOf(12f, 14f, 13f, 17f, 19f, 18f, 22f)
private val FALLING_WEEK = listOf(22f, 21f, 17f, 15f, 15f, 12f, 9f)
private val FLAT_WEEK = listOf(16f, 16f, 16f, 16f, 16f, 16f, 16f)

/** A low nine degrees under each high, so the pair reads as a range rather than as two lines. */
private const val SECOND_SERIES_GAP = 9f

/** A week's rain probability, 0–1, for the bars knob. */
private val RAIN_WEEK = listOf(0f, 0.2f, 0.6f, 0.9f, 0.4f, 0.1f, 0f)
private const val IMAGE_FRACTION = 0.5f

private val DEMO_DATE: LocalDate = LocalDate.of(2026, 9, 21)
private val DEMO_TIME: LocalTime = LocalTime.of(17, 30)
