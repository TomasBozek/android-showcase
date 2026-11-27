#!/usr/bin/env python3
"""
Scaffolds a reusable service module under `service/` and makes the registrations a hand copy skips.

    python3 scripts/create_service.py work --android
    python3 scripts/create_service.py pricing --jvm
    python3 scripts/create_service.py scanner --android --dry-run

A service module is one capability: a port, its default implementation and a `Fake<Name>`
in `testFixtures`, in a directory, a package and a namespace that agree, applying a
`convention.service`, which builds it from its row in `build-logic`'s `ServiceModules.kt`, so a
copied `service/` brings its build. This writes all of that and registers it in the
three places that know about modules — that row, `settings.gradle.kts` and `:core:di`'s build
file. `doctor.py` checks the row; `export_service.py` copies whatever is under `service/`.

`--android` is an Android library, for a capability that needs the framework; `--jvm` a Kotlin/JVM
module, which the compiler then keeps free of `android.*`. The port and the implementation are
placeholders: rename the operation, write the real one, bind it in `Koin.kt`.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from _common import (  # noqa: E402
    BASE_PACKAGE,
    BASE_PATH,
    CORE_DI_BUILD_FILE,
    REPO_ROOT,
    SETTINGS_FILE,
    edit_file,
    relative_to_repo,
    to_flat,
    to_pascal,
    write_file,
)

SERVICE_MODULES_FILE = REPO_ROOT / "build-logic/src/main/kotlin/ServiceModules.kt"

# A new row goes last in the table.
ROWS_ANCHOR = ").associateBy { it.path }"
SETTINGS_ANCHOR = "// What is genuinely this app's"
CORE_DI_DEPENDENCY = re.compile(r"^    implementation\(projects\.service\.[\w.]+\)\n", re.MULTILINE)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scaffold a reusable service module and register it.",
        epilog=(
            "Examples:\n"
            "  python3 scripts/create_service.py work --android\n"
            "  python3 scripts/create_service.py pricing --jvm\n"
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("name", help="The capability, in any case: work, codeScanner, code-scanner")
    kind = parser.add_mutually_exclusive_group(required=True)
    kind.add_argument("--android", action="store_true", help="An Android library, for a capability that needs the framework.")
    kind.add_argument("--jvm", action="store_true", help="A Kotlin/JVM module, kept free of android.* by the compiler.")
    parser.add_argument("--dry-run", action="store_true", help="Show what would happen, change nothing.")
    parser.add_argument("--force", action="store_true", help="Overwrite a module that already exists.")
    return parser.parse_args()


def service_row(flat: str, android: bool) -> str:
    """The module's row in `ServiceModules.kt`: test fixtures for its `Fake`, and on Android the
    `testing` bundle, which `convention.kotlin.jvm` already carries for a JVM module."""
    base = "AndroidLibrary" if android else "KotlinJvm"
    testing = "        testingBundle()\n" if android else ""
    return (
        f"    // Written by create_service.py: say why :service:{flat}'s build is the way it is.\n"
        f'    service(":service:{flat}", ServiceBase.{base}) {{\n'
        "        testFixtures()\n"
        f"{testing}"
        "    },\n"
    )


def port_source(package: str, pascal: str) -> str:
    return (
        f"package {package}\n"
        "\n"
        "/**\n"
        f" * The {pascal} port — what a caller names. Written by `create_service.py`: replace the\n"
        " * placeholder operation with the capability's own, here, in the default and in the fake.\n"
        " */\n"
        f"interface {pascal} {{\n"
        "\n"
        "    fun describe(): String\n"
        "}\n"
    )


def default_source(package: str, pascal: str) -> str:
    return (
        f"package {package}\n"
        "\n"
        f"/** The [{pascal}] a build binds. */\n"
        f"class Default{pascal} : {pascal} {{\n"
        "\n"
        f'    override fun describe(): String = "{pascal}"\n'
        "}\n"
    )


def fake_source(package: str, pascal: str) -> str:
    return (
        f"package {package}.test\n"
        "\n"
        f"import {package}.{pascal}\n"
        "\n"
        f"/** A [{pascal}] a test scripts, beside the port it fakes. */\n"
        f"class Fake{pascal}(var description: String = \"fake\") : {pascal} {{\n"
        "\n"
        "    override fun describe(): String = description\n"
        "}\n"
    )


def test_source(package: str, pascal: str) -> str:
    return (
        f"package {package}\n"
        "\n"
        f"import {package}.test.Fake{pascal}\n"
        "import org.junit.Assert.assertEquals\n"
        "import org.junit.Test\n"
        "\n"
        f"class Default{pascal}Test {{\n"
        "\n"
        "    @Test\n"
        "    fun `the default and the fake answer through the port`() {\n"
        f'        assertEquals("{pascal}", Default{pascal}().describe())\n'
        f'        assertEquals("scripted", (Fake{pascal}("scripted") as {pascal}).describe())\n'
        "    }\n"
        "}\n"
    )


def build_file(flat: str) -> str:
    return "plugins {\n    alias(libs.plugins.convention.service)\n}\n"


def insert_before(text: str, anchor: str, entry: str) -> str:
    if entry in text:
        return text
    index = text.find(anchor)
    if index < 0:
        sys.exit(f"anchor not found: {anchor!r}")
    return text[:index] + entry + text[index:]


def main() -> None:
    args = parse_args()
    flat = to_flat(args.name)
    pascal = to_pascal(args.name)
    if flat in ("core", "log") or not re.fullmatch(r"[a-z][a-z0-9]*", flat):
        sys.exit(f"'{args.name}' cannot be a service module name")
    android = args.android
    module_dir = REPO_ROOT / "service" / flat
    if module_dir.exists() and not args.force:
        sys.exit(f"{relative_to_repo(module_dir)} already exists; --force to overwrite")

    package = f"{BASE_PACKAGE}.service.{flat}"
    source = f"{BASE_PATH}/service/{flat}"
    print(f"{'Would create' if args.dry_run else 'Creating'} :service:{flat} ({'Android' if android else 'Kotlin/JVM'})")

    write_file(module_dir / "build.gradle.kts", build_file(flat), args.dry_run)
    write_file(module_dir / "src/main/kotlin" / source / f"{pascal}.kt", port_source(package, pascal), args.dry_run)
    write_file(module_dir / "src/main/kotlin" / source / f"Default{pascal}.kt", default_source(package, pascal), args.dry_run)
    write_file(module_dir / "src/testFixtures/kotlin" / source / f"test/Fake{pascal}.kt", fake_source(package, pascal), args.dry_run)
    write_file(module_dir / "src/test/kotlin" / source / f"Default{pascal}Test.kt", test_source(package, pascal), args.dry_run)
    row = service_row(flat, android)
    edit_file(SERVICE_MODULES_FILE, lambda t: insert_before(t, ROWS_ANCHOR, row), args.dry_run, "ServiceModules.kt row")

    settings_entry = f'// Written by create_service.py: say what :service:{flat} is.\nincludeModule(":service:{flat}", "service/{flat}")\n\n'
    edit_file(SETTINGS_FILE, lambda t: insert_before(t, SETTINGS_ANCHOR, settings_entry), args.dry_run, "settings")

    def add_core_di(text: str) -> str:
        line = f"    implementation(projects.service.{flat})\n"
        if line in text:
            return text
        matches = list(CORE_DI_DEPENDENCY.finditer(text))
        if not matches:
            sys.exit(f"no service dependency in {relative_to_repo(CORE_DI_BUILD_FILE)} to add beside")
        end = matches[-1].end()
        return text[:end] + line + text[end:]

    edit_file(CORE_DI_BUILD_FILE, add_core_di, args.dry_run, ":core:di dependency")

    print()
    print("Next:")
    print(f"  1. Replace describe() with the capability's operations in {pascal}, Default{pascal} and Fake{pascal}.")
    print(f"  2. Bind Default{pascal} in core/di's Koin.kt, and give it its first caller.")
    print(f"  3. Say what it is in settings.gradle.kts, and its row's comment in ServiceModules.kt.")


if __name__ == "__main__":
    main()
