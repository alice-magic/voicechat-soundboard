plugins {
    // Loom variant is selected automatically based on the Stonecutter target version.
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("me.fallenbreath.yamlang") version "1.4.0"
}

class ModData {
    val id: String = sc.properties["mod.id"]
    val name: String = sc.properties["mod.name"]
    val group: String = sc.properties["mod.group"]
    val version: String = sc.properties["mod.version"]
}

val mod = ModData()

// DO NOT set group via convention here — it must be applied per-project so Stonecutter can resolve.
group = mod.group
version = "${mod.version}+${sc.current.version}"
base { archivesName.set(mod.id) }

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

allprojects {
    repositories {
        fun strictMaven(url: String, vararg groups: String) = exclusiveContent {
            forRepository { maven(url) }
            filter { groups.forEach(::includeGroup) }
        }
        mavenCentral()
        maven("https://jitpack.io")
        strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
        strictMaven("https://maven.wispforest.io", "io.wispforest", "io.wispforest.endec")
        strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc")
        strictMaven("https://maven.maxhenkel.de/releases", "de.maxhenkel.voicechat")
        strictMaven("https://repo.plasmoverse.com/snapshots", "su.plo.voice", "su.plo.voice.api", "su.plo.slib")
        strictMaven("https://repo.plasmoverse.com/releases", "su.plo.config")
    }
}

dependencies {
    implementation(project(path = "kowoui", configuration = "namedElements"))

    fun fapi(vararg modules: String) {
        for (it in modules) modApi(fabricApi.module("fabric-$it", sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Applies Yarn on legacy versions, Mojang Mappings on 26.1+
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"]}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${sc.properties["deps.fabric_language_kotlin"]}")

    val okhttpDep = "com.squareup.okhttp3:okhttp:${sc.properties["deps.okhttp"]}"
    include(okhttpDep)
    api(okhttpDep)

    include("com.squareup.okio:okio:${sc.properties["deps.okio"]}")
    include("io.wispforest:owo-sentinel:${sc.properties["deps.owo_lib"]}")

    modApi("net.fabricmc.fabric-api:fabric-api:${sc.properties["deps.fabric_api"]}")
    modApi("com.terraformersmc:modmenu:${sc.properties["deps.modmenu"]}")
    modApi("io.wispforest:owo-lib:${sc.properties["deps.owo_lib"]}") {
        exclude(group = "net.fabricmc")
    }
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/soundboard.accesswidener")

    runConfigs.configureEach {
        ideConfigGenerated(false)
        vmArgs("-Dmixin.debug.export=true", "-Dfabric.log.level=debug")
    }

    decompilers {
        get("vineflower").apply {
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

tasks.processResources {
    val map = mapOf(
        "version" to "${mod.version}+${sc.current.version}",
        "minecraft" to sc.properties["mod.mc_compat"],
        "flk" to sc.properties["deps.flk_dep"],
        "owolib" to sc.properties["deps.owo_dep"],
    )
    map.forEach { (k, v) -> inputs.property(k, v) }

    filesMatching("fabric.mod.json") { expand(map) }
}

yamlang {
    targetSourceSets.set(mutableListOf(sourceSets["main"]))
    inputDir.set("assets/${mod.id}/lang")
    owolibRichTranslations = true
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}
