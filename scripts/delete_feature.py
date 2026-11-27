#!/usr/bin/env python3
"""
Removes a feature module and every registration `create_feature.py` made for it.

    python3 scripts/delete_feature.py userProfile
    python3 scripts/delete_feature.py userProfile --dry-run

This is the inverse of `create_feature.py`: it deletes `feature/<name>/`, the
`includeFeatureModule` block in `settings.gradle.kts`, the `:core:di` dependency, the Koin module
entry and the destinations registered in `AppNavHost.kt` — the same places that are easy to leave
half-edited by hand, which is what turns a throwaway experiment
into a broken build. And what `:app` holds for the feature: its `wiring/<flat>/` package in
every source set, the one `includes(...)` line `ApplicationModule` gives it, its bottom-bar tab and
its launcher shortcuts.

Then it names every `service/*` module no remaining module but `:core:di` declares, and
with `--prune-services` removes each — its directory, its `settings.gradle.kts` include, its row in
`ServiceModules.kt`, `:core:di`'s dependency and its `<name>ServiceModule` in `Koin.kt`.

A feature another module's build file still declares is refused before anything is written, and
those modules are named; `--force` deletes anyway. Cross-feature references in source (a lambda in
another destination, say) cannot be removed safely and are reported afterwards instead. A Maestro
flow that drives only this feature and the frame is deleted with it; a flow that names another
sample too, and a test outside the feature that imports it, are listed for a hand. Its `dev`
fixtures, `fixture_<flat>_*.json`, and `:app`'s `tab_<flat>` and `menu_<flat>` strings go too.
"""

from __future__ import annotations

import argparse
import re
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _common import (  # noqa: E402
    APP_NAV_HOST_FILE,
    BASE_PACKAGE,
    BASE_PATH,
    CORE_DI_BUILD_FILE,
    KOIN_FILE,
    REPO_ROOT,
    FRAME_FEATURES,
    TEMPLATE_FEATURE,
    edit_file,
    feature_koin_module_file,
    relative_to_repo,
    remove_lines,
    to_flat,
)

APP_SOURCES = REPO_ROOT / "app/src"
APPLICATION_MODULE_FILE = APP_SOURCES / "main/kotlin" / BASE_PATH / "ApplicationModule.kt"
TOP_LEVEL_DESTINATION_FILE = APP_SOURCES / "main/kotlin" / BASE_PATH / "TopLevelDestination.kt"

SERVICE_MODULES_FILE = REPO_ROOT / "build-logic/src/main/kotlin/ServiceModules.kt"
SERVICE_REFERENCE = re.compile(r'projects\.service\.(\w+)|project\(":service:(\w+)')

MAESTRO_DIR = REPO_ROOT / ".maestro"
RUN_FLOW = re.compile(r"runFlow:\s*(?:file:\s*)?\"?([\w./-]+\.yaml)")
TEST_SOURCE_SETS = {"test", "androidTest", "testFixtures"}

# A door in a feature's deep-link table: `"product" to …`.
DOOR = re.compile(r'^\s*"([\w-]+)" to\b', re.MULTILINE)

# Directories scanned for leftover references once the module is gone.
REFERENCE_ROOTS = ["app", "core", "feature", "service", "settings.gradle.kts"]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Delete a feature module and its registrations.",
        epilog=(
            'Examples:\n'
            '  python3 scripts/delete_feature.py userProfile\n'
            '  python3 scripts/delete_feature.py userProfile --dry-run\n'
            '\n'
            'Undoes the five registrations create_feature.py made, then greps for what it could not remove\n'
            'safely — a cross-feature navigation lambda, typically. Run doctor.py afterwards.'
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("name", help="Feature name, e.g. userProfile")
    parser.add_argument("--dry-run", action="store_true", help="Show what would happen, change nothing.")
    parser.add_argument(
        "--prune-services",
        action="store_true",
        help="Also remove every service module the delete leaves without a caller.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help=(
            "Allow deleting a frame feature, the template the generators clone from, or a "
            "feature other modules still declare."
        ),
    )
    return parser.parse_args()


def koin_module_class(flat: str) -> str | None:
    """The feature's Koin object name, read off disk rather than guessed from the feature name."""
    module_file = feature_koin_module_file(flat)
    return module_file.stem if module_file else None


def destination_functions(flat: str) -> list[str]:
    """
    The `xDestination` functions AppNavHost imports from this feature.

    Read from AppNavHost's own imports rather than from the feature's sources, so a screen in a
    sub-package or one named differently from the feature is still found.
    """
    if not APP_NAV_HOST_FILE.is_file():
        return []
    prefix = f"import {BASE_PACKAGE}.feature.{flat}.presentation"
    names = []
    for line in APP_NAV_HOST_FILE.read_text().split("\n"):
        if line.startswith(prefix):
            name = line.rsplit(".", 1)[-1].strip()
            if name.endswith("Destination") and name[0].islower():
                names.append(name)
    return names


def dependants(flat: str) -> list[str]:
    """
    The modules outside `feature/<flat>/` whose build file declares one of its layers.

    `:core:di`'s `api(...)` line is a registration this script removes, so it is not one.
    """
    needle = f"projects.feature.{flat}."
    found = []
    for build_file in sorted(REPO_ROOT.rglob("build.gradle.kts")):
        relative = build_file.parent.relative_to(REPO_ROOT)
        if "build" in relative.parts or relative.parts[:2] == ("feature", flat) or build_file == CORE_DI_BUILD_FILE:
            continue
        if needle in build_file.read_text(errors="ignore"):
            found.append(":" + ":".join(relative.parts))
    return found


def test_ids(directory: Path) -> set[str]:
    """The test ids a directory's shipped sources declare, read the way doctor.py reads them."""
    from doctor import SCREEN_ID_LITERAL, TAG_CONSTANT, TEST_TAG_LITERAL

    ids: set[str] = set()
    for path in directory.rglob("*.kt"):
        parts = path.relative_to(directory).parts
        if "src" not in parts or parts[parts.index("src") + 1] != "main":
            continue
        text = path.read_text(errors="ignore")
        for pattern in (TEST_TAG_LITERAL, SCREEN_ID_LITERAL, TAG_CONSTANT):
            ids |= set(pattern.findall(text))
    return ids


def classify_flows(flat: str) -> tuple[list[Path], list[Path]]:
    """
    The Maestro flows that are the feature's, and the ones that name it among other samples.

    An id is the feature's, another sample's, or the frame's — `:app`, `:core`, and the frame
    features, which every flow walks through to get anywhere. A flow is the feature's when
    it names the feature's ids and no other sample's: deleting the feature leaves it nothing to
    drive. One that names another sample's too, or an id nobody declares, is listed for a hand. So
    is a flow that runs one of the deleted ones.
    """
    from doctor import MAESTRO_ID, declared_id_matcher

    if not MAESTRO_DIR.is_dir():
        return [], []
    samples = [
        directory for directory in sorted((REPO_ROOT / "feature").iterdir())
        if directory.is_dir() and directory.name != flat and directory.name not in FRAME_FEATURES
    ]
    mine = declared_id_matcher(test_ids(REPO_ROOT / "feature" / flat))
    other = declared_id_matcher(set().union(*(test_ids(directory) for directory in samples)))
    frame = declared_id_matcher(
        set().union(*(test_ids(REPO_ROOT / root) for root in ("app", "core", "service")))
        | set().union(*(test_ids(REPO_ROOT / "feature" / name) for name in FRAME_FEATURES))
    )

    owned, shared = [], []
    for flow in sorted(MAESTRO_DIR.glob("*.yaml")):
        ids = {match.group(1) for line in flow.read_text().split("\n") if (match := MAESTRO_ID.match(line))}
        if not any(mine(identifier) for identifier in ids):
            continue
        if all(mine(identifier) or (frame(identifier) and not other(identifier)) for identifier in ids):
            owned.append(flow)
        else:
            shared.append(flow)
    names = {flow.name for flow in owned}
    for flow in sorted(MAESTRO_DIR.glob("*.yaml")):
        if flow not in owned and flow not in shared and names & set(RUN_FLOW.findall(flow.read_text())):
            shared.append(flow)
    return owned, shared


def outside_tests(flat: str) -> list[Path]:
    """Test sources outside `feature/<flat>/` that import the feature — they stop compiling."""
    needle = f"{BASE_PACKAGE}.feature.{flat}."
    found = []
    for path in sorted(REPO_ROOT.rglob("*.kt")):
        parts = path.relative_to(REPO_ROOT).parts
        if parts[:2] == ("feature", flat) or "src" not in parts or "build" in parts:
            continue
        if parts[parts.index("src") + 1] in TEST_SOURCE_SETS and needle in path.read_text(errors="ignore"):
            found.append(path)
    return found


def delete_fixtures(flat: str, dry_run: bool) -> None:
    """`:app`'s fixtures for the feature, `fixture_<flat>_<what>.json` in any source set."""
    for fixture in sorted(APP_SOURCES.glob(f"*/res/raw/fixture_{flat}_*")):
        if dry_run:
            print(f"  would delete {relative_to_repo(fixture)}")
        else:
            fixture.unlink()
            print(f"  deleted {relative_to_repo(fixture)}")


def remove_app_strings(flat: str, dry_run: bool) -> None:
    """`:app`'s `tab_<flat>` and `menu_<flat>` strings, in every locale."""
    entry = re.compile(rf'\n[ \t]*<string name="(?:tab|menu)_{re.escape(flat)}"[^>]*>.*?</string>', re.DOTALL)
    for strings in sorted(APP_SOURCES.glob("*/res/values*/strings.xml")):
        edit_file(strings, lambda text: entry.sub("", text), dry_run, "remove its tab and menu strings")


def delete_flows(flows: list[Path], dry_run: bool) -> None:
    for flow in flows:
        if dry_run:
            print(f"  would delete {relative_to_repo(flow)}")
        else:
            flow.unlink()
            print(f"  deleted {relative_to_repo(flow)}")


def doors(flat: str) -> set[str]:
    """The feature's deep-link doors, read off its api module, and its own name."""
    found = {flat}
    api = REPO_ROOT / "feature" / flat / "api"
    for source in api.rglob("*.kt") if api.is_dir() else []:
        text = source.read_text()
        if "DeepLinkTable" in text:
            found.update(DOOR.findall(text))
    return found


def wiring_directories(flat: str) -> list[Path]:
    """`:app`'s `wiring.<flat>` package in every source set, the tests' included."""
    return sorted(APP_SOURCES.glob(f"*/kotlin/{BASE_PATH}/wiring/{flat}"))


def delete_wiring(flat: str, dry_run: bool) -> None:
    for directory in wiring_directories(flat):
        if dry_run:
            print(f"  would delete {relative_to_repo(directory)}/")
        else:
            shutil.rmtree(directory)
            print(f"  deleted {relative_to_repo(directory)}/")


def imported_names(text: str, prefix: str) -> set[str]:
    return {
        line.rsplit(".", 1)[-1].strip()
        for line in text.split("\n")
        if line.startswith(prefix)
    }


def unregister_wiring(flat: str, dry_run: bool) -> None:
    """The import of the feature's wiring module and its one `includes(...)` line."""
    prefix = f"import {BASE_PACKAGE}.wiring.{flat}."

    def transform(text: str) -> str:
        includes = {f"includes({name})" for name in imported_names(text, prefix)}
        return remove_lines(text, lambda line: line.startswith(prefix) or line.strip() in includes)

    if APPLICATION_MODULE_FILE.is_file():
        edit_file(APPLICATION_MODULE_FILE, transform, dry_run, "remove its wiring include")


def unregister_tab(flat: str, dry_run: bool) -> None:
    """The feature's bottom-bar entry: an enum line whose key this feature declares."""
    prefix = f"import {BASE_PACKAGE}.feature.{flat}."

    def transform(text: str) -> str:
        keys = imported_names(text, prefix)
        entry = re.compile(rf"^\w+\(\s*(?:{'|'.join(map(re.escape, keys))})\b") if keys else None
        return remove_lines(
            text,
            lambda line: line.startswith(prefix) or (entry is not None and entry.match(line.strip()) is not None),
        )

    if TOP_LEVEL_DESTINATION_FILE.is_file():
        edit_file(TOP_LEVEL_DESTINATION_FILE, transform, dry_run, "remove its tab")


def unregister_shortcuts(flat: str, feature_doors: set[str], dry_run: bool) -> None:
    """Every launcher `<shortcut>` whose link opens one of the feature's doors, in every flavor."""
    door = "|".join(map(re.escape, sorted(feature_doors)))
    block = re.compile(
        rf'\n    <shortcut\b(?:(?!</shortcut>).)*?://(?:{door})[/"](?:(?!</shortcut>).)*?</shortcut>\n',
        re.DOTALL,
    )
    for shortcuts in sorted(APP_SOURCES.glob("*/res/xml/shortcuts.xml")):
        edit_file(shortcuts, lambda text: block.sub("", text), dry_run, "remove its shortcuts")


def declared_services(path: Path) -> set[str]:
    return {a or b for a, b in SERVICE_REFERENCE.findall(path.read_text(errors="ignore"))}


def orphaned_services(flat: str) -> list[str]:
    """
    The services `:core:di` declares that no module left after the delete declares.

    A module is a build file outside `feature/<flat>/` and `:core:di`, or a convention plugin.
    A service whose only callers are services pruned with it is pruned too, so this runs to a
    fixed point.
    """
    candidates = declared_services(CORE_DI_BUILD_FILE) - {"core", "log"}
    build_files = [
        path for path in REPO_ROOT.rglob("build.gradle.kts")
        if "build" not in path.relative_to(REPO_ROOT).parts[:-1]
        and not path.is_relative_to(REPO_ROOT / "feature" / flat)
        and path != CORE_DI_BUILD_FILE
    ]
    build_files += [
        path for path in (REPO_ROOT / "build-logic/src/main/kotlin").glob("*.kt") if path != SERVICE_MODULES_FILE
    ]
    declared = {path: declared_services(path) for path in build_files}
    orphans: set[str] = set()
    while True:
        alive = set().union(*(
            names for path, names in declared.items()
            if not any(path.is_relative_to(REPO_ROOT / "service" / orphan) for orphan in orphans)
        ))
        found = candidates - alive
        if found == orphans:
            return sorted(orphans)
        orphans = found


def remove_with_comments(lines: list[str], first: int, last: int) -> list[str]:
    """Drops lines `first..last` and the `//` comment directly above them, and a blank line the
    removal would double."""
    while first > 0 and lines[first - 1].strip().startswith("//"):
        first -= 1
    if first > 0 and not lines[first - 1].strip() and last + 1 < len(lines) and not lines[last + 1].strip():
        last += 1
    return lines[:first] + lines[last + 1:]


def single_line(lines: list[str], index: int) -> int:
    return index


def remove_entry(text: str, matches, end_of=single_line) -> str:
    lines = text.split("\n")
    for index in range(len(lines) - 1, -1, -1):
        if index < len(lines) and matches(lines[index]):
            lines = remove_with_comments(lines, index, end_of(lines, index))
    return "\n".join(lines)


def row_end(lines: list[str], index: int) -> int:
    """A `ServiceModules.kt` row is one line, or a block closed by `},` at its indent."""
    if not lines[index].rstrip().endswith("{"):
        return index
    indent = len(lines[index]) - len(lines[index].lstrip())
    return next(i for i in range(index + 1, len(lines)) if lines[i] == " " * indent + "},")


def without_service_module(text: str, name: str) -> str:
    """`Koin.kt` without `<name>ServiceModule`: its KDoc and block, its list line, its imports."""
    lines = text.split("\n")
    start = next((i for i, line in enumerate(lines) if line.startswith(f"val {name}ServiceModule")), None)
    if start is not None:
        end = next(i for i in range(start, len(lines)) if lines[i] == "}")
        if lines[start - 1].strip() == "*/":
            while not lines[start - 1].startswith("/**"):
                start -= 1
            start -= 1
        if start > 0 and not lines[start - 1].strip() and end + 1 < len(lines) and not lines[end + 1].strip():
            end += 1
        lines = lines[:start] + lines[end + 1:]
    prefix = f"import {BASE_PACKAGE}.service.{name}."
    return remove_lines(
        "\n".join(lines),
        lambda line: line.startswith(prefix) or line.strip() == f"{name}ServiceModule,",
    )


def prune_service(name: str, dry_run: bool) -> None:
    path = f":service:{name}"
    edit_file(
        REPO_ROOT / "settings.gradle.kts",
        lambda text: remove_entry(text, lambda line: line.startswith(f'includeModule("{path}",')),
        dry_run,
        f"unregister {path}",
    )
    edit_file(
        SERVICE_MODULES_FILE,
        lambda text: remove_entry(text, lambda line: line.strip().startswith(f'service("{path}",'), row_end),
        dry_run,
        f"remove {path}'s row",
    )
    edit_file(
        CORE_DI_BUILD_FILE,
        lambda text: remove_entry(text, lambda line: line.strip() == f"implementation(projects.service.{name})"),
        dry_run,
        f"remove {path} from :core:di",
    )
    edit_file(KOIN_FILE, lambda text: without_service_module(text, name), dry_run, f"remove {name}ServiceModule")
    directory = REPO_ROOT / "service" / name
    if dry_run:
        print(f"  would delete {relative_to_repo(directory)}/")
    else:
        shutil.rmtree(directory)
        print(f"  deleted {relative_to_repo(directory)}/")


def delete_directory(flat: str, dry_run: bool) -> bool:
    directory = REPO_ROOT / "feature" / flat
    if not directory.is_dir():
        print(f"  feature/{flat} does not exist")
        return False
    if dry_run:
        print(f"  would delete {relative_to_repo(directory)}/")
        return True
    shutil.rmtree(directory)
    print(f"  deleted {relative_to_repo(directory)}/")
    return True


def unregister_from_settings(flat: str, dry_run: bool) -> None:
    block = re.compile(
        rf'includeFeatureModule\(\n    "{re.escape(flat)}",\n(?:    ModuleSuffix\.\w+,\n)*\)\n\n?',
    )
    edit_file(
        REPO_ROOT / "settings.gradle.kts",
        lambda text: block.sub("", text),
        dry_run,
        "unregister modules",
    )


def unregister_from_core_di_build(flat: str, dry_run: bool) -> None:
    entry = f"api(projects.feature.{flat}.di)"
    edit_file(
        CORE_DI_BUILD_FILE,
        lambda text: remove_lines(text, lambda line: line.strip() == entry),
        dry_run,
        "remove :core:di dependency",
    )


def unregister_from_koin(flat: str, module_class: str | None, dry_run: bool) -> None:
    if module_class is None:
        print("  Koin: feature has no di module, nothing to unregister")
        return

    import_line = f"import {BASE_PACKAGE}.feature.{flat}.di.{module_class}"
    entry = f"{module_class}.module,"

    def transform(text: str) -> str:
        return remove_lines(text, lambda line: line == import_line or line.strip() == entry)

    edit_file(KOIN_FILE, transform, dry_run, "unregister Koin module")


def unregister_destinations(flat: str, functions: list[str], dry_run: bool) -> None:
    if not functions:
        print("  AppNavHost: no destinations from this feature")
        return

    import_prefix = f"import {BASE_PACKAGE}.feature.{flat}."
    calls = tuple(f"{name}(" for name in functions)

    def transform(text: str) -> str:
        return remove_lines(without_calls(text, calls), lambda line: line.startswith(import_prefix))

    edit_file(APP_NAV_HOST_FILE, transform, dry_run, f"remove {', '.join(functions)}")


def without_calls(text: str, calls: tuple[str, ...]) -> str:
    """
    Drops every statement that starts with one of `calls`, to the line its parentheses close on.

    A registration can span lines — a lambda argument, a trailing comma — and dropping only its
    first line leaves the rest dangling and the file unparseable.
    """
    lines = text.split("\n")
    kept: list[str] = []
    depth = 0
    for line in lines:
        if depth == 0 and line.strip().startswith(calls):
            depth = line.count("(") - line.count(")")
            continue
        if depth > 0:
            depth += line.count("(") - line.count(")")
            continue
        kept.append(line)
    return "\n".join(kept)


def report_leftovers(flat: str) -> None:
    """Reports references the script cannot remove — typically cross-feature navigation lambdas."""
    needles = (f"feature.{flat}.", f"feature/{flat}", f'"{flat}"')
    hits = []
    for root in REFERENCE_ROOTS:
        path = REPO_ROOT / root
        files = [path] if path.is_file() else [
            p for p in path.rglob("*")
            if p.is_file() and p.suffix in {".kt", ".kts", ".xml"} and "build" not in p.relative_to(path).parts
        ] if path.exists() else []
        for file in files:
            for number, line in enumerate(file.read_text(errors="ignore").split("\n"), start=1):
                if any(needle in line for needle in needles):
                    hits.append(f"    {relative_to_repo(file)}:{number}: {line.strip()}")

    if hits:
        print(f"\n{len(hits)} leftover reference(s) to '{flat}' — remove these by hand:")
        print("\n".join(hits))


def main() -> None:
    args = parse_args()
    flat = to_flat(args.name)

    if flat == TEMPLATE_FEATURE and not args.force:
        sys.exit(
            "Refusing to delete the template feature the generators clone from. Use --force if "
            "you really mean it."
        )
    if flat in FRAME_FEATURES and not args.force:
        sys.exit(
            f"Refusing to delete '{flat}': it is the frame the app is built on, not a sample — "
            f"the frame is {', '.join(FRAME_FEATURES)}. Use --force if you really mean it."
        )

    if not (REPO_ROOT / "feature" / flat).is_dir():
        sys.exit(f"No such feature: feature/{flat}")

    # Named before anything is written: a delete that leaves another module's build file naming
    # this one is a broken build, and the leftovers report came too late to stop it.
    callers = dependants(flat)
    if callers and not args.force:
        sys.exit(
            f"Refusing to delete '{flat}': {len(callers)} module(s) outside feature/{flat} declare it — "
            f"{', '.join(callers)}. Remove those dependencies first, or use --force and fix them after."
        )

    print(f"Deleting feature '{flat}'")
    if callers:
        print(f"  --force: still declared by {', '.join(callers)} — fix them after")
    if args.dry_run:
        print("-- dry run, nothing will be written --")

    # Both are read before the directory goes away.
    module_class = koin_module_class(flat)
    functions = destination_functions(flat)
    feature_doors = doors(flat)
    orphans = orphaned_services(flat)
    owned_flows, shared_flows = classify_flows(flat)
    tests = outside_tests(flat)

    unregister_destinations(flat, functions, args.dry_run)
    unregister_from_koin(flat, module_class, args.dry_run)
    unregister_from_core_di_build(flat, args.dry_run)
    unregister_from_settings(flat, args.dry_run)
    unregister_wiring(flat, args.dry_run)
    unregister_tab(flat, args.dry_run)
    unregister_shortcuts(flat, feature_doors, args.dry_run)
    delete_wiring(flat, args.dry_run)
    delete_flows(owned_flows, args.dry_run)
    delete_fixtures(flat, args.dry_run)
    remove_app_strings(flat, args.dry_run)
    delete_directory(flat, args.dry_run)

    if shared_flows or tests:
        print(f"\nStill naming '{flat}' — edit or delete these by hand:")
        for path in shared_flows + tests:
            print(f"    {relative_to_repo(path)}")

    if orphans:
        print(f"\nLeft without a caller: {', '.join(f':service:{name}' for name in orphans)}")
        if args.prune_services:
            for name in orphans:
                prune_service(name, args.dry_run)
        else:
            print("  --prune-services removes them, and their place in :core:di")

    if not args.dry_run:
        report_leftovers(flat)

    print(
        "\nDone. Check the result — `./gradlew build` is not needed, it assembles every variant and\n"
        "runs R8 three times:\n"
        "  python3 scripts/doctor.py && ./gradlew :app:assembleDevDebug test"
    )


if __name__ == "__main__":
    main()
