pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Vosk artifact location
        maven { url = uri("https://alphacephei.com/maven/") }
    }
}

rootProject.name = "Cerca de Ti"
include(":app")
