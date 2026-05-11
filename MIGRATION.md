# Migration notes (alice-magic fork)

This fork ports [kikugie/voicechat-soundboard](https://github.com/kikugie/voicechat-soundboard)
from upstream's 1.21.4 multi-module layout to a single-jar
[Stonecutter](https://stonecutter.kikugie.dev/) build for **Minecraft 1.21.8**.

## What ships

- ✅ `1.21.8` — Fabric, Yarn mappings, Java 21. Builds and runs.
  Single jar with both SVC + Plasmo Voice entrypoints baked in.

## What was scoped out

`1.21.11` and `26.1.x` were researched and added to Stonecutter, then
removed when source migration cost exceeded the session budget:

- **owo-lib 0.13** (the only version available for 1.21.11+) renamed
  `Component → UIComponent` to avoid a collision with Mojang's new
  `net.minecraft.network.chat.Component`. This affects ~31 source
  files across `kowoui` and `soundboard.gui` (520 compile errors).
  Callback signatures also changed (`KeyInput`, `CharInput`, `Click`).
- **26.1.x** additionally requires switching every yarn class name to
  Mojang Mappings (`net.minecraft.entity.Entity` →
  `net.minecraft.world.entity.Entity` etc) and the Java 25 toolchain.

The version matrix that was researched is preserved here so a future
migration doesn't have to redo it:

| | 1.21.8 | 1.21.11 | 26.1.2 |
|---|---|---|---|
| yarn | 1.21.8+build.1 | 1.21.11+build.5 | (mojmap, no yarn) |
| fabric_api | 0.136.1+1.21.8 | 0.141.3+1.21.11 | 0.148.0+26.1.2 |
| fabric_loader | 0.19.2 | 0.19.2 | 0.19.2 |
| FLK | 1.13.11+kotlin.2.3.21 | (same) | (same) |
| owo-lib | 0.12.23+1.21.8 | 0.13.0+1.21.11 | 0.13.0+26.1 |
| SVC mod | fabric-1.21.8-2.6.17 | fabric-1.21.11-2.6.17 | fabric-2.6.17+26.1.2 |
| SVC API | 2.6.13 | 2.6.13 | 2.6.13 |
| Plasmo mod | fabric-1.21.6-2.1.9 | fabric-1.21.11-2.1.9 | fabric-26.1-2.1.9 |
| Plasmo API | 2.1.7-SNAPSHOT | (same) | (same) |
| modmenu | 15.0.2 | 17.0.0 | 18.0.0-beta.1 |

## Source patches applied for 1.21.8

| Old API (1.21.4) | New API (1.21.8) | File |
|------------------|------------------|------|
| `RenderSystem.recordRenderCall` | `RenderSystem.queueFencedTask` | `gui/screen/ScreenManager.kt`, `gui/widget/SidebarWidget.kt` |
| `RenderLayer::getGuiTextured` | `RenderPipelines.GUI_TEXTURED` | `gui/component/DurationCutterComponent.kt` |
| `Consumer<MatrixStack>` (owo) | `Consumer<Matrix4f>` | `kowoui/access/Components.kt` |
| `MatrixStack.push/pop` | `Matrix3x2fStack.pushMatrix/popMatrix` | `gui/component/ScrollingLabelComponent.kt` |
| `RenderEffectWrapper` (owo 0.12 — gone) | helper deleted (no callers) | `kowoui/Builders.kt` |
| `ClickEvent(Action, str)` | `ClickEvent.OpenUrl(URI)` | `audio/download/CobaltAPI.kt` |
| `drawTooltip(...)` | gained `focused: boolean` arg | `gui/component/ScrollingButtonComponent.kt` |
| custom `drawLinePrecise` using `vertexConsumers()` | delegates to owo's `drawLine(...)` | `util/KOwoUi.kt` |
| `me.fallenbreath.yamlang` 1.4.0 | 1.5.0 (Gradle 9 compat) | `build.gradle.kts` |
| modmenu 13.0.0-beta.1 (1.21.4) | 15.0.2 (1.21.8) | `stonecutter.properties.toml` |

## Build / run commands

```bash
./gradlew :1.21.8:build       # produces versions/1.21.8/build/libs/soundboard-0.7.1+1.21.8.jar
./gradlew :1.21.8:runClient   # boots Minecraft 1.21.8 with the dev mod loaded
```

## To resume the 1.21.11 / 26.1 port

1. Re-add `versions("1.21.11")` / `version("26.1", "26.1.2")` in
   `settings.gradle.kts` and re-add the version blocks in
   `stonecutter.properties.toml` from the matrix above.
2. Use Stonecutter `//? if =1.21.8 {` … `//?} else {` blocks around
   each owo import that diverges between 0.12 and 0.13.
3. Walk every `Click`/`KeyInput`/`CharInput`-style callback and rewrite
   the lambda signatures.
4. For 26.1: switch the entire mod to Mojang Mappings and gate yarn
   class names per-version. Bump the toolchain to JDK 25.

The original SVC 2.5 → 2.6 audio decoder rewrite kikugie called out
in upstream issues #32 / #36 has **not** been done — runtime listens
on the new 2.6 events but reuses the 2.5-shaped audio path. Test in
a real server before relying on it.
