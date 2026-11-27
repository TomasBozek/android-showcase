# scripts/

Generators and convention checks for this project. Python 3.10+, standard library only.

The generators perform the registrations a hand copy tends to skip — `settings.gradle.kts`, the
Koin module list, `AppNavHost.kt` and the rest — and `doctor.py` fails on each one that is
missing. Every script explains itself with `--help`, including worked examples, and every
generator takes `--dry-run`.

## Setup

```bash
git config core.hooksPath .githooks
```

Points Git at the committed `pre-commit` hook, which runs `doctor.py` — silent when it passes,
loud when it does not. `doctor.py` prints a `[note]` until the hook is installed, and
`init_project.py` installs it on the clone it rewrites.

To turn this template into a project of your own, once, on a fresh clone:

```bash
python3 scripts/init_project.py --package com.acme.tracker --name "Field Tracker" --dry-run
```

It starts the copy clean — deletes the sample features, sign-in, onboarding, profile and Settings,
copies the shell in `clean_start/` over `:app`, and generates `feature/home` from the template — then
rewrites the base package across every source file, moves the package directories in every source
set, and renames the Gradle project, the Android theme, the launcher label and `_common.py`.
`--keep-samples` skips the clean start. It refuses a dirty working tree, so `git checkout .` stays
an escape hatch.

`clean_start/` mirrors the repository: each file replaces the one at the same path. It is not
compiled here, so a change to `:app`'s shell that it should share is made in both places, and
`test_scripts.py --with-gradle` builds a clean start to prove the copy still compiles.

## The scripts

| Script | What it does | Registers |
|---|---|---|
| `create_feature.py` | Clones `feature/template` into a new feature | `settings.gradle.kts`, `core/di/build.gradle.kts`, `Koin.kt`, `AppNavHost.kt` |
| `create_screen.py` | A screen — its six production files plus a ViewModel test and a screen test — in a directory of its own, in an existing feature | the ViewModel in the feature's Koin module, the destination in `AppNavHost.kt`, its strings in every locale |
| `create_component.py` | A Compose component and its preview, in `:core:ui` or a feature's `component/` | in `:core:ui`: its gallery entry and its playground entry; a feature's component needs none |
| `create_datasource.py` | A data source in `data`, optionally its repository in `domain` and `data` | the Koin bindings |
| `create_service.py` | A reusable service module under `service/` — port, default, `Fake…` in `testFixtures`, a test — `--android` or `--jvm` | its row in `build-logic`'s `ServiceModules.kt`, `settings.gradle.kts`, `core/di/build.gradle.kts` |
| `delete_feature.py` | The inverse of `create_feature.py` | undoes every registration above |
| `export_service.py` | Copies `service/` and `build-logic/` into a *different* project | prints the `settings.gradle.kts` block |
| `init_project.py` | Renames the template into a project of your own | — |
| `doctor.py` | The convention checks a compiler cannot make; `--list` names them | — |
| `test_scripts.py` | Tests for everything above; `--with-gradle` also compiles what a generator wrote | — |

### Flags worth knowing

**`create_screen.py --with-args`** for a screen that takes route arguments:

```bash
python3 scripts/create_screen.py catalog ProductReview --with-args 'productId:String,rating:Int'
```

It generates the `@Serializable data class` route key, a ViewModel taking that key as a
constructor parameter, the `parametersOf(key)` hand-over in the destination, a plain JVM test and
the `KoinGraphTest` entry that stops `verify()` calling the key a missing definition. Do not
hand-convert a `data object` route into a `data class` afterwards — a screen that loads from a
`LaunchedEffect` instead of from the key it was handed re-fires on recomposition and restores
nothing after process death.

**`create_screen.py --sub`** when the screen's own name makes a poor directory. A screen lands in
`presentation/<screen name, flat lowercase>/` by default; `--sub search` puts it in
`presentation/search/` instead. The directory holds that screen's six files and nothing else —
`doctor.py` fails on a stranger in it, and on a composable left in a screen file. Anything that is
not the screen goes to the feature's `component/` with `create_component.py --feature`.

**`create_datasource.py --repository`** when the data source needs one. Both halves of the source
land in `data.source` and the repository in `data.repository`; the only thing above `data` that may
name any of it is the `XRepository` interface in `domain`.

## Checks

```bash
python3 scripts/doctor.py
python3 scripts/test_scripts.py
./gradlew ktlintCheck
./gradlew test :app:lintDevDebug :app:assembleDevDebug
./gradlew verifyRoborazziDebug
```

This is the order CI runs them in: the conventions fail in seconds, the build takes minutes.
`test_scripts.py` copies the repository into a temporary directory per test and runs the scripts
there, so it never touches the working tree; it takes several minutes.

## Changing what gets generated

`feature/template` **is** the template. `create_feature.py` and `create_screen.py` clone it and only
rewrite names, so to change the shape of every future feature or screen, edit `feature/template` —
not the scripts. It holds two screens and one component:

- `template/Template*` — the plain screen, cloned by default.
- `templateargs/TemplateArgs*` — the argument-carrying variant, cloned by `--with-args`.
  `create_feature.py` deliberately skips it: a new feature starts with one screen, and copying the
  second would leave an unregistered destination behind.
- `component/TemplateHeadline.kt` — the one feature-local composable both screens compose, cloned
  alongside whichever screen is generated, so the `component/` package exists in every generated
  feature.
- `res/values/strings.xml` and `res/values-cs/strings.xml` — the same names in both, because
  `doctor.py` fails on a module that ships one and not the other. Adding a locale means adding its
  file here and its code to `TRANSLATED_LOCALES` in `_common.py`; both generators then write it.

The template is compiled by `./gradlew test` and checked by `doctor.py`, so a broken template fails
before it can generate anything broken.

`create_component.py`, `create_datasource.py` and `create_service.py` generate from templates held
in the scripts instead, because none of them has a counterpart in `feature/template`, and adding a
fake one would ship a placeholder in the app.

`scripts/_common.py` holds the shared naming rules, paths and the idempotent file-editing helpers.
Anything used by two scripts belongs there.

### Renaming caveat

`create_feature.py` and `create_screen.py` use targeted replacements — `feature.template`,
`feature/template`, `Template`, `template` followed by an uppercase letter, and `template_` followed
by a resource name — rather than a blanket rename, so the word appearing in a comment or a string
survives. Preserve that if you edit them; see `_common.py:rewrite_source` and
`rewrite_resource_names`.

`init_project.py` and `export_service.py` are the deliberate exceptions: moving the whole base
package is the point there. Both rewrite the dotted **and** slash-separated forms —
`test_scripts.py` holds the package as a path, and missing that form leaves the suite pointed at a
directory the rename has just emptied.
