plugins {
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    id("me.fallenbreath.yamlang") version "1.4.0"
}

fun prop(key: String): String = sc.properties[key]

class ModData {
    val id: String = prop("mod.id")
    val name: String = prop("mod.name")
    val group: String = prop("mod.group")
    val version: String = prop("mod.version")
}

val mod = ModData()

group = mod.group
version = "${mod.version}+${sc.current.version}"
base { archivesName.set(mod.id) }

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

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
    strictMaven("https://repo.plasmoverse.com/snapshots", "su.plo.voice", "su.plo.voice.api")
    strictMaven("https://repo.plasmoverse.com/releases", "su.plo.config", "su.plo.slib")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    // 26.1+ has no Yarn mappings yet — fall back to Mojang Mappings on those targets.
    if (prop("deps.yarn_mappings").isNotBlank()) {
        mappings("net.fabricmc:yarn:${prop("deps.yarn_mappings")}:v2")
    } else {
        loomx.applyMojangMappings()
    }

    modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${prop("deps.fabric_language_kotlin")}")

    val okhttpDep = "com.squareup.okhttp3:okhttp:${prop("deps.okhttp")}"
    include(okhttpDep)
    api(okhttpDep)

    include("com.squareup.okio:okio:${prop("deps.okio")}")
    include("io.wispforest:owo-sentinel:${prop("deps.owo_lib")}")

    modApi("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric_api")}")
    modApi("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
    modApi("io.wispforest:owo-lib:${prop("deps.owo_lib")}") {
        exclude(group = "net.fabricmc")
    }

    // Voice chat backends — both compileOnly so the mod loads even if neither is present.
    // Runtime presence drives entrypoint activation via fabric-language-kotlin's lazy init.
    compileOnly("de.maxhenkel.voicechat:voicechat-api:${prop("deps.svc_api")}")
    compileOnly("su.plo.voice.api:client:${prop("deps.plasmo_api")}")
    compileOnly("su.plo.config:config:${prop("deps.plasmo_config")}")

    // Local runtime mods for client testing
    modLocalRuntime("maven.modrinth:simple-voice-chat:${prop("deps.svc_mod")}")
    modLocalRuntime("maven.modrinth:plasmo-voice:${prop("deps.plasmo_mod")}")
}

loom {
    accessWidenerPath = sc.process(
        rootProject.file("src/main/resources/soundboard.accesswidener"),
        "build/processed.accesswidener"
    )

    runConfigs.configureEach {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true", "-Dfabric.log.level=debug")
        runDir = "../run"
    }

    decompilers {
        get("vineflower").apply {
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

tasks.processResources {
    val map = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to "${mod.version}+${sc.current.version}",
        "minecraft" to prop("mod.mc_compat"),
        "flk" to prop("deps.flk_dep"),
        "owolib" to prop("deps.owo_dep"),
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

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(loomx.modJar.map { it.archiveFile }, loomx.modSourcesJar.map { it.archiveFile })
    into(rootProject.layout.buildDirectory.file("libs/${version}"))
    dependsOn("build")
}
