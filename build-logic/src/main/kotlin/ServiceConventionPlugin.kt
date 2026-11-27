import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * `convention.service` — every `service/` module's build, read from its row in [SERVICE_MODULES].
 * One plugin and one table rather than a plugin per module: the rows differ only in which
 * libraries they add, and twenty-five files of the same six lines hid that.
 */
class ServiceConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val module = SERVICE_MODULES[path]
            ?: error("$path applies convention.service but has no row in build-logic's ServiceModules.kt — add one, or run create_service.py")
        when (module.base) {
            ServiceBase.KotlinJvm -> pluginManager.apply("convention.kotlin.jvm")
            ServiceBase.PlainKotlinJvm -> {
                pluginManager.apply(libs.findPlugin("kotlin-jvm").get().get().pluginId)
                // Coverage. Aggregated by the root build, which lists every module; a signal, never a gate.
                pluginManager.apply("org.jetbrains.kotlinx.kover")
                extensions.configure<JavaPluginExtension> {
                    sourceCompatibility = ProjectConfig.JAVA_VERSION
                    targetCompatibility = ProjectConfig.JAVA_VERSION
                }
                configureKotlinJvmTarget()
            }
            ServiceBase.AndroidLibrary -> pluginManager.apply("convention.android.library")
            ServiceBase.ComposeLibrary -> pluginManager.apply("convention.android.library.compose")
            ServiceBase.FeatureData -> pluginManager.apply("convention.feature.data")
        }
        // A service is reused by directory copy, so what it exports is a contract: explicit API
        // mode makes every public declaration say so, and everything else `internal`. Main code
        // only — a test or a fixture is not an export.
        tasks.withType<KotlinCompile>().configureEach {
            if (!name.contains("Test")) compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
        }
        if (module.testFixtures) {
            // Consumed with `testImplementation(testFixtures(projects...))`.
            pluginManager.apply(if (module.base.isAndroid) "convention.android.library.testfixtures" else "java-test-fixtures")
        }
        if (module.serialization) {
            pluginManager.apply(libs.findPlugin("kotlin-serialization").get().get().pluginId)
        }
        if (module.androidResourcesInTests) {
            extensions.configure<LibraryExtension> {
                @Suppress("UnstableApiUsage")
                testOptions.unitTests.isIncludeAndroidResources = true
            }
        }
        dependencies {
            module.dependencies.forEach { dependency ->
                val notation: Any = when (dependency.kind) {
                    DependencyKind.Library -> libs.findLibrary(dependency.alias).get()
                    DependencyKind.Bundle -> libs.findBundle(dependency.alias).get()
                    DependencyKind.Platform -> platform(libs.findLibrary(dependency.alias).get())
                }
                add(dependency.configuration, notation)
            }
        }
    }
}

/** The plugin a service row starts from. */
internal enum class ServiceBase(val isAndroid: Boolean) {
    /** `convention.kotlin.jvm`: Kotlin/JVM with coroutines, so the compiler keeps out `android.*`. */
    KotlinJvm(isAndroid = false),

    /** Kotlin/JVM without the coroutines `convention.kotlin.jvm` adds. */
    PlainKotlinJvm(isAndroid = false),

    /** `convention.android.library`. */
    AndroidLibrary(isAndroid = true),

    /** `convention.android.library.compose`. */
    ComposeLibrary(isAndroid = true),

    /** `convention.feature.data`. */
    FeatureData(isAndroid = true),
}

internal enum class DependencyKind { Library, Bundle, Platform }

/** A catalog alias added to one configuration. */
internal data class ServiceDependency(val configuration: String, val alias: String, val kind: DependencyKind)

/** One service module's row. Built with [service]. */
internal class ServiceModule(val path: String, val base: ServiceBase) {
    var testFixtures = false
        private set
    var serialization = false
        private set
    var androidResourcesInTests = false
        private set
    val dependencies = mutableListOf<ServiceDependency>()

    /** Ships `src/testFixtures/` — the module's `Fake*`. */
    fun testFixtures() {
        testFixtures = true
    }

    /** The kotlinx.serialization compiler plugin. */
    fun serialization() {
        serialization = true
    }

    /** Unit tests see the module's resources. */
    fun androidResourcesInTests() {
        androidResourcesInTests = true
    }

    fun api(vararg aliases: String) = add("api", aliases)

    fun apiBundle(alias: String) = add("api", arrayOf(alias), DependencyKind.Bundle)

    fun implementation(vararg aliases: String) = add("implementation", aliases)

    fun testImplementation(vararg aliases: String) = add("testImplementation", aliases)

    fun testImplementationPlatform(alias: String) = add("testImplementation", arrayOf(alias), DependencyKind.Platform)

    fun testFixturesImplementation(vararg aliases: String) = add("testFixturesImplementation", aliases)

    fun testFixturesApi(vararg aliases: String) = add("testFixturesApi", aliases)

    fun testFixturesApiPlatform(alias: String) = add("testFixturesApi", arrayOf(alias), DependencyKind.Platform)

    /** The `testing` bundle: JUnit, coroutines-test and the rest every test module takes. */
    fun testingBundle() = add("testImplementation", arrayOf("testing"), DependencyKind.Bundle)

    /** The `testing` bundle plus Robolectric and androidx.test's core, for a test on the platform. */
    fun robolectricTests() {
        testingBundle()
        testImplementation("robolectric", "androidx-test-core")
    }

    private fun add(configuration: String, aliases: Array<out String>, kind: DependencyKind = DependencyKind.Library) {
        aliases.forEach { dependencies += ServiceDependency(configuration, it, kind) }
    }
}

internal fun service(path: String, base: ServiceBase, spec: ServiceModule.() -> Unit = {}): ServiceModule =
    ServiceModule(path, base).apply(spec)
