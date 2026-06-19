pluginManagement {
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
        // No JitPack: the only artifact that needed it (SmoothBottomBar) was removed in
        // audit 2026-06-19 P3-5. Every dependency now resolves from google()/mavenCentral(),
        // so no GitHub-built jar can enter the build. Glide is on Maven Central under
        // com.github.bumptech.glide. Do not re-add JitPack without a security review.
    }
}

rootProject.name = "EduLife"
include(":app")
 