plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        // Identifier was renamed in 1.21.11+ (yarn) and is part of mojmap on 26.1+
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        // 26.1+ uses official mappings for class tweakers
        string(current.parsed >= "26.1") {
            replace("classTweaker v1 named", "classTweaker v1 official")
        }
    }
}
