pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.2.0"
        id("org.jetbrains.kotlin.android") version "1.9.20"
        id("com.google.dagger.hilt.android") version "2.48.1"
        id("com.google.gms.google-services") version "4.4.0"
        id("com.google.firebase.crashlytics") version "2.9.9"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NordicBeaconScanner"
include(":app")
