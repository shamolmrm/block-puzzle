pluginManagement {
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

rootProject.name = "PuzzleBlockBlast"

include(":app")

include(":core:engine")
include(":core:model")
include(":core:designsystem")
include(":core:data")
include(":core:analytics")
include(":core:audio")
include(":core:testing")

include(":feature:home")
include(":feature:game")
include(":feature:levels")
include(":feature:settings")
