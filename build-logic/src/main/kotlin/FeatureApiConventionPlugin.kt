import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature's `api` module: its public route keys and its deep-link table, and nothing else.
 * The one module of a feature another feature may depend on, so it is Kotlin/JVM — no manifest, no
 * resources, no `android.*` — and `navigation3-runtime` compiles here through its JVM variant.
 */
class FeatureApiConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.findPlugin("kotlin-jvm").get().get().pluginId)
        pluginManager.apply(libs.findPlugin("kotlin-serialization").get().get().pluginId)
        // Coverage. Aggregated by the root build, which lists every module; a signal, never a gate.
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = ProjectConfig.JAVA_VERSION
            targetCompatibility = ProjectConfig.JAVA_VERSION
        }
        configureKotlinJvmTarget()

        dependencies {
            // `api`: a route key is a `NavKey`, so every module that names one sees the interface.
            add("api", libs.findLibrary("androidx-navigation3-runtime").get())
            // @Serializable route keys.
            add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
        }
    }
}
