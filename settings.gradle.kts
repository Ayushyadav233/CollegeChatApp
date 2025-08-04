// settings.gradle.kts (in your project root)

// This file configures global settings for your Gradle project.
// It defines how plugins are managed and how dependencies are resolved across modules.

pluginManagement {
    // Define repositories where Gradle should look for plugins.
    repositories {
        google()        // Google's Maven repository for Android-specific plugins
        mavenCentral()  // Maven Central repository for general Java/Kotlin plugins
        gradlePluginPortal() // Gradle Plugin Portal for community plugins
    }
    // Configure toolchain management to specify where Gradle should download JDKs/toolchains from.
    toolchainManagement {
        repositories {
            // By default, Gradle looks for toolchains in Maven Central.
            mavenCentral()
            // You can add other repositories here if your toolchains are hosted elsewhere, e.g.:
            // maven { url = uri("https://repo.gradle.org/gradle/toolchains/") }
        }
    }
}

// Configure dependency resolution for all projects in this build.
dependencyResolutionManagement {
    // Set repository mode to fail if a project tries to use a repository not defined here.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    // Define repositories where Gradle should look for project dependencies.
    repositories {
        google()
        mavenCentral()
    }
    // Define version catalogs. The 'libs' catalog is used to manage dependencies
    // and plugins from 'gradle/libs.versions.toml'.
    // IMPORTANT: The 'from' method can only be called ONCE for each catalog.
    versionCatalogs {
        create("libs") { // Creates a catalog named 'libs'
            from("gradle/libs.versions.toml") // Links it to the TOML file
        }
    }
}

// Define the root project's name.
rootProject.name = "CollegeChatApp"
// Include sub-modules in your project. Each module should be listed here.
include(":app") // Includes the 'app' module
// If you have other modules (e.g., ':data', ':domain'), add them like:
// include(":data")
// include(":domain")
