import me.modmuss50.mpp.ReleaseType

plugins {
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm")
    id("me.modmuss50.mod-publish-plugin") version "0.8.+"
}

class ModData {
    val id: String = project.property("id").toString()
    val name: String = project.property("name").toString()
    val group: String = sc.properties["mod.group"]
    val version: String = sc.properties["mod.version"]
}
val mod = ModData()

group = mod.group
version = "${mod.version}+${sc.current.version}"
base { archivesName.set(mod.id) }

dependencies {
    include(project(":"))
    include(project(":kowoui"))
    implementation(project(path = ":", configuration = "namedElements"))
    runtimeOnly(project(path = ":kowoui", configuration = "namedElements"))

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"]}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${sc.properties["deps.fabric_language_kotlin"]}")

    compileOnly("su.plo.voice.api:client:${sc.properties["deps.plasmo_api"]}")
    compileOnly("su.plo.config:config:${sc.properties["deps.plasmo_config"]}")
    modLocalRuntime("maven.modrinth:plasmo-voice:${sc.properties["deps.plasmo_mod"]}")
}

loom {
    runConfigs["client"].apply {
        ideConfigGenerated(true)
        runDir = "../run"
    }
}

tasks.processResources {
    val map = mapOf(
        "version" to "${mod.version}+${sc.current.version}",
    )
    map.forEach { (k, v) -> inputs.property(k, v) }

    filesMatching("fabric.mod.json") { expand(map) }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${version}"))
    dependsOn("build")
}

publishMods {
    val minecraft: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
        ?.asList().orEmpty().map { it.toString() }
    val modrinthToken = findProperty("modrinthToken")
    val curseforgeToken = findProperty("curseforgeToken")
    dryRun = modrinthToken == null || curseforgeToken == null

    file = tasks.remapJar.get().archiveFile
    displayName = "Plasmo Soundboard ${version}"
    this.version = version.toString()
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = ReleaseType.of(sc.properties["release"])
    modLoaders.add("fabric")

    modrinth {
        projectId = property("publish.modrinth").toString()
        accessToken = modrinthToken as String?
        minecraftVersions.addAll(minecraft)
        requires { slug = "fabric-api" }
        requires { slug = "fabric-language-kotlin" }
        requires { slug = "plasmo-voice" }
        requires { slug = "owo-lib" }
    }

    curseforge {
        projectId = property("publish.curseforge").toString()
        accessToken = curseforgeToken as String?
        minecraftVersions.addAll(minecraft)
        requires { slug = "fabric-api" }
        requires { slug = "fabric-language-kotlin" }
        requires { slug = "plasmo-voice" }
        requires { slug = "owo-lib" }
    }
}
