package com.example.androidproject1.feature.gallery.presentation

/**
 * The sections the gallery is read in, and the one list the other two are read from.
 *
 * The order here is the order on screen. [Foundations] comes first because the roots are what the
 * leaves are made of; the seven in the middle are `COMPONENTS.md`'s Group column, in that table's
 * own order, and `doctor.py` holds the two together; [Motion] comes last because its entries are
 * second appearances — a component that moves is already listed under the group it belongs to.
 *
 * An enum rather than a `String`: a string gave the gallery its groups in encounter order and let
 * any new entry invent a group of one.
 */
enum class GalleryGroup(val label: String) {

    /** A colour role, a type style, a spacing step, a shape, an elevation level, an icon, a token. */
    Foundations("Foundations"),

    /** Text, surfaces, lists, images — what a screen is mostly made of. */
    Content("Content"),

    /** Buttons and the things that are pressed. */
    Action("Action"),

    /** Fields, switches, pickers — everything that takes input. */
    Form("Form"),

    /** Bars, tabs, rails, drawers. */
    Navigation("Navigation"),

    /** Dialogs, sheets, menus, tooltips — anything drawn in a window of its own. */
    Overlay("Overlay"),

    /** The screen shell itself. */
    Shell("Shell"),

    /** Badges, banners, progress, skeletons — what the app says about itself. */
    Status("Status"),

    /** The components that move, with a replay on each variant. */
    Motion("Motion"),
    ;

    companion object {

        /**
         * The groups a `COMPONENTS.md` row may carry: every name but the two that label pages
         * rather than components. `doctor.py` reads this through the enum, so the tuple it used to
         * keep by hand is gone.
         */
        val componentGroups: List<GalleryGroup> = entries - Foundations - Motion
    }
}
