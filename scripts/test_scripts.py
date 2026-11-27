#!/usr/bin/env python3
"""
Smoke tests for the scaffolding scripts.

    python3 scripts/test_scripts.py
    python3 scripts/test_scripts.py -v ScaffoldingTest.test_delete_feature_restores_every_registration
    python3 scripts/test_scripts.py --with-gradle

Each test copies the repository's sources into a temporary directory and runs the scripts there as
subprocesses, so nothing touches the working tree. They check the registrations and the generated
text — not that the result compiles; the Gradle build is still the real gate.

`--with-gradle` adds the one test that does compile what a generator wrote. It costs minutes
rather than seconds, so it is off by default and CI runs it on the weekly schedule only.

Plain `unittest`, so there is no dependency to install.
"""

from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
BASE_PATH = "com/example/androidproject1"

# Read here rather than in `__main__`, because `skipUnless` below is evaluated when the class is
# defined — which happens on import, before `__main__` would have had a chance to look at argv.
WITH_GRADLE = "--with-gradle" in sys.argv
if WITH_GRADLE:
    sys.argv.remove("--with-gradle")

# Built rather than written out: init_project.py rewrites the literal forms, so spelling them here
# would leave this file asserting against whatever the project was renamed to.
TEMPLATE_PACKAGE_WORD = "android" + "project1"
TEMPLATE_PROJECT_NAME = "Android" + "Project1"

# Build output and IDE state; everything else is copied so the scripts see a realistic repo.
# `screenshots` is `:core:ui`'s Roborazzi goldens — megabytes of PNG that no script reads,
# copied once per test.
_IGNORED_NAMES = shutil.ignore_patterns(
    "build", ".gradle", ".git", ".idea", ".kotlin", "__pycache__", ".DS_Store", "screenshots"
)


def ignore_for_copy(directory: str, names: list[str]) -> set[str]:
    """
    The patterns above, plus `.claude/worktrees`.

    A nested worktree is a checkout of this same repository, so copying one makes the copy mostly
    copies of itself, and the suite many times slower with nothing failing to say so.
    `test_the_repository_copy_stays_small` is what catches that.
    """
    ignored = set(_IGNORED_NAMES(directory, names))
    if Path(directory).name == ".claude":
        ignored.add("worktrees")
    return ignored


# What one test's copy of the repository may hold, with room for the project to grow. A directory
# that pushes it past this belongs in `ignore_for_copy` above.
MAX_COPIED_FILES = 1700


def hermetic_env() -> dict[str, str]:
    """
    The ambient environment minus git's own variables.

    A git hook runs with GIT_DIR, GIT_INDEX_FILE and friends set, and in a worktree GIT_DIR is an
    absolute path. Every `git` these tests run inside their temp copy would otherwise operate on
    the real repository: `git init` initialising the wrong gitdir, `git status` reporting the
    commit in progress as uncommitted changes and making init_project.py refuse. That is what made
    the pre-commit hook fail on exactly the commits it exists to check.
    """
    return {key: value for key, value in os.environ.items() if not key.startswith("GIT_")}


def ktlint_order(imports: list[str]) -> list[str]:
    """
    What `standard:import-ordering` accepts, under the default layout -- `.editorconfig` sets no
    `ij_kotlin_imports_layout`, so ktlint falls back to `*,java.**,javax.**,kotlin.**,^`: every
    import sorted lexicographically by its dotted path, except that `java.`, `javax.` and
    `kotlin.` imports (and an alias of one of them) sort after every other import rather than in
    their alphabetical place -- `kotlinx.coroutines.delay` is not one of them, and stays in the
    first group. Plain `sorted(imports)` does not know this and fails on any such import.
    """

    def path_of(line: str) -> str:
        return line.removeprefix("import ").split(" as ")[0].strip()

    def key(line: str) -> tuple[bool, str]:
        path = path_of(line)
        return (path.startswith(("java.", "javax.", "kotlin.")), path)

    return sorted(imports, key=key)


class ScaffoldingTest(unittest.TestCase):

    def setUp(self) -> None:
        self._temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self._temporary.cleanup)
        self.repo = Path(self._temporary.name) / "repo"
        shutil.copytree(REPO_ROOT, self.repo, ignore=ignore_for_copy)

    def test_doctor_lets_a_service_layer_take_its_sibling_but_not_a_higher_layer(self) -> None:
        """`:service:core:domain` may re-export `:service:log:domain`, a sibling of the same
        layer; a domain that takes another service's data is still a step up."""
        build = self.repo / "service/core/domain/build.gradle.kts"
        build.write_text(build.read_text() + "\ndependencies {\n    api(projects.service.log.domain)\n}\n")
        self.run_script("doctor.py")
        build.write_text(build.read_text().replace("projects.service.log.domain", "projects.service.log.data"))
        result = self.run_script("doctor.py", expect_success=False)
        self.assertIn("service domain must not depend on data", result.stdout)

    def test_create_service_writes_a_module_doctor_accepts(self) -> None:
        """Both flavours of service module, every registration, and doctor green on the result."""
        self.run_script("create_service.py", "codeScanner", "--android")
        self.run_script("create_service.py", "pricing", "--jvm")
        for flat, pascal in (("codescanner", "CodeScanner"), ("pricing", "Pricing")):
            module = self.repo / "service" / flat
            source = f"{BASE_PATH}/service/{flat}"
            self.assertEqual("plugins {\n    alias(libs.plugins.convention.service)\n}\n", (module / "build.gradle.kts").read_text())
            self.assertTrue((module / "src/main/kotlin" / source / f"{pascal}.kt").is_file())
            self.assertTrue((module / "src/main/kotlin" / source / f"Default{pascal}.kt").is_file())
            self.assertTrue((module / "src/testFixtures/kotlin" / source / f"test/Fake{pascal}.kt").is_file())
            self.assertIn(f'service(":service:{flat}", ', self.read("build-logic/src/main/kotlin/ServiceModules.kt"))
            self.assertIn(f'includeModule(":service:{flat}", "service/{flat}")', self.read("settings.gradle.kts"))
            self.assertIn(f"implementation(projects.service.{flat})", self.read("core/di/build.gradle.kts"))
        # A row in the one table, not a plugin of its own.
        table = self.read("build-logic/src/main/kotlin/ServiceModules.kt")
        self.assertIn('service(":service:codescanner", ServiceBase.AndroidLibrary) {\n        testFixtures()\n        testingBundle()\n    },', table)
        self.assertIn('service(":service:pricing", ServiceBase.KotlinJvm) {\n        testFixtures()\n    },', table)
        self.assertFalse((self.repo / "build-logic/src/main/kotlin/ServiceCodeScannerConventionPlugin.kt").exists())
        self.run_script("doctor.py")

        # Run twice, it refuses rather than writing a second registration.
        result = self.run_script("create_service.py", "pricing", "--jvm", expect_success=False)
        self.assertIn("already exists", result.stderr)

    def test_create_service_dry_run_changes_nothing_and_doctor_catches_a_hand_copy(self) -> None:
        before = self.read("settings.gradle.kts")
        self.run_script("create_service.py", "pricing", "--jvm", "--dry-run")
        self.assertEqual(before, self.read("settings.gradle.kts"))
        self.assertFalse((self.repo / "service/pricing").exists())

        # A module copied by hand, registered in settings but with no plugin or row of its own.
        (self.repo / "service/pricing").mkdir()
        (self.repo / "service/pricing/build.gradle.kts").write_text("plugins {\n    alias(libs.plugins.convention.kotlin.jvm)\n}\n")
        settings = self.repo / "settings.gradle.kts"
        settings.write_text(settings.read_text() + '\nincludeModule(":service:pricing", "service/pricing")\n')
        result = self.run_script("doctor.py", expect_success=False)
        self.assertIn("applies no `libs.plugins.convention.service`", result.stdout)
        self.assertIn("has no row for :service:pricing", result.stdout)

    # -- helpers ------------------------------------------------------------------------------

    def run_script(self, name: str, *args: str, expect_success: bool = True) -> subprocess.CompletedProcess:
        result = subprocess.run(
            [sys.executable, str(self.repo / "scripts" / name), *args],
            capture_output=True,
            text=True,
            env=hermetic_env(),
        )
        if expect_success and result.returncode != 0:
            self.fail(f"{name} {' '.join(args)} failed:\n{result.stdout}\n{result.stderr}")
        return result

    def read(self, relative: str) -> str:
        return (self.repo / relative).read_text()

    def presentation(self, feature: str, source_set: str = "main") -> Path:
        return self.repo / f"feature/{feature}/presentation/src/{source_set}/kotlin/{BASE_PATH}/feature/{feature}/presentation"

    def screen(self, feature: str, name: str, source_set: str = "main") -> Path:
        """A screen's directory: `presentation/<the screen name, flat lowercase>/`."""
        return self.presentation(feature, source_set) / name.lower()

    def route_key(self, feature: str, key: str) -> str:
        """A route key's file in the feature's api module."""
        return (self.repo / f"feature/{feature}/api/src/main/kotlin/{BASE_PATH}/feature/{feature}/api/{key}.kt").read_text()

    def component(self, feature: str) -> Path:
        """A feature's own components: `presentation/component/`."""
        return self.presentation(feature) / "component"

    def assert_doctor_passes(self) -> None:
        result = self.run_script("doctor.py", expect_success=False)
        self.assertEqual(0, result.returncode, f"doctor.py reported problems:\n{result.stdout}")

    def resource_names(self, strings_xml: str) -> set[str]:
        return set(re.findall(r'<(?:string|plurals) name="([^"]+)"', strings_xml))

    def tree_snapshot(self) -> set[tuple[str, int]]:
        return {
            (str(p.relative_to(self.repo)), p.stat().st_size)
            for p in self.repo.rglob("*")
            if p.is_file() and "__pycache__" not in p.parts
        }

    # -- tests --------------------------------------------------------------------------------

    def test_doctor_passes_on_a_clean_checkout(self) -> None:
        self.assert_doctor_passes()

    def test_the_repository_copy_stays_small(self) -> None:
        """
        Every test copies the repository, so anything that joins the copy is paid 56 times over.
        This asserts the thing that is easy to regress and impossible to notice: the suite has no
        other way of telling you it has become fourteen times slower, because it still passes.
        """
        copied = sum(len(names) for _, _, names in os.walk(self.repo))
        self.assertLess(
            copied,
            MAX_COPIED_FILES,
            f"a test's copy of the repository holds {copied} files, over the {MAX_COPIED_FILES} "
            "budget. Add whatever directory grew to `ignore_for_copy`, or raise the budget in the "
            "same commit that grew the repository, and say which directory it was.",
        )

    def test_create_feature_registers_every_module(self) -> None:
        self.run_script("create_feature.py", "userProfile")

        for layer in ("api", "domain", "data", "presentation", "di"):
            self.assertTrue((self.repo / f"feature/userprofile/{layer}/build.gradle.kts").is_file(), layer)

        self.assertIn('includeFeatureModule(\n    "userprofile",', self.read("settings.gradle.kts"))
        self.assertIn("api(projects.feature.userprofile.di)", self.read("core/di/build.gradle.kts"))

        koin = self.read(f"core/di/src/main/kotlin/{BASE_PATH}/core/di/Koin.kt")
        self.assertIn("UserProfileModule.module,", koin)
        self.assertIn("import com.example.androidproject1.feature.userprofile.di.UserProfileModule", koin)

        # camelCase, the same spelling create_screen.py produces; the package stays flat.
        nav_host = self.read(f"app/src/main/kotlin/{BASE_PATH}/AppNavHost.kt")
        self.assertIn("userProfileDestination(navigator = navigator)", nav_host)
        self.assertIn(
            "import com.example.androidproject1.feature.userprofile.presentation.userprofile.userProfileDestination",
            nav_host,
        )
        self.assertNotIn("userprofileDestination(", nav_host)

        destination = (self.screen("userprofile", "UserProfile") / "UserProfileDestination.kt").read_text()
        self.assertIn("fun EntryProviderScope<NavKey>.userProfileDestination(", destination)

        self.assert_doctor_passes()

    def test_create_feature_puts_the_route_key_in_its_api_module(self) -> None:
        """A feature's route keys live in its api module, the one another feature may name."""
        self.run_script("create_feature.py", "userProfile")

        settings = self.read("settings.gradle.kts")
        block = settings[settings.index('includeFeatureModule(\n    "userprofile",'):]
        self.assertIn("ModuleSuffix.Api,", block[: block.index(")")])

        key = self.repo / f"feature/userprofile/api/src/main/kotlin/{BASE_PATH}/feature/userprofile/api/UserProfileDestination.kt"
        self.assertIn("data object UserProfileDestination : NavKey", key.read_text())
        self.assertIn("convention.feature.api", self.read("feature/userprofile/api/build.gradle.kts"))
        self.assertIn("api(projects.feature.userprofile.api)", self.read("feature/userprofile/presentation/build.gradle.kts"))

        destination = (self.screen("userprofile", "UserProfile") / "UserProfileDestination.kt").read_text()
        self.assertIn("import com.example.androidproject1.feature.userprofile.api.UserProfileDestination", destination)
        self.assertNotIn(": NavKey", destination)
        # A plain screen is no list pane until it says so: a list pane draws a second pane.
        self.assertIn("entry<UserProfileDestination> {", destination)

        # A second screen's key joins the first in the api module, and doctor holds the module's shape.
        self.run_script("create_screen.py", "userprofile", "UserProfileDetail", "--with-args", "userId:String")
        detail_key = key.with_name("UserProfileDetailDestination.kt").read_text()
        self.assertIn("data class UserProfileDetailDestination(val userId: String) : NavKey", detail_key)
        graph_test = self.read(f"app/src/test/kotlin/{BASE_PATH}/KoinGraphTest.kt")
        self.assertIn("import com.example.androidproject1.feature.userprofile.api.UserProfileDetailDestination", graph_test)
        self.assert_doctor_passes()

        key.with_name("Helper.kt").write_text("package x\n\nfun helper() = 1\n")
        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("fun helper — an api module holds route keys", result.stdout)

    def test_create_component_lists_a_core_ui_component_in_the_gallery(self) -> None:
        """The one registration a component has: doctor.py fails on a component with none."""
        self.run_script("create_component.py", "PriceTicker", "--group", "content")

        catalog = self.read(
            "feature/gallery/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/gallery/presentation/GalleryCatalog.kt"
        )
        self.assertIn('"priceTicker", "PriceTicker", GalleryGroup.Content', catalog)
        self.assert_doctor_passes()

    def test_create_component_gallery_entry_names_a_group_the_enum_declares(self) -> None:
        """The starter's group is an enum member, not a string the catalogue invented: a
        generator that wrote a string could put a component in a group nobody had declared.
        """
        self.run_script("create_component.py", "PriceTicker", "--group", "content")

        enum = self.read(
            "feature/gallery/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/gallery/presentation/GalleryGroup.kt"
        )
        catalog = self.read(
            "feature/gallery/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/gallery/presentation/GalleryCatalog.kt"
        )
        group = re.search(r'"priceTicker", "PriceTicker", GalleryGroup\.(\w+)', catalog)
        self.assertIsNotNone(group, "the starter entry names no GalleryGroup member")
        self.assertIn(f"    {group.group(1)}(", enum)
        self.assert_doctor_passes()

    def test_create_component_gallery_entry_names_the_import_and_the_required_parameter(self) -> None:
        """The starter entry is written from the signature the generator just emitted — a bare
        `PriceTicker()` against a required `label` would not compile, and it needs its import."""
        catalog_path = (
            "feature/gallery/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/gallery/presentation/GalleryCatalog.kt"
        )
        self.run_script("create_component.py", "PriceTicker", "--group", "content")
        catalog = self.read(catalog_path)
        self.assertIn(f"import {BASE_PATH.replace('/', '.')}.core.ui.component.content.PriceTicker\n", catalog)
        self.assertIn('"Default" to { PriceTicker(label = "PriceTicker") }', catalog)
        imports = [l for l in catalog.splitlines() if l.startswith("import ")]
        self.assertEqual(imports, ktlint_order(imports), "the import is not in ktlint's sorted position")

        self.run_script("create_component.py", "StockRow", "--group", "content", "--state")
        catalog = self.read(catalog_path)
        self.assertIn(f"import {BASE_PATH.replace('/', '.')}.core.ui.component.content.StockRowState\n", catalog)
        self.assertIn('"Default" to { StockRow(state = StockRowState.PREVIEW) }', catalog)

    def test_create_component_lists_a_core_ui_component_in_the_playground(self) -> None:
        """The gallery entry's twin: a starter bench entry whose render is the same call
        the gallery's variant runs, and doctor.py fails on a component with none."""
        self.run_script("create_component.py", "PriceTicker", "--group", "content")

        catalog = self.read(
            "feature/devmenu/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/devmenu/presentation/PlaygroundCatalog.kt"
        )
        self.assertIn('entry("priceticker", "PriceTicker") { PriceTicker(label = "PriceTicker") },', catalog)
        self.assertIn(f"import {BASE_PATH.replace('/', '.')}.core.ui.component.content.PriceTicker\n", catalog)
        self.assert_doctor_passes()

    def test_doctor_catches_a_component_missing_from_the_playground(self) -> None:
        """A component with a gallery entry and no bench entry fails."""
        catalog = self.repo / (
            "feature/devmenu/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/devmenu/presentation/PlaygroundCatalog.kt"
        )
        text = catalog.read_text()
        start = text.index('    entry(\n        "stepper",')
        end = text.index("    entry(", start + 1)
        catalog.write_text(text[:start] + text[end:])

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("AppStepper.kt", result.stdout)
        self.assertIn("playground", result.stdout)

    def test_doctor_catches_a_control_id_on_a_node_not_clickable(self) -> None:
        """`cart_checkoutButton` on the checkout bar's row, whose centre is the total, is an id on
        the wrong node, and a flow tapping it would miss. A clickable call passes."""
        screen = self.screen("cart", "Cart") / "CartScreen.kt"
        clean = screen.read_text()
        line = 'actionTestTag = "cart_checkoutButton",'
        self.assertIn(line, clean)
        screen.write_text(clean.replace(line, 'modifier = Modifier.testTag("cart_checkoutButton"),', 1))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("cart_checkoutButton", result.stdout)
        self.assertIn("AppBottomActionBar", result.stdout)

        screen.write_text(clean)
        self.assert_doctor_passes()

    def test_create_component_for_a_feature_stays_out_of_the_gallery(self) -> None:
        """The gallery lists `:core:ui`, which is the set every feature may compose from."""
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_component.py", "ProductCard", "--feature", "userprofile")

        catalog = self.read(
            "feature/gallery/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/gallery/presentation/GalleryCatalog.kt"
        )
        self.assertNotIn("ProductCard", catalog)
        bench = self.read(
            "feature/devmenu/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/devmenu/presentation/PlaygroundCatalog.kt"
        )
        self.assertNotIn("ProductCard", bench)
        self.assert_doctor_passes()

    def test_create_feature_writes_no_screenshot_test(self) -> None:
        """A generated feature records no goldens: the harness is `:core:ui`'s alone.

        The template used to carry a `PreviewScreenshotTest` that every clone inherited, so a new
        feature's first `recordRoborazziDebug` wrote a golden per preview that nothing then looked
        at. The template holds none now, and a clone that grew one back would be the old gate
        returning one feature at a time.
        """
        self.run_script("create_feature.py", "userProfile")

        test_root = self.presentation("userprofile", "test")
        self.assertEqual(
            [],
            sorted(str(p.relative_to(test_root)) for p in test_root.rglob("*Screenshot*")),
            "a generated feature carries a screenshot test",
        )
        self.assertFalse(
            (self.repo / "feature/userprofile/presentation/src/test/screenshots").exists(),
            "a generated feature carries goldens",
        )

    def test_create_feature_renames_string_resources(self) -> None:
        """The template's `template_title` must not survive into a generated feature."""
        self.run_script("create_feature.py", "userProfile")

        strings = self.read("feature/userprofile/presentation/src/main/res/values/strings.xml")
        self.assertIn('name="user_profile_title"', strings)
        self.assertNotIn("template_", strings)

        screen = (self.screen("userprofile", "UserProfile") / "UserProfileScreen.kt").read_text()
        self.assertIn("R.string.user_profile_title", screen)
        self.assertNotIn("template_", screen)

    def test_create_feature_registers_only_the_screens_it_copied(self) -> None:
        """The di module names both template ViewModels; only one screen is copied.

        `is_copyable` skips the args screen by file name, which leaves the di module — a single
        file — importing and registering a ViewModel that does not exist. The generated feature
        did not compile.
        """
        self.run_script("create_feature.py", "userProfile")

        module = self.read(
            "feature/userprofile/di/src/main/kotlin/com/example/androidproject1"
            "/feature/userprofile/di/UserProfileModule.kt"
        )
        self.assertIn("viewModelOf(::UserProfileViewModel)", module)
        self.assertNotIn("UserProfileArgsViewModel", module)

    def test_create_feature_with_partial_layers(self) -> None:
        self.run_script("create_feature.py", "userProfile", "--layers", "presentation,di")

        self.assertFalse((self.repo / "feature/userprofile/domain").exists())
        build_file = self.read("feature/userprofile/di/build.gradle.kts")
        self.assertNotIn("projects.feature.userprofile.domain", build_file)
        self.assertIn("projects.feature.userprofile.presentation", build_file)
        self.assert_doctor_passes()

    def test_adding_a_layer_later_updates_the_settings_block(self) -> None:
        """`--layers X --force` on an existing feature must extend its includeFeatureModule block."""
        self.run_script("create_feature.py", "userProfile", "--layers", "presentation,di")
        self.run_script("create_feature.py", "userProfile", "--layers", "domain", "--force")

        settings = self.read("settings.gradle.kts")
        block = settings[settings.index('includeFeatureModule(\n    "userprofile",'):]
        block = block[: block.index(")")]
        self.assertIn("ModuleSuffix.Domain,", block)
        self.assertIn("ModuleSuffix.Presentation,", block)
        self.assertIn("ModuleSuffix.Di,", block)
        # Canonical order, not the order the layers were generated in.
        self.assertLess(block.index("Domain"), block.index("Presentation"))

        self.assert_doctor_passes()

    def test_create_screen_generates_a_usable_unit(self) -> None:
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileList")

        # The unit lands in a directory named after the screen, not in the flat package.
        directory = self.screen("userprofile", "UserProfileList")
        for suffix in ("Destination", "Screen", "State", "Event", "Navigation", "ViewModel"):
            self.assertTrue((directory / f"UserProfileList{suffix}.kt").is_file(), suffix)
        self.assertFalse((self.presentation("userprofile") / "UserProfileListScreen.kt").exists())

        # The bug this test exists for: the screen used to reference a string it did not bring.
        strings = self.read("feature/userprofile/presentation/src/main/res/values/strings.xml")
        self.assertIn('name="user_profile_list_title"', strings)
        self.assertIn("R.string.user_profile_list_title", (directory / "UserProfileListScreen.kt").read_text())

        module = self.read(f"feature/userprofile/di/src/main/kotlin/{BASE_PATH}/feature/userprofile/di/UserProfileModule.kt")
        self.assertIn("viewModelOf(::UserProfileListViewModel)", module)

        nav_host = self.read(f"app/src/main/kotlin/{BASE_PATH}/AppNavHost.kt")
        self.assertIn("userProfileListDestination(navigator = navigator)", nav_host)

        self.assert_doctor_passes()

    def test_create_screen_in_a_sub_package_imports_r(self) -> None:
        """R lives in the module namespace, which a sub-package is no longer part of."""
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileDetail", "--sub", "detail")

        screen = (self.presentation("userprofile") / "detail/UserProfileDetailScreen.kt").read_text()
        self.assertFalse((self.presentation("userprofile") / "userprofiledetail").exists())
        self.assertIn("package com.example.androidproject1.feature.userprofile.presentation.detail", screen)
        self.assertIn("import com.example.androidproject1.feature.userprofile.presentation.R", screen)

        module = self.read(f"feature/userprofile/di/src/main/kotlin/{BASE_PATH}/feature/userprofile/di/UserProfileModule.kt")
        self.assertIn(
            "import com.example.androidproject1.feature.userprofile.presentation.detail.UserProfileDetailViewModel",
            module,
        )
        self.assert_doctor_passes()

    def test_create_feature_translates_the_strings_it_renames(self) -> None:
        """A generated feature starts bilingual, or doctor.py fails on the next commit."""
        self.run_script("create_feature.py", "userProfile")

        czech = self.read("feature/userprofile/presentation/src/main/res/values-cs/strings.xml")
        self.assertIn('name="user_profile_title"', czech)
        self.assertNotIn("template_", czech)

        default = self.read("feature/userprofile/presentation/src/main/res/values/strings.xml")
        self.assertEqual(
            self.resource_names(default),
            self.resource_names(czech),
            "values/ and values-cs/ must declare the same names",
        )

    def test_create_screen_adds_its_strings_to_every_locale(self) -> None:
        """The bug this exists for: the new screen's Czech was merged into `values/` only."""
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileList")

        for directory in ("values", "values-cs"):
            strings = self.read(f"feature/userprofile/presentation/src/main/res/{directory}/strings.xml")
            self.assertIn('name="user_profile_list_title"', strings, directory)
            self.assertNotIn("template_", strings, directory)

        self.assert_doctor_passes()

    def test_create_screen_with_args_adds_its_strings_to_every_locale(self) -> None:
        """The args screen has its own `template_args_*` set, which is renamed separately."""
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileDetail", "--with-args", "userId:String")

        for directory in ("values", "values-cs"):
            strings = self.read(f"feature/userprofile/presentation/src/main/res/{directory}/strings.xml")
            self.assertIn('name="user_profile_detail_title"', strings, directory)

        self.assert_doctor_passes()

    def test_create_screen_twice_does_not_collide_on_strings(self) -> None:
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileList")
        self.run_script("create_screen.py", "userprofile", "UserProfileDetail")

        strings = self.read("feature/userprofile/presentation/src/main/res/values/strings.xml")
        self.assertIn('name="user_profile_list_title"', strings)
        self.assertIn('name="user_profile_detail_title"', strings)
        self.assert_doctor_passes()

    def test_create_datasource_puts_each_half_in_the_right_package(self) -> None:
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_datasource.py", "userprofile", "LocalUserProfile", "--repository")

        data = self.repo / f"feature/userprofile/data/src/main/kotlin/{BASE_PATH}/feature/userprofile/data"
        domain = self.repo / f"feature/userprofile/domain/src/main/kotlin/{BASE_PATH}/feature/userprofile/domain"

        # Both halves of the source sit together; the repository is one package over, and only its
        # interface — the one in domain — is visible above the data layer.
        self.assertTrue((data / "source/LocalUserProfileDataSource.kt").is_file())
        self.assertTrue((data / "source/DefaultLocalUserProfileDataSource.kt").is_file())
        self.assertTrue((domain / "UserProfileRepository.kt").is_file())
        self.assertTrue((data / "repository/DefaultUserProfileRepository.kt").is_file())

        # The repository depends on the interface, never on the implementation — doctor.py's
        # "no repository imports a data source implementation" check, asserted at the source.
        repository = (data / "repository/DefaultUserProfileRepository.kt").read_text()
        self.assertIn(
            "import com.example.androidproject1.feature.userprofile.data.source.LocalUserProfileDataSource",
            repository,
        )
        self.assertNotIn("DefaultLocalUserProfileDataSource", repository)
        # The logger is `:service:log:domain`'s; the old `service.core.domain` path compiles nowhere.
        self.assertIn("import com.example.androidproject1.service.log.domain.Logger", repository)

        module = self.read(f"feature/userprofile/di/src/main/kotlin/{BASE_PATH}/feature/userprofile/di/UserProfileModule.kt")
        self.assertIn("singleOf(::DefaultLocalUserProfileDataSource) bind LocalUserProfileDataSource::class", module)
        self.assertIn("singleOf(::DefaultUserProfileRepository) bind UserProfileRepository::class", module)
        self.assertIn("import org.koin.core.module.dsl.singleOf", module)
        self.assertIn("import org.koin.dsl.bind", module)

        # A data source touches disk, so it switches to IO itself — BaseRepository runs on the
        # caller's context, and that caller is viewModelScope.
        implementation = (data / "source/DefaultLocalUserProfileDataSource.kt").read_text()
        self.assertIn("import com.example.androidproject1.service.core.domain.coroutines.DispatcherProvider", implementation)
        self.assertIn("private val dispatcherProvider: DispatcherProvider,", implementation)
        self.assertIn(".flowOn(dispatcherProvider.io)", implementation)
        self.assertIn("withContext(dispatcherProvider.io)", implementation)

        self.assert_doctor_passes()

    def test_delete_feature_restores_every_registration(self) -> None:
        watched = [
            "settings.gradle.kts",
            "core/di/build.gradle.kts",
            f"core/di/src/main/kotlin/{BASE_PATH}/core/di/Koin.kt",
            f"app/src/main/kotlin/{BASE_PATH}/AppNavHost.kt",
        ]
        before = {path: self.read(path) for path in watched}

        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileList")
        self.run_script("create_datasource.py", "userprofile", "LocalUserProfile", "--repository")
        self.run_script("delete_feature.py", "userProfile")

        self.assertFalse((self.repo / "feature/userprofile").exists())
        for path in watched:
            self.assertEqual(before[path], self.read(path), f"{path} was not restored")

        self.assert_doctor_passes()

    def test_delete_feature_removes_what_app_holds_for_the_feature(self) -> None:
        """What only :app can give a feature — a `wiring.<feature>` package in any
        source set, its one `includes(...)` line, a tab entry and a launcher shortcut — goes with
        the feature, and every file it touched is as it was before the feature came."""
        app = f"app/src/main/kotlin/{BASE_PATH}"
        watched = [
            f"{app}/ApplicationModule.kt",
            f"{app}/TopLevelDestination.kt",
            "app/src/main/res/xml/shortcuts.xml",
            "app/src/dev/res/xml/shortcuts.xml",
        ]
        before = {path: self.read(path) for path in watched}

        self.run_script("create_feature.py", "userProfile")
        for source_set in ("main", "dev", "test"):
            wiring = self.repo / f"app/src/{source_set}/kotlin/{BASE_PATH}/wiring/userprofile"
            wiring.mkdir(parents=True)
            (wiring / "UserProfileWiring.kt").write_text("package com.example.androidproject1.wiring.userprofile\n")
        module = self.repo / f"{app}/ApplicationModule.kt"
        module.write_text(
            module.read_text()
            .replace(
                "import com.example.androidproject1.wiring.checkout.checkoutWiring\n",
                "import com.example.androidproject1.wiring.checkout.checkoutWiring\n"
                "import com.example.androidproject1.wiring.userprofile.userProfileWiring\n",
            )
            .replace("        includes(checkoutWiring)\n", "        includes(checkoutWiring)\n        includes(userProfileWiring)\n"),
        )
        tabs = self.repo / f"{app}/TopLevelDestination.kt"
        tabs.write_text(
            tabs.read_text()
            .replace(
                "import com.example.androidproject1.feature.trips.api.TripsDestination\n",
                "import com.example.androidproject1.feature.trips.api.TripsDestination\n"
                "import com.example.androidproject1.feature.userprofile.api.UserProfileDestination\n",
            )
            .replace(
                "    Settings(",
                '    UserProfile(UserProfileDestination, R.string.tab_home, Icons.Filled.Home, testTag = "tabs_userProfileTab"),\n'
                "    Settings(",
            ),
        )
        for flavor, scheme in (("main", "com.example.androidproject1"), ("dev", "com.example.androidproject1.dev")):
            shortcuts = self.repo / f"app/src/{flavor}/res/xml/shortcuts.xml"
            shortcut = (
                "\n    <shortcut\n        android:shortcutId=\"openProfile\">\n"
                f"        <intent android:data=\"{scheme}://userprofile/edit\" />\n    </shortcut>\n"
            )
            shortcuts.write_text(shortcuts.read_text().replace("\n</shortcuts>", shortcut + "\n</shortcuts>"))
        self.assertNotEqual(before[watched[0]], self.read(watched[0]))

        self.run_script("delete_feature.py", "userProfile")

        for source_set in ("main", "dev", "test"):
            self.assertFalse((self.repo / f"app/src/{source_set}/kotlin/{BASE_PATH}/wiring/userprofile").exists())
        self.assertTrue((self.repo / f"{app}/wiring/checkout").is_dir())
        for path in watched:
            self.assertEqual(before[path], self.read(path), f"{path} was not restored")

    def test_delete_feature_names_and_prunes_the_services_it_leaves_without_a_caller(self) -> None:
        """A service only the deleted feature called is named by a dry run, and
        `--prune-services` removes it from every place `create_service.py` put it; a service
        another module still declares stays."""
        dry = self.run_script("delete_feature.py", "weather", "--dry-run", "--force")
        self.assertIn(":service:location", dry.stdout)
        self.assertNotIn(":service:permission", dry.stdout)
        self.assertTrue((self.repo / "service/location").is_dir())

        self.run_script("delete_feature.py", "weather", "--prune-services", "--force")

        self.assertFalse((self.repo / "service/location").exists())
        self.assertNotIn(":service:location", self.read("settings.gradle.kts"))
        self.assertNotIn("projects.service.location", self.read("core/di/build.gradle.kts"))
        koin = self.read(f"core/di/src/main/kotlin/{BASE_PATH}/core/di/Koin.kt")
        self.assertNotIn("locationServiceModule", koin)
        self.assertNotIn(".service.location.", koin)
        self.assertIn("permissionServiceModule,", koin)
        self.assertNotIn(":service:location", self.read("build-logic/src/main/kotlin/ServiceModules.kt"))
        self.assertTrue((self.repo / "service/permission").is_dir())

    def test_delete_feature_removes_a_registration_that_spans_lines(self) -> None:
        """A destination registered over several lines — a lambda argument with calls of its
        own — is removed to its closing parenthesis, not just its first line."""
        nav_host = f"app/src/main/kotlin/{BASE_PATH}/AppNavHost.kt"
        before = self.read(nav_host)

        self.run_script("create_feature.py", "userProfile")
        path = self.repo / nav_host
        one_line = "userProfileDestination(navigator = navigator)"
        text = path.read_text()
        self.assertIn(one_line, text)
        path.write_text(
            text.replace(
                one_line,
                "userProfileDestination(\n"
                "        navigator = navigator,\n"
                "        onPicked = { id ->\n"
                "            listOf(id).map { it.trim() }.firstOrNull()\n"
                "        },\n"
                "    )",
            ),
        )

        self.run_script("delete_feature.py", "userProfile")

        self.assertEqual(before, self.read(nav_host))

    def test_delete_feature_names_its_dependants_and_stops_before_writing(self) -> None:
        """A feature another module still declares is refused before anything is written —
        the modules are named, and the exit is 1 unless --force; a real run is refused the same."""
        before = self.tree_snapshot()

        dry = self.run_script("delete_feature.py", "cart", "--dry-run", expect_success=False)
        self.assertEqual(1, dry.returncode)
        self.assertIn(":app", dry.stderr)
        self.assertIn(":feature:checkout:", dry.stderr)
        self.assertNotIn(":core:di", dry.stderr)
        self.assertIn("--force", dry.stderr)

        real = self.run_script("delete_feature.py", "cart", expect_success=False)
        self.assertEqual(1, real.returncode)
        self.assertEqual(before, self.tree_snapshot())

        forced = self.run_script("delete_feature.py", "cart", "--dry-run", "--force")
        self.assertIn(":app", forced.stdout)

    def test_delete_feature_deletes_its_own_flows_and_lists_what_else_names_it(self) -> None:
        """A flow whose every id is the feature's or the frame's goes with the feature; a flow
        that also names another sample's ids is listed, and so is a test outside the feature that
        imports it. A dry run says which flows it would delete and deletes none."""
        maestro = self.repo / ".maestro"
        (maestro / "zz-weather-only.yaml").write_text(
            'appId: x\n---\n- tapOn:\n    id: "tabs_homeTab"\n- assertVisible:\n    id: "WeatherScreen"\n'
        )
        (maestro / "zz-weather-and-cart.yaml").write_text(
            'appId: x\n---\n- assertVisible:\n    id: "WeatherScreen"\n- assertVisible:\n    id: "CartScreen"\n'
        )
        (maestro / "zz-cart-only.yaml").write_text('appId: x\n---\n- assertVisible:\n    id: "CartScreen"\n')
        outside = self.repo / f"app/src/test/kotlin/{BASE_PATH}/ZzWeatherTest.kt"
        outside.write_text(
            "package com.example.androidproject1\n\n"
            "import com.example.androidproject1.feature.weather.domain.Place\n"
        )

        dry = self.run_script("delete_feature.py", "weather", "--dry-run", "--force")
        self.assertIn("would delete .maestro/zz-weather-only.yaml", dry.stdout)
        self.assertIn("would delete .maestro/weather.yaml", dry.stdout)
        self.assertTrue((maestro / "zz-weather-only.yaml").is_file())

        run = self.run_script("delete_feature.py", "weather", "--force")
        self.assertFalse((maestro / "zz-weather-only.yaml").exists())
        self.assertTrue((maestro / "zz-weather-and-cart.yaml").is_file())
        self.assertIn(".maestro/zz-weather-and-cart.yaml", run.stdout)
        self.assertNotIn(".maestro/zz-cart-only.yaml", run.stdout)
        self.assertTrue((maestro / "zz-cart-only.yaml").is_file())
        self.assertIn(f"app/src/test/kotlin/{BASE_PATH}/ZzWeatherTest.kt", run.stdout)

    def test_delete_feature_takes_its_fixtures_and_app_strings(self) -> None:
        """A sample's `dev` fixtures are `fixture_<feature>_<what>.json`, and they go with
        the feature, as do `:app`'s `tab_<flat>` and `menu_<flat>` strings in every locale."""
        raw = self.repo / "app/src/dev/res/raw"
        self.assertTrue(list(raw.glob("fixture_catalog_*.json")))
        locales = sorted((self.repo / "app/src/main/res").glob("values*/strings.xml"))
        self.assertGreater(len(locales), 1)
        for strings in locales:
            text = strings.read_text()
            strings.write_text(text.replace("</resources>", '    <string name="menu_catalog">Shop</string>\n</resources>'))

        self.run_script("delete_feature.py", "catalog", "--force")

        self.assertEqual([], list(raw.glob("fixture_catalog_*")))
        self.assertTrue((raw / "fixture_messages.json").is_file())
        self.assertTrue(list(raw.glob("fixture_weather_*.json")))
        for strings in locales:
            text = strings.read_text()
            self.assertNotIn('name="tab_catalog"', text, strings)
            self.assertNotIn('name="menu_catalog"', text, strings)
            self.assertIn('name="tab_cart"', text, strings)

    def test_delete_feature_refuses_the_template(self) -> None:
        result = self.run_script("delete_feature.py", "template", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertTrue((self.repo / "feature/template").is_dir())

    def test_delete_feature_refuses_the_frame(self) -> None:
        """The frame is what the shell is built on, not a sample — even a dry run
        stops, names it and says how to insist."""
        result = self.run_script("delete_feature.py", "devmenu", "--dry-run", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("frame", result.stderr)
        self.assertIn("--force", result.stderr)
        self.assertTrue((self.repo / "feature/devmenu").is_dir())
        self.run_script("delete_feature.py", "weather", "--dry-run", "--force")

    def test_dry_run_writes_nothing(self) -> None:
        before = self.tree_snapshot()
        self.run_script("create_feature.py", "userProfile", "--dry-run")
        self.run_script("create_screen.py", "template", "UserProfileList", "--dry-run")
        self.assertEqual(before, self.tree_snapshot())

    def test_create_screen_with_args_generates_the_savedstatehandle_wiring(self) -> None:
        self.run_script(
            "create_screen.py", "catalog", "ProductReview",
            "--with-args", "productId:String,rating:Int",
        )

        destination = (self.screen("catalog", "ProductReview") / "ProductReviewDestination.kt").read_text()
        self.assertIn(
            "data class ProductReviewDestination(val productId: String, val rating: Int) : NavKey",
            self.route_key("catalog", "ProductReviewDestination"),
        )
        # The key goes straight into the ViewModel, not into a load() call from here. Match the
        # call, not the word: the template's comment explains why LaunchedEffect is wrong.
        self.assertIn("parametersOf(key)", destination)
        # A screen of one thing is the detail pane a wide window shows beside its list.
        self.assertIn("entry<ProductReviewDestination>(metadata = detailPane())", destination)
        self.assertIn("import com.example.androidproject1.core.ui.layout.detailPane", destination)
        self.assertNotIn("LaunchedEffect(", destination)
        self.assertNotIn("viewModel.load(", destination)

        view_model = (self.screen("catalog", "ProductReview") / "ProductReviewViewModel.kt").read_text()
        self.assertIn("private val args: ProductReviewDestination", view_model)
        self.assertNotIn("SavedStateHandle", view_model)

        # The sixth registration: Koin's verify() cannot see a parametersOf argument.
        graph_test = self.read(f"app/src/test/kotlin/{BASE_PATH}/KoinGraphTest.kt")
        self.assertIn("definition<ProductReviewViewModel>(ProductReviewDestination::class)", graph_test)

        # Every argument reaches the key the view model test constructs, not only the first — a
        # second one was dropped and the generated test did not compile.
        test_dir = self.repo / f"feature/catalog/presentation/src/test/kotlin/{BASE_PATH}/feature/catalog/presentation/productreview"
        view_model_test = (test_dir / "ProductReviewViewModelTest.kt").read_text()
        self.assertIn('ProductReviewDestination(productId = "example", rating = 1)', view_model_test)

    def test_create_screen_with_args_generates_a_plain_jvm_test(self) -> None:
        self.run_script("create_screen.py", "catalog", "ProductReview", "--with-args", "productId:String")

        test = (
            self.screen("catalog", "ProductReview", "test") / "ProductReviewViewModelTest.kt"
        ).read_text()
        # Navigation 3 hands the key over as a plain object, so there is no Bundle to decode and
        # no Robolectric runner.
        self.assertIn("ProductReviewDestination(productId = \"example\")", test)
        self.assertNotIn("RobolectricTestRunner", test)

    def test_create_datasource_remote_generates_a_ktor_source(self) -> None:
        self.run_script("create_datasource.py", "catalog", "RemoteWeather", "--remote", "--repository")

        source = self.read(
            f"feature/catalog/data/src/main/kotlin/{BASE_PATH}"
            "/feature/catalog/data/source/DefaultRemoteWeatherDataSource.kt"
        )
        # Ktor, not DataStore — and the status table is applied at the source, so the repository
        # above it sees a DomainError and never an HTTP code.
        self.assertIn("io.ktor.client.HttpClient", source)
        self.assertIn("HttpErrorMapper.map(throwable)", source)
        self.assertNotIn("DataStoreProvider", source)
        # The DTO is the wire shape and stays in the data layer.
        self.assertIn("data class RemoteWeatherDto", source)

        repository = self.read(
            f"feature/catalog/data/src/main/kotlin/{BASE_PATH}"
            "/feature/catalog/data/repository/DefaultWeatherRepository.kt"
        )
        self.assertIn("execute {", repository)
        # The repository returns the domain type; the DTO does not escape the data layer.
        self.assertIn("Outcome<Weather>", repository)
        self.assertNotIn("RemoteWeatherDto", repository)

    def test_create_datasource_without_remote_is_unchanged(self) -> None:
        """The default path must not pick up the Ktor template."""
        self.run_script("create_datasource.py", "catalog", "LocalWeather", "--repository")

        source = self.read(
            f"feature/catalog/data/src/main/kotlin/{BASE_PATH}"
            "/feature/catalog/data/source/DefaultLocalWeatherDataSource.kt"
        )
        self.assertIn("DataStoreProvider", source)
        self.assertNotIn("io.ktor", source)

    def test_create_screen_generates_a_screen_test(self) -> None:
        self.run_script("create_screen.py", "catalog", "ProductReview")

        test = (
            self.screen("catalog", "ProductReview", "test") / "ProductReviewScreenTest.kt"
        ).read_text()
        # The half a ViewModel test cannot reach: what is on screen, and what a tap does.
        self.assertIn("class ProductReviewScreenTest", test)
        self.assertIn("RobolectricTestRunner", test)
        self.assertNotIn("Template", test)

    def test_a_generated_screen_and_its_test_agree_on_the_tag(self) -> None:
        """A tag's stem is camelCase; the resource beside it is snake_case. They rewrite apart."""
        self.run_script("create_screen.py", "catalog", "ProductReview")

        screen = (self.screen("catalog", "ProductReview") / "ProductReviewScreen.kt").read_text()
        test = (self.screen("catalog", "ProductReview", "test") / "ProductReviewScreenTest.kt").read_text()

        # camelCase stem in both, so the test can actually find what the screen tags.
        self.assertIn('testTag("productReview_incrementButton")', screen)
        self.assertIn('onNodeWithTag("productReview_incrementButton")', test)
        # ...while the string resource it sits next to stays snake_case.
        self.assertIn("R.string.product_review_increment", screen)
        self.assertNotIn("product_review_incrementButton", screen + test)

    def test_create_screen_with_args_defaults_to_a_single_id(self) -> None:
        self.run_script("create_screen.py", "catalog", "ProductReview", "--with-args")

        self.assertIn(
            "data class ProductReviewDestination(val id: String)",
            self.route_key("catalog", "ProductReviewDestination"),
        )

    def test_create_screen_with_args_names_files_without_the_args_suffix(self) -> None:
        """The template class is `TemplateArgs`; the generated files must not inherit the suffix."""
        self.run_script("create_screen.py", "catalog", "ProductReview", "--with-args")

        self.assertTrue((self.screen("catalog", "ProductReview") / "ProductReviewViewModel.kt").is_file())
        self.assertFalse((self.screen("catalog", "ProductReview") / "ProductReviewArgsViewModel.kt").exists())
        self.assert_doctor_passes()

    def test_create_screen_with_args_rejects_an_unsupported_type(self) -> None:
        result = self.run_script(
            "create_screen.py", "catalog", "ProductReview", "--with-args", "productId:Uri",
            expect_success=False,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("not a supported", result.stdout + result.stderr)

    def test_create_screen_without_args_is_unchanged(self) -> None:
        """The plain path must not pick up the args template — `Template*.kt` matches both."""
        self.run_script("create_screen.py", "catalog", "PlainScreen")

        self.assertIn("data object PlainScreenDestination", self.route_key("catalog", "PlainScreenDestination"))
        self.assertFalse((self.screen("catalog", "PlainScreen") / "PlainScreenArgsDestination.kt").exists())
        view_model = (self.screen("catalog", "PlainScreen") / "PlainScreenViewModel.kt").read_text()
        self.assertNotIn("SavedStateHandle", view_model)
        self.assert_doctor_passes()


    def test_every_script_documents_itself(self) -> None:
        """--help must work and carry a worked example; the README must list every script."""
        import subprocess
        readme = (self.repo / "scripts/README.md").read_text()

        scripts = sorted(
            p for p in (self.repo / "scripts").glob("*.py")
            if p.name not in {"_common.py", "test_scripts.py"}
        )
        self.assertTrue(scripts)

        for script in scripts:
            result = subprocess.run(
                [sys.executable, str(script), "--help"], capture_output=True, text=True,
                env=hermetic_env(),
            )
            self.assertEqual(0, result.returncode, f"{script.name} --help failed:\n{result.stderr}")
            self.assertIn(
                f"scripts/{script.name}", result.stdout,
                f"{script.name} --help has no worked example",
            )
            self.assertIn(script.name, readme, f"scripts/README.md does not mention {script.name}")


    # -- .githooks -----------------------------------------------------------------------------

    def test_pre_commit_hook_is_executable_and_runs_doctor_only(self) -> None:
        hook = REPO_ROOT / ".githooks/pre-commit"
        self.assertTrue(hook.is_file())
        self.assertTrue(hook.stat().st_mode & 0o111, "hook is not executable")
        text = hook.read_text()
        self.assertIn("scripts/doctor.py", text)
        # The suite takes about fifteen minutes, which is CI's job, not every commit's.
        self.assertNotIn("scripts/test_scripts.py", text)

    # -- create_component.py ------------------------------------------------------------------

    def test_create_component_lands_in_core_ui_by_default(self) -> None:
        self.run_script("create_component.py", "PrimaryButton", "--group", "action")

        component = self.repo / f"core/ui/src/main/kotlin/{BASE_PATH}/core/ui/component/action/PrimaryButton.kt"
        self.assertTrue(component.is_file())
        text = component.read_text()
        self.assertIn(f"package {BASE_PATH.replace('/', '.')}.core.ui.component.action", text)
        # The convention doctor.py enforces, generated rather than remembered.
        self.assertIn("modifier: Modifier = Modifier", text)
        self.assertIn("@ComponentPreview", text)

    def test_create_component_in_core_ui_needs_a_group(self) -> None:
        """A :core:ui component lands in a group package, never in a flat `component/`."""
        result = self.run_script("create_component.py", "PrimaryButton", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("needs --group, one of: action, choice, content, field, list, navigation, overlay, status", result.stderr)
        self.assertFalse((self.repo / f"core/ui/src/main/kotlin/{BASE_PATH}/core/ui/component/PrimaryButton.kt").exists())

    def test_create_component_in_a_feature(self) -> None:
        self.run_script("create_component.py", "ProductCard", "--feature", "catalog")

        # A feature's own components live in `component/`, never beside a screen.
        component = self.component("catalog") / "ProductCard.kt"
        self.assertTrue(component.is_file())
        self.assertIn("feature.catalog.presentation.component", component.read_text())

    def test_create_component_with_state_generates_a_preview_fixture(self) -> None:
        self.run_script("create_component.py", "ProductCard", "--feature", "catalog", "--state")

        # The state lives in the component's own file: every file in a component package is read
        # as a component, so a separate state file has no preview, gallery or playground entry
        # — the shape `AppCardField` keeps.
        self.assertFalse((self.component("catalog") / "ProductCardState.kt").exists())
        text = (self.component("catalog") / "ProductCard.kt").read_text()
        self.assertIn("data class ProductCardState(", text)
        self.assertIn("val PREVIEW", text)
        self.assertIn("@Immutable", text)
        self.assertIn("state: ProductCardState", text)
        self.assert_doctor_passes()

    def test_create_component_with_state_in_core_ui_passes_doctor(self) -> None:
        self.run_script("create_component.py", "AppReproField", "--group", "field", "--state")

        directory = self.repo / f"core/ui/src/main/kotlin/{BASE_PATH}/core/ui/component/field"
        self.assertFalse((directory / "AppReproFieldState.kt").exists())
        self.assertIn("data class AppReproFieldState(", (directory / "AppReproField.kt").read_text())
        self.assert_doctor_passes()

    def test_create_component_sub_package(self) -> None:
        self.run_script("create_component.py", "Badge", "--feature", "catalog", "--sub", "product")

        component = self.component("catalog") / "product/Badge.kt"
        self.assertTrue(component.is_file())
        self.assertIn("feature.catalog.presentation.component.product", component.read_text())

    def test_create_component_refuses_an_unknown_feature(self) -> None:
        result = self.run_script(
            "create_component.py", "Ghost", "--feature", "nosuchfeature", expect_success=False,
        )
        self.assertNotEqual(0, result.returncode)


    # -- the pre-commit hook -------------------------------------------------------------------

    def test_doctor_notes_a_missing_hook_and_init_project_installs_it(self) -> None:
        """A note, never a failure: CI has no hooks and stays green; a clone without the config
        line is told the one command, and init_project.py runs it on the clone it rewrites.
        The note compares resolved paths, so the relative form init_project.py writes and
        its absolute twin — what a worktree's `core.hooksPath` holds — both read as installed;
        only a path that resolves somewhere else, missing included, is the miss."""
        import subprocess
        git = lambda *a: subprocess.run(["git", *a], cwd=self.repo, capture_output=True, text=True, env=hermetic_env(), check=True)
        git("init", "-q")
        note = "the pre-commit hook is not installed on this clone: git config core.hooksPath .githooks"

        # missing: no core.hooksPath set at all.
        result = self.run_script("doctor.py")
        self.assertIn(f"[note] {note}", result.stdout)

        # relative: what init_project.py writes on a fresh clone.
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker", "--force")
        self.assertEqual(".githooks", git("config", "core.hooksPath").stdout.strip())
        result = self.run_script("doctor.py")
        self.assertNotIn(note, result.stdout)

        # absolute: the same directory, spelled the way `git worktree add` writes it, which is
        # installed even though the string is not `.githooks`.
        git("config", "core.hooksPath", str((self.repo / ".githooks").resolve()))
        result = self.run_script("doctor.py")
        self.assertNotIn(note, result.stdout)

        # missing again: a path that resolves nowhere near `.githooks` is still a miss.
        git("config", "core.hooksPath", str(self.repo / "not-the-hooks-dir"))
        result = self.run_script("doctor.py")
        self.assertIn(f"[note] {note}", result.stdout)

    # -- init_project.py ----------------------------------------------------------------------

    def test_init_project_renames_the_package_everywhere(self) -> None:
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker")

        # No source file may still name the template — except the script itself, which keeps the
        # constants so it can rename a second project later.
        leftovers = []
        for path in sorted(self.repo.rglob("*")):
            if not path.is_file() or path.suffix not in {".kt", ".kts", ".xml", ".toml", ".py", ".md", ".conf", ".json"}:
                continue
            relative = path.relative_to(self.repo)
            if {"build", ".gradle", ".git", "__pycache__"}.intersection(relative.parts):
                continue
            # init_project.py keeps the constants so it can rename a second project later, and
            # this file necessarily contains the very strings it is asserting the absence of.
            if path.name in {"init_project.py", "test_scripts.py"}:
                continue
            text = path.read_text()
            if TEMPLATE_PACKAGE_WORD in text or TEMPLATE_PROJECT_NAME in text:
                leftovers.append(str(relative))
        self.assertEqual([], leftovers, "these files still name the template")

    def test_init_project_rewrites_json(self) -> None:
        """A `.json` file names the application id too — the messages fixture's deep link — and
        a copy that kept it would open the template's app. The messages fixture is a sample's, so the
        demo is kept to have one to read."""
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker", "--keep-samples")

        messages = self.read("app/src/dev/res/raw/fixture_messages.json")
        self.assertIn("com.acme.tracker.dev://", messages)
        self.assertNotIn(TEMPLATE_PACKAGE_WORD, messages)

    def test_init_project_starts_clean(self) -> None:
        """A copy opens on one empty screen: the samples, sign-in, onboarding, profile and Settings
        are gone with every registration, `:app` is the clean shell, and doctor.py passes."""
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker")

        features = sorted(p.name for p in (self.repo / "feature").iterdir() if p.is_dir())
        self.assertEqual(["apphold", "devmenu", "gallery", "home", "template"], features)
        samples = re.compile(
            r"feature\.(auth|onboarding|profile|settings|catalog|cart|checkout|orders|inventory|movies|weather|trips)\b"
        )
        named = [
            str(path.relative_to(self.repo))
            for root in ("app", "core", "feature", "service", "settings.gradle.kts")
            for path in ([self.repo / root] if (self.repo / root).is_file() else (self.repo / root).rglob("*"))
            if path.is_file() and path.suffix in {".kt", ".kts", ".xml"} and samples.search(path.read_text())
        ]
        self.assertEqual([], named, "these files still name a deleted feature")
        self.assertIn("homeDestination(navigator = navigator)", self.read("app/src/main/kotlin/com/acme/tracker/AppNavHost.kt"))
        self.assertFalse((self.repo / "app/src/main/kotlin/com/acme/tracker/TopLevelDestination.kt").exists())
        self.assertFalse((self.repo / "scripts/clean_start").exists())
        self.assertEqual(["config.yaml"], sorted(p.name for p in (self.repo / ".maestro").iterdir()))
        self.assert_doctor_passes()

    def test_init_project_keeps_the_samples_when_asked(self) -> None:
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker", "--keep-samples")

        self.assertTrue((self.repo / "feature/catalog").is_dir())
        self.assertTrue((self.repo / "app/src/main/kotlin/com/acme/tracker/TopLevelDestination.kt").is_file())
        self.assertTrue((self.repo / "scripts/clean_start").is_dir())

    def test_init_project_moves_every_source_set(self) -> None:
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker")

        self.assertTrue((self.repo / "service/core/ui/src/main/kotlin/com/acme/tracker/service/core/ui").is_dir())
        # testFixtures and test source sets move too, not just main.
        self.assertTrue(
            (self.repo / "service/core/ui/src/testFixtures/kotlin/com/acme/tracker/service/core/ui/test").is_dir()
        )
        self.assertTrue((self.repo / "app/src/main/kotlin/com/acme/tracker/MainActivity.kt").is_file())
        self.assertFalse((self.repo / "service/core/ui/src/main/kotlin/com/example").exists())

    def test_init_project_updates_the_project_name_and_label(self) -> None:
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker")

        self.assertIn('rootProject.name = "FieldTracker"', self.read("settings.gradle.kts"))
        # The applicationId and every namespace derive from this one property — see build-logic/.
        self.assertIn("basePackage=com.acme.tracker", self.read("gradle.properties"))
        # The Gradle name is safe in a theme; the launcher label is the human-readable one.
        self.assertIn("Theme.FieldTracker", self.read("app/src/main/res/values/themes.xml"))
        self.assertIn(">Field Tracker<", self.read("app/src/main/res/values/strings.xml"))

    def test_init_project_rewrites_the_licence_holder(self) -> None:
        """
        `LICENSE` has no suffix, so the suffix-driven walk never saw it and every project generated
        from this template shipped the template's own copyright holder.
        """
        self.run_script(
            "init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker",
            "--author", "Acme Ltd",
        )

        licence = self.read("LICENSE")
        self.assertIn("Copyright (c) 2026 Acme Ltd.", licence)
        self.assertNotIn("Božek", licence)
        # The terms are the new owner's to change; only the holder moves.
        self.assertIn("No licence is granted.", licence)

    def test_init_project_defaults_the_licence_holder_to_the_app_name(self) -> None:
        """--author is optional, but shipping someone else's name is never the default."""
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker")

        self.assertIn("Copyright (c) 2026 Field Tracker.", self.read("LICENSE"))

    def test_init_project_leaves_the_generators_working(self) -> None:
        """
        The regression this guards: `_common.py` holds the package as dots and this file holds it
        as slashes. Rewriting only the dotted form leaves the script suite pointed at a directory
        the rename has just emptied. The demo is kept, since a clean start deletes this file.
        """
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker", "--keep-samples")

        self.assertIn('BASE_PACKAGE = "com.acme.tracker"', self.read("scripts/_common.py"))
        self.assertIn('BASE_PATH = "com/acme/tracker"', self.read("scripts/test_scripts.py"))

        # And the generators still work against the renamed project.
        self.run_script("create_feature.py", "billing")
        self.assertTrue(
            (self.repo / "feature/billing/presentation/src/main/kotlin/com/acme/tracker"
             "/feature/billing/presentation/billing/BillingViewModel.kt").is_file()
        )
        self.assert_doctor_passes()

    def test_init_project_rejects_an_invalid_package(self) -> None:
        result = self.run_script(
            "init_project.py", "--package", "Com.Acme", "--name", "Field Tracker",
            expect_success=False,
        )
        self.assertNotEqual(0, result.returncode)
        self.assertIn("not a valid", result.stdout + result.stderr)

    def test_init_project_dry_run_changes_nothing(self) -> None:
        before = self.read("settings.gradle.kts")

        self.run_script(
            "init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker", "--dry-run",
        )

        self.assertEqual(before, self.read("settings.gradle.kts"))
        self.assertTrue((self.repo / f"service/core/ui/src/main/kotlin/{BASE_PATH}").is_dir())


    def test_export_service_rewrites_the_package(self) -> None:
        target = Path(self._temporary.name) / "target"
        self.run_script("export_service.py", "--to", str(target), "--package", "com.acme.myapp")

        self.assertTrue((target / "service/core/ui/src/main/kotlin/com/acme/myapp/service/core/ui/component/Screen.kt").is_file())

        # The service build files apply convention.* plugins, so build-logic/ has to come too.
        self.assertTrue((target / "build-logic/settings.gradle.kts").is_file())
        self.assertTrue((target / "build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt").is_file())

        leftovers = [
            str(path.relative_to(target))
            for path in target.rglob("*")
            if path.is_file() and path.suffix in {".kt", ".kts", ".xml"}
            and "com.example.androidproject1" in path.read_text()
        ]
        self.assertEqual([], leftovers, "the base package survived the export")

        # No catalog merge any more: the printed output points at this project's own
        # gradle/libs.versions.toml instead of writing one.
        result = self.run_script("export_service.py", "--to", str(target), "--package", "com.acme.myapp", "--force")
        self.assertIn("gradle/libs.versions.toml", result.stdout)
        self.assertFalse((target / "gradle/libs.versions.toml").exists())

    def test_doctor_catches_an_unregistered_view_model(self) -> None:
        self.run_script("create_feature.py", "userProfile")
        module = self.repo / f"feature/userprofile/di/src/main/kotlin/{BASE_PATH}/feature/userprofile/di/UserProfileModule.kt"
        module.write_text(module.read_text().replace("viewModelOf(::UserProfileViewModel)", ""))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("UserProfileViewModel", result.stdout)


    def test_doctor_catches_a_feature_drawing_its_own_ui(self) -> None:
        """The rule the design system rests on: a feature composes components, it never draws."""
        screen = self.screen("home", "Home") / "HomeScreen.kt"

        original = screen.read_text()
        for violation, expected in (
            ("import androidx.compose.material3.Button\n", "Material's Button"),
            ("import androidx.compose.foundation.layout.Row\n@Suppress\nval pad = 12.dp\n", "12.dp"),
        ):
            screen.write_text(violation + original)
            result = self.run_script("doctor.py", expect_success=False)
            self.assertNotEqual(0, result.returncode)
            self.assertIn(expected, result.stdout)
        screen.write_text(original)

    def test_doctor_catches_a_toast_outside_the_outlet(self) -> None:
        """A platform Toast or a Material Snackbar skips the outlet, the tones and the queue."""
        screen = self.screen("home", "Home") / "HomeScreen.kt"
        original = screen.read_text()
        screen.write_text("import android.widget.Toast\n" + original)

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("[FAIL] every message goes through the outlet", result.stdout)
        self.assertIn("HomeScreen.kt", result.stdout)
        self.assertIn("android.widget.Toast", result.stdout)
        screen.write_text(original)

        shell = self.repo / f"app/src/main/kotlin/{BASE_PATH}/Shell.kt"
        shell.write_text("package com.example.androidproject1\n\nfun say() = Snackbar(snackbarData = data)\n")
        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Shell.kt", result.stdout)
        self.assertIn("Material's Snackbar", result.stdout)

    def test_doctor_catches_a_feature_animating_itself(self) -> None:
        """Motion is composed the way widgets are. A feature that names a spec is a feature
        that draws, and one that reduced motion cannot reach."""
        screen = self.screen("home", "Home") / "HomeScreen.kt"
        screen.write_text("import androidx.compose.animation.AnimatedVisibility\n" + screen.read_text())

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("animates itself with androidx.compose.animation.AnimatedVisibility", result.stdout)
        self.assertIn("AppReveal", result.stdout)

    def test_init_project_renames_the_launcher_label(self) -> None:
        """The flavors compose their labels from `appName`, so the rename has to reach it."""
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker")

        properties = self.read("gradle.properties")
        self.assertIn("appName=Field Tracker", properties)
        self.assertIn("basePackage=com.acme.tracker", properties)
        self.assertNotIn("AndroidProject1", properties)

    def test_doctor_catches_a_state_that_is_not_immutable(self) -> None:
        state = self.screen("home", "Home") / "HomeState.kt"
        state.write_text(state.read_text().replace("@Immutable\n", ""))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("HomeState", result.stdout)
        self.assertIn("@Immutable", result.stdout)

    def test_generated_screens_are_immutable(self) -> None:
        """A generated state must pass the check the moment it is written."""
        self.run_script("create_feature.py", "userProfile")
        self.run_script("create_screen.py", "userprofile", "UserProfileDetail")
        self.assert_doctor_passes()

    def test_doctor_catches_a_component_without_a_preview(self) -> None:
        component = self.repo / f"core/ui/src/main/kotlin/{BASE_PATH}/core/ui/component/action/AppButton.kt"
        component.write_text(component.read_text().replace("@ComponentPreview", "@Suppress(\"unused\")"))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("AppButton.kt", result.stdout)
        self.assertIn("@ComponentPreview", result.stdout)

    def test_doctor_catches_a_koin_built_class_with_a_defaulted_parameter(self) -> None:
        """Koin's `*Of` builders resolve every parameter through `get()` and never consult a
        Kotlin default, so a defaulted `Clock` nobody bound compiles, reads as safe, passes
        `verify()` and throws the first time the screen opens.
        """
        view_model = self.repo / (
            "feature/trips/presentation/src/main/kotlin/"
            f"{BASE_PATH}/feature/trips/presentation/trips/TripsViewModel.kt"
        )
        view_model.write_text(
            view_model.read_text().replace(
                "private val clock: Clock,",
                "private val clock: Clock = Clock.systemDefaultZone(),",
                1,
            )
        )

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("TripsViewModel", result.stdout)
        self.assertIn("bind the type instead", result.stdout)

    # -- the presentation layout --------------------------------------------------------

    def test_create_screen_brings_the_feature_component_it_composes(self) -> None:
        """The generated screen composes one feature-local component; it has to arrive with it."""
        self.run_script("create_screen.py", "catalog", "ProductReview")

        component = self.component("catalog") / "ProductReviewHeadline.kt"
        self.assertTrue(component.is_file())
        self.assertIn("feature.catalog.presentation.component", component.read_text())
        self.assertIn("@ComponentPreview", component.read_text())
        self.assertIn(
            "import com.example.androidproject1.feature.catalog.presentation.component.ProductReviewHeadline",
            (self.screen("catalog", "ProductReview") / "ProductReviewScreen.kt").read_text(),
        )
        self.assert_doctor_passes()

    def test_doctor_catches_a_composable_left_beside_a_screen(self) -> None:
        """The other half of the screen-directory rule: a screen file holds the screen and its previews, nothing else."""
        screen = self.screen("home", "Home") / "HomeScreen.kt"
        original = screen.read_text()
        screen.write_text(
            original + "\n@Composable\nprivate fun Stray(modifier: Modifier = Modifier) {\n}\n"
        )

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("composable 'Stray' is not the screen", result.stdout)
        screen.write_text(original)

    def test_doctor_catches_a_screen_test_that_never_asserts_accessibility(self) -> None:
        """The assertion a generated screen test carries is only kept if something holds it there.
        The screen is generated, so the test does not depend on which hand-written tests exist."""
        self.run_script("create_feature.py", "userProfile")
        test = self.screen("userprofile", "UserProfile", source_set="test") / "UserProfileScreenTest.kt"
        original = test.read_text()
        self.assertIn("assertAccessible()", original)
        test.write_text(original.replace("compose.assertAccessible()", "// no assertAccessible here"))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("UserProfileScreenTest.kt", result.stdout)
        self.assertIn("never calls assertAccessible()", result.stdout)
        test.write_text(original)

    def test_doctor_accepts_a_screen_without_tests_but_not_without_a_production_file(self) -> None:
        """A screen is its six production files; its ViewModel and screen tests are welcome but
        not required. A missing production file still fails."""
        self.run_script("create_feature.py", "userProfile")
        tests = self.screen("userprofile", "UserProfile", source_set="test")
        for name in ("UserProfileViewModelTest.kt", "UserProfileScreenTest.kt"):
            (tests / name).unlink()
        self.assert_doctor_passes()

        (self.screen("userprofile", "UserProfile") / "UserProfileEvent.kt").unlink()
        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("screen 'UserProfile' is missing UserProfileEvent.kt", result.stdout)

    def test_doctor_catches_a_stranger_in_a_screen_directory(self) -> None:
        stray = self.screen("home", "Home") / "Helpers.kt"
        stray.write_text("package com.example.androidproject1.feature.home.presentation.home\n")

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("is not part of screen 'Home'", result.stdout)
    def test_doctor_catches_a_maestro_id_that_is_not_in_the_code(self) -> None:
        """A renamed tag breaks a flow silently; the grep is what finds it before the emulator."""
        flow = self.repo / ".maestro/sign-in.yaml"
        flow.write_text(flow.read_text().replace("login_emailField", "login_emailBox"))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("sign-in.yaml", result.stdout)
        self.assertIn("login_emailBox", result.stdout)

    def test_doctor_catches_a_library_in_a_module_build_file(self) -> None:
        """A module build file is plugins, project dependencies and resourcePrefix. A library
        line belongs to the convention plugin the module applies."""
        build_file = self.repo / "feature/cart/domain/build.gradle.kts"
        build_file.write_text(build_file.read_text() + "\ndependencies {\n    implementation(libs.junit)\n}\n")

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("feature/cart/domain/build.gradle.kts", result.stdout)
        self.assertIn("names a library", result.stdout)

    def test_doctor_catches_a_hand_written_test_fixtures_block(self) -> None:
        """The other half of the thin-build-file rule: `convention.android.library.testfixtures` exists for this."""
        build_file = self.repo / "feature/cart/data/build.gradle.kts"
        build_file.write_text(build_file.read_text() + "\nandroid {\n    testFixtures {\n        enable = true\n    }\n}\n")

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("feature/cart/data/build.gradle.kts", result.stdout)
        self.assertIn("enables test fixtures by hand", result.stdout)

    def test_doctor_catches_a_test_id_named_after_a_component(self) -> None:
        """The vocabulary stays closed. A stepper in a form is a `Field`, never a `Stepper`."""
        screen = self.screen("home", "Home") / "HomeScreen.kt"
        original = screen.read_text()
        # A tag the screen has: a replace that matches nothing would let the check pass without
        # being exercised.
        self.assertIn('testTag("home_greetingValue")', original)
        screen.write_text(original.replace(
            'testTag("home_greetingValue")', 'testTag("foo_barStepper")', 1,
        ))

        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("test id 'foo_barStepper' ends in no vocabulary word", result.stdout)

    def test_doctor_accepts_a_row_id_after_item_and_nothing_else(self) -> None:
        """
        A row is `<stem>_item_<id>` so a flow can tap the one it means. The suffix is
        allowed after `item` only — `cart_itemX` is not a vocabulary word and `cart_button_42`
        is a button with a number, which is not a thing.
        """
        # The greeting's tag: one no flow drives, so the Maestro check has nothing to miss.
        screen = self.screen("home", "Home") / "HomeScreen.kt"
        original = screen.read_text()

        screen.write_text(original.replace('testTag("home_greetingValue")', 'testTag("cart_item_42")', 1))
        self.assert_doctor_passes()

        for refused in ("cart_itemX", "cart_button_42"):
            screen.write_text(original.replace('testTag("home_greetingValue")', f'testTag("{refused}")', 1))
            result = self.run_script("doctor.py", expect_success=False)
            self.assertNotEqual(0, result.returncode, refused)
            self.assertIn(f"test id '{refused}' ends in no vocabulary word", result.stdout)
        screen.write_text(original)

    def test_doctor_reads_a_templated_row_tag_as_every_row_it_makes(self) -> None:
        """
        A flow taps `categories_item_beverages`; the code declares
        `categories_item_${category.id}`. The check reads the template as every id it can make,
        and a `<stem>_item_.*` flow id as the rows of that stem.
        """
        flow = self.repo / ".maestro/browse-to-a-product.yaml"
        original = flow.read_text()
        self.assertIn('id: "categories_item_beverages"', original)
        self.assert_doctor_passes()

        flow.write_text(original.replace('id: "categories_item_beverages"', 'id: "categories_row_beverages"', 1))
        result = self.run_script("doctor.py", expect_success=False)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("categories_row_beverages", result.stdout)

        flow.write_text(original.replace('id: "categories_item_beverages"', 'id: "categories_item_.*"', 1))
        self.assert_doctor_passes()
        flow.write_text(original)

    @unittest.skipUnless(
        WITH_GRADLE,
        "compiles a generated feature; pass --with-gradle (minutes, not seconds)",
    )
    def test_generated_feature_compiles(self) -> None:
        """
        The half every other test here cannot reach.

        The rest of this file checks the *text* a generator wrote — the registrations, the names,
        the strings. Text can be perfectly correct and still not compile: an import the template
        stopped needing, a signature that moved in `:core:ui`, an `R` reference that a sub-package
        broke. `feature/template` is compiled by the ordinary build, so it is the *rewriting* that
        is unproven, and nothing notices until someone runs a generator and gets a red project.

        Compiles the presentation module, which is where the generated Compose, the `R` references
        and the navigation wiring all live, and so where a template change breaks first.
        """
        self.run_script("create_feature.py", "userProfile")

        result = subprocess.run(
            [
                "./gradlew",
                ":feature:userprofile:presentation:assembleDebug",
                "--console=plain",
                # The temp copy is thrown away, so a stored entry would be written for a project
                # directory that is about to stop existing.
                "--no-configuration-cache",
            ],
            cwd=self.repo,
            capture_output=True,
            text=True,
            env=hermetic_env(),
        )
        if result.returncode != 0:
            # The compiler's own message, not "exit 1": the error should be in the log rather than
            # reproducible only by re-running it locally.
            self.fail(
                "a generated feature does not compile:\n"
                f"{result.stdout[-6000:]}\n{result.stderr[-4000:]}"
            )

    @unittest.skipUnless(
        WITH_GRADLE,
        "builds and tests a clean start; pass --with-gradle (minutes, not seconds)",
    )
    def test_a_clean_start_builds_and_passes_its_tests(self) -> None:
        """`scripts/clean_start/` is not compiled in the template, so a shell change it should have
        shared shows up here first: the copy init_project.py makes must build and pass its tests."""
        self.run_script("init_project.py", "--package", "com.acme.tracker", "--name", "Field Tracker", "--force")

        result = subprocess.run(
            ["./gradlew", ":app:assembleDevDebug", "test", "--console=plain", "--no-configuration-cache"],
            cwd=self.repo, capture_output=True, text=True, env=hermetic_env(),
        )
        if result.returncode != 0:
            self.fail(
                "a clean start does not build or pass its tests:\n"
                f"{result.stdout[-6000:]}\n{result.stderr[-4000:]}"
            )

    @unittest.skipUnless(
        WITH_GRADLE,
        "compiles the gallery after a generated component; pass --with-gradle (minutes, not seconds)",
    )
    def test_generated_component_compiles_in_the_gallery(self) -> None:
        """The gallery entry a generator writes is the one line of it a compiler judges — an
        import that is missing or a call that does not match the signature breaks
        `:feature:gallery:presentation`."""
        self.run_script("create_component.py", "PriceTicker", "--group", "content")
        self.run_script("create_component.py", "StockRow", "--group", "content", "--state")

        result = subprocess.run(
            ["./gradlew", ":feature:gallery:presentation:compileDebugKotlin", "--console=plain", "--no-configuration-cache"],
            cwd=self.repo, capture_output=True, text=True, env=hermetic_env(),
        )
        if result.returncode != 0:
            self.fail(
                "the gallery does not compile after a generated component:\n"
                f"{result.stdout[-6000:]}\n{result.stderr[-4000:]}"
            )



if __name__ == "__main__":
    unittest.main(verbosity=2)
