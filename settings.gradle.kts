pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "discord-relay"

include(":android")
include(":desktop")
include(":console")
include(":common")
include(":lib")
include(":server")
