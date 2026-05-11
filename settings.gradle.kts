pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.3"
    id("dev.kikugie.loom-back-compat") version "0.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // 1.21.11 + 26.1 require porting to owo-lib 0.13's UIComponent rebrand
        // (520 compile errors on 1.21.11). Tracked in MIGRATION.md.
        versions("1.21.8")
        vcsVersion = "1.21.8"
    }
}

rootProject.name = "Soundboard"
