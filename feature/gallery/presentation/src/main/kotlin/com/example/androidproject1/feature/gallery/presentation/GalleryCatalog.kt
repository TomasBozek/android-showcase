package com.example.androidproject1.feature.gallery.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.androidproject1.core.ui.component.content.AppVerticalDivider
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
import com.example.androidproject1.core.ui.component.navigation.AppScreenChrome
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
import com.example.androidproject1.core.ui.component.overlay.AppDialog
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
import com.example.androidproject1.core.ui.component.status.AppIndeterminateProgress
import com.example.androidproject1.core.ui.component.status.AppProgress
import com.example.androidproject1.core.ui.component.status.AppSkeleton
import com.example.androidproject1.core.ui.component.status.AppSpinner
import com.example.androidproject1.core.ui.component.status.AppStatusDot
import com.example.androidproject1.core.ui.component.status.AppStepProgress
import com.example.androidproject1.core.ui.component.status.AppTag
import com.example.androidproject1.core.ui.component.status.BannerTone
import com.example.androidproject1.core.ui.component.status.TagTone
import com.example.androidproject1.core.ui.layout.LocalCoachTargets
import com.example.androidproject1.core.ui.layout.appCoachTarget
import com.example.androidproject1.core.ui.layout.appReorderItem
import com.example.androidproject1.core.ui.layout.rememberCoachTargets
import com.example.androidproject1.core.ui.layout.rememberReorderState
import com.example.androidproject1.core.ui.theme.AppTheme
import com.example.androidproject1.feature.gallery.presentation.component.MotionReplay
import com.example.androidproject1.feature.gallery.presentation.gallery.GalleryItem
import com.example.androidproject1.service.core.ui.form.required
import com.example.androidproject1.service.core.ui.state.ContentState
import com.example.androidproject1.service.core.ui.state.LoadingState
import com.example.androidproject1.service.core.ui.text.toUiText
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.roundToInt

/** One rendered state of a component, with the label that says which state it is. */
@Immutable
data class GalleryVariant(val label: String, val content: @Composable () -> Unit)

/**
 * One component in the gallery.
 *
 * The demos live here rather than in `:core:ui` on purpose: the gallery is a feature, and a feature
 * composes components. Keeping them here means `:core:ui` ships no sample code.
 */
@Immutable
data class GalleryEntry(
    val id: String,
    val name: String,
    val group: GalleryGroup,
    val summary: String,
    val variants: List<GalleryVariant>,
)

private fun entry(
    id: String,
    name: String,
    group: GalleryGroup,
    summary: String,
    vararg variants: Pair<String, @Composable () -> Unit>,
) = GalleryEntry(id, name, group, summary, variants.map { GalleryVariant(it.first, it.second) })

/**
 * A page that draws a token rather than a component.
 *
 * It is a constructor of its own so that `check_gallery_lists_every_component` can skip it by
 * name: that check reads the second string of every `entry(` as a file in `core/ui/component`,
 * and "Colours" is not one. Everything else about a foundation page is an entry like any other.
 */
private fun foundation(
    id: String,
    name: String,
    summary: String,
    vararg variants: Pair<String, @Composable () -> Unit>,
) = GalleryEntry(
    id,
    name,
    GalleryGroup.Foundations,
    summary,
    variants.map { GalleryVariant(it.first, it.second) },
)

/**
 * Every component in `:core:ui`, with the states worth looking at.
 *
 * This is the running counterpart to the design system's component document: if something is not
 * here, a screen should not be inventing it. A component added to `:core:ui` is added here in the
 * same change, and a variant the document names is a variant listed here.
 *
 * Two files in `core/ui/component` are deliberately not entries. `AppScaffold` is the screen shell
 * rather than something on a screen — every page of this gallery is already one, so a card
 * containing a scaffold would demonstrate it worse than the gallery does by existing. `ControlSize`
 * is the shared sm / md / lg scale and not a component at all; it shows up as the size variants of
 * the controls that read it.
 *
 * `AppScreenChrome` is the one entry nothing else composes: it is what `Screen()` draws around
 * every feature, so rendering it here is the only way to look at it. Its alert dialog and its
 * snackbar host draw in windows of their own and are goldens in `OverlayScreenshotTest` instead.
 */
/**
 * The system's roots: a page for a colour role, a type style, a spacing step, a shape, an
 * elevation level, an icon size and a motion token.
 *
 * Each page reads the loaded theme rather than restating `Ramp.kt`, so it says what the device is
 * rendering in the theme it is rendering it in. What they draw is `GalleryFoundations.kt`.
 */
internal val foundationPages: List<GalleryEntry> = listOf(
    foundation(
        "colours",
        "Colours",
        "Every role in AppColors, with the hex it resolves to in the loaded theme.",
        "Surfaces" to { SurfaceRoles() },
        "Text" to { TextRoles() },
        "Actions" to { ActionRoles() },
        "Feedback" to { FeedbackRolesPage() },
        "Lines and overlays" to { LineRoles() },
    ),
    foundation(
        "typography",
        "Typography",
        "Eleven roles, each set in itself, with the size and line height it resolved to.",
        "Roles" to { TypeRoles() },
    ),
    foundation(
        "spacing",
        "Spacing",
        "The three role sets over the 4 dp scale: inset, stack and inline, drawn at their widths.",
        "Inset" to { InsetSteps() },
        "Stack" to { StackSteps() },
        "Inline" to { InlineSteps() },
    ),
    foundation(
        "shapes",
        "Shapes",
        "Eight corner roles, each on a key-sized tile — the surface they are cut from.",
        "Roles" to { ShapeRoles() },
    ),
    foundation(
        "elevation",
        "Elevation",
        "Five levels, each as a key that really presses. An edge that shortens, never a shadow.",
        "Levels" to { ElevationLevels() },
    ),
    foundation(
        "icons",
        "Icons",
        "Three sizes and only three; the job picks which. None of them is a touch target.",
        "Sizes" to { IconSizes() },
    ),
    foundation(
        "motion",
        "Motion",
        "The seven durations, each through a component that reads it. Replayed on tap.",
        "Tokens" to { MotionTokens() },
    ),
)

private val componentEntries: List<GalleryEntry> = listOf(
    entry(
        "text", "AppText", GalleryGroup.Content,
        "Text asked for by role — the role decides both size and colour.",
        "Display large" to { AppText("1 248,00", role = TextRole.DisplayLarge) },
        "Display" to { AppText("Order 12 — On its way", role = TextRole.Display) },
        "Title" to { AppText("Drinks · Hot drinks", role = TextRole.Title) },
        "Body" to { AppText("The default size for list rows and dialog copy.") },
        "Label" to { AppText("Appearance", role = TextRole.Label) },
        "Label small" to { AppText("OPEN ORDERS", role = TextRole.LabelSmall) },
        "Numeric (tabular)" to { AppText("118,00", role = TextRole.Numeric) },
        "Numeric — shrinks rather than wraps" to {
            Box(modifier = Modifier.width(AppTheme.density.listRowHeight * 2)) {
                AppText("1 234 567,00 Kč", role = TextRole.Numeric)
            }
        },
        "Secondary" to { AppText("Opened 19:24 · Jana N.", role = TextRole.Secondary) },
        "Tertiary — missing value" to { AppText("—", role = TextRole.Tertiary) },
    ),
    entry(
        "button", "AppButton", GalleryGroup.Action,
        "Six kinds, three sizes. The press shortens the key's edge so it travels 3 dp.",
        "Confirm" to { AppButton("Pay", {}) },
        "Destructive" to { AppButton("Delete", {}, kind = ButtonKind.Destructive) },
        "Info" to { AppButton("Learn more", {}, kind = ButtonKind.Info) },
        "Neutral" to { AppButton("Later", {}, kind = ButtonKind.Neutral) },
        "Outline" to { AppButton("Back", {}, kind = ButtonKind.Outline) },
        "Ghost" to { AppButton("More options", {}, kind = ButtonKind.Ghost) },
        "Disabled — loses the body" to { AppButton("Unavailable", {}, enabled = false) },
        "Loading — label and width hold" to { AppButton("Processing", {}, loading = true) },
        "Small 40 (mouse only)" to { AppButton("Small", {}, size = ControlSize.Small) },
        "Large 64" to { AppButton("Large", {}, size = ControlSize.Large) },
        "Press and hold — an edgeless key scales" to { AppButton("Hold me", {}, kind = ButtonKind.Outline) },
    ),
    entry(
        "iconbutton",
        "AppIconButton",
        GalleryGroup.Action,
        "An icon that does something. The description is required, for the reader and the test.",
        "Default" to { AppIconButton(Icons.Filled.Add, "Add item", {}) },
        "Confirm" to { AppIconButton(Icons.Filled.Done, "Confirm", {}, kind = IconButtonKind.Confirm) },
        "Destructive" to {
            AppIconButton(Icons.Filled.Delete, "Delete", {}, kind = IconButtonKind.Destructive)
        },
        "Square — for a row of them" to {
            AppIconButton(Icons.Filled.Add, "Add item", {}, kind = IconButtonKind.Confirm, square = true)
        },
        "Small — pointer only" to { AppIconButton(Icons.Filled.Add, "Add item", {}, size = ControlSize.Small) },
        "Large" to { AppIconButton(Icons.Filled.Add, "Add item", {}, size = ControlSize.Large) },
        "Disabled" to { AppIconButton(Icons.Filled.Add, "Add item", {}, enabled = false) },
    ),
    entry(
        "fab",
        "AppFab",
        GalleryGroup.Action,
        "One per screen, for the thing the screen exists to do. Travels like every other key.",
        "Icon only" to { AppFab(Icons.Filled.Add, "New order", {}) },
        "Extended" to { AppFab(Icons.Filled.Add, "New order", {}, label = "New order") },
    ),
    entry(
        "textfield", "AppTextField", GalleryGroup.Form,
        "A sunken field. An error always carries text — colour alone is invisible to too many.",
        "Empty with placeholder" to {
            Demo("") { value, onChange ->
                AppTextField(value, onChange, label = "Search", placeholder = "Product name")
            }
        },
        "Filled with helper" to {
            Demo("Pilsner") { value, onChange ->
                AppTextField(value, onChange, label = "Item", helperText = "From the catalogue")
            }
        },
        "Numeric — tabular figures" to {
            Demo("12") { value, onChange ->
                AppTextField(value, onChange, label = "Quantity", numeric = true)
            }
        },
        "Password" to {
            Demo("hunter2") { value, onChange ->
                AppTextField(value, onChange, label = "PIN", password = true)
            }
        },
        "Error" to {
            Demo("abc") { value, onChange ->
                AppTextField(value, onChange, label = "Code", errorText = "No product with this code")
            }
        },
        "Unit suffix — the unit is not the value" to {
            Demo("0,420") { value, onChange ->
                AppTextField(value, onChange, label = "Weight", numeric = true, suffix = "kg")
            }
        },
        "Small — pointer only" to {
            Demo("40") { value, onChange -> AppTextField(value, onChange, size = ControlSize.Small) }
        },
        "Large" to {
            Demo("56") { value, onChange -> AppTextField(value, onChange, size = ControlSize.Large) }
        },
        "Disabled" to { AppTextField("Locked", {}, label = "Username", enabled = false) },
        "In a form — Next, and offered to a password manager" to {
            Demo("ada@example.com") { value, onChange ->
                AppTextField(
                    value,
                    onChange,
                    label = "Address",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    contentType = ContentType.Username + ContentType.EmailAddress,
                )
            }
        },
        "The last field — Done submits" to {
            Demo("hunter2") { value, onChange ->
                AppTextField(
                    value,
                    onChange,
                    label = "Password",
                    password = true,
                    imeAction = ImeAction.Done,
                    onImeAction = {},
                    contentType = ContentType.Password,
                )
            }
        },
    ),
    entry(
        "searchfield",
        "AppSearchField",
        GalleryGroup.Form,
        "Glass on the left, clearing on the right — the most common thing to undo.",
        "Empty" to {
            Demo("") { value, onChange ->
                AppSearchField(value, onChange, placeholder = "Search products")
            }
        },
        "With a query" to { Demo("Pilsner") { value, onChange -> AppSearchField(value, onChange) } },
    ),
    entry(
        "select",
        "AppSelect",
        GalleryGroup.Form,
        "One choice from more than a handful. Below five, prefer radio or segmented.",
        "Selected" to {
            Demo(1) { selected, onSelect ->
                AppSelect(listOf("Daily", "Weekly", "Monthly", "Quarterly", "Yearly"), selected, onSelect)
            }
        },
        "Nothing chosen" to {
            Demo(-1) { selected, onSelect ->
                AppSelect(listOf("A", "B"), selected, onSelect, placeholder = "Choose a price list")
            }
        },
    ),
    entry(
        "checkbox",
        "AppCheckbox",
        GalleryGroup.Form,
        "The whole row is the target. Indeterminate belongs to a group toggle and nowhere else.",
        "On" to { CheckboxDemo(CheckState.On, "Email me a summary") },
        "Off" to { CheckboxDemo(CheckState.Off, "Send a weekly digest") },
        // Tapping resolves it: a group toggle that is partly on becomes fully on.
        "Indeterminate" to { CheckboxDemo(CheckState.Indeterminate, "All items") },
        "Error — never colour alone" to {
            AppCheckbox(
                CheckState.Off,
                {},
                "Accept the terms",
                errorText = "This has to be ticked before the order can be sent",
            )
        },
        "Small — pointer only" to {
            Demo(CheckState.On) { checked, onChange ->
                AppCheckbox(
                    checked,
                    { onChange(if (it) CheckState.On else CheckState.Off) },
                    "Dense list row",
                    size = ControlSize.Small,
                )
            }
        },
        "Disabled" to { AppCheckbox(CheckState.Off, {}, "Unavailable", enabled = false) },
    ),
    entry(
        "radio",
        "AppRadio",
        GalleryGroup.Form,
        "Exactly one of a group. Above five options this is the wrong control.",
        "A group" to {
            Demo(0) { selected, onSelect ->
                AppRadioGroup {
                    AppRadio(selected == 0, { onSelect(0) }, "Light")
                    AppRadio(selected == 1, { onSelect(1) }, "Dark")
                    AppRadio(false, {}, "System", enabled = false)
                }
            }
        },
    ),
    entry(
        "switch",
        "AppSwitch",
        GalleryGroup.Form,
        "An immediate change with no confirmation — so never for a destructive choice.",
        "On" to {
            Demo(true) { checked, onChange -> AppSwitch(checked, onChange, "Notify me about price drops") }
        },
        "With supporting text" to {
            Demo(false) { checked, onChange ->
                AppSwitch(checked, onChange, "Sounds", supporting = "Feedback on every key press")
            }
        },
        "Small — pointer only" to {
            Demo(true) { checked, onChange ->
                AppSwitch(checked, onChange, "Dense list row", size = ControlSize.Small)
            }
        },
        "Disabled" to { AppSwitch(false, {}, "Unavailable", enabled = false) },
    ),
    entry(
        "segmented",
        "AppSegmented",
        GalleryGroup.Form,
        "Two to four short options that switch immediately.",
        "Three options" to {
            Demo(0) { selected, onSelect -> AppSegmented(listOf("Today", "Week", "Month"), selected, onSelect) }
        },
        "Two options" to {
            Demo(1) { selected, onSelect -> AppSegmented(listOf("Percent", "Amount"), selected, onSelect) }
        },
        "Small — pointer only" to {
            Demo(0) { selected, onSelect ->
                AppSegmented(listOf("Today", "Week"), selected, onSelect, size = ControlSize.Small)
            }
        },
    ),
    entry(
        "stepper",
        "AppStepper",
        GalleryGroup.Form,
        "Quantity, one at a time. Fixed width, so the row does not shift from 9 to 10.",
        "One" to { Demo(1) { value, onChange -> AppStepper(value, onChange) } },
        "Twelve" to { Demo(12) { value, onChange -> AppStepper(value, onChange) } },
        "At the floor" to { Demo(0) { value, onChange -> AppStepper(value, onChange) } },
    ),
    entry(
        "slider",
        "AppSlider",
        GalleryGroup.Form,
        "Continuous values only. Never a price — nearly right is wrong.",
        "With a value label" to {
            Demo(0.4f) { value, onChange ->
                AppSlider(
                    value,
                    onChange,
                    label = "Brightness",
                    valueLabel = "${(value * 100).roundToInt()} %",
                )
            }
        },
        "Range — a filter, not a total" to {
            Demo(0.2f..0.7f) { range, onChange ->
                AppRangeSlider(
                    range,
                    onChange,
                    label = "Price",
                    valueLabel = "${(range.start * 200).roundToInt()} — ${(range.endInclusive * 200).roundToInt()} Kc",
                )
            }
        },
    ),
    entry(
        "formfield",
        "AppFormField",
        GalleryGroup.Form,
        "Label above, help or error below. A required field says the word.",
        "Required" to {
            AppFormField("Appearance", required = true) {
                Demo(0) { selected, onSelect -> AppSegmented(listOf("Light", "Dark"), selected, onSelect) }
            }
        },
        "Error" to {
            AppFormField("Discount", errorText = "Above the limit for this role") {
                Demo(2) { selected, onSelect ->
                    AppSegmented(listOf("10 %", "20 %", "50 %"), selected, onSelect)
                }
            }
        },
    ),
    entry(
        "tag", "AppTag", GalleryGroup.Status,
        "A state you cannot press. The label always carries the meaning, not just the colour.",
        "Positive" to { AppTag("Active", tone = TagTone.Positive) },
        "Warning" to { AppTag("Pending", tone = TagTone.Warning) },
        "Negative" to { AppTag("Failed", tone = TagTone.Negative) },
        "Info" to { AppTag("New", tone = TagTone.Info) },
        "Neutral" to { AppTag("Draft") },
    ),
    entry(
        "statusdot",
        "AppStatusDot",
        GalleryGroup.Status,
        "A dot and a word. The dot never stands alone.",
        "Positive" to { AppStatusDot("Active", tone = TagTone.Positive) },
        "Warning" to { AppStatusDot("Pending", tone = TagTone.Warning) },
        "Negative" to { AppStatusDot("Failed", tone = TagTone.Negative) },
    ),
    entry(
        "badge",
        "AppBadge",
        GalleryGroup.Status,
        "A count over an icon or in a tab. Above 99 it becomes 99+.",
        "Small" to { AppBadge(3) },
        "Two digits" to { AppBadge(42) },
        "Over the cap" to { AppBadge(128) },
    ),
    entry(
        "avatar",
        "AppAvatar",
        GalleryGroup.Status,
        "Initials, not a photograph. The colour is derived from the id, so it needs no storage.",
        "Two names" to { AppAvatar("Jana Nováková") },
        "Another person" to { AppAvatar("Petr Svoboda") },
        "One name" to { AppAvatar("Root") },
        "With status — online" to {
            AppAvatar("Jana Nováková", status = TagTone.Positive, statusDescription = "Online")
        },
        "With status — away" to {
            AppAvatar(
                "Petr Svoboda",
                size = AppTheme.density.listRowHeight,
                status = TagTone.Negative,
                statusDescription = "Away",
            )
        },
    ),
    entry(
        "avatarPhoto",
        "AppAvatarPhoto",
        GalleryGroup.Status,
        "The photo if there is one, the initials if there is not. One rule, in one place.",
        "No photo yet" to { AppAvatarPhoto("Jana Nováková") },
        "Photo unavailable" to {
            AppAvatarPhoto("Jana Nováková", photo = "https://example.invalid/avatar.jpg")
        },
    ),
    entry(
        "spinner",
        "AppSpinner",
        GalleryGroup.Status,
        "Waiting for under two seconds. Longer than that needs a skeleton.",
        "Default" to { AppSpinner() },
    ),
    entry(
        "progress",
        "AppProgress",
        GalleryGroup.Status,
        "Determinate progress. Past ten seconds the label stops being optional.",
        "Bare" to { AppProgress(0.35f) },
        "Labelled" to { AppProgress(0.8f, label = "Uploading receipts") },
        "Indeterminate — no share to report" to {
            AppIndeterminateProgress(label = "Checking for updates")
        },
        "Steps — a place in a process" to {
            AppStepProgress(steps = 4, currentStep = 1, label = "Step 2 of 4")
        },
    ),
    entry(
        "skeleton",
        "AppSkeleton",
        GalleryGroup.Status,
        "The outline of what is loading. It copies the target's shape and does not blink.",
        "Text line" to { AppSkeleton() },
        "A list row" to { AppSkeleton(height = AppTheme.density.listRowHeight) },
    ),
    entry(
        "image",
        "AppImage",
        GalleryGroup.Content,
        "A remote image with its two real states. The one place that knows Coil exists.",
        "Failed to load" to {
            AppImage(
                model = "https://example.invalid/product.jpg",
                contentDescription = null,
                // A thumbnail the height of a list row, which is where one usually sits.
                modifier = Modifier.size(AppTheme.density.listRowHeight),
            )
        },
    ),
    entry(
        "card",
        "AppCard",
        GalleryGroup.Content,
        "A raised surface. Three levels exist and no more — the fourth is a dialog.",
        "Raised — the default" to {
            AppCard {
                AppText("Notifications", role = TextRole.Title)
                AppText("Turn these on to hear about orders.", role = TextRole.Secondary)
            }
        },
        "Sunken — an inert panel" to {
            AppCard(level = SurfaceLevel.Sunken) {
                AppText("Archived trip", role = TextRole.Title)
                AppText("Read only until it is restored.", role = TextRole.Secondary)
            }
        },
        "Base — grouping only" to {
            AppCard(level = SurfaceLevel.Base) {
                AppText("On the ground it is already on", role = TextRole.Secondary)
            }
        },
    ),
    entry(
        "divider",
        "AppDivider",
        GalleryGroup.Content,
        "A hairline where a gap is not enough. Decorative, so it is allowed to be quiet.",
        "Between two lines" to {
            AppText("Above")
            AppDivider()
            AppText("Below")
        },
        "Strong — between two sections" to {
            AppText("Section")
            AppDivider(strong = true)
            AppText("Next section")
        },
        "Labelled" to { AppLabelledDivider("EARLIER TODAY") },
        "Vertical — needs a height" to {
            Row(
                modifier = Modifier.height(AppTheme.density.minTouchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText("Left")
                AppVerticalDivider(
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.inline.md),
                )
                AppText("Right")
            }
        },
    ),
    entry(
        "listitem",
        "AppListItem",
        GalleryGroup.Content,
        "The base of every list. Its height is the density's row height.",
        "Clickable" to { AppListItem("Pilsner Urquell", supporting = "0,5 l", onClick = {}) },
        "Selected — the one shown beside it" to {
            AppListItem("Kofola", supporting = "0,5 l", onClick = {}, selected = true)
        },
        "With a trailing tag" to {
            AppListItem(
                "Camera",
                supporting = "android.permission.CAMERA",
                trailing = { AppTag("Granted", tone = TagTone.Positive) },
            )
        },
        "With a leading avatar" to {
            AppListItem(
                "Cordless drill",
                supporting = "Jana Nováková",
                onClick = {},
                leading = { AppAvatar("Jana Nováková") },
                trailing = { AppTag("Good") },
            )
        },
        "Destructive — a danger zone's row" to { AppListItem("Log out", onClick = {}, destructive = true) },
        "Swipe to reveal a key — swipe it back" to {
            AppListItem(
                "Kofola",
                supporting = "Swipe left; the key is a button",
                onClick = {},
                swipeActions = SwipeActions(
                    end = SwipeAction("Remove", Icons.Filled.Delete, onAction = {}, tone = TagTone.Negative),
                    initiallyRevealed = true,
                ),
            )
        },
    ),
    entry(
        "descriptionlist",
        "AppDescriptionList",
        GalleryGroup.Content,
        "Label left, value right. A missing value is a dash, never a blank or a zero.",
        "An order's detail" to {
            AppDescriptionList(
                listOf(
                    DescriptionRow("Opened", "19:24"),
                    DescriptionRow("Server", "Jana N."),
                    DescriptionRow("Total", "1 248,00", numeric = true),
                    DescriptionRow("Tip", null),
                ),
            )
        },
    ),
    entry(
        "sectionheader",
        "AppSectionHeader",
        GalleryGroup.Content,
        "A heading inside a screen, with at most one action.",
        "Plain" to { AppSectionHeader("OPEN ORDERS") },
        "With an action" to { AppSectionHeader("DEVICES", actionLabel = "Add", onAction = {}) },
    ),
    entry(
        "emptystate",
        "AppEmptyState",
        GalleryGroup.Content,
        "Why it is empty, and the first step out. Never just \"nothing here\".",
        "With an action" to {
            AppEmptyState(
                "No open orders",
                "Orders you start will show up here until they are paid.",
                actionLabel = "New order",
                onAction = {},
            )
        },
    ),
    entry(
        "accordion",
        "AppAccordion",
        GalleryGroup.Content,
        "Collapsible content — for settings, never for an operational screen.",
        "Expanded" to {
            Demo(true) { expanded, onChange ->
                AppAccordion("Advanced", expanded = expanded, onToggle = { onChange(!expanded) }) {
                    AppText("Settings most people never open.", role = TextRole.Secondary)
                }
            }
        },
        "Collapsed" to {
            Demo(false) { expanded, onChange ->
                AppAccordion("Diagnostics", expanded = expanded, onToggle = { onChange(!expanded) }) {
                    AppText("Logs, versions, the device id.", role = TextRole.Secondary)
                }
            }
        },
    ),
    entry(
        "toast",
        "AppToast",
        GalleryGroup.Overlay,
        "Something that already happened. Above every layer; only an error waits to be read.",
        "Success" to { AppToast("Order placed", tone = ToastTone.Success) },
        "With an action" to { AppToast("Item moved to your wishlist", actionLabel = "Undo", onAction = {}) },
        "Error" to { AppToast("Printer not responding", tone = ToastTone.Error) },
        "Info" to { AppToast("Syncing in the background", tone = ToastTone.Info) },
        "Warning" to { AppToast("Two items are low on stock", tone = ToastTone.Warning) },
    ),
    entry(
        "tooltip",
        "AppTooltip",
        GalleryGroup.Overlay,
        "A label for an icon that has none, on a long press and on hover. Every AppIconButton carries one by default; never anything you cannot continue without.",
        "On an icon — long-press" to {
            AppTooltip(text = "Split the bill") {
                AppIconButton(icon = Icons.Filled.Add, contentDescription = "Split the bill", onClick = {
                }, tooltip = false)
            }
        },
    ),
    entry(
        "topbar",
        "AppTopBar",
        GalleryGroup.Navigation,
        "Where I am, and at most three things I can do. A tab root carries no up arrow.",
        "Tab root" to { AppTopBar("Settings") },
        "With up" to { AppTopBar("Permissions", onNavigateUp = {}) },
    ),
    entry(
        "tabs",
        "AppTabs",
        GalleryGroup.Navigation,
        "Switching content inside a screen. Takes a longer label and a count than segmented.",
        "With a badge" to {
            Demo(0) { selected, onSelect ->
                AppTabs(listOf(TabItem("All", badge = 4), TabItem("Unread"), TabItem("Archived")), selected, onSelect)
            }
        },
    ),
    entry(
        "bottomnav",
        "AppBottomNav",
        GalleryGroup.Navigation,
        "At most five destinations. The selected one always shows its label.",
        "Three destinations" to {
            Demo(0) { selected, onSelect ->
                AppBottomNav(
                    listOf(
                        NavItem("Home", Icons.Filled.Home),
                        NavItem("Orders", Icons.AutoMirrored.Filled.List, badge = 3),
                        NavItem("Settings", Icons.Filled.Settings),
                    ),
                    selected,
                    onSelect,
                )
            }
        },
        "A badge that counts up" to {
            Demo(0) { orders, onChange ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppBottomNav(
                        listOf(
                            NavItem("Home", Icons.Filled.Home),
                            NavItem("Orders", Icons.AutoMirrored.Filled.List, badge = orders),
                            NavItem("Settings", Icons.Filled.Settings),
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
                        AppButton("New order", { onChange(orders + 1) }, kind = ButtonKind.Outline)
                        AppButton("Clear", { onChange(0) }, kind = ButtonKind.Ghost)
                    }
                }
            }
        },
    ),
    entry(
        "navrail",
        "AppNavRail",
        GalleryGroup.Navigation,
        "The same destinations, down the side, for the wider classes.",
        "Two destinations" to {
            Demo(1) { selected, onSelect ->
                AppNavRail(
                    listOf(
                        NavItem("Home", Icons.Filled.Home),
                        NavItem("Orders", Icons.AutoMirrored.Filled.List, badge = 3),
                    ),
                    selected,
                    onSelect,
                )
            }
        },
    ),
    entry(
        "pager",
        "AppPager",
        GalleryGroup.Navigation,
        "Pages read in order and then left — a first-run tour. Dots say how many, not what.",
        "Three pages" to {
            AppPager(pageCount = 3) { page ->
                AppText("Page ${page + 1} of 3", role = TextRole.Title)
            }
        },
        "Two pages" to {
            AppPager(pageCount = 2) { page ->
                AppText(if (page == 0) "Swipe left" else "And back again", role = TextRole.Title)
            }
        },
    ),
    entry(
        "toolbar",
        "AppToolbar",
        GalleryGroup.Navigation,
        "Appears when something is selected. Inverted, because it is a different mode.",
        "Three selected" to {
            AppToolbar("3 selected") {
                AppCheckbox(CheckState.Indeterminate, {}, "All", inverse = true)
                AppIconButton(Icons.Filled.Delete, "Delete", {}, kind = IconButtonKind.Inverse)
            }
        },
    ),
    entry(
        "bottomactionbar",
        "AppBottomActionBar",
        GalleryGroup.Navigation,
        "A screen's one main action, anchored. Total left, action right and wider.",
        "With a total" to {
            AppBottomActionBar(label = "TOTAL", value = "1 248,00", actionLabel = "Pay", onAction = {})
        },
        "Action only" to { AppBottomActionBar(actionLabel = "Continue", onAction = {}) },
    ),
    entry(
        "datefield",
        "AppDateField",
        GalleryGroup.Form,
        "A date from the platform's picker. Never typed — 01/02 is January here and February there.",
        "With a value" to {
            Demo<LocalDate?>(LocalDate.of(2026, 9, 9)) { value, onChange ->
                AppDateField(value, { onChange(it) }, label = "Delivery")
            }
        },
        "Empty" to {
            Demo<LocalDate?>(null) { value, onChange ->
                AppDateField(value, { onChange(it) }, label = "Closed until")
            }
        },
        "Time" to {
            Demo<LocalTime?>(LocalTime.of(19, 24)) { value, onChange ->
                AppTimeField(value, { onChange(it) }, label = "Opened")
            }
        },
        "Disabled" to { AppTimeField(null, {}, label = "Last order", enabled = false) },
    ),
    entry(
        "fieldgroup",
        "AppFieldGroup",
        GalleryGroup.Form,
        "Several fields that are one answer, in one frame — an address, a card.",
        "An address" to {
            AppFieldGroup(label = "Delivery address", helperText = "Where the order goes") {
                AppFieldGroupRow {
                    Demo("Nádražní 12") { v, on -> AppTextField(v, on, frame = false) }
                }
                AppFieldGroupRow {
                    Demo("Praha 5") { v, on -> AppTextField(v, on, frame = false) }
                }
                AppFieldGroupRow(last = true) {
                    Demo("150 00") { v, on -> AppTextField(v, on, frame = false, numeric = true) }
                }
            }
        },
        "In error — the group carries it, not each row" to {
            AppFieldGroup(label = "Card", errorText = "This card has expired") {
                AppFieldGroupRow { AppText("4242 4242 4242 4242", role = TextRole.Numeric) }
                AppFieldGroupRow(last = true) { AppText("01 / 24", role = TextRole.Numeric) }
            }
        },
    ),
    entry(
        "scrollshadow",
        "AppScrollShadow",
        GalleryGroup.Content,
        "The cue that a list carries on past the fold. Present only while there is more.",
        "Both edges of a short list" to {
            val scroll = rememberScrollState()
            // Two rows tall, so there is always more of the list than there is room for it.
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * 2)) {
                Column(modifier = Modifier.verticalScroll(scroll)) {
                    repeat(8) { AppListItem("Row ${it + 1}") }
                }
                AppScrollShadow(scroll, ScrollEdge.Top, Modifier.align(Alignment.TopCenter))
                AppScrollShadow(scroll, ScrollEdge.Bottom, Modifier.align(Alignment.BottomCenter))
            }
        },
    ),
    entry(
        "menu",
        "AppMenu",
        GalleryGroup.Overlay,
        "Actions on the thing you opened it from. Destructive items sort to the bottom.",
        "Opens from its anchor" to {
            var expanded by remember { mutableStateOf(false) }
            AppMenu(
                expanded = expanded,
                onDismiss = { expanded = false },
                items = listOf(
                    MenuItem("Rename") { expanded = false },
                    MenuItem("Duplicate") { expanded = false },
                    MenuItem("Delete", destructive = true) { expanded = false },
                ),
                anchor = {
                    AppButton("Actions", { expanded = true }, kind = ButtonKind.Outline)
                },
            )
        },
    ),
    entry(
        "sheet",
        "AppSheet",
        GalleryGroup.Overlay,
        "The default way to open a detail on a phone. The handle is always drawn.",
        "Opens over the screen" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Open the sheet", { open = true })
            if (open) {
                AppSheet(onDismiss = { open = false }, title = "Appearance") {
                    AppText("Everything inside a sheet is in a window of its own.", role = TextRole.Secondary)
                    AppButton("Close", { open = false }, kind = ButtonKind.Neutral)
                }
            }
        },
    ),
    entry(
        "dialog",
        "AppDialog",
        GalleryGroup.Overlay,
        "The top layer. The confirming button carries a verb — \"Delete trip\", never \"OK\".",
        "Dialog with actions" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Open the dialog", { open = true })
            if (open) {
                AppDialog(
                    title = "Delete this trip?",
                    message = "Its itinerary and notes go with it.",
                    onDismiss = { open = false },
                    actions = {
                        AppButton("Cancel", { open = false }, kind = ButtonKind.Ghost)
                        AppButton("Delete trip", { open = false }, kind = ButtonKind.Destructive)
                    },
                )
            }
        },
        "Confirm dialog — the gap is deliberate" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Open the confirm dialog", { open = true }, kind = ButtonKind.Destructive)
            if (open) {
                AppConfirmDialog(
                    title = "Delete this trip?",
                    message = "Its itinerary and notes go with it. This cannot be undone.",
                    confirmLabel = "Delete trip",
                    onConfirm = { open = false },
                    onDismiss = { open = false },
                )
            }
        },
    ),
    entry(
        "screenChrome",
        "AppScreenChrome",
        GalleryGroup.Status,
        "What Screen() draws around every feature: the base surface, the loading overlay and the " +
            "empty and error states. A feature never composes these — it puts a ContentState or a " +
            "LoadingState in its UiState and this renders it.",
        "Error, with a retry" to {
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * CHROME_DEMO_ROWS)) {
                AppScreenChrome.ContentMessage(
                    state = ContentState.Error(message = "Nothing came back. Try again.".toUiText()),
                    onAction = {},
                    modifier = Modifier,
                )
            }
        },
        "Empty" to {
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * CHROME_DEMO_ROWS)) {
                AppScreenChrome.ContentMessage(
                    state = ContentState.Empty(message = "No orders on this table yet.".toUiText()),
                    onAction = {},
                    modifier = Modifier,
                )
            }
        },
        "Loading overlay" to {
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * CHROME_DEMO_ROWS)) {
                AppScreenChrome.LoadingOverlay(state = LoadingState(), modifier = Modifier)
            }
        },
        "Loading overlay, worded" to {
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * CHROME_DEMO_ROWS)) {
                AppScreenChrome.LoadingOverlay(
                    state = LoadingState(message = "Saving the photo".toUiText()),
                    modifier = Modifier,
                )
            }
        },
    ),
    entry(
        "banner",
        "AppBanner",
        GalleryGroup.Status,
        "One line above the screen, in a feedback role, for a fact that stays true until it stops: " +
            "the device is offline. Not a toast — nothing happened, something is so.",
        "Warning — offline" to { AppBanner(text = "You're offline") },
        "Info" to { AppBanner(text = "Synced a minute ago", tone = BannerTone.Info) },
        "Success" to { AppBanner(text = "All receipts printed", tone = BannerTone.Success) },
        "Error" to { AppBanner(text = "The printer is out of paper", tone = BannerTone.Error) },
        "Arrives and leaves" to {
            Demo(true) { visible, onChange ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppBanner(text = "You're offline", visible = visible)
                    AppButton(if (visible) "Back online" else "Go offline", {
                        onChange(!visible)
                    }, kind = ButtonKind.Outline)
                }
            }
        },
    ),
    entry(
        "pulltorefresh",
        "AppPullToRefresh",
        GalleryGroup.Content,
        "A list that reloads when pulled. The screen says when the reload is done; the indicator follows.",
        "Pull the list" to {
            var refreshing by remember { mutableStateOf(false) }
            // A demo has nothing to fetch, so the pull spins for a moment and settles by itself.
            LaunchedEffect(refreshing) {
                if (refreshing) {
                    delay(DEMO_REFRESH_MILLIS)
                    refreshing = false
                }
            }
            AppPullToRefresh(
                refreshing = refreshing,
                onRefresh = { refreshing = true },
                modifier = Modifier.height(AppTheme.density.listRowHeight * 4),
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    repeat(8) { AppListItem("Row ${it + 1}") }
                }
            }
        },
        "Refreshing" to {
            AppPullToRefresh(
                refreshing = true,
                onRefresh = {},
                modifier = Modifier.height(AppTheme.density.listRowHeight * 3),
            ) {
                Column { repeat(3) { AppListItem("Row ${it + 1}") } }
            }
        },
    ),
    entry(
        "sparkline",
        "AppSparkline",
        GalleryGroup.Content,
        "A trend as one or two lines, no axes, with an optional 0–1 series as columns under them.",
        "Highs and lows, as a week is read" to {
            AppSparkline(
                values = listOf(18f, 19f, 22f, 20f, 17f, 16f, 19f),
                values2 = listOf(9f, 10f, 12f, 11f, 8f, 7f, 9f),
                contentDescription = "Daily highs and lows from 7 to 22 degrees over the week",
                highlightIndex = 0,
            )
        },
        "A day, with rain under it" to {
            AppSparkline(
                values = listOf(12f, 13f, 15f, 18f, 20f, 19f, 16f, 14f),
                bars = listOf(0f, 0.1f, 0.3f, 0.8f, 0.9f, 0.4f, 0.1f, 0f),
                contentDescription = "Hourly temperature over the day, with rain probability",
                highlightIndex = 0,
            )
        },
        "Rising, with today marked" to {
            AppSparkline(
                values = listOf(12f, 14f, 13f, 17f, 19f, 18f, 22f),
                contentDescription = "Rising from 12 to 22 degrees over the week",
                highlightIndex = 4,
            )
        },
        "Falling" to {
            AppSparkline(
                values = listOf(22f, 21f, 17f, 15f, 15f, 12f, 9f),
                contentDescription = "Falling from 22 to 9 degrees over the week",
            )
        },
        "Flat" to {
            AppSparkline(
                values = listOf(16f, 16f, 16f, 16f, 16f, 16f, 16f),
                contentDescription = "A flat 16 degrees all week",
            )
        },
    ),
    entry(
        "drawer",
        "AppDrawer",
        GalleryGroup.Navigation,
        "The main menu: every place in the app, in sections, over the screen. The five-tab bar is the five most-used; this is all of them.",
        "Opens over the screen" to {
            var open by remember { mutableStateOf(false) }
            // Six rows of drawer: a header-less list of four doors, and room for the scrim to read.
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * DRAWER_DEMO_ROWS)) {
                AppDrawer(
                    open = open,
                    onOpenChange = { open = it },
                    sections = listOf(
                        DrawerSection(
                            "Places",
                            listOf(
                                DrawerItem("home", "Home", Icons.Filled.Home),
                                DrawerItem("catalog", "Catalog", Icons.AutoMirrored.Filled.List),
                                DrawerItem("cart", "Cart", Icons.Filled.ShoppingCart, badge = 3),
                            ),
                        ),
                        DrawerSection("You", listOf(DrawerItem("settings", "Settings", Icons.Filled.Settings))),
                    ),
                    selectedId = "home",
                    onSelect = {},
                ) {
                    AppButton("Open the menu", { open = true })
                }
            }
        },
    ),
    entry(
        "icon",
        "AppIcon",
        GalleryGroup.Content,
        "An icon that is not a control — the glyph on a door, the mark beside a row — at one of the theme's three sizes.",
        "The three sizes" to {
            Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
                AppIcon(icon = Icons.Filled.Home, contentDescription = null, size = AppTheme.icons.sm)
                AppIcon(icon = Icons.Filled.Home, contentDescription = null)
                AppIcon(icon = Icons.Filled.Home, contentDescription = null, size = AppTheme.icons.lg)
            }
        },
    ),
    entry(
        "fabMenu",
        "AppFabMenu",
        GalleryGroup.Action,
        "A speed dial: one key that opens into a few, for a screen that creates more than one thing. The actions stagger up as labelled mini keys over a scrim.",
        "Open" to {
            var expanded by remember { mutableStateOf(true) }
            Box(modifier = Modifier.height(AppTheme.density.listRowHeight * DRAWER_DEMO_ROWS)) {
                AppFabMenu(
                    icon = Icons.Filled.Add,
                    contentDescription = "Create",
                    items = listOf(
                        FabAction("New trip", Icons.Filled.DateRange) {},
                        FabAction("New item", Icons.Filled.Add) {},
                        FabAction("Add a place", Icons.Filled.Home) {},
                    ),
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    ),
    entry(
        "reveal",
        "AppReveal",
        GalleryGroup.Content,
        "Something that comes and goes on a condition, arriving and leaving on the tokens rather than " +
            "popping on an `if`: a key while nothing is selected, a toolbar while something is.",
        "Fades and scales" to {
            Demo(true) { shown, onChange ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppButton(if (shown) "Hide" else "Show", { onChange(!shown) }, kind = ButtonKind.Outline)
                    AppReveal(visible = shown) {
                        AppFab(icon = Icons.Filled.Add, contentDescription = "New item", onClick = {})
                    }
                }
            }
        },
        "Slides from the top" to {
            Demo(true) { shown, onChange ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppButton(if (shown) "Clear selection" else "Select", {
                        onChange(!shown)
                    }, kind = ButtonKind.Outline)
                    AppReveal(visible = shown, slide = true) {
                        AppToolbar(title = "2 selected")
                    }
                }
            }
        },
    ),
    entry(
        "switcher",
        "AppSwitcher",
        GalleryGroup.Content,
        "Content that switches with a selection and travels rather than swaps: a tab's panel, a " +
            "wizard's step. The one AnimatedContent a feature composes.",
        "Slides with the tabs" to {
            Demo(0) { tab, onSelect ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppTabs(
                        tabs = listOf(TabItem("Today"), TabItem("Week"), TabItem("Month")),
                        selectedIndex = tab,
                        onSelect = onSelect,
                    )
                    AppSwitcher(target = tab) { index ->
                        AppText(text = listOf("Sunny, 24°", "Rain on Thursday", "A warm month")[index])
                    }
                }
            }
        },
        "Cross-fades" to {
            Demo(0) { shown, onChange ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppButton("Toggle", { onChange(1 - shown) }, kind = ButtonKind.Outline)
                    AppSwitcher(target = shown, slide = false) { index ->
                        AppText(text = if (index == 0) "Results" else "Nothing here yet", role = TextRole.Secondary)
                    }
                }
            }
        },
    ),
    entry(
        "counter",
        "AppCounter",
        GalleryGroup.Content,
        "A figure that rolls when it changes rather than swapping in a frame: a digit that grows " +
            "rolls up, one that shrinks rolls down, and 9 to 10 adds a cell without moving one.",
        "Count up" to {
            Demo(8) { count, onChange ->
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                    AppCounter(text = count.toString(), role = TextRole.DisplayLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.inline.md)) {
                        AppButton("Count up", { onChange(count + 1) }, kind = ButtonKind.Outline)
                        AppButton("Reset", { onChange(8) }, kind = ButtonKind.Ghost)
                    }
                }
            }
        },
        "A total" to { AppCounter(text = "1 248,00") },
        "Words roll too" to { AppCounter(text = "2 items", role = TextRole.Secondary) },
    ),
    entry(
        "actionSheet",
        "AppActionSheet",
        GalleryGroup.Overlay,
        "Actions on the thing you long-pressed, from below; a centred card on a wide window.",
        "Opens over the screen" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Long-pressed a row", { open = true })
            if (open) {
                AppActionSheet(
                    title = "Fight Club",
                    onDismiss = { open = false },
                    items = listOf(
                        MenuItem("Open", icon = Icons.Filled.Edit) {},
                        MenuItem("Share", icon = Icons.Filled.Share) {},
                        MenuItem("Remove", destructive = true, icon = Icons.Filled.Delete) {},
                    ),
                )
            }
        },
    ),
    entry(
        "pickerSheet",
        "AppPickerSheet",
        GalleryGroup.Overlay,
        "One choice from a short list, from below — a sort order, a view; a choice closes it.",
        "Opens over the screen" to {
            var open by remember { mutableStateOf(false) }
            var selected by remember { mutableIntStateOf(0) }
            val options = listOf("Name", "Price", "Acquired")
            AppButton("Sort by ${options[selected]}", { open = true })
            if (open) {
                AppPickerSheet(
                    title = "Sort by",
                    options = options,
                    selectedIndex = selected,
                    onSelect = { selected = it },
                    onDismiss = { open = false },
                )
            }
        },
    ),
    entry(
        "chip",
        "AppChip",
        GalleryGroup.Form,
        "A pill you can press — a filter, a recent search. AppTag is the same shape and cannot be.",
        "Toggles" to {
            var selected by remember { mutableStateOf(true) }
            AppChip(label = "Fragile", selected = selected, onClick = { selected = !selected })
        },
        "With a leading icon" to
            { AppChip(label = "Insured", selected = false, onClick = {}, leadingIcon = Icons.Filled.Star) },
        "Disabled" to { AppChip(label = "Sold", selected = false, onClick = {}, enabled = false) },
    ),
    entry(
        "chipGroup",
        "AppChipGroup",
        GalleryGroup.Form,
        "Chips that wrap; the caller holds the chosen set, and `single` makes it a choice of one.",
        "Multi — the tags of a filter" to {
            var selected by remember { mutableStateOf(setOf(0, 2)) }
            AppChipGroup(
                options = listOf("Fragile", "Lent out", "Insured", "Needs repair", "Boxed"),
                selected = selected,
                onSelectionChange = { selected = it },
            )
        },
        "Single — a view" to {
            var selected by remember { mutableStateOf(setOf(0)) }
            AppChipGroup(
                options = listOf("All", "Mine", "Lent out"),
                selected = selected,
                onSelectionChange = { selected = it },
                single = true,
            )
        },
    ),
    entry(
        "reorder",
        "AppReorder",
        GalleryGroup.Content,
        "The handle a row is dragged by. A long press on it lifts the row; the row itself stays " +
            "scrollable, and the caller owns the order.",
        "Three rows to drag" to { ReorderDemo() },
    ),
    entry(
        "coachMark",
        "AppCoachMark",
        GalleryGroup.Overlay,
        "A tour's one step: everything dimmed but the element being talked about, and a card " +
            "saying what it is. The hole is over a real element, which registered itself.",
        "A live tour over three buttons" to { CoachMarkDemo() },
    ),
    entry(
        "codeField",
        "AppCodeField",
        GalleryGroup.Form,
        "A one-time code: six boxes over one input, so a paste or the SMS autofill fills them all.",
        "Typing" to {
            Demo("42") { value, onChange -> AppCodeField(value, onChange, label = "Code") }
        },
        "Wrong code" to {
            AppCodeField(value = "123456", onValueChange = {}, label = "Code", errorText = "That code is not right")
        },
    ),
    entry(
        "imageViewer",
        "AppImageViewer",
        GalleryGroup.Overlay,
        "Pictures full screen: swipe between them, pinch to zoom and pan, close. A window of its own.",
        "Open the viewer" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Open two pictures", { open = true })
            if (open) AppImageViewer(images = listOf(null, null), onClose = { open = false })
        },
    ),
    entry(
        "htmlText",
        "AppHtmlText",
        GalleryGroup.Content,
        "A server's HTML read as markup: bold, a list, a link the caller opens in a Custom Tab.",
        "A description" to {
            AppHtmlText(
                html = "Freshly <b>ground</b>, brewed to order.<ul><li>Arabica</li><li>Light roast</li></ul>" +
                    "Read <a href=\"https://example.com\">where it grows</a>.",
            )
        },
    ),
    entry(
        "promptDialog",
        "AppPromptDialog",
        GalleryGroup.Overlay,
        "A question whose answer is a line of text. Confirm waits for a value the validator passes.",
        "Rename, with a required name" to {
            var open by remember { mutableStateOf(false) }
            var name by remember { mutableStateOf("Lisbon") }
            AppButton("Rename \"$name\"", { open = true })
            if (open) {
                AppPromptDialog(
                    title = "Rename this trip",
                    confirmLabel = "Rename",
                    onConfirm = {
                        name = it
                        open = false
                    },
                    onDismiss = { open = false },
                    label = "Name",
                    placeholder = "Summer in Porto",
                    initialValue = name,
                    validator = required("Give it a name".toUiText()),
                )
            }
        },
    ),
    entry(
        "choiceDialog",
        "AppChoiceDialog",
        GalleryGroup.Overlay,
        "A choice among a few options, in a dialog: radios for one, checkboxes for several. A long list is AppPickerSheet.",
        "One — sort by" to {
            var open by remember { mutableStateOf(false) }
            var sort by remember { mutableStateOf(listOf("name")) }
            AppButton("Sort by ${sort.joinToString()}", { open = true })
            if (open) {
                AppChoiceDialog(
                    title = "Sort by",
                    options = GALLERY_SORT_OPTIONS,
                    confirmLabel = "Sort",
                    onConfirm = {
                        sort = it
                        open = false
                    },
                    onDismiss = { open = false },
                    initialSelection = sort.toSet(),
                )
            }
        },
        "Several — remind me" to {
            var open by remember { mutableStateOf(false) }
            var reminders by remember { mutableStateOf(listOf<String>()) }
            AppButton("Remind me: ${reminders.size}", { open = true })
            if (open) {
                AppChoiceDialog(
                    title = "Remind me",
                    options = GALLERY_REMIND_OPTIONS,
                    confirmLabel = "Save",
                    onConfirm = {
                        reminders = it
                        open = false
                    },
                    onDismiss = { open = false },
                    multiple = true,
                    initialSelection = reminders.toSet(),
                )
            }
        },
    ),
    entry(
        "permissionRationale",
        "AppPermissionRationale",
        GalleryGroup.Overlay,
        "Why the app wants a permission, before it asks again — and Open settings once it was refused for good.",
        "Before asking again" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Explain notifications", { open = true })
            if (open) {
                AppPermissionRationale(
                    icon = Icons.Filled.Notifications,
                    title = "Allow notifications?",
                    message = "We tell you when an order ships, and nothing else.",
                    refusedForGood = false,
                    onContinue = { open = false },
                    onNotNow = { open = false },
                )
            }
        },
        "Refused for good" to {
            var open by remember { mutableStateOf(false) }
            AppButton("Explain, refused for good", { open = true })
            if (open) {
                AppPermissionRationale(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications are off",
                    message = "Turn them on in the app's settings to hear when an order ships.",
                    refusedForGood = true,
                    onContinue = { open = false },
                    onNotNow = { open = false },
                )
            }
        },
    ),
    entry(
        "cardField",
        "AppCardField",
        GalleryGroup.Form,
        "A payment card: number grouped in fours with its brand, expiry and a hidden code, each " +
            "with its autofill hint. A wrong check digit or a past expiry is the field's error.",
        "Typing" to {
            Demo(AppCardFieldState()) { value, onChange -> AppCardField(value, onChange) }
        },
        "Declined by the field" to {
            AppCardField(
                value = AppCardFieldState(number = "4242424242424241", expiry = "0120", code = "12"),
                onValueChange = {},
            )
        },
    ),
    entry(
        "phoneField",
        "AppPhoneField",
        GalleryGroup.Form,
        "A phone number and its country: the flag and dial code inside the frame open a sheet of " +
            "regions, and the number is formatted for the region as it is typed.",
        "Typing" to {
            Demo(AppPhoneFieldState.PREVIEW) { value, onChange -> AppPhoneField(value, onChange, label = "Phone") }
        },
        "Another region" to {
            AppPhoneField(value = AppPhoneFieldState(region = "US", number = "2025550123"), onValueChange = {
            }, label = "Phone")
        },
    ),
    entry(
        "imageCropper",
        "AppImageCropper",
        GalleryGroup.Content,
        "A picture under a square frame, panned and pinched until the frame holds the part wanted — the avatar crop.",
        // Picsum is public and keyless; with no network the frame holds its skeleton.
        "A landscape photo" to { AppImageCropper(model = "https://picsum.photos/id/1015/900/600", onCropChange = {}) },
    ),
    // create_component.py appends a starter entry here; doctor.py fails on a component with none.
)

/**
 * A tour the gallery can actually walk: three targets, and Next moving the hole from one to the
 * next. Skip puts it back at the start, because a gallery page nobody can restart is a page that
 * works once.
 */
@Composable
private fun CoachMarkDemo() {
    val targets = rememberCoachTargets()
    var step by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(LocalCoachTargets provides targets) {
        Box(modifier = Modifier.fillMaxWidth().height(AppTheme.density.listRowHeight * COACH_DEMO_ROWS)) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack.sm)) {
                COACH_DEMO_STEPS.forEach { demo ->
                    AppButton(
                        label = demo.title,
                        onClick = {},
                        modifier = Modifier.appCoachTarget(demo.targetId),
                    )
                }
            }
            AppCoachMark(
                step = COACH_DEMO_STEPS[step],
                stepIndex = step,
                stepCount = COACH_DEMO_STEPS.size,
                onNext = { step = (step + 1) % COACH_DEMO_STEPS.size },
                onSkip = { step = 0 },
            )
        }
    }
}

private val COACH_DEMO_STEPS = listOf(
    CoachStep("gallery_coachWeather", "Weather", "Everywhere you keep an eye on."),
    CoachStep("gallery_coachCart", "Cart", "What you are about to buy."),
    CoachStep("gallery_coachSettings", "Settings", "The theme, the language, and the way out."),
)

/** Tall enough for the three targets and the card under them. */
private const val COACH_DEMO_ROWS = 7

/** A list the gallery can actually drag: the rows are the caller's state, which is the point. */
@Composable
private fun ReorderDemo() {
    var rows by remember { mutableStateOf(listOf("Praha", "Brno", "Ostrava")) }
    val state = rememberReorderState { from, to ->
        rows = rows.toMutableList().apply { add(to, removeAt(from)) }
    }
    LazyColumn(
        state = state.listState,
        modifier = Modifier.height(AppTheme.density.listRowHeight * REORDER_DEMO_ROWS),
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

/** The demo is exactly its three rows tall, so the page has no empty strip under it. */
private const val REORDER_DEMO_ROWS = 3

/**
 * The screen's half of a stateless component, for a demo.
 *
 * Every component here takes its value and hands changes back — a screen holds the state. A demo
 * that passes a constant and an empty callback is therefore something to look at and not to try:
 * tapping it changes nothing. This holds the value for it, so a checkbox in the gallery toggles the
 * way it does on a real screen. Only the disabled variants stay constant, which is their point.
 */
@Composable
private fun <T> Demo(
    initial: T,
    content: @Composable (value: T, onChange: (T) -> Unit) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    content(value) { value = it }
}

@Composable
private fun CheckboxDemo(initial: CheckState, label: String) = Demo(initial) { checked, onChange ->
    AppCheckbox(checked, { onChange(if (it) CheckState.On else CheckState.Off) }, label)
}

/**
 * The Motion group: the nine components that move, each shown again with a Replay on every
 * variant.
 *
 * A second look rather than a second entry written by hand — the variants are the component's own,
 * wrapped in a [MotionReplay] whose key throws the content away and composes it again. So a
 * variant added to `AppSwitch` above is replayable here without anyone remembering to add it, and
 * the gallery never learns what any of these nine actually does.
 *
 * The id is prefixed, because two entries with one id is a `LazyColumn` that crashes on a
 * duplicate key rather than a list with a repeat in it.
 */
private fun motionEntries(): List<GalleryEntry> = MOVING_COMPONENTS.mapNotNull { id ->
    componentEntries.firstOrNull { it.id == id }?.let { source ->
        GalleryEntry(
            id = "motion_$id",
            name = source.name,
            group = GalleryGroup.Motion,
            summary = source.summary,
            variants = source.variants.map { variant ->
                GalleryVariant(variant.label) { MotionReplay { variant.content() } }
            },
        )
    }
}

/** The nine that move. `COMPONENTS.md`'s Moves column is the list this is kept against. */
private val MOVING_COMPONENTS = listOf(
    "switch",
    "checkbox",
    "accordion",
    "pager",
    "progress",
    "skeleton",
    "counter",
    "fabMenu",
    "switcher",
)

/**
 * Every component in `:core:ui`, the foundation pages the components are made of, and the Motion
 * group's second look at the ones that move.
 *
 * The pages come first because the roots are what the leaves are cut from; the replays come last
 * because each is a component already listed above under its own group.
 */
val galleryCatalog: List<GalleryEntry> = foundationPages + componentEntries + motionEntries()

/** Everything the gallery list needs, without the composables. */
fun galleryItems(): List<GalleryItem> = galleryCatalog.map {
    GalleryItem(id = it.id, name = it.name, group = it.group, summary = it.summary)
}

/** Tall enough that an overlay reads as an overlay rather than as a strip. */
private const val CHROME_DEMO_ROWS = 3

/** Tall enough for the drawer's four doors and the divider between its two sections. */
private const val DRAWER_DEMO_ROWS = 6

/** Long enough to see the indicator spin, short enough not to look stuck. */
private const val DEMO_REFRESH_MILLIS = 1_200L

fun galleryEntry(id: String): GalleryEntry? = galleryCatalog.firstOrNull { it.id == id }

private val GALLERY_SORT_OPTIONS = listOf(
    ChoiceOption("name", "Name"),
    ChoiceOption("price", "Price"),
    ChoiceOption("added", "Date added"),
)

private val GALLERY_REMIND_OPTIONS = listOf(
    ChoiceOption("day", "A day before"),
    ChoiceOption("hour", "An hour before"),
    ChoiceOption("start", "When it starts"),
)
