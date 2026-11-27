#!/usr/bin/env python3
"""
Checks the conventions this template relies on but cannot enforce with a compiler.

    python3 scripts/doctor.py
    python3 scripts/doctor.py --list
    python3 scripts/doctor.py --undriven   # the test ids no Maestro flow taps, for whoever writes one

There is no detekt/ktlint here (detekt 1.23 cannot read the JDK 25 the daemon is pinned to), so
these greps are the only automated defence for the project's conventions: the portability of
`service/`, the six-file screen unit, module and DI registration, and versions coming from the
version catalog. Exits non-zero when something is wrong, so it can run in CI.
"""

from __future__ import annotations

import argparse
import fnmatch
import os
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _common import (  # noqa: E402
    APP_NAV_HOST_FILE,
    KOIN_GRAPH_TEST_FILE,
    BASE_PACKAGE,
    CORE_DI_BUILD_FILE,
    KOIN_FILE,
    REPO_ROOT,
    SETTINGS_FILE,
    TEMPLATE_FEATURE,
    TRANSLATED_LOCALES,
    block_end,
    relative_to_repo,
    to_snake,
)

SCREEN_FILE_SUFFIXES = ["Destination", "Screen", "State", "Event", "Navigation", "ViewModel"]

# Files in a presentation module that end in `Destination` or `NavGraph` but are not screens.
NOT_A_SCREEN = re.compile(r"(NavGraph)\.kt$")

SUFFIX_TO_LAYER = {
    "Api": "api",
    "Domain": "domain",
    "Data": "data",
    "Presentation": "presentation",
    "Di": "di",
    "Ui": "ui",
}

CHECKS = []


def check(name: str):
    def decorate(function):
        CHECKS.append((name, function))
        return function

    return decorate


_TREE: list[Path] | None = None


def tree() -> list[Path]:
    """
    Every file in the repository worth looking at, from one pruned walk.

    `Path.rglob` cannot prune. It descends into `build/`, `.gradle/` and `.git/` — 62,583 files
    against the 920 that are source — and every check then discarded most of the result with a
    filter. Seventeen of those walks was fourteen seconds, nine of them kernel time in `stat`, for
    thirty checks that between them read a few hundred files.

    Walking once and filtering the list is the whole of the fix. `os.walk` can prune, because
    editing `subdirectories` in place stops it descending.
    """
    global _TREE
    if _TREE is None:
        found: list[Path] = []
        for directory, subdirectories, names in os.walk(REPO_ROOT):
            # A dot directory is `.git`, `.gradle`, `.idea` — or a nested worktree, which is a whole
            # second checkout of this repository and would report every module in it twice.
            # `scripts/clean_start` is the shell init_project.py copies over `:app`: files to be,
            # not modules of this build.
            subdirectories[:] = [
                name for name in subdirectories
                if name != "build" and name != "__pycache__" and not name.startswith(".")
                and not (directory == str(REPO_ROOT / "scripts") and name == "clean_start")
            ]
            base = Path(directory)
            # Dot *files* go too — `.gitignore`, `.DS_Store`, `.gitkeep`. No check reads one, and
            # the filter this replaces dropped them as well, because it tested every component of
            # the path including the name. A `.DS_Store` that Finder leaves in a `values/`
            # directory would otherwise read as a resource.
            found.extend(base / name for name in names if not name.startswith("."))
        _TREE = sorted(found)
    return _TREE


def walk(directory: Path, pattern: str = "*") -> list[Path]:
    """
    `directory.rglob(pattern)`, answered from `tree()` rather than from the disk.

    Sorted, pruned, files only, and `[]` for a directory that does not exist — which is what every
    call site wanted, and why most of them no longer need an `is_dir()` guard of their own.
    """
    if not directory.is_dir():
        return []
    prefix = str(directory) + os.sep
    if "/" in pattern:
        # A path-shaped pattern — `src/main/res/values/strings.xml` — matches a trailing run of
        # components, exactly as `rglob` treats one.
        tail = os.sep + pattern.replace("/", os.sep)
        return [path for path in tree() if str(path).startswith(prefix) and str(path).endswith(tail)]
    return [
        path for path in tree()
        if str(path).startswith(prefix) and fnmatch.fnmatch(path.name, pattern)
    ]


def kotlin_files(root: Path):
    yield from walk(root, "*.kt")


def build_files(root: Path):
    yield from walk(root, "build.gradle.kts")


def feature_names() -> list[str]:
    """Directories under `feature/`, minus any Gradle output that has landed there."""
    root = REPO_ROOT / "feature"
    if not root.is_dir():
        return []
    return sorted(p.name for p in root.iterdir() if p.is_dir() and p.name != "build")


def presentation_dir(feature: str) -> Path:
    return (
        REPO_ROOT / "feature" / feature / "presentation/src/main/kotlin"
        / BASE_PACKAGE.replace(".", "/") / "feature" / feature / "presentation"
    )


def problem(path: Path, line_number: int | None, message: str) -> str:
    location = f"{relative_to_repo(path)}:{line_number}" if line_number else relative_to_repo(path)
    return f"{location}: {message}"


# --------------------------------------------------------------------------------------------
# service/ portability
# --------------------------------------------------------------------------------------------

APP_PROJECT_ACCESSOR = re.compile(r"\bprojects\.(core|feature|app)\b")


@check("service/ depends on nothing app-specific")
def check_service_isolation() -> list[str]:
    problems = []
    for path in build_files(REPO_ROOT / "service"):
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            match = APP_PROJECT_ACCESSOR.search(line)
            if match:
                problems.append(problem(path, number, f"depends on :{match.group(1)} — service/ must stay portable"))
    for path in kotlin_files(REPO_ROOT / "service"):
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            if line.startswith(f"import {BASE_PACKAGE}.feature."):
                problems.append(problem(path, number, "imports a feature — service/ must stay portable"))
    return problems


@check(":service:core:domain is free of the Android framework")
def check_domain_has_no_android() -> list[str]:
    problems = []
    for path in kotlin_files(REPO_ROOT / "service/core/domain"):
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            if re.match(r"import androidx?\.", line):
                problems.append(problem(path, number, f"domain must not import the framework: {line.strip()}"))
    return problems


@check(":service:core:ui resources are prefixed core_")
def check_resource_prefix() -> list[str]:
    module = REPO_ROOT / "service/core/ui"
    if not module.is_dir():
        return []

    problems = []
    build_file = module / "build.gradle.kts"
    if build_file.is_file() and 'resourcePrefix = "core_"' not in build_file.read_text():
        problems.append(problem(build_file, None, 'missing resourcePrefix = "core_"'))

    for path in walk(module / "src/main/res", "*.xml"):
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            for name in re.findall(r'\bname="([^"]+)"', line):
                if not name.startswith("core_"):
                    problems.append(problem(path, number, f"resource '{name}' is missing the core_ prefix"))
    return problems


# --------------------------------------------------------------------------------------------
# Screens
# --------------------------------------------------------------------------------------------


@check("every screen is a complete six-file unit")
def check_screen_units() -> list[str]:
    problems = []
    for feature in feature_names():
        for destination in walk(presentation_dir(feature), "*Destination.kt"):
            if NOT_A_SCREEN.search(destination.name):
                continue
            screen = destination.name[: -len("Destination.kt")]
            for suffix in SCREEN_FILE_SUFFIXES:
                sibling = destination.parent / f"{screen}{suffix}.kt"
                if not sibling.is_file():
                    problems.append(problem(destination, None, f"screen '{screen}' is missing {sibling.name}"))
    return problems


@check("every XState has a PREVIEW fixture")
def check_state_previews() -> list[str]:
    problems = []
    for feature in feature_names():
        directory = presentation_dir(feature)
        for state in walk(directory, "*State.kt"):
            if not re.search(r"\bval PREVIEW\b", state.read_text()):
                problems.append(problem(state, None, "no `val PREVIEW` — it is the preview fixture and the usual initialState"))
    return problems


@check("no ViewModel clears its loading state in an init block")
def check_no_loading_in_init() -> list[str]:
    """
    `BaseViewModel` takes `initialState`, and `execute` drives the overlay. An
    `init { uiState.update { ... loading = null } }` is the pattern that once left a forgotten line
    stranding a screen behind a permanent spinner.

    Subscribing to a flow from `init` and updating `data` is fine — see `SettingsViewModel` — so
    only writes that touch `loading` are flagged.
    """
    problems = []
    for feature in feature_names():
        directory = presentation_dir(feature)
        for view_model in walk(directory, "*ViewModel.kt"):
            lines = view_model.read_text().split("\n")
            for number, line in enumerate(lines):
                if line.strip() != "init {":
                    continue
                body = "\n".join(lines[number: block_end(lines, number) + 1])
                if re.search(r"uiState\.update\s*\{[^}]*\bloading\s*=", body, re.DOTALL):
                    problems.append(
                        problem(view_model, number + 1, "init block writes `loading` — pass initialState and let execute drive the overlay")
                    )
    return problems


@check("no feature presentation depends on another feature's presentation")
def check_cross_feature_presentation() -> list[str]:
    problems = []
    for feature in feature_names():
        build_file = REPO_ROOT / "feature" / feature / "presentation/build.gradle.kts"
        if not build_file.is_file():
            continue
        for number, line in enumerate(build_file.read_text().split("\n"), start=1):
            match = re.search(r"projects\.feature\.(\w+)\.presentation", line)
            if match and match.group(1) != feature:
                problems.append(
                    problem(build_file, number, f"depends on :feature:{match.group(1)}:presentation — depend on :feature:{match.group(1)}:api for its route keys instead")
                )
    return problems


API_DECLARATION = re.compile(
    r"^(?:(?:public|internal|private|data|sealed|enum|abstract|open|fun|const)\s+)*"
    r"(class|object|interface|val|var|typealias|fun)\s+(?:<[^>]*>\s*)?([\w.]+)"
)


@check("an api module holds only route keys and a deep-link table")
def check_api_modules_hold_only_routes() -> list[str]:
    """
    A feature's `api` module is the one another feature may depend on, so whatever lands in
    it becomes another feature's business. It holds `@Serializable` route keys, plain objects, and
    a deep-link table — a top-level `val` named `…DeepLinks` — and nothing else: a function, a
    ViewModel or a model class there is presentation or domain leaking through the one open door.
    """
    problems = []
    for feature in feature_names():
        for path in kotlin_files(REPO_ROOT / "feature" / feature / "api/src/main"):
            annotations: list[str] = []
            for number, line in enumerate(path.read_text().split("\n"), start=1):
                if not line or line[0].isspace() or line.startswith(("package ", "import ", "//", "/*", " *", "*/")):
                    continue
                inline = re.match(r"^((?:@\w+(?:\([^)]*\))?(?:\s+|$))*)(.*)$", line)
                annotations += re.findall(r"@(\w+)", inline.group(1))
                rest = inline.group(2)
                if not rest:
                    continue
                declaration = API_DECLARATION.match(rest)
                if declaration is None:
                    annotations = []
                    continue
                kind, name = declaration.groups()
                serializable = "Serializable" in annotations
                annotations = []
                if kind in ("class", "interface") or (kind == "object" and rest.startswith("data ")):
                    if not serializable:
                        problems.append(problem(path, number, f"{name} is not @Serializable — an api module holds route keys only"))
                elif kind == "object":
                    continue
                elif kind == "val" and name.endswith("DeepLinks"):
                    continue
                else:
                    problems.append(
                        problem(path, number, f"{kind} {name} — an api module holds route keys and a `…DeepLinks` table only; move it to presentation or domain")
                    )
    return problems


# --------------------------------------------------------------------------------------------
# Registration
# --------------------------------------------------------------------------------------------

FEATURE_BLOCK = re.compile(r'includeFeatureModule\(\s*"([^"]+)",\s*((?:ModuleSuffix\.\w+,\s*)*)\)')
CORE_BLOCK = re.compile(r"includeCoreModule\(\s*((?:ModuleSuffix\.\w+,\s*)*)\)")
SERVICE_BLOCK = re.compile(r'includeServiceModule\(\s*"([^"]+)",\s*((?:ModuleSuffix\.\w+,\s*)*)\)')


def registered_modules() -> set[str]:
    """Every module path settings.gradle.kts includes, as a repo-relative directory."""
    text = SETTINGS_FILE.read_text()
    paths = set()

    def layers(raw: str) -> list[str]:
        return [SUFFIX_TO_LAYER[s] for s in re.findall(r"ModuleSuffix\.(\w+)", raw) if s in SUFFIX_TO_LAYER]

    for name, raw in FEATURE_BLOCK.findall(text):
        paths.update(f"feature/{name}/{layer}" for layer in layers(raw))
    for name, raw in SERVICE_BLOCK.findall(text):
        paths.update(f"service/{name}/{layer}" for layer in layers(raw))
    for raw in CORE_BLOCK.findall(text):
        paths.update(f"core/{layer}" for layer in layers(raw))
    # Plain includes, for the modules that are not features, services or core layers — `:app` is
    # the only one today. Parsed rather than listed, so the next one needs no edit here.
    for path in re.findall(r'include\(\s*"(:[\w:-]+)"\s*\)', text):
        paths.add(path.removeprefix(":").replace(":", "/"))
    # `includeModule(":service:network", "service/network")` — a service that is one module rather
    # than a set of layers, so `includeServiceModule`'s suffixes have nothing to say about it. The
    # interpolated calls inside the helpers do not match: `$` is not in the character class.
    for path in re.findall(r'includeModule\(\s*"(:[\w:-]+)"', text):
        paths.add(path.removeprefix(":").replace(":", "/"))
    return paths


@check("every module on disk is included in settings.gradle.kts")
def check_modules_registered() -> list[str]:
    included = registered_modules()
    problems = []
    for build_file in build_files(REPO_ROOT):
        module = build_file.parent
        # build-logic is an included build, not a module: it is wired in with includeBuild.
        if module == REPO_ROOT or {"buildSrc", "build-logic"} & set(module.parts):
            continue
        relative = relative_to_repo(module)
        if relative not in included:
            problems.append(problem(build_file, None, f"module '{relative}' is not in settings.gradle.kts"))
    return problems


SERVICE_MODULES_FILE = REPO_ROOT / "build-logic/src/main/kotlin/ServiceModules.kt"
# `service(":service:files", ServiceBase.AndroidLibrary)` — a row's module path.
SERVICE_MODULE_ROW = re.compile(r'^\s*service\("(:service:[\w:]+)",', re.MULTILINE)


@check("every service module has a row in ServiceModules.kt")
def check_service_module_registrations() -> list[str]:
    """A service module applies `convention.service`, which builds it from its row in
    `build-logic`'s `ServiceModules.kt`, so a copied `service/` brings its build with it.
    `create_service.py` writes both; a hand copy is what forgets one. A row whose module is gone is
    held too, so the table stays the truth.
    """
    service_dir = REPO_ROOT / "service"
    if not service_dir.is_dir():
        return []
    table_file = SERVICE_MODULES_FILE
    table = table_file.read_text() if table_file.is_file() else ""
    rows = set(SERVICE_MODULE_ROW.findall(table))
    problems = []
    modules = set()
    for build_file in sorted(service_dir.glob("*/build.gradle.kts")) + sorted(service_dir.glob("*/*/build.gradle.kts")):
        module = ":service:" + build_file.parent.relative_to(service_dir).as_posix().replace("/", ":")
        modules.add(module)
        if "alias(libs.plugins.convention.service)" not in build_file.read_text():
            problems.append(problem(build_file, None, "applies no `libs.plugins.convention.service` — a service module's build is its row in ServiceModules.kt"))
        if module not in rows:
            problems.append(problem(table_file, None, f"has no row for {module} — add one, or run create_service.py"))
    for module in sorted(rows - modules):
        problems.append(problem(table_file, None, f"has a row for {module}, which has no build file under service/"))
    return problems


@check("every feature di module is wired into Koin")
def check_koin_registration() -> list[str]:
    if not KOIN_FILE.is_file():
        return [problem(KOIN_FILE, None, "not found")]

    koin = KOIN_FILE.read_text()
    core_di_build = CORE_DI_BUILD_FILE.read_text() if CORE_DI_BUILD_FILE.is_file() else ""
    problems = []

    for feature in feature_names():
        di_dir = (
            REPO_ROOT / "feature" / feature / "di/src/main/kotlin"
            / BASE_PACKAGE.replace(".", "/") / "feature" / feature / "di"
        )
        for module_file in sorted(di_dir.glob("*Module.kt")) if di_dir.is_dir() else []:
            if feature == TEMPLATE_FEATURE:
                continue
            if f"{module_file.stem}.module," not in koin:
                problems.append(problem(KOIN_FILE, None, f"{module_file.stem}.module is not in initKoin()"))
            if f"api(projects.feature.{feature}.di)" not in core_di_build:
                problems.append(problem(CORE_DI_BUILD_FILE, None, f"missing api(projects.feature.{feature}.di)"))
    return problems


@check("every ViewModel is registered in its feature's Koin module")
def check_view_models_registered() -> list[str]:
    problems = []
    for feature in feature_names():
        directory = presentation_dir(feature)
        if not directory.is_dir():
            continue
        di_dir = (
            REPO_ROOT / "feature" / feature / "di/src/main/kotlin"
            / BASE_PACKAGE.replace(".", "/") / "feature" / feature / "di"
        )
        di_text = "\n".join(p.read_text() for p in di_dir.glob("*Module.kt")) if di_dir.is_dir() else ""
        for view_model in walk(directory, "*ViewModel.kt"):
            if f"viewModelOf(::{view_model.stem})" not in di_text:
                problems.append(problem(view_model, None, f"no viewModelOf(::{view_model.stem}) in feature/{feature}/di"))
    return problems


@check("every destination is registered in AppNavHost")
def check_destinations_registered() -> list[str]:
    if not APP_NAV_HOST_FILE.is_file():
        return [problem(APP_NAV_HOST_FILE, None, "not found")]

    nav_host = APP_NAV_HOST_FILE.read_text()
    problems = []
    for feature in feature_names():
        # The template feature is compiled but deliberately unreachable.
        if feature == TEMPLATE_FEATURE:
            continue
        directory = presentation_dir(feature)
        for destination in walk(directory, "*Destination.kt"):
            for function in re.findall(r"fun EntryProviderScope<NavKey>\.(\w+)\(", destination.read_text()):
                if f"{function}(" not in nav_host:
                    problems.append(problem(destination, None, f"{function}() is never called in AppNavHost.kt"))
    return problems


ROUTE_KEY_PARAMETER = re.compile(r"^\s*(?:private val )?\w+: (\w+Destination),?$", re.MULTILINE)


@check("every route key is declared in KoinGraphTest")
def check_route_keys_verified() -> list[str]:
    """
    A screen with navigation arguments takes its route key as a constructor parameter, and the
    destination passes it in with `parametersOf`. Koin's `verify()` cannot know that, so it reports
    the key as a missing definition unless `KoinGraphTest` names it — which is a confusing way to
    find out that a perfectly good screen is fine.
    """
    if not KOIN_GRAPH_TEST_FILE.is_file():
        return [problem(KOIN_GRAPH_TEST_FILE, None, "not found")]

    graph_test = KOIN_GRAPH_TEST_FILE.read_text()
    problems = []
    for feature in feature_names():
        # The template feature is compiled but never registered in the graph.
        if feature == TEMPLATE_FEATURE:
            continue
        directory = presentation_dir(feature)
        for view_model in walk(directory, "*ViewModel.kt"):
            if not ROUTE_KEY_PARAMETER.search(view_model.read_text()):
                continue
            entry = f"definition<{view_model.stem}>("
            if entry not in graph_test:
                problems.append(
                    problem(view_model, None, f"takes a route key but {entry}...) is not in KoinGraphTest.kt")
                )
    return problems


# --------------------------------------------------------------------------------------------
# Layer direction
# --------------------------------------------------------------------------------------------

# What each layer of a feature is allowed to depend on: api and domain stand alone, data sees
# domain, presentation sees api and domain, and di wires all of them.
ALLOWED_FEATURE_DEPENDENCIES = {
    "api": {"api"},
    "domain": {"domain"},
    "data": {"domain", "data"},
    "presentation": {"api", "domain", "presentation"},
    "di": {"api", "domain", "data", "presentation", "di"},
}

FEATURE_PROJECT_ACCESSOR = re.compile(r"projects\.feature\.(\w+)\.(\w+)")


@check("feature layers depend only downwards")
def check_layer_direction() -> list[str]:
    """
    `presentation` reaching into `data` compiles perfectly well and quietly undoes the layering —
    the ViewModel ends up talking to a data source instead of a repository. The cross-feature rule
    is checked separately; this one is about layers within a feature.
    """
    problems = []
    for feature in feature_names():
        for layer, allowed in ALLOWED_FEATURE_DEPENDENCIES.items():
            build_file = REPO_ROOT / "feature" / feature / layer / "build.gradle.kts"
            if not build_file.is_file():
                continue
            for number, line in enumerate(build_file.read_text().split("\n"), start=1):
                match = FEATURE_PROJECT_ACCESSOR.search(line)
                if not match:
                    continue
                target_feature, target_layer = match.group(1), match.group(2)
                if target_feature != feature:
                    continue  # cross-feature deps are check_cross_feature_presentation's business
                if target_layer not in allowed:
                    problems.append(
                        problem(
                            build_file,
                            number,
                            f"{layer} must not depend on {target_layer} — allowed: {', '.join(sorted(allowed))}",
                        )
                    )
    return problems


DEFAULT_DATA_SOURCE_IMPORT = re.compile(r"^import [\w.]*\.(Default\w*DataSource)$", re.MULTILINE)


@check("no repository imports a data source implementation")
def check_repository_depends_on_interface() -> list[str]:
    """
    The rule the `gateway` module used to enforce by living in a different module. Now that both
    halves sit in `:feature:x:data`, nothing but this stops `DefaultXRepository` from constructing
    a `DefaultXDataSource` directly — and a repository wired to an implementation cannot have its
    source swapped for a cache or a fake, which is the whole point of the layer.
    """
    problems = []
    for feature in feature_names():
        data_dir = REPO_ROOT / "feature" / feature / "data"
        for path in kotlin_files(data_dir):
            if not path.stem.endswith("Repository"):
                continue
            for number, line in enumerate(path.read_text().split("\n"), start=1):
                match = DEFAULT_DATA_SOURCE_IMPORT.match(line)
                if match:
                    interface = match.group(1)[len("Default"):]
                    problems.append(
                        problem(path, number, f"imports {match.group(1)} — depend on {interface} instead")
                    )
    return problems


SERVICE_LAYER_ALLOWED = {"domain": set(), "data": {"domain"}, "ui": {"domain"}}


@check("service layers depend only downwards")
def check_service_layer_direction() -> list[str]:
    problems = []
    for module in sorted((REPO_ROOT / "service").glob("*/*")) if (REPO_ROOT / "service").is_dir() else []:
        build_file = module / "build.gradle.kts"
        allowed = SERVICE_LAYER_ALLOWED.get(module.name)
        if not build_file.is_file() or allowed is None:
            continue
        for number, line in enumerate(build_file.read_text().split("\n"), start=1):
            match = re.search(r"projects\.service\.(\w+)\.(\w+)", line)
            if not match:
                continue
            service, layer = match.groups()
            # Another layered service's same layer is a sibling, not a step up: `:service:core:domain`
            # re-exports `:service:log:domain`. Every other layer follows the table.
            sibling = service != module.parent.name and layer == module.name
            if layer not in allowed and not sibling:
                problems.append(
                    problem(build_file, number, f"service {module.name} must not depend on {layer}")
                )
    return problems


# --------------------------------------------------------------------------------------------
# Compose conventions
# --------------------------------------------------------------------------------------------

@check("every public composable takes a Modifier parameter")
def check_modifier_parameter() -> list[str]:
    """
    The first rule in Compose's own API guidelines: a composable that emits UI takes
    `modifier: Modifier = Modifier` so its caller can position it. Previews and screen-level
    composables that fill the window are exempt, as is everything outside `src/main`.
    """
    problems = []
    roots = [REPO_ROOT / "core/ui", REPO_ROOT / "service/core/ui"]
    roots += [REPO_ROOT / "feature" / f / "presentation" for f in feature_names()]

    for root in roots:
        for path in kotlin_files(root):
            # Production composables only. A preview in `screenshotTest` positions nothing — it is
            # the thing being rendered.
            if "/src/main/" not in path.as_posix():
                continue
            text = path.read_text()
            if "@Composable" not in text:
                continue
            lines = text.split("\n")
            for number, line in enumerate(lines):
                if line.strip() != "@Composable":
                    continue
                # find the fun declaration that follows the annotation block
                index = number + 1
                while index < len(lines) and not lines[index].lstrip().startswith("fun "):
                    if lines[index].lstrip().startswith("private fun ") or lines[index].strip() == "":
                        break
                    index += 1
                if index >= len(lines):
                    continue
                declaration = lines[index].lstrip()
                if not declaration.startswith("fun "):
                    continue
                name = declaration[4:].split("(")[0].split("<")[0].strip()
                # An extension (`fun UiText.resolve()`) is a helper, not something that emits UI.
                if "." in name or not name[:1].isupper():
                    continue
                body = "\n".join(lines[index:index + 60])
                # Match parens rather than splitting on the first ")": a parameter list is full of
                # them (`onConfirm: () -> Unit`), and splitting truncates before `modifier`.
                start = body.index("(")
                depth, end = 0, start
                for position in range(start, len(body)):
                    depth += (body[position] == "(") - (body[position] == ")")
                    if depth == 0:
                        end = position
                        break
                signature = body[start:end + 1]
                returns_value = re.match(r"\s*:\s*(?!Unit\b)\w", body[end + 1:])
                # Exempt what has nothing to position: a value-returning helper, a theme or
                # CompositionLocal wrapper, a preview, and a screen-level composable that fills
                # the window it is given.
                if (
                    returns_value
                    or name.endswith(("Screen", "Preview", "Theme", "Provider"))
                    # An effect emits nothing to position — `LaunchedEffect`'s own shape, and
                    # `NavResultEffect`/`ScreenViewEffect` here.
                    or name.endswith("Effect")
                    # Nor does a handler: Compose's own `BackHandler` and `PredictiveBackHandler`
                    # take no modifier, because there is nothing on screen to place.
                    or name.endswith("Handler")
                    # Compose's own idiom for a CompositionLocal wrapper — `ProvideTextStyle`.
                    or name.startswith("Provide")
                ):
                    continue
                if "modifier: Modifier" not in signature:
                    problems.append(
                        problem(path, index + 1, f"composable '{name}' takes no `modifier: Modifier = Modifier`")
                    )
    return problems


# --------------------------------------------------------------------------------------------
# Design system
# --------------------------------------------------------------------------------------------

# A feature composes components from :core:ui; it never draws one. These are what "never draws"
# means in grep terms: no Material widget, no bare dp/sp, no raw colour — the theme's roles instead.
MATERIAL_IMPORT = re.compile(r"^import androidx\.compose\.material3\.(\w+)", re.MULTILINE)
DIMENSION_LITERAL = re.compile(r"(?<![\w.])\d+(?:\.\d+)?\.(dp|sp)\b")
RAW_COLOR = re.compile(r"\bColor\(0x|\bMaterialTheme\.colorScheme\b")

# A number a person reads has a role, the same way a colour does. What this catches
# is the two copies of `Price.kt` that grew before there was one: a feature formatting its own
# money, and doing it slightly differently in each place.
NUMBER_FORMATTER = re.compile(
    r"^import (java\.text\.(?:NumberFormat|DecimalFormat)|java\.time\.format\.DateTimeFormatter)$",
    re.MULTILINE,
)

# Material types a screen legitimately names because they are types, not widgets: they appear in
# a component's own signature and a feature has to spell them to call it.
MATERIAL_TYPE_ALLOWLIST = {
    "ExperimentalMaterial3Api",
}

# A feature composes motion the way it composes widgets: `appAnimateItem()`,
# `appStaggeredEntrance()`, `AppReveal` and `AppSwitcher` read the theme's tokens, and a feature
# that names a spec is a feature that draws — and one reduced motion cannot reach.
ANIMATION_IMPORT = re.compile(r"^import androidx\.compose\.animation\.([\w.]+)$", re.MULTILINE)

# Animation types a feature may name because they are types a `:core:ui` signature takes. Empty
# today; the shape is MATERIAL_TYPE_ALLOWLIST's.
ANIMATION_TYPE_ALLOWLIST: set[str] = set()


@check("no feature draws its own UI")
def check_features_use_the_design_system() -> list[str]:
    problems = []
    for feature in feature_names():
        root = REPO_ROOT / "feature" / feature / "presentation/src/main"
        for path in kotlin_files(root):
            text = path.read_text()
            for match in MATERIAL_IMPORT.finditer(text):
                if match.group(1) in MATERIAL_TYPE_ALLOWLIST:
                    continue
                line = text[: match.start()].count("\n") + 1
                problems.append(
                    problem(
                        path,
                        line,
                        f"imports Material's {match.group(1)} — compose the :core:ui component "
                        "instead, or add one with create_component.py",
                    )
                )
            for match in ANIMATION_IMPORT.finditer(text):
                if match.group(1) in ANIMATION_TYPE_ALLOWLIST:
                    continue
                line = text[: match.start()].count("\n") + 1
                problems.append(
                    problem(
                        path,
                        line,
                        f"animates itself with androidx.compose.animation.{match.group(1)} — compose "
                        "core.ui.layout's appAnimateItem() / appStaggeredEntrance(), AppReveal or "
                        "AppSwitcher instead",
                    )
                )
            for match in DIMENSION_LITERAL.finditer(text):
                line = text[: match.start()].count("\n") + 1
                problems.append(
                    problem(
                        path,
                        line,
                        f"has a bare `{match.group(0)}` — ask AppTheme.spacing, "
                        "AppTheme.typography or AppTheme.icons for a role",
                    )
                )
            for match in RAW_COLOR.finditer(text):
                line = text[: match.start()].count("\n") + 1
                problems.append(
                    problem(path, line, "names a colour directly — ask AppTheme.colors for a role")
                )
            for match in NUMBER_FORMATTER.finditer(text):
                line = text[: match.start()].count("\n") + 1
                problems.append(
                    problem(
                        path,
                        line,
                        f"formats a number itself with {match.group(1).rsplit('.', 1)[1]} — ask "
                        "LocalFormats.current for a role (money, weight, percent, date, ...)",
                    )
                )
    return problems


# Four ways to tell the user something, and one road for the first two. A platform Toast or a
# Material Snackbar written straight into a screen or the shell skips the outlet, its tones and
# its queue — and dies with the screen that raised it.
PLATFORM_TOAST = re.compile(r"^import android\.widget\.Toast\b|\bToast\.makeText\(", re.MULTILINE)
MATERIAL_SNACKBAR = re.compile(r"(?<![\w.])(Snackbar|SnackbarHost)\(|^import androidx\.compose\.material3\.Snackbar\w*", re.MULTILINE)


@check("every message goes through the outlet")
def check_messages_go_through_the_outlet() -> list[str]:
    roots = [REPO_ROOT / "app/src"] + [
        REPO_ROOT / "feature" / feature / "presentation/src/main" for feature in feature_names()
    ]
    problems = []
    for root in roots:
        for path in kotlin_files(root):
            if "/src/test/" in str(path) or "/src/androidTest/" in str(path):
                continue
            text = path.read_text()
            for pattern, what in (
                (PLATFORM_TOAST, "tells the user with android.widget.Toast"),
                (MATERIAL_SNACKBAR, "calls Material's Snackbar"),
            ):
                for match in pattern.finditer(text):
                    line = text[: match.start()].count("\n") + 1
                    problems.append(
                        problem(
                            path,
                            line,
                            f"{what} — post an AppMessage to AppMessages, or showToast / showSnackbar "
                            "from a view model",
                        )
                    )
    return problems


# Features whose text fields are a showcase, not a form: the gallery's demos and the bench's knobs
# have no next field to move to.
IME_ACTION_EXEMPT_FEATURES = {"gallery", "devmenu"}

TEXT_FIELD_CALL = re.compile(r"\bAppTextField\(")


@check("every feature text field names its IME action")
def check_text_fields_name_their_ime_action() -> list[str]:
    """A field in a form says what comes next.

    `AppTextField` defaults `imeAction` to `ImeAction.Default`, so a call that leaves it out gives
    the keyboard the same key on every field of a form — which is what three wizard steps shipped
    with while the two sign-in forms named theirs. Every call under a feature's `presentation`
    names it; the gallery and the bench are exempt because their fields lead nowhere.
    """
    problems = []
    for feature in feature_names():
        if feature in IME_ACTION_EXEMPT_FEATURES:
            continue
        for path in kotlin_files(presentation_dir(feature)):
            text = path.read_text()
            for match in TEXT_FIELD_CALL.finditer(text):
                # Match parens rather than splitting on the first ")": an argument list is full
                # of them (`onValueChange = { onEvent(NameChanged(it)) }`).
                start = match.end() - 1
                depth, end = 0, start
                for position in range(start, len(text)):
                    depth += (text[position] == "(") - (text[position] == ")")
                    if depth == 0:
                        end = position
                        break
                if "imeAction =" in text[start:end + 1]:
                    continue
                line = text[: match.start()].count("\n") + 1
                problems.append(
                    problem(
                        path,
                        line,
                        "AppTextField names no `imeAction` — `ImeAction.Next` on every field but "
                        "the form's last, `ImeAction.Done` on the last",
                    )
                )
    return problems


@check("every XState is @Immutable")
def check_state_immutability() -> list[str]:
    """Strong skipping covers most of it, but the annotation is what documents the intent.

    A state holds lists — `List<Product>` is an unstable type to the compiler, so without this the
    screen recomposes on every parent recomposition whether or not anything it shows has changed.
    """
    problems = []
    for feature in feature_names():
        for path in kotlin_files(presentation_dir(feature)):
            if not path.name.endswith("State.kt"):
                continue
            text = path.read_text()
            declaration = re.search(r"^data class (\w+State)", text, re.MULTILINE)
            if declaration and "@Immutable" not in text:
                problems.append(
                    problem(path, None, f"`{declaration.group(1)}` is not annotated @Immutable")
                )
    return problems


@check("every :core:ui component has a preview")
def check_component_previews() -> list[str]:
    """A component nobody can look at is a component nobody trusts.

    The preview is also what the gallery and `:core:ui`'s goldens are built on, so a
    missing one is a hole in both.
    """
    problems = []
    root = REPO_ROOT / "core/ui/src/main/kotlin" / BASE_PACKAGE.replace(".", "/") / "core/ui/component"
    for path in kotlin_files(root):
        text = path.read_text()
        if "@ComponentPreview" not in text:
            problems.append(problem(path, None, "has no @ComponentPreview"))
    return problems


# --------------------------------------------------------------------------------------------
# Provenance
# --------------------------------------------------------------------------------------------

# Identifiers that must never appear in this repo. This template's architecture evolved from a
# client codebase; a name carried in by a hurried copy-paste is the way that becomes a problem.
FOREIGN_IDENTIFIERS = [
    "mcedison",
    "prometheus",
    "com.mobile.plugin",
]


@check("no foreign project identifiers")
def check_foreign_identifiers() -> list[str]:
    problems = []
    for path in walk(REPO_ROOT):
        if path.suffix not in {".kt", ".kts", ".py", ".xml", ".toml", ".md", ".yml", ".pro"}:
            continue
        if path.name == "doctor.py":
            continue  # this list lives here
        lowered = path.read_text(errors="ignore").lower()
        for identifier in FOREIGN_IDENTIFIERS:
            if identifier in lowered:
                problems.append(problem(path, None, f"contains '{identifier}' — an identifier from another codebase"))
    return problems


# --------------------------------------------------------------------------------------------
# Build files
# --------------------------------------------------------------------------------------------

HARDCODED_COORDINATE = re.compile(
    r"^\s*(?:api|implementation|compileOnly|runtimeOnly|testImplementation|androidTestImplementation|debugImplementation)"
    r"\s*\(\s*(?:platform\(\s*)?\"[\w.\-]+:[\w.\-]+"
)


# Configuration a module must not set for itself: it belongs to a convention plugin in
# build-logic/, and 24 copies of it were the reason `minSdk` used to be 24 edits.
SHARED_ANDROID_CONFIG = re.compile(r"^\s*(compileSdk|minSdk|targetSdk|compileOptions|lint)\b\s*[({=]")

# resourcePrefix is genuinely per module — its value is the module's own name.
MODULE_OWNED_ANDROID_CONFIG = {"resourcePrefix"}


@check("no module build file repeats the shared Android configuration")
def check_no_duplicated_android_config() -> list[str]:
    """
    The convention plugins only pay for themselves while the modules stay thin: one build file that
    sets its own `compileSdk` drifts silently, and the next `minSdk` change misses it.
    """
    problems = []
    for path in build_files(REPO_ROOT):
        if path.parent == REPO_ROOT or "build-logic" in path.parts:
            continue
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            match = SHARED_ANDROID_CONFIG.match(line)
            if match and match.group(1) not in MODULE_OWNED_ANDROID_CONFIG:
                problems.append(
                    problem(path, number, f"sets {match.group(1)} — that belongs to a build-logic convention plugin")
                )
    return problems


# A catalog reference that is not a plugin alias — `libs.junit`, `libs.bundles.testing`,
# `platform(libs.androidx.compose.bom)` — and a hand-written test-fixtures block, which
# `convention.android.library.testfixtures` exists for.
LIBRARY_REFERENCE = re.compile(r"\blibs\.(?!plugins\.)")
TEST_FIXTURES_BLOCK = re.compile(r"^\s*testFixtures\s*\{")


@check("every module build file is a plugins block, its project dependencies and resourcePrefix")
def check_module_build_files_are_thin() -> list[str]:
    """
    A module build file is a `plugins` block and its project dependencies, plus its own
    `resourcePrefix`. Library dependencies live in the convention plugin the module applies — which
    is what makes a copied `service/` bring its build with it, and what keeps a feature from
    reaching for a library its layer is not allowed.
    """
    problems = []
    for path in build_files(REPO_ROOT):
        if path.parent == REPO_ROOT or "build-logic" in path.parts:
            continue
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            code = line.split("//", 1)[0]
            if LIBRARY_REFERENCE.search(code):
                problems.append(
                    problem(path, number, "names a library — that belongs to the convention plugin this module applies")
                )
            elif TEST_FIXTURES_BLOCK.match(code):
                problems.append(
                    problem(path, number, "enables test fixtures by hand — apply convention.android.library.testfixtures")
                )
    return problems


@check("no dependency version is hardcoded outside the version catalog")
def check_no_hardcoded_versions() -> list[str]:
    problems = []
    for path in build_files(REPO_ROOT):
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            if HARDCODED_COORDINATE.match(line):
                problems.append(problem(path, number, f"hardcoded coordinate: {line.strip()} — use libs.versions.toml"))
    return problems


# --------------------------------------------------------------------------------------------
# Presentation layout
# --------------------------------------------------------------------------------------------

# `@TabletPreview` is the fourth: a screen worth looking at in a wide window, where the
# scaffold's content column is capped and centred. It is an annotation rather than a bare
# `@Preview` so the width is written once.
PREVIEW_ANNOTATIONS = {"@ScreenPreview", "@ComponentPreview", "@TabletPreview", "@Preview"}


def screen_destinations(feature: str):
    """`(destination file, screen stem)` for every screen in a feature's presentation module."""
    directory = presentation_dir(feature)
    if not directory.is_dir():
        return
    for destination in walk(directory, "*Destination.kt"):
        if NOT_A_SCREEN.search(destination.name):
            continue
        yield destination, destination.name[: -len("Destination.kt")]


@check("every screen has a directory of its own, holding nothing else")
def check_screen_directories() -> list[str]:
    """
    A screen's six-file unit lives in a sub-package named after it, even when the feature has
    only one screen — so a second screen never forces a move, and every feature reads the same.

    A flat package lets a feature's own composable live in whichever screen file it was written
    in. The rule only holds while nothing else moves in beside a screen, which is what this
    checks: the directory holds that screen's six files and not one more.
    """
    problems = []
    for feature in feature_names():
        root = presentation_dir(feature)
        for destination, screen in screen_destinations(feature):
            directory = destination.parent
            if directory == root:
                problems.append(
                    problem(destination, None, f"screen '{screen}' sits in the presentation package root — give it a directory, as create_screen.py does")
                )
                continue
            own = {f"{screen}{suffix}.kt" for suffix in SCREEN_FILE_SUFFIXES}
            for path in sorted(directory.glob("*.kt")):
                if path.name not in own:
                    problems.append(
                        problem(path, None, f"is not part of screen '{screen}' — a screen's directory holds its own unit and nothing else; a composable goes to the feature's component/")
                    )
            for path in sorted(p for p in directory.iterdir() if p.is_dir()):
                problems.append(
                    problem(destination, None, f"screen '{screen}' has a nested directory '{path.name}' — a screen's directory holds its own unit and nothing else")
                )
    return problems


ASSERT_ACCESSIBLE_CALL = re.compile(r"\bassertAccessible\s*\(")


@check("every screen test asserts accessibility")
def check_screen_tests_assert_accessibility() -> list[str]:
    """
    An icon with no label or a 40 dp touch target is found by a reviewer or by nobody, while an
    `XScreenTest` already renders the screen with a fixed state. `assertAccessible()` from
    `:service:core:ui`'s test fixtures holds the two facts a screen reader and a thumb need, so
    every screen test that exists calls it: a screen test that never does is a screen nobody is
    checking for accessibility.
    """
    problems = []
    for feature in feature_names():
        tests = REPO_ROOT / "feature" / feature / "presentation/src/test/kotlin"
        if not tests.is_dir():
            continue
        for path in walk(tests, "*ScreenTest.kt"):
            if "build" in path.parts:
                continue
            if not ASSERT_ACCESSIBLE_CALL.search(path.read_text()):
                problems.append(
                    problem(path, None, "never calls assertAccessible() — every screen test holds names and touch targets in one `is accessible` test")
                )
    return problems


def top_level_composables(text: str) -> list[tuple[str, set[str], int]]:
    """`(name, annotations, line number)` for every top-level `@Composable fun` in a file."""
    found = []
    annotations: set[str] = set()
    for number, line in enumerate(text.split("\n"), start=1):
        if line.startswith("@"):
            annotations.add(line.split("(")[0].rstrip())
            continue
        match = re.match(r"(?:public |internal |private )?fun ([A-Za-z_]\w*)\s*[(<]", line)
        if match and "@Composable" in annotations:
            found.append((match.group(1), annotations, number))
        annotations = set()
    return found


@check("a screen file holds no composable but the screen and its previews")
def check_screen_files_hold_only_the_screen() -> list[str]:
    """
    The other half of the screen-directory rule. A screen file that grows its own composables is
    how a feature's own component ends up unpreviewable and unreachable from a second screen. Anything that is not the screen goes to the feature's `component/`, one to a file with
    a `@ComponentPreview`; a component a second feature wants goes to `:core:ui` through
    `create_component.py`.
    """
    problems = []
    for feature in feature_names():
        for destination, screen in screen_destinations(feature):
            for path in sorted(destination.parent.glob("*.kt")):
                for name, annotations, number in top_level_composables(path.read_text()):
                    if name == f"{screen}Screen" or annotations & PREVIEW_ANNOTATIONS:
                        continue
                    problems.append(
                        problem(path, number, f"composable '{name}' is not the screen — move it to feature/{feature}/presentation/.../component/ with create_component.py --feature {feature}")
                    )
    return problems



# Maestro flows
# --------------------------------------------------------------------------------------------

# Notes a check wants printed without failing the run. Advisory only — main() prints them after
# the checks, and they never change the exit code.
NOTES: list[str] = []

# The test ids no Maestro flow drives, filled by `check_maestro_ids_exist` and printed only on
# `--undriven`: the note carries the count, this carries the names.
UNDRIVEN_IDS: list[str] = []

MAESTRO_DIR = REPO_ROOT / ".maestro"

# `id: "login_emailField"` under a tapOn or an assertVisible, quoted or not. `appId:` does not
# match, because the key has to start the line.
MAESTRO_ID = re.compile(r'^\s*-?\s*id:\s*"?([^"\n]+?)"?\s*$')

# The three ways an id reaches the device: a literal tag, the screen's own name, and the tag
# constants the shared components expose (`ALERT_DIALOG_TAG` and friends). A literal is
# `testTag("x")` at a call site or `<something>TestTag = "x"` handed to a component that puts it on
# an element the caller's modifier cannot reach — `navigateUpTestTag`, `actionTestTag`.
TEST_TAG_LITERAL = re.compile(r'[tT]estTag\s*[(=]\s*"([^"]+)"')
SCREEN_ID_LITERAL = re.compile(r'screenId\s*=\s*"([^"]+)"')
TAG_CONSTANT = re.compile(r'const\s+val\s+[A-Z0-9_]*_TAG\s*(?::\s*String\s*)?=\s*"([^"]+)"')

# Ids a library's own screen carries, which a flow may find there and no source here declares:
# Play services' open-source licences list, which the release smoke flow opens.
LIBRARY_TEST_IDS = {"license_list"}


def main_source_kotlin_files():
    """Every `src/main` Kotlin file in the repo — what actually ships, so what a flow can drive."""
    for path in walk(REPO_ROOT, "*.kt"):
        parts = path.relative_to(REPO_ROOT).parts
        if "src" not in parts:
            continue
        if parts[parts.index("src") + 1] != "main":
            continue
        yield path


def declared_test_ids() -> set[str]:
    ids: set[str] = set()
    for path in main_source_kotlin_files():
        text = path.read_text()
        ids |= set(TEST_TAG_LITERAL.findall(text))
        ids |= set(SCREEN_ID_LITERAL.findall(text))
        ids |= set(TAG_CONSTANT.findall(text))
    return ids


@check("every Maestro id exists in the code")
def check_maestro_ids_exist() -> list[str]:
    """
    A flow finds by id, and nothing tells it the id is gone until an emulator runs it — an hour of
    CI, or a week if the flows are on a schedule. Renaming a `testTag` is a one-line change that
    breaks a flow silently, so the grep is the cheap half of the design's check four.

    The reverse — a tag no flow uses — is a note rather than a failure: most tags exist for the
    screen tests, and only the golden paths have flows.
    """
    if not MAESTRO_DIR.is_dir():
        return []

    used: dict[str, list[str]] = {}
    for path in sorted(MAESTRO_DIR.glob("*.yaml")):
        for number, line in enumerate(path.read_text().split("\n"), start=1):
            match = MAESTRO_ID.match(line)
            if match:
                used.setdefault(match.group(1), []).append(f"{relative_to_repo(path)}:{number}")

    declared = declared_test_ids()
    is_declared = declared_id_matcher(declared | LIBRARY_TEST_IDS)
    problems = [
        f"{where}: id '{identifier}' is in no testTag, screenId or tag constant"
        for identifier, locations in sorted(used.items())
        if not is_declared(identifier)
        for where in locations
    ]

    # A templated tag counts as driven when a flow taps any row it makes.
    driven = {
        identifier
        for identifier in declared
        if identifier in used or any(declared_id_matcher({identifier})(flow_id) for flow_id in used)
    }
    # The count, not the list: two hundred ids on every run is a page nobody reads. `python3 scripts/doctor.py --undriven` prints them when a flow is
    # being written.
    unused = sorted(declared - driven)
    if unused:
        UNDRIVEN_IDS.extend(unused)
        NOTES.append(f"{len(unused)} test id(s) no Maestro flow drives (--undriven lists them)")
    return problems


# The closed vocabulary an element test id ends in. Kinds of
# element, never the name of the component that draws one — a stepper, a slider, a segmented
# control and an accordion are all `Field` when a form holds them.
TEST_ID_VOCABULARY = {
    "Button", "Field", "Switch", "Checkbox", "List", "Item", "Tile", "Key", "Dialog", "Sheet",
    "Tab", "Badge", "Value", "Card", "Empty", "Skeleton", "Progress", "Group",
}

# The last camelCase word of an element: `Button` in `addToCartButton`, `item` in `item`.
LAST_CAMEL_WORD = re.compile(r"(?:^|[A-Z])[a-z0-9]*$")

# A row's own id after `item`: `cart_item_42`, `categories_item_beverages`,
# `weather_placeItem_praha` — the one element that may carry a domain id, so a flow can tap the
# row it means rather than the third one. `item` only: `cart_button_42` is still refused. In
# the source the id is a Kotlin template — `"${stem}_item_${row.id}"` — so the suffix may be one.
ROW_ID_SUFFIX = re.compile(r"^(.*[iI]tem)_(?:[A-Za-z0-9-]+|\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*)$")

# What a templated tag declares: `categories_item_${category.id}` is every `categories_item_<id>`.
KOTLIN_TEMPLATE = re.compile(r"\$\{[^}]*\}|\$[A-Za-z_][A-Za-z0-9_]*")


# A Maestro id is a regex: a flow that cannot know a row's id — one it created a moment ago —
# names the rows with `<stem>_item_.*` and picks by index.
MAESTRO_ROW_PATTERN = re.compile(r"^(.*[iI]tem_)\.\*$")


def declared_id_matcher(declared: set[str]):
    """
    A predicate over a flow's id: declared literally, by a templated tag it fits, or —
    for a `<stem>_item_.*` id — by a templated tag with that stem.
    """
    literal = {identifier for identifier in declared if not KOTLIN_TEMPLATE.search(identifier)}
    templated = [identifier for identifier in declared if KOTLIN_TEMPLATE.search(identifier)]
    patterns = [
        re.compile("^" + "[A-Za-z0-9-]+".join(re.escape(part) for part in KOTLIN_TEMPLATE.split(identifier)) + "$")
        for identifier in templated
    ]

    def is_declared(identifier: str) -> bool:
        if identifier in literal or any(pattern.match(identifier) for pattern in patterns):
            return True
        row = MAESTRO_ROW_PATTERN.match(identifier)
        return row is not None and any(t.startswith(row.group(1)) for t in templated)

    return is_declared


def element_kind_is_in_vocabulary(element: str) -> bool:
    """
    Folds case and plural before judging: a single-word element is lowercase (`cart_item`, not
    `cart_Item`), and a set of tabs is `tripDetail_tabs`. A reading that forgets either flags tags
    that are nothing of the kind. A row's `_<id>` suffix is stripped first, and only after `item`.
    """
    row = ROW_ID_SUFFIX.match(element)
    if row is not None:
        element = row.group(1)
    match = LAST_CAMEL_WORD.search(element)
    if match is None:
        return False
    word = match.group(0).lower()
    vocabulary = {entry.lower() for entry in TEST_ID_VOCABULARY}
    return word in vocabulary or (word.endswith("s") and word[:-1] in vocabulary)


@check("every element test id ends in a vocabulary word")
def check_test_id_vocabulary() -> list[str]:
    """
    An element id is `<screenStem>_<element>`, and the element ends in one of the eighteen words
    in `TEST_ID_VOCABULARY`. The vocabulary is worth having only while it stays closed — the
    property being that `settings_permissionsButton` can be guessed without opening the file —
    so a component's name never becomes a new word.

    Reads what ships: every `testTag` literal and tag constant in `src/main`. A screen id has no
    underscore and is checked by `check_screens_pass_screen_id`, so it is not looked at here.
    """
    problems = []
    for path in main_source_kotlin_files():
        for line_number, line in enumerate(path.read_text().split("\n"), start=1):
            for identifier in TEST_TAG_LITERAL.findall(line) + TAG_CONSTANT.findall(line):
                stem, separator, element = identifier.partition("_")
                if not separator or not stem or not element:
                    problems.append(
                        problem(path, line_number, f"test id '{identifier}' is not <screenStem>_<element>")
                    )
                elif not element_kind_is_in_vocabulary(element):
                    problems.append(
                        problem(
                            path,
                            line_number,
                            f"test id '{identifier}' ends in no vocabulary word; "
                            f"one of {', '.join(sorted(TEST_ID_VOCABULARY))}",
                        )
                    )
    return problems




# An element that is a control: a flow taps it, so it must sit on the node that answers. `Item` and
# `Tile` are left out — a row can be read-only — and so is a tag handed to a component as a
# parameter (`navigateUpTestTag`, `actionTestTag`): the component puts it on its own control.
CONTROL_ELEMENT = re.compile(r"(?:Button|Key|Switch|Checkbox|Tab)$")
MODIFIER_TEST_TAG = re.compile(r'\btestTag\s*\(\s*"([^"]+)"')

# What makes a call's node the one that answers a tap. `onAction` is not in it: a component that
# takes one has its control inside, and the caller's modifier goes to the container around it.
CLICK_HANDLER = re.compile(
    r"\b(?:onClick|onCheckedChange|onSelect|onSelected|onToggle|onLongClick)\s*="
    r"|\.(?:clickable|combinedClickable|toggleable|triStateToggleable|selectable)\s*\("
)


def enclosing_call(text: str, index: int) -> tuple[str, str]:
    """The call whose argument list holds `index`: its name and its text, from the name through
    the closing parenthesis and a trailing lambda, if one follows."""
    depth = 0
    for opening in range(index - 1, -1, -1):
        character = text[opening]
        if character == ")":
            depth += 1
        elif character == "(":
            if depth:
                depth -= 1
                continue
            start = opening
            while start and (text[start - 1].isalnum() or text[start - 1] in "_."):
                start -= 1
            end, level = opening, 0
            for end in range(opening, len(text)):
                level += {"(": 1, ")": -1}.get(text[end], 0)
                if level == 0:
                    break
            after = end + 1
            while after < len(text) and text[after] in " \t":
                after += 1
            if after < len(text) and text[after] == "{":
                level = 0
                for after in range(after, len(text)):
                    level += {"{": 1, "}": -1}.get(text[after], 0)
                    if level == 0:
                        break
                end = after
            return text[start:opening], text[start:end + 1]
    return "", ""


@check("an id that names a control sits on a node that can be clicked")
def check_control_ids_are_clickable() -> list[str]:
    """An element ending in `Button`, `Key`, `Switch`, `Checkbox` or `Tab`, set with
    `Modifier.testTag(...)`, sits on a call that takes a click handler or chains one.

    An id on the wrong node breaks a Maestro flow, while a screen test walks around it:
    `performClick` on a row lands on whatever is at its centre. The flows need an emulator; this
    runs at every commit. A syntactic reading — the call around the tag — so
    it cannot see into a component, which is why `onAction` does not count: the component takes
    the action and the caller's modifier lands on the container. Such a component takes an
    `actionTestTag` instead, as `AppSectionHeader` and `AppBottomActionBar` do.
    """
    problems = []
    for path in main_source_kotlin_files():
        text = path.read_text()
        for match in MODIFIER_TEST_TAG.finditer(text):
            identifier = match.group(1)
            if not CONTROL_ELEMENT.search(identifier):
                continue
            line_start = text.rfind("\n", 0, match.start()) + 1
            if text[line_start:match.start()].lstrip().startswith(("*", "//")):
                continue
            name, call = enclosing_call(text, match.start())
            if name.endswith("testTag"):
                # Out of `testTag(` itself, to the call whose modifier it is.
                name, call = enclosing_call(text, match.start() + len("testTag") - len(name))
            if not CLICK_HANDLER.search(call):
                problems.append(problem(
                    path, text.count("\n", 0, match.start()) + 1,
                    f"test id '{identifier}' names a control but sits on {name or 'a call'}, which takes "
                    "no click handler — put it on the control, or pass the component's own test tag",
                ))
    return problems

# --------------------------------------------------------------------------------------------
# Resource prefixes
# --------------------------------------------------------------------------------------------

# A resource name in a `values*` file, and the file name of anything outside `values*` — a
# drawable, a font, an xml/ document — which is the resource's name in its own right.
RESOURCE_NAME = re.compile(r'\bname="([^"]+)"')


def screen_prefixes(feature: str) -> set[str]:
    """
    The snake_case names a resource in this feature may start with.

    One per screen — `LoginScreen.kt` gives `login_`, `ProductDetailScreen.kt` gives
    `product_detail_` — plus the feature's own name, which is what a resource shared by two of its
    screens uses (`catalog_stale`). Read off the files rather than listed, so a screen added by
    `create_screen.py` needs no edit here and a screen in its own directory is still found.
    """
    sources = REPO_ROOT / "feature" / feature / "presentation/src/main/kotlin"
    prefixes = {to_snake(feature)}
    if sources.is_dir():
        prefixes.update(
            to_snake(path.stem.removesuffix("Screen"))
            for path in walk(sources, "*Screen.kt")
            if path.stem != "Screen" and "build" not in path.parts
        )
    return {prefix for prefix in prefixes if prefix}


@check("every feature resource is prefixed with its screen or its feature")
def check_feature_resource_prefixes() -> list[str]:
    """
    A feature's resources are prefixed with the screen or the feature they belong to.

    Not AGP's `resourcePrefix`, which allows one prefix per module: this repo prefixes by screen,
    so `:feature:auth:presentation` legitimately holds both `login_` and `sign_up_`. It also could
    not match the generators, which write `user_profile_title` into a directory called
    `userprofile` — a path-derived prefix would fail on the first feature anyone generates.
    """
    problems = []
    for feature in feature_names():
        res = REPO_ROOT / "feature" / feature / "presentation/src/main/res"
        if not res.is_dir():
            continue

        prefixes = screen_prefixes(feature)
        expected = " or ".join(sorted(f"{prefix}_" for prefix in prefixes))

        for path in walk(res):
            if not path.is_file() or "build" in path.parts:
                continue

            if path.parent.name.startswith("values"):
                names = [
                    (number, name)
                    for number, line in enumerate(path.read_text().split("\n"), start=1)
                    for name in RESOURCE_NAME.findall(line)
                ]
            else:
                # Outside `values*` the file is the resource: `xml/profile_file_paths.xml`.
                names = [(None, path.stem)]

            for number, name in names:
                if any(name == prefix or name.startswith(f"{prefix}_") for prefix in prefixes):
                    continue
                problems.append(problem(path, number, f"resource '{name}' is not prefixed {expected}"))
    return problems

# --------------------------------------------------------------------------------------------
# Analytics
# --------------------------------------------------------------------------------------------

APP_SCAFFOLD_CALL = re.compile(r"\bAppScaffold\s*\(")


@check("every screen passes screenId to its scaffold")
def check_screens_pass_screen_id() -> list[str]:
    """
    `AppScaffold` reports the screen view, and it reports nothing for a screen that did
    not name itself — so a missing `screenId` is a screen that is invisible in the funnel, and
    invisible in exactly the silent way that is noticed a quarter later.

    The same argument the id already had for testing: it is the one identifier the screen reader,
    the Maestro flow and now the analytics all read, so it is worth one argument per screen. A
    file that composes no `AppScaffold` is skipped rather than failed — a screen inside a
    `Scaffold` of its own is legitimate, and this check is about the scaffold that reports.
    """
    problems = []
    for feature in feature_names():
        sources = REPO_ROOT / "feature" / feature / "presentation/src/main/kotlin"
        if not sources.is_dir():
            continue

        for path in walk(sources, "*Screen.kt"):
            if "build" in path.parts or path.stem == "Screen":
                continue
            text = path.read_text()
            if not APP_SCAFFOLD_CALL.search(text):
                continue
            if not SCREEN_ID_LITERAL.search(text):
                problems.append(
                    problem(path, None, "composes AppScaffold without a screenId")
                )
    return problems


# --------------------------------------------------------------------------------------------
# Translations
# --------------------------------------------------------------------------------------------

# Czech has four CLDR plural categories against English's two, which is why it is the second locale: a
# `values-cs` that ships only `one` and `other` falls back to `other` at 2, and "2 položek" is
# the kind of wrong that reads as machine translation.
PLURAL_QUANTITIES = {
    "cs": {"one", "few", "many", "other"},
}


def default_string_files() -> list[Path]:
    """Every `res/values/strings.xml` in the repo, in path order."""
    return walk(REPO_ROOT, "src/main/res/values/strings.xml")


def resource_entries(path: Path) -> dict[str, str] | str:
    """
    `{name: tag}` for the `<string>` and `<plurals>` a resource file declares, or a message.

    Parsed rather than grepped, so a translation that forgot to escape an `&` fails here with the
    line number instead of a hundred lines into `mergeDebugResources`. A string marked
    `translatable="false"` is skipped: it is a brand name or a glyph, and asking a translator for
    a copy of it is how "—" ends up as "-" in one locale.
    """
    import xml.etree.ElementTree as ElementTree

    try:
        root = ElementTree.parse(path).getroot()
    except ElementTree.ParseError as error:
        return f"is not well-formed XML: {error}"

    return {
        element.get("name"): element.tag
        for element in root
        if element.tag in ("string", "plurals")
        and element.get("name")
        and element.get("translatable") != "false"
    }


@check("every string resource is translated into every locale")
def check_translations_are_complete() -> list[str]:
    """
    A module whose `values-cs` is missing, or is one string short of `values/`.

    Lint's `MissingTranslation` catches the second and not the first — a module with no
    `values-cs` at all is, as far as lint is concerned, a module that ships one locale, so the
    failure this exists to prevent is precisely the one lint is quiet about: a feature generated
    tomorrow whose Czech was never written. The reverse is checked too, because a name that
    exists only in `values-cs` is a string nothing will ever resolve.
    """
    problems = []
    for default in default_string_files():
        expected = resource_entries(default)
        if isinstance(expected, str):
            problems.append(problem(default, None, expected))
            continue

        for locale in TRANSLATED_LOCALES:
            translated = default.parent.parent / f"values-{locale}" / "strings.xml"
            if not translated.is_file():
                problems.append(
                    problem(default, None, f"has no values-{locale}/strings.xml beside it — every string ships in every locale")
                )
                continue

            actual = resource_entries(translated)
            if isinstance(actual, str):
                problems.append(problem(translated, None, actual))
                continue

            for name, tag in expected.items():
                if name not in actual:
                    problems.append(problem(translated, None, f"is missing '{name}'"))
                elif actual[name] != tag:
                    problems.append(problem(translated, None, f"declares '{name}' as <{actual[name]}>, not <{tag}>"))
            for name in actual:
                if name not in expected:
                    problems.append(problem(translated, None, f"translates '{name}', which no values/strings.xml declares"))
    return problems


@check("every translated plurals has the locale's full set of forms")
def check_translated_plurals_are_complete() -> list[str]:
    """
    A `<plurals>` short of a form does not fail to build: Android falls back to `other`, and the
    screen reads "2 položek" for the rest of the product's life. Czech wants one/few/many/other,
    and `many` — the decimal form — is the one every hand-written translation forgets.
    """
    import xml.etree.ElementTree as ElementTree

    problems = []
    for locale, quantities in PLURAL_QUANTITIES.items():
        for path in walk(REPO_ROOT, f"src/main/res/values-{locale}/strings.xml"):
            try:
                root = ElementTree.parse(path).getroot()
            except ElementTree.ParseError:
                continue  # Reported by the check above, with its message.

            for element in root:
                if element.tag != "plurals":
                    continue
                present = {item.get("quantity") for item in element}
                missing = sorted(quantities - present)
                if missing:
                    problems.append(
                        problem(path, None, f"plurals '{element.get('name')}' has no {', '.join(missing)} form")
                    )
                extra = sorted(present - quantities)
                if extra:
                    problems.append(
                        problem(path, None, f"plurals '{element.get('name')}' has a {', '.join(extra)} form, which {locale} never selects")
                    )
    return problems


# A `TopLevelDestination` entry: `Home(HomeDestination, R.string.tab_home, …)`.
TAB_ENTRY = re.compile(r"^    ([A-Z]\w*)\(", re.MULTILINE)

TAB_TEST_TAG = re.compile(r'testTag\s*=\s*"([^"]+)"')

TOP_LEVEL_DESTINATION_FILE = REPO_ROOT / "app/src/main/kotlin" / BASE_PACKAGE.replace(".", "/") / "TopLevelDestination.kt"


@check("every tab carries its own test id")
def check_tab_test_ids() -> list[str]:
    """Tests and flows find an element by id, never by its text.

    The tabs were the one place that broke it — four Maestro flows tapped the English labels, so a
    wording fix or the Czech locale turned them red for a reason that had nothing to do with the
    app. The tag lives on the enum entry rather than at the call site, which is what makes a fifth
    tab impossible to add without one.
    """
    # A copy started clean has one root and no tab bar until it adds one.
    if not TOP_LEVEL_DESTINATION_FILE.is_file():
        return []

    problems = []
    for line_number, line in enumerate(TOP_LEVEL_DESTINATION_FILE.read_text().split("\n"), start=1):
        entry = TAB_ENTRY.match(line)
        if not entry:
            continue
        name = entry.group(1)
        expected = f"tabs_{name[0].lower()}{name[1:]}Tab"
        tag = TAB_TEST_TAG.search(line)
        if tag is None:
            problems.append(problem(TOP_LEVEL_DESTINATION_FILE, line_number, f"tab {name} has no testTag; expected {expected}"))
        elif tag.group(1) != expected:
            problems.append(
                problem(TOP_LEVEL_DESTINATION_FILE, line_number, f"tab {name} is tagged '{tag.group(1)}'; expected '{expected}'")
            )
    return problems


GALLERY_CATALOG_FILE = (
    REPO_ROOT / "feature/gallery/presentation/src/main/kotlin" / BASE_PACKAGE.replace(".", "/")
    / "feature/gallery/presentation/GalleryCatalog.kt"
)

# `entry("id", "AppName", …)` — the second string is the file in `core/ui/component` the entry
# stands for. A `foundation(` page names a token rather than a component ("Colours", "Motion"), so
# it is skipped by construction: the word this matches is not in it.
GALLERY_ENTRY = re.compile(r'\bentry\(\s*\n?\s*"[^"]+",\s*\n?\s*"(\w+)"')

# The two files in `core/ui/component` that are deliberately not entries, for the reasons
# GalleryCatalog.kt's own KDoc gives: the scaffold is the shell every gallery page already is, and
# ControlSize is a scale rather than a component — it shows up as the size variants of the controls
# that read it.
NOT_GALLERY_ENTRIES = {"AppScaffold", "ControlSize"}


@check("every :core:ui component is in the gallery")
def check_gallery_lists_every_component() -> list[str]:
    """The gallery is the screen that shows every `:core:ui` component.

    Without a check, a component added in a hurry is invisible to the one screen whose job
    is to show every component — and to anyone deciding whether the thing they need already exists.
    `create_component.py` now writes a starter entry and this fails when one is missing, which is
    the pair that keeps it true rather than merely true today.
    """
    root = REPO_ROOT / "core/ui/src/main/kotlin" / BASE_PACKAGE.replace(".", "/") / "core/ui/component"
    if not root.is_dir() or not GALLERY_CATALOG_FILE.is_file():
        return []

    listed = set(GALLERY_ENTRY.findall(GALLERY_CATALOG_FILE.read_text()))
    problems = []
    for path in sorted(kotlin_files(root)):
        if path.stem in NOT_GALLERY_ENTRIES or path.stem in listed:
            continue
        problems.append(problem(path, None, "is in no gallery entry — run create_component.py, or add one by hand"))

    on_disk = {path.stem for path in kotlin_files(root)}
    for name in sorted(listed - on_disk):
        problems.append(problem(GALLERY_CATALOG_FILE, None, f"lists {name}, which is not a file in core/ui/component"))
    return problems


PLAYGROUND_CATALOG_FILE = (
    REPO_ROOT / "feature/devmenu/presentation/src/main/kotlin" / BASE_PACKAGE.replace(".", "/")
    / "feature/devmenu/presentation/PlaygroundCatalog.kt"
)

# The playground's entries are the gallery's shape — `entry("<id>", "<Pascal>", …)` — so one scan
# reads both.
PLAYGROUND_ENTRY = GALLERY_ENTRY

# The files in `core/ui/component` that are deliberately not on the bench: the two the gallery
# also leaves out, and the chrome, which is an object `AppTheme` installs rather than a composable
# with parameters to knob.
NOT_PLAYGROUND_ENTRIES = {"AppScaffold", "AppScreenChrome", "ControlSize"}

# A public `App*` composable at the top of a file — a file may declare more than one.
PUBLIC_APP_COMPOSABLE = re.compile(r"^fun (App\w+)\(", re.MULTILINE)


@check("every :core:ui component is in the playground")
def check_playground_lists_every_component() -> list[str]:
    """The dev menu's playground is the bench every component is put on, and the gallery
    check's twin: `create_component.py` writes a starter entry and this fails on a component with
    none, so the bench stays complete rather than complete today.
    """
    root = REPO_ROOT / "core/ui/src/main/kotlin" / BASE_PACKAGE.replace(".", "/") / "core/ui/component"
    if not root.is_dir() or not PLAYGROUND_CATALOG_FILE.is_file():
        return []

    listed = set(PLAYGROUND_ENTRY.findall(PLAYGROUND_CATALOG_FILE.read_text()))
    problems = []
    for path in sorted(kotlin_files(root)):
        if path.stem in NOT_PLAYGROUND_ENTRIES or path.stem in listed:
            continue
        problems.append(problem(path, None, "is in no playground entry — run create_component.py, or add one by hand"))

    # A bench entry may name a composable rather than a file — `AppTimeField` lives in
    # `AppDateField.kt` — so the other direction reads both.
    declared = {path.stem for path in kotlin_files(root)}
    for path in kotlin_files(root):
        declared.update(PUBLIC_APP_COMPOSABLE.findall(path.read_text()))
    for name in sorted(listed - declared):
        problems.append(problem(PLAYGROUND_CATALOG_FILE, None, f"lists {name}, which core/ui/component does not declare"))
    return problems


# `viewModelOf(::Foo)`, `singleOf(::Foo)`, `factoryOf(::Foo)` — Koin's constructor DSL.
KOIN_CONSTRUCTOR_DSL = re.compile(r"\b(?:viewModelOf|singleOf|factoryOf)\(::(\w+)\)")


@check("no class Koin constructs has a defaulted constructor parameter")
def check_koin_constructor_defaults() -> list[str]:
    """Koin's `*Of` builders resolve every constructor parameter through `get()`.

    They never consult a Kotlin default. So `class TripsViewModel(..., clock: Clock = systemUTC())`
    compiles, reads as safe, passes `KoinGraphTest` — `verify()` treats a defaulted parameter as
    already satisfied — and throws `NoDefinitionFoundException` the first time the screen opens.
    That is exactly how the Trips tab crashed: three view models defaulted a `Clock` nobody bound,
    and the feature was unreachable, so nothing constructed them until a tab existed.

    Bind the type instead, and let the constructor say what it needs.
    """
    constructed = set()
    for path in walk(REPO_ROOT / "core", "*.kt") + walk(REPO_ROOT / "feature", "*.kt"):
        if "/src/main/" not in path.as_posix():
            continue
        constructed.update(KOIN_CONSTRUCTOR_DSL.findall(path.read_text()))
    if not constructed:
        return []

    problems = []
    for directory in ("core", "feature", "service", "app"):
        for path in walk(REPO_ROOT / directory, "*.kt"):
            if "/src/main/" not in path.as_posix():
                continue
            lines = path.read_text().splitlines()
            for number, line in enumerate(lines, 1):
                name = line.partition("class ")[2].partition("(")[0].strip()
                if not line.lstrip().startswith("class ") or name not in constructed:
                    continue
                # A one-line constructor closes on the `class` line itself; a wrapped one runs to
                # the line that closes it — `) : Base<...>` or `) {`. Without the first case the
                # scan walks into the class body and reads every named argument as a default.
                if ")" in line:
                    parameters = [line.partition("(")[2].rpartition(")")[0]]
                else:
                    parameters = []
                    for parameter in lines[number:]:
                        if parameter.startswith(")"):
                            break
                        parameters.append(parameter)
                for parameter in parameters:
                    if " = " in parameter:
                        problems.append(
                            problem(
                                path,
                                number,
                                f"{name} is built by Koin's *Of DSL and defaults "
                                f"`{parameter.strip().rstrip(',')}` — bind the type instead",
                            )
                        )
    return problems


# --------------------------------------------------------------------------------------------


def hooks_path_note() -> str | None:
    """A note, never a failure: CI's checkout has no hooks and must stay green. Comparing
    `core.hooksPath` to the literal string `.githooks` missed a clone whose value is that same
    directory spelled as an absolute path — what a worktree sees — so the hook ran on every
    commit there while this note reported it unset. `git rev-parse --git-path hooks` is
    the directory git will actually invoke the hook from, relative or absolute the same way
    `core.hooksPath` itself resolves one; resolving both sides is what makes the absolute twin of
    the committed relative path read as a match rather than a second miss.
    """
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--git-path", "hooks"], cwd=REPO_ROOT, capture_output=True, text=True
        )
    except (OSError, FileNotFoundError):
        return None
    if not (REPO_ROOT / ".git").exists():
        return None
    if result.returncode == 0 and hooks_dir_is_ours((REPO_ROOT / result.stdout.strip()).resolve()):
        return None
    return "the pre-commit hook is not installed on this clone: git config core.hooksPath .githooks"


def hooks_dir_is_ours(hooks: Path) -> bool:
    """
    This checkout's `.githooks`, or another checkout's carrying the same committed hook: a
    worktree shares its main checkout's config, so its `core.hooksPath` names the main
    checkout's directory as an absolute path, and the hook that runs on every commit there is
    the one this repository ships. What is checked is the hook, not the spelling of its path.
    """
    ours = REPO_ROOT / ".githooks"
    if hooks == ours.resolve():
        return True
    if hooks.name != ".githooks" or not (hooks / "pre-commit").is_file():
        return False
    return (hooks / "pre-commit").read_bytes() == (ours / "pre-commit").read_bytes()


# A top-level Kotlin declaration: no leading whitespace, optional annotations and modifiers, then
# the keyword. Kotlin's default visibility is public, so a declaration is public unless it says
# otherwise — which is why the modifiers are captured rather than skipped.
TOP_LEVEL_DECLARATION = re.compile(
    r"^((?:@\w+(?:\([^)]*\))?\s+)*(?:\w+\s+)*?)(class|interface|object|fun|val|var|typealias)\s",
    re.MULTILINE,
)

NOT_PUBLIC = ("private", "internal", "protected")


def print_module_shapes() -> None:
    """One row per module: how many files it is spread over and how much of it is public.

    A module is deep when a lot of behaviour sits behind a small interface, and shallow when a
    little sits behind a large one. Nothing in the tree measured that, so nothing pushed back on
    the habit of adding a file rather than widening a module — and a tree of many small public
    surfaces is hard to navigate, because a reader has to reach the right file before they can
    read anything.

    This is a report, never a check: no number here is wrong on its own. Read the top of the list,
    ask whether those public names are one idea or several, and fold what belongs together behind
    one interface.
    """
    rows = []
    for build_file in sorted(walk(REPO_ROOT, "build.gradle.kts")):
        module = build_file.parent
        if module == REPO_ROOT or "build-logic" in module.parts:
            continue
        sources = module / "src/main/kotlin"
        if not sources.is_dir():
            continue
        files = sorted(walk(sources, "*.kt"))
        if not files:
            continue
        public = 0
        for path in files:
            for modifiers, _ in TOP_LEVEL_DECLARATION.findall(path.read_text()):
                if not any(word in modifiers.split() for word in NOT_PUBLIC):
                    public += 1
        name = ":" + str(module.relative_to(REPO_ROOT)).replace("/", ":")
        rows.append((name, len(files), public))

    rows.sort(key=lambda row: (-row[2], -row[1]))
    width = max((len(row[0]) for row in rows), default=10)
    print(f"{'module'.ljust(width)}  files  public  per file")
    for name, files, public in rows:
        print(f"{name.ljust(width)}  {files:5d}  {public:6d}  {public / files:8.1f}")
    print()
    print("Widest first. A module near the top with names that are not one idea is the next fold;")
    print("read the interface before the implementation.")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Check this project's architectural conventions.",
        epilog=(
            'Examples:\n'
            '  python3 scripts/doctor.py\n'
            '  python3 scripts/doctor.py --list\n'
            '  python3 scripts/doctor.py --undriven\n'
            '\n'
            'Exits non-zero, so it gates CI and the pre-commit hook. If a check looks wrong, fix the check\n'
            'rather than working around it — each one exists because a compiler cannot catch it.'
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--list", action="store_true", help="List the checks and exit.")
    parser.add_argument(
        "--modules", action="store_true",
        help="Print each module's shape — files, public declarations, and the ratio — widest first, and exit.",
    )
    parser.add_argument(
        "--undriven", action="store_true",
        help="After the checks, list the test ids no Maestro flow drives, one per line.",
    )
    args = parser.parse_args()

    if args.list:
        for name, _ in CHECKS:
            print(f"  {name}")
        return

    if args.modules:
        print_module_shapes()
        return

    failed = 0
    for name, function in CHECKS:
        problems = function()
        if problems:
            failed += 1
            print(f"[FAIL] {name}")
            for line in problems:
                print(f"         {line}")
        else:
            print(f"[ ok ] {name}")

    hook_note = hooks_path_note()
    if hook_note:
        NOTES.append(hook_note)
    for note in NOTES:
        print(f"[note] {note}")
    if args.undriven:
        for identifier in UNDRIVEN_IDS:
            print(f"       {identifier}")

    print()
    if failed:
        print(f"{failed} of {len(CHECKS)} checks failed.")
        sys.exit(1)
    print(f"All {len(CHECKS)} checks passed.")


if __name__ == "__main__":
    main()
