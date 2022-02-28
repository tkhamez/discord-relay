import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.6.4"

plugins {
    kotlin("jvm")
}

kotlin {
    sourceSets {
        @Suppress("UNUSED_VARIABLE") val main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation("io.ktor:ktor-client-websockets:$ktorVersion")
                implementation("io.ktor:ktor-client-gson:$ktorVersion")
            }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
