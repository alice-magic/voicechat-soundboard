# Migration to Stonecutter (alice-magic fork)

This fork was set up by **alice-magic** to add multi-version support
for Minecraft **1.21.8**, **1.21.11**, and **26.1.2** on Fabric.

It uses [Stonecutter](https://stonecutter.kikugie.dev/) (built by the
original author, kikugie) so a single source tree can target multiple
incompatible Minecraft releases.

## What was done in this scaffold commit

- Replaced the old `gradle/libs.versions.toml` and per-module
  `gradle.properties` Minecraft pins with `stonecutter.properties.toml`
  centralizing every per-version dependency.
- Added `settings.gradle.kts` Stonecutter declaration:
  - `versions("1.21.8", "1.21.11")` (Yarn mappings)
  - `version("26.1", "26.1.2")` (Mojang Mappings; Java 25 toolchain)
- Added root `stonecutter.gradle.kts` with parameter swaps and
  `Identifier`/`ResourceLocation` rename rules for 1.21.11+ targets.
- Switched root and sub-project `build.gradle.kts` to the
  `dev.kikugie.loom-back-compat` plugin and `loomx.applyMojangMappings()`
  so loom selects the correct mappings per target automatically.
- Bumped Kotlin to `2.3.21` to match
  `fabric-language-kotlin 1.13.11+kotlin.2.3.21`.

## What is NOT done yet

The build will **not** compile until source-level migration is finished.
Quoting kikugie on upstream issues
[#32](https://github.com/kikugie/voicechat-soundboard/issues/32) and
[#36](https://github.com/kikugie/voicechat-soundboard/issues/36):

> "most of the mod has to be reimplemented" for 1.21.8+
> "the audio decoder wasn't implemented"

The remaining work, in rough order:

1. **Simple Voice Chat 2.5 → 2.6 plugin API** — `MergeClientSoundEvent`,
   client audio decoder, channel constructor signatures all changed.
   See `vc-simple/src/main/kotlin/.../entrypoint/SVCEntrypoint.kt`.
2. **Plasmo Voice 2.0 → 2.1.9 client API** — addon registration moved.
3. **owo-lib 0.12 → 0.13** breaking changes (1.21.11+ targets).
   See `src/main/kotlin/.../gui/component/*.kt` and the owo mixins under
   `src/main/java/.../mixin/owo_ui/`.
4. **Identifier API rename** (`ResourceLocation` ↔ `Identifier`) —
   handled by Stonecutter `replacements` for source compat, but verify
   any places that build Identifier strings explicitly.
5. **Mojang Mappings** for 26.1+ — class tweakers (`*.ct`) and mixin
   targets need to compile under both yarn and mojmap. Use Stonecutter
   `//? if >=26.1` blocks where the API names diverge.
6. **Java 25 toolchain** — make sure CI / Adoptium install is
   available when building the 26.1 target.

## Useful commands once source migration begins

```bash
# Switch the active version locally (writes IntelliJ run configs):
./gradlew "Set active project to 1.21.11"
./gradlew "Set active project to 1.21.8"
./gradlew "Set active project to 26.1.2"

# Build only the active version:
./gradlew build

# Build every Stonecutter version:
./gradlew chiseledBuild
```
