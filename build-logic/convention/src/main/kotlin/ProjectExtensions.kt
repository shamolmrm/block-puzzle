import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

internal fun VersionCatalog.intVersion(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

internal val PbbJavaVersion = JavaVersion.VERSION_17

/** Namespace for a module path such as `:core:engine` → `com.puzzleblockblast.game.core.engine`. */
internal fun Project.pbbNamespace(): String =
    "com.puzzleblockblast.game" + path.replace(':', '.').replace('-', '_')

internal fun Project.addUnitTestDependencies() {
    dependencies.add("testImplementation", libs.lib("junit"))
    dependencies.add("testImplementation", libs.lib("truth"))
    dependencies.add("testImplementation", libs.lib("kotlinx-coroutines-test"))
    dependencies.add("testImplementation", libs.lib("turbine"))
}
