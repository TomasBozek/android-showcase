#!/usr/bin/env python3
"""
Scaffolds a Compose component with its preview.

    python3 scripts/create_component.py PrimaryButton --group action
    python3 scripts/create_component.py ProductCard --feature catalog
    python3 scripts/create_component.py EmptyState --group content --state
    python3 scripts/create_component.py Badge --feature catalog --sub product --dry-run

With no `--feature` the component lands in `:core:ui`, where every feature can reach it, in the
group package `--group` names — `component/action/`, `component/field/` and so on, one per directory
already there, so seventy components are not one flat directory. Pass
`--feature` for one that belongs to a single feature and should not be shared; it lands in that
module's `component/` package, never beside a screen.

Unlike `create_feature.py` and `create_screen.py`, this generates from templates held in this file
rather than cloning `feature/template` — the same choice `create_datasource.py` makes. A component
has no counterpart in the template module, and adding a fake one there would ship a placeholder
component in the app.

What it produces is deliberately unopinionated: a `modifier` parameter first (the convention
`doctor.py` enforces), a `@ComponentPreview`, and a TODO where the real content goes.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _common import (  # noqa: E402
    BASE_PACKAGE,
    BASE_PATH,
    REPO_ROOT,
    feature_source_dir,
    register_in_gallery,
    register_in_playground,
    to_flat,
    to_pascal,
    write_file,
)

CORE_UI_DIR = REPO_ROOT / "core/ui/src/main/kotlin" / BASE_PATH / "core/ui"
CORE_UI_PACKAGE = f"{BASE_PACKAGE}.core.ui"

PREVIEW_PACKAGE = f"{BASE_PACKAGE}.core.ui.common"
COMPONENT_PACKAGE = f"{BASE_PACKAGE}.core.ui.component"
# Every generated component builds from AppText, so it imports from here unless it lands here.
TEXT_PACKAGE = f"{COMPONENT_PACKAGE}.content"
COMPONENT_DIR = CORE_UI_DIR / "component"


def component_groups() -> list[str]:
    """The group packages under `:core:ui`'s `component/` — a new group is a new directory."""
    if not COMPONENT_DIR.is_dir():
        return []
    return sorted(path.name for path in COMPONENT_DIR.iterdir() if path.is_dir())


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scaffold a Compose component and its preview.",
        epilog=(
            "Examples:\n"
            "  python3 scripts/create_component.py PrimaryButton --group action\n"
            "  python3 scripts/create_component.py ProductCard --feature catalog --state"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("name", help="Component name in PascalCase, e.g. PrimaryButton")
    parser.add_argument(
        "--feature",
        default=None,
        help="Put it in this feature's presentation module instead of :core:ui",
    )
    parser.add_argument(
        "--group",
        default=None,
        help=f"The :core:ui group package it belongs to, required without --feature: {', '.join(component_groups())}",
    )
    parser.add_argument("--sub", default="", help="Optional sub-package of a feature's component/, e.g. product")
    parser.add_argument(
        "--state",
        action="store_true",
        help="Also generate a <Name>State data class with a PREVIEW fixture, and take it as a parameter",
    )
    parser.add_argument("--dry-run", action="store_true", help="Show what would happen, change nothing.")
    parser.add_argument("--force", action="store_true", help="Overwrite files that already exist.")
    return parser.parse_args()


def resolve_target(args: argparse.Namespace) -> tuple[Path, str]:
    """Returns the directory to write into and the package to declare."""
    if args.feature:
        feature = to_flat(args.feature)
        presentation = feature_source_dir(feature, "presentation")
        if not presentation.is_dir():
            sys.exit(
                f"No presentation module at feature/{feature}/presentation. "
                f"Create the feature first: python3 scripts/create_feature.py {feature}"
            )
        # One `component/` per feature, beside the screens rather than inside one of them:
        # a screen-private composable and a feature-shared one would otherwise need two homes,
        # and the day one is used from a second screen it would have to move.
        directory = presentation / "component"
        package = f"{BASE_PACKAGE}.feature.{feature}.presentation.component"
    else:
        groups = component_groups()
        if args.group not in groups:
            sys.exit(
                f"A :core:ui component needs --group, one of: {', '.join(groups)}. "
                f"A group that does not exist yet is a directory under {COMPONENT_DIR.relative_to(REPO_ROOT)}."
            )
        directory = COMPONENT_DIR / args.group
        package = f"{COMPONENT_PACKAGE}.{args.group}"

    sub = args.sub.strip("/")
    if sub:
        directory = directory / sub
        package = f"{package}.{sub.replace('/', '.')}"
    return directory, package


def state_class(pascal: str) -> str:
    """The state a `--state` component takes, written into the component's own file: every file in
    a component package is read as a component, so a file of its own would need a preview, a
    gallery entry and a playground entry it cannot have. `AppCardField` keeps the shape."""
    return f"""
/**
 * What {pascal} renders. Immutable so Compose can skip it when nothing has changed.
 */
@Immutable
data class {pascal}State(
    // TODO: replace with this component's real state.
    val label: String,
) {{

    companion object {{

        val PREVIEW = {pascal}State(
            label = "{pascal}",
        )
    }}
}}
"""


def component_source(package: str, pascal: str, with_state: bool) -> str:
    parameter = f"state: {pascal}State" if with_state else "label: String"
    body_text = "state.label" if with_state else "label"
    preview_argument = f"state = {pascal}State.PREVIEW" if with_state else f'label = "{pascal}"'
    # Only a component that lands in AppText's own group skips the import. `common` sorts before
    # `component`, which is why these come second.
    design_imports = (
        ""
        if package == TEXT_PACKAGE
        else f"import {TEXT_PACKAGE}.AppText\nimport {TEXT_PACKAGE}.TextRole\n"
    )

    state_import = "import androidx.compose.runtime.Immutable\n" if with_state else ""
    state = state_class(pascal) if with_state else ""
    return f"""package {package}

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
{state_import}import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import {PREVIEW_PACKAGE}.ComponentPreview
import {PREVIEW_PACKAGE}.ThemedComponentPreview
{design_imports}
/**
 * TODO: say what this component shows and when to reach for it.
 *
 * `modifier` comes second and defaults to `Modifier` so the caller decides where this sits — the
 * convention `doctor.py` enforces. Keep it, and pass it to the outermost element.
 *
 * Build it from the design system: `AppText` and the rest of `:core:ui`'s components for content,
 * `AppTheme.colors` / `.typography` / `.spacing` / `.shapes` for anything else. Never a Material
 * widget, never a `.dp` literal, never a colour — `doctor.py` fails on all three.
 */
@Composable
fun {pascal}(
    {parameter},
    modifier: Modifier = Modifier,
) {{
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {{
        AppText(
            text = {body_text},
            role = TextRole.Body,
        )
    }}
}}

@ComponentPreview
@Composable
private fun Preview() = ThemedComponentPreview {{
    {pascal}({preview_argument})
}}
{state}"""


def emit(path: Path, content: str, force: bool, dry_run: bool) -> bool:
    if path.exists() and not force:
        print(f"  skipping {path.relative_to(REPO_ROOT)}: already exists (use --force)")
        return False
    write_file(path, content, dry_run)
    return True


def main() -> None:
    args = parse_args()

    pascal = to_pascal(args.name)
    directory, package = resolve_target(args)

    location = "core/ui" if not args.feature else f"feature/{to_flat(args.feature)}/presentation"
    print(f"Creating component '{pascal}' in {location}")
    print(f"Package: {package}")
    if args.dry_run:
        print("-- dry run, nothing will be written --")

    wrote = False
    wrote |= emit(
        directory / f"{pascal}.kt",
        component_source(package, pascal, args.state),
        args.force,
        args.dry_run,
    )

    if not wrote:
        sys.exit("Nothing was generated.")

    # A feature's own component is not in the gallery: the gallery lists `:core:ui`, which is the
    # set every feature may compose from.
    if not args.feature:
        # The call is the template's own preview call, so what the gallery runs is what the
        # component's signature takes; a `--state` component also needs its state class imported.
        call = f"{pascal}(state = {pascal}State.PREVIEW)" if args.state else f'{pascal}(label = "{pascal}")'
        imports = (f"{package}.{pascal}",) + ((f"{package}.{pascal}State",) if args.state else ())
        register_in_gallery(pascal, args.dry_run, call=call, imports=imports)
        register_in_playground(pascal, args.dry_run, call=call, imports=imports)

    print(
        "\nDone. A component needs no Koin binding and no nav entry — the gallery entry and the\n"
        "playground entry above are its registrations, and doctor.py fails without them. Replace\n"
        "the placeholder content, the entry's summary and the bench's knobs, then:\n"
        "  ./gradlew :core:ui:assembleDebug"
    )


if __name__ == "__main__":
    main()
