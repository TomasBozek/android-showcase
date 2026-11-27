import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Paging 3, for the feature that pages: the same plugin on all three of its layers, each
 * getting what it uses.
 *
 * - **Every layer**: `paging-common` — `PagingData`, `Pager`, `RemoteMediator` — which is plain
 *   Kotlin, so the domain (a JVM module) can name `Flow<PagingData<T>>` in its repository.
 * - **A module with Room** (`convention.android.room`): `room-paging`, which lets a `@Query`
 *   return a `PagingSource`.
 * - **A presentation module**: `paging-compose`, `collectAsLazyPagingItems`.
 * - **Tests**: `paging-testing`, `asSnapshot` and `TestPager`, which read a `PagingData` on the JVM.
 *
 * Applied *last* in a module's `plugins` block, because it looks at what is already there.
 */
class PagingConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        dependencies {
            add("api", libs.findLibrary("androidx-paging-common").get())
            add("testImplementation", libs.findLibrary("androidx-paging-testing").get())
            if (pluginManager.hasPlugin("androidx.room")) {
                add("api", libs.findLibrary("androidx-room-paging").get())
            }
            if (pluginManager.hasPlugin("convention.feature.presentation")) {
                add("api", libs.findLibrary("androidx-paging-compose").get())
            }
        }
    }
}
