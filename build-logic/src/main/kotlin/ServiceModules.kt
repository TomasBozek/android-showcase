/**
 * Every `service/` module's build, one row each. `convention.service` finds the row by the
 * module's path, so a service's build file is its `plugins` block and project dependencies alone,
 * and a copied `service/` brings its build with it because `build-logic` comes too.
 *
 * A row names the plugin it starts from, then what it adds: test fixtures, the libraries it
 * compiles against, and what its tests run on. A comment here says only why the build is the way
 * it is, not what each module *is*.
 * `create_service.py` writes a new row.
 */
internal val SERVICE_MODULES: Map<String, ServiceModule> = listOf(
    // -- the layered core ----------------------------------------------------------------------

    // The architecture's pure half. Ships TestDispatchers, which needs coroutines-test to compile,
    // because DispatcherProvider is declared here.
    service(":service:core:domain", ServiceBase.KotlinJvm) {
        testFixtures()
        testFixturesImplementation("kotlinx-coroutines-test")
    },
    // The preference-backed bases: DataStore is the one store the module's own sources name.
    service(":service:core:data", ServiceBase.FeatureData) {
        api("androidx-datastore-preferences")
    },
    // The MVI shell, Navigation 3 re-exported — a feature's XDestination.kt names `NavKey` and
    // `EntryProviderScope` while depending only on this app's own ui module — and the fixtures every
    // presentation test stands on: MainDispatcherRule and assertAccessible(), whose Compose test
    // rule a consumer compiles against, hence `testFixturesApi`. The goldens are `:core:ui`'s.
    service(":service:core:ui", ServiceBase.ComposeLibrary) {
        testFixtures()
        api(
            "androidx-activity-compose",
            "androidx-lifecycle-viewmodel-ktx",
            "androidx-lifecycle-viewmodel-savedstate",
            "androidx-navigation3-runtime",
            "androidx-navigation3-ui",
            "androidx-lifecycle-viewmodel-navigation3",
        )
        testFixturesImplementation("junit", "kotlinx-coroutines-test", "robolectric")
        testFixturesApiPlatform("androidx-compose-bom")
        testFixturesApi("androidx-compose-ui-test-junit4")
        testingBundle()
    },

    // -- Kotlin/JVM: nothing here needs the framework, and the compiler keeps it that way --------

    // `FakeLogger` ships to every module that constructs something taking a Logger.
    service(":service:log:domain", ServiceBase.KotlinJvm) {
        testFixtures()
    },
    // The engine is not a dependency: the client factory takes one, `:app` supplies OkHttp and a
    // test supplies MockEngine.
    service(":service:network", ServiceBase.KotlinJvm) {
        serialization()
        api("kotlinx-serialization-json")
        apiBundle("ktor-client")
        testingBundle()
        testImplementation("ktor-client-mock")
    },
    // An api module's deep-link table: plain Kotlin/JVM, without the coroutines
    // `convention.kotlin.jvm` adds. `api`: a target is a list of `NavKey`s.
    service(":service:deeplink", ServiceBase.PlainKotlinJvm) {
        api("androidx-navigation3-runtime")
        testImplementation("junit")
    },
    // The dev menu's parts that draw nothing; `feature/devmenu` draws them.
    service(":service:devtools", ServiceBase.KotlinJvm),

    // -- Android libraries ---------------------------------------------------------------------

    // A sink writes through `android.util.Log`.
    service(":service:log:data", ServiceBase.AndroidLibrary) {
        testingBundle()
    },
    service(":service:connectivity", ServiceBase.AndroidLibrary) {
        api("kotlinx-coroutines-core")
        robolectricTests()
    },
    service(":service:device", ServiceBase.AndroidLibrary) {
        testFixtures()
        robolectricTests()
    },
    service(":service:biometric", ServiceBase.AndroidLibrary) {
        testFixtures()
        api("androidx-biometric", "kotlinx-coroutines-core")
        robolectricTests()
    },
    service(":service:location", ServiceBase.AndroidLibrary) {
        testFixtures()
        api("androidx-core-ktx", "kotlinx-coroutines-core")
        robolectricTests()
    },
    // The channels' names are this module's resources, and the test reads them back.
    service(":service:notification", ServiceBase.AndroidLibrary) {
        testFixtures()
        androidResourcesInTests()
        api("androidx-core-ktx")
        robolectricTests()
    },
    // `FileProvider` is androidx.core's and reads the module's `file_paths.xml`, which the test
    // reads back; the downloader is tested on MockEngine.
    service(":service:files", ServiceBase.AndroidLibrary) {
        testFixtures()
        androidResourcesInTests()
        api("androidx-core-ktx")
        robolectricTests()
        testImplementation("ktor-client-mock")
    },
    // A file's uri goes through `:service:files`' provider, whose paths file is a resource. Custom
    // Tabs (androidx.browser) for a page over the app in its own colour.
    service(":service:intents", ServiceBase.AndroidLibrary) {
        testFixtures()
        androidResourcesInTests()
        api("androidx-core-ktx")
        implementation("androidx-browser")
        robolectricTests()
    },
    // The camera path goes through `:service:files`' provider. The picker's host activity
    // registers the two result contracts; EXIF orientation is read before it is stripped.
    service(":service:media", ServiceBase.AndroidLibrary) {
        testFixtures()
        androidResourcesInTests()
        api("androidx-core-ktx", "kotlinx-coroutines-core")
        implementation("androidx-activity-compose", "androidx-exifinterface")
        robolectricTests()
    },
    service(":service:session", ServiceBase.AndroidLibrary) {
        testFixtures()
        serialization()
        robolectricTests()
        testImplementation("ktor-client-mock")
    },
    service(":service:work", ServiceBase.AndroidLibrary) {
        testFixtures()
        api("androidx-work-runtime", "kotlinx-coroutines-core")
        robolectricTests()
        testImplementation("androidx-work-testing")
    },
    service(":service:play", ServiceBase.AndroidLibrary) {
        testFixtures()
        api("play-review-ktx", "play-app-update-ktx", "kotlinx-coroutines-core")
        robolectricTests()
    },
    // Google Pay's test environment. play-services-wallet pulls an old Fragment, and
    // `registerForActivityResult` wants 1.3.0 or later (lint's InvalidFragmentVersionForActivityResult).
    service(":service:pay", ServiceBase.AndroidLibrary) {
        testFixtures()
        api("play-services-wallet", "kotlinx-coroutines-core")
        implementation("kotlinx-serialization-json", "androidx-activity-compose", "androidx-fragment")
        testingBundle()
    },
    service(":service:scanner", ServiceBase.AndroidLibrary) {
        testFixtures()
        api("play-code-scanner", "kotlinx-coroutines-core")
        testingBundle()
    },

    // -- Compose libraries ---------------------------------------------------------------------

    // `api`: the navigator's whole surface is `NavKey` and `NavBackStack`. Activity Compose for
    // PredictiveBackHandler, under DiscardBackHandler.
    service(":service:navigation", ServiceBase.ComposeLibrary) {
        testFixtures()
        api("androidx-navigation3-runtime")
        implementation("androidx-activity-compose")
        testingBundle()
    },
    // The request is an activity result and the gate a composable; a permission changes outside the
    // app, so LifecycleResumeEffect re-reads it on resume.
    service(":service:permission", ServiceBase.ComposeLibrary) {
        testFixtures()
        implementation("androidx-activity-compose", "androidx-lifecycle-runtime-compose")
        testingBundle()
    },
    // `LocalFormats` is a composition local; AppCompat because the language is the platform's own
    // per-app setting.
    service(":service:locale", ServiceBase.ComposeLibrary) {
        testFixtures()
        implementation("androidx-appcompat")
        testingBundle()
    },
    // `LocalAnalytics` and `ScreenViewEffect` are a composition local and an effect; the install id
    // is kept in preferences; ScreenViewTest composes under Robolectric.
    service(":service:telemetry", ServiceBase.ComposeLibrary) {
        testFixtures()
        implementation("androidx-datastore-preferences")
        testingBundle()
        testImplementationPlatform("androidx-compose-bom")
        testImplementation("androidx-compose-ui-test-junit4", "robolectric")
    },
).associateBy { it.path }
