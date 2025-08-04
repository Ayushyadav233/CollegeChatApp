// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Specify the plugin ID and version directly for the top-level build file.
    // The 'apply false' is correct here as it indicates these plugins are applied to sub-modules.
    id("com.android.application") version "8.12.0" apply false // Ensure this matches your AGP version
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false // Ensure this matches your Kotlin version
    // If you are using Compose, uncomment and ensure this matches your Kotlin version:
    // id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
}

// You might also have a 'buildscript' or 'allprojects' block here,
// ensure they are outside the 'plugins' block and correctly formatted.
