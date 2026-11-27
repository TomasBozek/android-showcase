pluginManagement {
    // The convention plugins the modules apply. An included build rather than `buildSrc`, so it
    // has its own settings file and can be copied into another project alongside `service/` —
    // see scripts/export_service.py.
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AndroidProject1"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

/**
 * A layer of a `service`, `core` or `feature` module. [projectPath] is the Gradle path segment,
 * [filePath] the matching directory on disk.
 */
sealed class ModuleSuffix(name: String) {

    val projectPath: String = name
    val filePath: String = name.replace(":", "/")

    data object Api : ModuleSuffix("api")
    data object Di : ModuleSuffix("di")
    data object Domain : ModuleSuffix("domain")
    data object Data : ModuleSuffix("data")
    data object Presentation : ModuleSuffix("presentation")
    data object Ui : ModuleSuffix("ui")
}

/** Includes a module, failing at settings time if its directory or build file is missing. */
fun includeModule(fullName: String, path: String) {
    include(fullName)
    val project = project(fullName)
    project.projectDir = File(settingsDir, path)
    require(project.projectDir.isDirectory) { "${project.projectDir} doesn't exist" }
    require(project.buildFile.isFile) { "${project.buildFile} doesn't exist" }
}

/** Includes a layer of a reusable `service` module — copyable into another project as-is. */
fun includeServiceModule(name: String, vararg suffixes: ModuleSuffix) {
    suffixes.forEach { includeModule(":service:$name:${it.projectPath}", "service/$name/${it.filePath}") }
}

fun includeCoreModule(vararg suffixes: ModuleSuffix) {
    suffixes.forEach { includeModule(":core:${it.projectPath}", "core/${it.filePath}") }
}

fun includeFeatureModule(name: String, vararg suffixes: ModuleSuffix) {
    suffixes.forEach { includeModule(":feature:$name:${it.projectPath}", "feature/$name/${it.filePath}") }
}

include(":app")

// The reusable architecture. Nothing here knows this app's features, theme or DI graph.
includeServiceModule(
    "core",
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Ui,
)

// Logging: the port on the JVM, the sinks on Android.
includeServiceModule(
    "log",
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
)

// Kotlin/JVM: the dev menu's parts that draw nothing; feature/devmenu draws them.
includeModule(":service:devtools", "service/devtools")

// Flat: the ConnectivityManager half of :service:network's ConnectivityMonitor port.
includeModule(":service:connectivity", "service/connectivity")

// Flat: the per-app language and Formats, so a copied app has both before it has a Settings screen.
includeModule(":service:locale", "service/locale")

// Flat: Analytics, ErrorTracker, TrackingLogger and the screen view — the port a vendor binds behind.
includeModule(":service:telemetry", "service/telemetry")

// Reusable too, but flat rather than layered: it is one port to the outside world, so there is no
// domain/data split to make. `includeServiceModule` takes layer suffixes, which this has none of.
includeModule(":service:network", "service/network")

// Flat too: the Navigator over the tab stack, the result store and the discard back handler.
includeModule(":service:navigation", "service/navigation")

// Kotlin/JVM, so a feature's api module can export its deep links in these types.
includeModule(":service:deeplink", "service/deeplink")

// Flat too: a permission's status, the request and the gate over it, and the checker a view model
// reads.
includeModule(":service:permission", "service/permission")

// Flat: who is signed in and the tokens — the encrypted store, the refresh, the one place a 401 ends it.
includeModule(":service:session", "service/session")

// Flat: a Scheduler over WorkManager — one-shot, periodic, constrained and expedited work.
includeModule(":service:work", "service/work")

// Flat: a Notifier over NotificationManagerCompat — the channels, a tap that opens a deep link.
includeModule(":service:notification", "service/notification")

// Flat: where files live by purpose, the one FileProvider, a resumable download, sharing a file.
includeModule(":service:files", "service/files")

// Flat: one fix from the platform's location manager, and a Geocoding port over Android's Geocoder.
includeModule(":service:location", "service/location")

// Flat: a picture from the photo picker or the camera as one call, and the crop/strip/cap behind it.
includeModule(":service:media", "service/media")

// Flat: the platform's commands — share, open a url, dial, mail, map, calendar, settings, the store — in one place.
includeModule(":service:intents", "service/intents")

// Flat: one DeviceInfo — the app's version, the OS, the model, the locale — read once.
includeModule(":service:device", "service/device")

// Flat: a BiometricGate — one prompt, a biometric or the device credential, over androidx.biometric.
includeModule(":service:biometric", "service/biometric")

// Flat: the Play facade — in-app review and in-app update behind ports, Google's fakes where there is no store account.
includeModule(":service:play", "service/play")

// Flat: a PaymentSheet over Google Pay's test environment — the requests are JSON, the sheet is Google's.
includeModule(":service:pay", "service/pay")

// Flat: a CodeScanner over Google's Play services code scanner — no camera permission, no key.
includeModule(":service:scanner", "service/scanner")

// What is genuinely this app's: the Compose theme and the Koin registration point.
includeCoreModule(
    ModuleSuffix.Ui,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "auth",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "home",
    ModuleSuffix.Api,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "settings",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "catalog",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "gallery",
    ModuleSuffix.Api,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "cart",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "profile",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "devmenu",
    ModuleSuffix.Api,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "onboarding",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "trips",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "inventory",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "movies",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "weather",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "apphold",
    ModuleSuffix.Api,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "checkout",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

includeFeatureModule(
    "orders",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)

// Cloned by scripts/create_feature.py. Included so `./gradlew test` keeps the template compiling:
// nothing depends on it, so its own module tests are what prove the generators' source still builds.
includeFeatureModule(
    "template",
    ModuleSuffix.Api,
    ModuleSuffix.Domain,
    ModuleSuffix.Data,
    ModuleSuffix.Presentation,
    ModuleSuffix.Di,
)
