import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.0.0"
    id("com.android.library")
}

kotlin {
    android {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE") val commonMain by getting {
            dependencies {
                implementation(project(":lib"))
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
            }
        }
        @Suppress("UNUSED_VARIABLE") val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.4.1")
                api("androidx.core:core-ktx:1.7.0")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }
        @Suppress("UNUSED_VARIABLE") val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }

        // Workaround for "The Kotlin source set androidAndroidTestRelease was configured but not added ..." warning.
        val androidAndroidTestRelease by getting
        @Suppress("UNUSED_VARIABLE") val androidTest by getting {
            dependsOn(androidAndroidTestRelease)
        }
    }
}

android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
