import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.0.0"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE") val jvmMain by getting {
            dependencies {
                implementation(project(":lib"))
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

compose.desktop {
    application {
        mainClass = "tkhamez.discordRelay.desktop.MainKt"
        nativeDistributions {
            modules("java.sql")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            //targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Rpm)
            packageName = "DiscordRelay"
            packageVersion = "1.3.0"
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/jvmMain/resources/app-icon.png"))
            }
        }
    }
}
