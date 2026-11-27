# AndroidProject1

A multi-module Android app template and a catalog of samples built on it: Kotlin, Jetpack Compose,
Material 3, Navigation 3, Koin, Coroutines and Flow, Room, DataStore and Ktor, in one activity.

The project has two halves. **The frame** is what a real app needs before its first feature:
sign-in and the session, onboarding, settings, a design system with a gallery of its components, a
debug menu, a forced-update screen, and a set of reusable platform services. **The samples** are
small features on top of it, each showing one kind of screen or one kind of data: an e-shop flow, an
offline-first editor with a sync outbox, a paged list cached in Room, a multi-step wizard, a search
against a public API.

## What's inside

### Features

| Feature | Shows |
|---|---|
| `auth` | sign-in, sign-up, e-mail verification with a code, password reset through a deep link, the encrypted session |
| `onboarding` | a first-run flow, a guided tour over the running app, a "what's new" sheet after an update |
| `home` | the landing tab: a card per place in the app, each contributed by its feature, so Home names no other feature |
| `catalog` | categories, products, product detail with rendered HTML, search with recent queries, favourites, a product picker that returns a result |
| `cart` | the cart and its badge, quantities, totals |
| `checkout` | a multi-step checkout with validation, a draft that survives process death, Google Pay in the `dev` flavor through a pretend sheet |
| `orders` | the order history and an order's detail with its status |
| `inventory` | a searchable, filterable, sortable list with selection mode, a four-step editor, and writes that are saved at once and sent through an outbox when there is a network |
| `trips` | a list, a three-step wizard, a detail with tabs, a destination picker, a dashboard, reminders |
| `movies` | TVmaze shows paged as you scroll (Paging 3), cached in Room so they read offline, pull to refresh, favourites |
| `weather` | Open-Meteo: a place search, saved places in Room, a forecast per place |
| `profile` | name, e-mail and an avatar from the photo picker, cropped and uploaded with retry |
| `settings` | theme, per-app language (English, Czech), notifications, app lock, permissions, in-app review, account deletion |
| `apphold` | the screen the app is held behind when the startup config says so: an update required, or maintenance |
| `gallery` | every design-system component with its states, light and dark |
| `devmenu` | build info, the fixture engine's switches (offline, latency, failure rate), jumps to any deep screen, logs, network inspector. Debug builds only |
| `template` | the feature the code generators clone; compiled with the rest so it cannot go stale |

### Services

`service/` is the reusable half: it knows nothing about this app's features, theme or object graph,
and can be copied into another project.

| Module | What it is |
|---|---|
| `core:domain` · `core:data` · `core:ui` | the architecture: `Outcome` and `DomainError`, `BaseRepository`, the MVI `BaseViewModel`, the screen shell, dispatchers, encrypted DataStore, form drafts |
| `network` | the Ktor client: auth headers and token refresh, error mapping, a `MockEngine` fixture engine for the `dev` flavor, a network log |
| `session` | the stored session and its lifecycle |
| `navigation` · `deeplink` | the navigator over the Navigation 3 back stack, result passing, deep-link routing |
| `log` · `telemetry` | a redacting logger, a file log for debug builds, crash and ANR records, analytics and error-tracking seams |
| `connectivity` · `locale` · `device` | network state, per-app language, device facts |
| `permission` · `notification` · `work` | runtime permissions, notification channels and posting, WorkManager with a Koin-built worker factory |
| `biometric` · `pay` · `play` · `scanner` | BiometricPrompt, Google Pay, in-app updates and review, a code scanner |
| `media` · `files` · `location` · `intents` | the photo picker and cropping, file sharing through a FileProvider, location, Custom Tabs and system intents |
| `devtools` | what the debug menu drives |

## Architecture

Clean architecture over MVI, one activity, type-safe Compose navigation, Koin for injection.
Dependencies point one way only:

```
build-logic/                    the convention.* plugins, an included build
service/…                       the reusable architecture and platform services
core/{ui,di}                    this app's theme and design system, its single Koin registration point
app                             the activity, AppNavHost, MainViewModel, the tabs
feature/<name>/{api,domain,data,presentation,di}
```

| Layer | May depend on |
|---|---|
| `domain` | `service:core:domain` only: a plain Kotlin/JVM module, so `android.*` does not compile there |
| `data` | `service:core:data` and its own `domain` |
| `presentation` | `core:ui`, its own `domain`, and another feature's `domain` |
| `di` | all of the above |

A feature's `presentation` never depends on another feature's `presentation`; crossing features is
pushing the other feature's route key from its `api` module. `scripts/doctor.py` enforces these
rules, together with the naming and the shape of each module.

A screen is a directory of six files: `XDestination`, `XScreen` (stateless), `XState`, `XEvent`,
`XNavigation` and `XViewModel`. Repositories return `Outcome`, never throw; a ViewModel turns a
failure into an alert or an inline retry, and one shell renders loading, error and empty states for
every screen. More in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

A module's build file is its `plugins` block and its project dependencies, nothing else:

```kotlin
plugins { alias(libs.plugins.convention.feature.presentation) }

dependencies {
    api(projects.core.ui)
    api(projects.feature.auth.domain)
}
```

SDK levels, the Java target, lint, the namespace and every library dependency live in
`build-logic/`.

## Build and run

| | Version | Note |
|---|---|---|
| JDK | 25 | pinned in `gradle/gradle-daemon-jvm.properties`; Gradle provisions it on first run |
| Android SDK | API 37 | `compileSdk` and `targetSdk`; `minSdk` is 29. Found through `ANDROID_HOME` or `local.properties` |
| Python | 3.10+ | the scripts in `scripts/`; standard library only |

Three flavors on one dimension, `dev`, `staging` and `prod`, so a variant is `devDebug` or
`prodRelease`. `dev` serves its data from local fixtures, so it runs with no backend and no keys:

```bash
./gradlew :app:installDevDebug        # onto a connected device or emulator
./gradlew test                        # every module's unit tests, Robolectric included
./gradlew lint ktlintCheck
./gradlew verifyRoborazziDebug        # the design system's screenshot tests
./gradlew koverHtmlReport             # coverage
```

`python3 scripts/doctor.py` checks the conventions in a few seconds;
`git config core.hooksPath .githooks` runs it before every commit.

Sign-in is a local mock that accepts any valid e-mail address; on `dev`, sign-up's e-mail code is
`000000`. Release builds run R8.

## Tests

| Kind | Where | Runs on |
|---|---|---|
| domain rules and repositories, with fakes from `testFixtures` | `*/domain`, `*/data` | JVM |
| ViewModels on virtual time, with Turbine | `*/presentation` | JVM |
| Compose screens: what is on screen, what a tap sends, accessibility | `*/presentation` | Robolectric |
| the design system's goldens, light and dark | `core/ui` | Roborazzi |
| the Koin graph, navigation and recreation of the real app | `app` | Robolectric |
| notifications and other system integration | `app/src/androidTest` | Gradle managed device |

Tests use hand-written fakes rather than a mocking library, and find elements by test tag rather
than by text.

## Generating code

A screen is eight files and a handful of registrations, so the scripts generate them:

| I need | Command |
|---|---|
| a feature, full stack | `python3 scripts/create_feature.py userProfile` |
| a screen-only feature | `python3 scripts/create_feature.py userProfile --layers presentation,di` |
| another screen | `python3 scripts/create_screen.py userprofile UserProfileDetail` |
| a screen with route arguments | `python3 scripts/create_screen.py userprofile Detail --with-args 'id:String'` |
| a shared UI component | `python3 scripts/create_component.py PrimaryButton --group action` |
| a data source | `python3 scripts/create_datasource.py userprofile LocalUserProfile --repository` |
| to remove a feature | `python3 scripts/delete_feature.py userProfile` |
| to check the conventions | `python3 scripts/doctor.py` |

Every script takes `--dry-run` and `--help`. Details in [scripts/README.md](scripts/README.md).

### Starting a new project from it

```bash
python3 scripts/init_project.py --package com.acme.tracker --name "Field Tracker" --author "Acme Ltd"
```

It starts the copy clean: the samples, sign-in, onboarding, profile and Settings go, `:app` becomes
the shell in `scripts/clean_start/`, and the app opens on one empty screen generated from the
template. The architecture, `:core`, every service, the build logic, the forced-update screen and,
on `dev`, the debug menu and the component gallery stay. Then it rewrites the base package
everywhere, moves the package directories and renames the project, the theme and the launcher label.
`--keep-samples` keeps the whole catalog instead.

Then add your own features with `create_feature.py`.

## Stack

Kotlin 2.4 · Jetpack Compose (BOM 2026.09) · Material 3 · Navigation 3 · Koin 4 · Coroutines and
Flow · Ktor 3 · kotlinx.serialization · Room · DataStore · Paging 3 · WorkManager · Coil ·
Turbine · Robolectric · Roborazzi · Kover · ktlint · Gradle convention plugins · GitHub Actions.
Versions are in [gradle/libs.versions.toml](gradle/libs.versions.toml).

## License

All rights reserved. The source is published to be read; see [LICENSE](LICENSE).
