# Migration to Stonecutter (alice-magic fork)

This fork was set up by **alice-magic** to add multi-version support
for Minecraft **1.21.8**, **1.21.11**, and **26.1.2** on Fabric.

It uses [Stonecutter](https://stonecutter.kikugie.dev/) (built by the
original author, kikugie) so a single source tree can target multiple
incompatible Minecraft releases.

## Scaffolding status

| Step | State |
|------|-------|
| Stonecutter scaffold (1.21.8 / 1.21.11 / 26.1) | ✅ |
| Merged `kowoui` + `vc-simple` + `vc-plasmo` into single jar | ✅ |
| Per-version dependency catalog (`stonecutter.properties.toml`) | ✅ |
| Yarn for 1.21.x, Mojang Mappings fallback for 26.1+ | ✅ |
| Gradle config / repos / Stonecutter switch tasks | ✅ — all 3 versions configure cleanly |
| **Source-level compile** | ❌ — see below |

## Build status (verified locally on 2026-05-11)

`./gradlew tasks` succeeds for all three targets.

`./gradlew :1.21.8:compileKotlin` currently fails with ~30 compile
errors across **9 files**, all caused by Minecraft 1.21.4 → 1.21.8 API
breaks (not by Stonecutter or our scaffold).

### Files needing 1.21.8 patches

1. `src/main/kotlin/dev/kikugie/kowoui/Builders.kt`
2. `src/main/kotlin/dev/kikugie/kowoui/access/Components.kt`
3. `src/main/kotlin/dev/kikugie/soundboard/audio/download/CobaltAPI.kt`
4. `src/main/kotlin/dev/kikugie/soundboard/gui/component/DurationCutterComponent.kt`
5. `src/main/kotlin/dev/kikugie/soundboard/gui/component/ScrollingButtonComponent.kt`
6. `src/main/kotlin/dev/kikugie/soundboard/gui/component/ScrollingLabelComponent.kt`
7. `src/main/kotlin/dev/kikugie/soundboard/gui/screen/ScreenManager.kt`
8. `src/main/kotlin/dev/kikugie/soundboard/gui/widget/SidebarWidget.kt`
9. `src/main/kotlin/dev/kikugie/soundboard/util/KOwoUi.kt`

### Breaking API changes to address

| Old API (1.21.4) | New API (1.21.8) | Where |
|------------------|------------------|-------|
| `MatrixStack` (`net.minecraft.client.util.math.MatrixStack`) | `Matrix3x2fStack` / `Matrix3x2f` | DrawContext lambdas, owo render hooks |
| `MatrixStack.push()` / `.pop()` | `Matrix3x2fStack.pushMatrix()` / `.popMatrix()` | ScrollingLabelComponent etc. |
| `DrawContext#getGuiTextured(...)` | `DrawContext#drawGuiTexture(...)` | DurationCutterComponent |
| `RenderEffectWrapper` (owo 0.12 — gone in 0.13) | rewrite without wrapper, use `RenderEffect` directly | Builders.kt |
| `ClickEvent(Action, String)` constructor | Sealed subclass per action type | CobaltAPI.kt |
| `RenderSystem.recordRenderCall(...)` | inline / `RenderSystem.assertOnRenderThread` | ScreenManager.kt |

For 1.21.11 add: `Identifier` is still `Identifier` in yarn but mojmap
classes (used by some accessors) renamed.

For 26.1: switch to **Mojang Mappings** entirely — every yarn class
name must be replaced. ~50 files. Use Stonecutter `//? if >=26.1 {` /
`//?} else` comment blocks to swap imports per-version.

## Quoting upstream

From kikugie on issues
[#32](https://github.com/kikugie/voicechat-soundboard/issues/32) and
[#36](https://github.com/kikugie/voicechat-soundboard/issues/36):

> "most of the mod has to be reimplemented" for 1.21.8+
> "the audio decoder wasn't implemented"

In addition to the source-level API breaks above, the **Simple Voice
Chat 2.5 → 2.6 plugin API** rewrite (audio decoder, MergeClientSoundEvent,
channel constructors) and the **Plasmo Voice 2.0 → 2.1.9 client API**
require non-trivial reimplementation in:

- `src/main/kotlin/dev/kikugie/soundboard/entrypoint/SVCEntrypoint.kt`
- `src/main/kotlin/dev/kikugie/soundboard/entrypoint/PlasmoEntrypoint.kt`

## Useful commands

```bash
# Switch the active Stonecutter version locally:
./gradlew "Set active project to 1.21.11"
./gradlew "Set active project to 1.21.8"
./gradlew "Set active project to 26.1"

# Compile only the active version:
./gradlew :1.21.8:compileKotlin

# Build the active version (jar + remap):
./gradlew :1.21.8:build

# Build every Stonecutter version at once:
./gradlew chiseledBuild

# Run the Minecraft client for the active version (after compile passes):
./gradlew :1.21.8:runClient
```

## CI

`.github/workflows/build.yml` runs on Linux (`ubuntu-latest`) with
JDK 21 + JDK 25 installed, executes `./gradlew chiseledBuild`, and
uploads every produced jar as a workflow artifact. CI is **expected to
fail** until the source migration above is complete — keep the workflow
visible so the failure surface is the same as the local one.
