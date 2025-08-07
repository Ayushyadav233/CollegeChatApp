plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
}

// ✅ No repositories here

allprojects {
    repositories {
        google() // ← this is important!
        mavenCentral()
    }
}

