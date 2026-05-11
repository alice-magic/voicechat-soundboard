import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("dev.kikugie.loom-back-compat")
    kotlin("jvm")
}

class ModData {
    val id: String = sc.properties["mod.kowoui.id"]
    val name: String = sc.properties["mod.kowoui.name"]
    val group: String = sc.properties["mod.group"]
    val version: String = sc.properties["mod.kowoui.version"]
}
val mod = ModData()

group = mod.group
version = "${mod.version}+${sc.current.version}"
base { archivesName.set(mod.id) }

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${sc.properties["deps.fabric_loader"]}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${sc.properties["deps.fabric_language_kotlin"]}")
    modApi("io.wispforest:owo-lib:${sc.properties["deps.owo_lib"]}") {
        exclude(group = "net.fabricmc")
    }
}

tasks.processResources {
    val map = mapOf(
        "id" to mod.id,
        "name" to mod.name,
        "version" to "${mod.version}+${sc.current.version}",
        "minecraft" to sc.properties["mod.mc_compat"],
        "flk" to sc.properties["deps.flk_dep"],
        "owolib" to sc.properties["deps.owo_dep"],
    )
    map.forEach { (k, v) -> inputs.property(k, v) }

    filesMatching("fabric.mod.json") { expand(map) }
}
