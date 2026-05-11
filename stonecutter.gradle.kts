plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.8"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        // 26.1+ uses Mojang Mappings → AW header must declare 'official' instead of 'named'.
        // Source-level mappings differences (Identifier ↔ ResourceLocation, package renames)
        // are handled per-file with Stonecutter `//?` comment syntax — never blanket-replaced
        // here, as that would corrupt the canonical source tree.
        string(current.parsed >= "26.1") {
            replace("accessWidener v2 named", "accessWidener v2 official")
        }
    }
}
