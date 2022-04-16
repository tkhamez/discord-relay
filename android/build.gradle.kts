plugins {
    id("org.jetbrains.compose") version "1.2.0"
    id("com.android.application")
    kotlin("android")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.6.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "tkhamez.discordRelay.android"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.3.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    namespace = "tkhamez.discordRelay.android"
}
