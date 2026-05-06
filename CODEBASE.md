# Multiworld Mod — Codebase Reference

> This document is intended as a guide for agents and contributors to quickly understand the repository's structure, design decisions, and conventions before making changes.

---

## Table of Contents

1. [What This Mod Does](#1-what-this-mod-does)
2. [Technology Stack](#2-technology-stack)
3. [Repository Layout](#3-repository-layout)
4. [Module Breakdown](#4-module-breakdown)
5. [Key Abstractions & Patterns](#5-key-abstractions--patterns)
6. [Lifecycle & Initialization Flow](#6-lifecycle--initialization-flow)
7. [Command System](#7-command-system)
8. [World Creation & Persistence](#8-world-creation--persistence)
9. [Permission System](#9-permission-system)
10. [Portal System](#10-portal-system)
11. [Config & I18n](#11-config--i18n)
12. [Multi-Version Preprocessor](#12-multi-version-preprocessor)
13. [Build System](#13-build-system)
14. [Adding a New Feature — Checklist](#14-adding-a-new-feature--checklist)
15. [Known TODOs & Limitations](#15-known-todos--limitations)

---

## 1. What This Mod Does

Multiworld is a Minecraft server mod (LGPL v3) that allows server administrators to:

- Create additional worlds at runtime with custom dimensions, generators, seeds, and difficulty.
- Teleport players between worlds.
- Set per-world spawns, gamerules, and difficulty.
- Delete worlds (console-only, for safety).
- Create in-game portals that teleport players to other worlds or portal destinations.

The mod supports **Fabric**, **NeoForge**, and **Forge** mod loaders, and targets many Minecraft versions (1.18.2 through 1.21.11+).

---

## 2. Technology Stack

| Technology | Version / Notes |
|---|---|
| **Java** | Source compiled with Java 21 (`jabel` down-targets to Java 17 bytecode) |
| **Gradle** | Multi-project, Groovy DSL root + Kotlin DSL per subproject |
| **Architectury Loom** | `1.13-SNAPSHOT`, cross-platform mod toolchain |
| **Fabric Loader** | Various (per MC version) |
| **NeoForge** | 21.x (multiple minor versions) |
| **Forge** | 1.20.1-47.x (primary), legacy 1.18.2 / 1.19.4 directories present but excluded |
| **Fantasy (NucleoidMC)** | Runtime world creation; used on Fabric via Maven, bundled for NeoForge |
| **Mixin (SpongePowered)** | Bytecode patching of vanilla Minecraft |
| **Brigadier** | Mojang's command framework (argument parsing) |
| **fabric-permissions-api** | LuckPerms compatibility (Fabric) |
| **CyberPerms** | Alternative permission plugin (Fabric) |
| **iCommon** | Cross-version compatibility helpers (Fabric) |
| **SLF4J** | Logging (`LoggerFactory.getLogger("multiworld")`) |
| **jabel** | Allows Java 17 syntax compiled to older bytecode for compatibility |

---

## 3. Repository Layout

```
/
├── Multiworld-Common/               # Shared code for ALL platforms & versions
├── fabric/
│   ├── Multiworld-Fabric-1.18.2/
│   ├── Multiworld-Fabric-1.19.2/
│   ├── Multiworld-Fabric-1.19.4/
│   ├── Multiworld-Fabric-1.20.1/
│   ├── Multiworld-Fabric-1.20.4/
│   ├── Multiworld-Fabric-1.20.6/
│   ├── Multiworld-Fabric-1.21.1/
│   ├── Multiworld-Fabric-1.21.4/
│   ├── Multiworld-Fabric-1.21.8/
│   ├── Multiworld-Fabric-1.21.10/
│   └── Multiworld-Fabric-1.21.11/
├── neoforge/
│   ├── Multiworld-NeoForged-1.21.1/
│   ├── Multiworld-NeoForged-1.21.2/
│   └── Multiworld-NeoForged-1.21.10/
├── Multiworld-Forge-1.18.2/         # Legacy (excluded from active build)
├── Multiworld-Forge-1.19.4/         # Legacy (excluded from active build)
├── Multiworld-Forge-1.20.1/         # Active Forge build
├── Multiworld-Fabric-bundle/        # Merged/combined Fabric artifact project
├── output/                          # Built JARs are copied here
├── build.gradle                     # Root build: shared config + preprocessor task
├── settings.gradle                  # Multi-project includes; version lists
├── gradle.properties                # Version numbers (mod version, MC versions, loader versions)
└── CODEBASE.md                      # This file
```

Active platform/version lists are defined at the top of `settings.gradle`:
```groovy
def versions_fabric   = ['1.18.2', '1.19.2', '1.19.4', '1.20.1', '1.20.4', '1.20.6', '1.21.1', '1.21.4', '1.21.8', '1.21.10', '1.21.11']
def versions_forge    = ['1.20.1']
def versions_neoforge = ['1.21.1', '1.21.2', '1.21.10']
```

---

## 4. Module Breakdown

### `Multiworld-Common/`

Contains **all** game logic shared across platforms and versions. Every platform subproject includes these sources via `srcDir(...)` in Gradle.

```
src/main/java/
├── me/isaiah/multiworld/
│   ├── MultiworldMod.java        # Central class: command dispatch, server lifecycle hooks
│   ├── ICreator.java             # Interface: platform-specific world creation & teleport
│   ├── Utils.java                # World path resolution, save search, config loading
│   ├── I18n.java                 # Internationalisation: message keys loaded from i18n.yml
│   ├── ConsoleCommand.java       # Console-side command handler
│   ├── InfoSuggest.java          # Tab-complete suggestions for the /mw command
│   ├── command/
│   │   ├── Command.java          # Marker interface; getWorldFor() helper
│   │   ├── CreateCommand.java    # /mw create
│   │   ├── DeleteCommand.java    # /mw delete
│   │   ├── TpCommand.java        # /mw tp
│   │   ├── SpawnCommand.java     # /mw spawn
│   │   ├── SetspawnCommand.java  # /mw setspawn
│   │   ├── GameruleCommand.java  # /mw gamerule (excluded on 1.21.11+)
│   │   ├── IGameruleCommand.java # Interface for GameruleCommand
│   │   ├── DifficultyCommand.java# /mw difficulty
│   │   ├── PortalCommand.java    # /mw portal
│   │   └── Util.java             # Constants (dimension IDs), config helpers, platform checks
│   ├── config/
│   │   ├── Configuration.java    # In-memory key-value store (YAML-like)
│   │   └── FileConfiguration.java# Reads/writes flat YAML files on disk
│   ├── fabric/
│   │   ├── FabricEvents.java     # Fabric AttackBlockCallback / UseBlockCallback for wand
│   │   ├── ICommonCheck.java     # Runtime check: is iCommon available?
│   │   ├── ICommonHooks.java     # iCommon hook interface
│   │   ├── CyberHandler.java     # CyberPerms permission handler
│   │   ├── LuckHandler.java      # LuckPerms permission handler
│   │   ├── PermFabric.java       # Fabric-side: selects and registers the right Perm handler
│   │   └── MySaveProperties.java # Custom save-properties for Fantasy worlds
│   ├── perm/
│   │   └── Perm.java             # Static permission API; delegates to INSTANCE (set per platform)
│   └── portal/
│       ├── Portal.java           # Portal entity: bounds, destination, save/load, block building
│       ├── PortalUtil.java       # Geometry helpers (center, safe exit, axis detection)
│       └── WandEventHandler.java # Portal wand: left/right click selection logic
├── multiworld/api/
│   ├── IMultiworldWorld.java     # Interface for Multiworld-managed ServerWorld
│   └── WorldFolderMode.java      # Enum: VANILLA or BUKKIT world folder layout
├── multiworld/mixin/
│   ├── MixinGameruleCommand.java # Hooks into /gamerule for per-world support
│   ├── MixinLevelInfo.java       # Level info hooks
│   ├── MixinLevelStorageSession.java # Storage session hooks (excluded on 1.18/1.19)
│   └── MixinNetherPortalBlock.java   # Intercepts nether portal block use for custom portals
└── dimapi/
    ├── FabricDimensionInternals.java # Excerpt from Fabric Dimensions API (Apache 2.0)
    └── mixin/
        ├── EntityMixin.java
        └── ServerBugfixMixin.java
```

### `fabric/Multiworld-Fabric-{version}/`

Platform entry point and `ICreator` implementation for each supported Fabric/MC version.

```
src/main/java/me/isaiah/multiworld/fabric/
├── MultiworldModFabric.java   # ModInitializer: registers events, commands, lifecycle
├── FabricWorldCreator.java    # ICreator impl: creates worlds via Fantasy, handles teleport
└── MultiworldWorld.java       # Extends ServerWorld; reads level.dat gamerules on load
```

**Key dependency**: `xyz.nucleoid:fantasy` is pulled from Maven for Fabric builds.

### `neoforge/Multiworld-NeoForged-{version}/`

Platform entry point, `ICreator` implementation, and an **embedded copy of Fantasy** for NeoForge.

```
src/main/java/
├── me/isaiah/multiworld/neoforge/
│   ├── MultiworldModNeoForge.java  # NeoForge mod entry point
│   ├── NeoForgeWorldCreator.java   # ICreator impl for NeoForge
│   ├── CyberHandler.java           # NeoForge CyberPerms handler
│   └── PermForge.java              # NeoForge permission handler
└── xyz/nucleoid/fantasy/           # Fantasy library bundled directly for NeoForge
    ├── Fantasy.java
    ├── RuntimeWorld.java
    ├── RuntimeWorldConfig.java
    ├── RuntimeWorldHandle.java
    ├── RuntimeWorldManager.java
    └── mixin/, util/               # Fantasy's own mixins and utilities
```

### `Multiworld-Forge-1.20.1/`

Legacy Forge (Forge API, not NeoForge). Structure mirrors the neoforge subprojects.

---

## 5. Key Abstractions & Patterns

### `ICreator` — Platform Bridge

`ICreator` (`Multiworld-Common/src/main/java/me/isaiah/multiworld/ICreator.java`) is the central interface that decouples all platform-specific operations from the shared logic:

| Method | Purpose |
|---|---|
| `create_world(id, dim, gen, dif, seed)` | Creates and registers a new `ServerWorld` at runtime |
| `teleleport(player, world, x, y, z)` | Teleports a player to another dimension |
| `set_difficulty(id, dif)` | Updates the difficulty of a running world |
| `get_pos(x, y, z)` | Cross-version `BlockPos` construction |
| `get_spawn(world)` | Reads the world's spawn position |
| `get_flat_chunk_gen(mc)` / `get_void_chunk_gen(mc)` | Returns generators for FLAT/VOID worlds |
| `delete_world(id)` | Removes a world from the registry and disk |
| `permissionLevel(source, level)` | Vanilla OP-level check (cross-version) |

Each platform provides one concrete class implementing `ICreator` and registers it via `MultiworldMod.setICreator(...)` during mod initialization.

**Access it anywhere:** `MultiworldMod.get_world_creator()` or `MultiworldMod.versionSupport()` (same object, two aliases).

---

### `Perm` — Permission Abstraction

`Perm` (`perm/Perm.java`) is a static class that delegates all permission checks to `Perm.INSTANCE`, which is set during platform init:

- **Fabric**: `PermFabric` detects LuckPerms / fabric-permissions-api / CyberPerms and sets the appropriate handler.
- **NeoForge/Forge**: `PermForge` / `PermNeoForge` fallback.

If `INSTANCE` is null, all checks fall back to vanilla OP level 1.

Key methods:
- `Perm.check(player, "multiworld.spawn")` — returns true if player has the permission OR is admin (`multiworld.admin`) OR is OP.
- `Perm.has(player, "multiworld.spawn")` — exact permission check without admin bypass.

---

### `Fantasy` — Runtime World Creation

The [Fantasy library by NucleoidMC](https://github.com/NucleoidMC/fantasy) handles the complex task of dynamically creating and registering `ServerWorld` instances at runtime.

- **Fabric**: Depended on via `modImplementation("xyz.nucleoid:fantasy:0.6.x+1.21")`.
- **NeoForge**: Source is bundled directly in `neoforge/Multiworld-NeoForged-*/src/main/java/xyz/nucleoid/fantasy/`.

Worlds are created as **persistent** (survive server restarts):
```java
Fantasy fantasy = Fantasy.get(MultiworldMod.mc);
RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(id, config);
ServerWorld world = worldHandle.asWorld();
```

---

### `Configuration` / `FileConfiguration` — Config System

A lightweight custom YAML-like config system (no external dependency):

- `Configuration` — in-memory `LinkedHashMap<String, Object>` with typed getters.
- `FileConfiguration` — extends `Configuration`; reads/writes a flat `.yml` file.

Config keys use dot notation for sections: `"portals.myPortal.destination"`.  
Use `config.hasSection("portals")` and `config.getSection("portals")` to iterate sub-keys.

---

## 6. Lifecycle & Initialization Flow

### Fabric

```
MultiworldModFabric.onInitialize()
  ├── PermFabric.init()                          # Set up permission handler
  ├── FabricWorldCreator.init()                  # Register ICreator with MultiworldMod
  ├── ServerLifecycleEvents.SERVER_STARTED       # → MultiworldMod.on_server_started(mc)
  ├── CommandRegistrationCallback                # → MultiworldMod.register_commands(dispatcher)
  └── MultiworldMod.init()                       # Load I18n config
```

### `MultiworldMod.on_server_started(mc)`

This is where previously-created worlds are reloaded:

1. Scans `config/multiworld/worlds/` for legacy YAML configs → calls `CreateCommand.reinit_world_from_config()`.
2. Scans the Vanilla dimension storage path (`world/dimensions/**`) for `multiworld-world.yml` files → calls `Utils.loadSavedMultiworldWorld()`.
3. Loads portals from `config/multiworld/portals.yml` → `Portal.reinit_portals_from_config()`.

---

## 7. Command System

All commands are registered under `/mw` (alias `CMD = "mw"`).  
The entire command tree is dispatched through a single `greedyString` argument parsed manually in `MultiworldMod.broadcast()`.

| Subcommand | Class | Permission |
|---|---|---|
| `help` | inline in `broadcast()` | `multiworld.cmd` |
| `list` | inline in `broadcast()` | `multiworld.cmd` |
| `version` | inline in `broadcast()` | `multiworld.cmd` |
| `tp <world>` | `TpCommand` | `multiworld.tp` |
| `spawn` | `SpawnCommand` | `multiworld.spawn` |
| `setspawn` | `SetspawnCommand` | `multiworld.setspawn` |
| `gamerule <rule> <value>` | `GameruleCommand` | `multiworld.gamerule` |
| `difficulty <value> [world]` | `DifficultyCommand` | `multiworld.difficulty` |
| `create <id> <env> [opts]` | `CreateCommand` | `multiworld.create` |
| `delete <id>` | `DeleteCommand` | `multiworld.admin` (console only) |
| `portal <sub> [args]` | `PortalCommand` | `multiworld.portal` |
| `debugtick` | inline | (any op) |

**Console** commands are handled separately by `ConsoleCommand.broadcast_console()`.

**`/mw create` options:**
- `<id>` — World ID; if no `:` namespace, `multiworld:` is prepended automatically.
- `<env>` — Environment: `NORMAL`, `NETHER`, `END`, `FLAT`, `VOID`, or a registered custom generator name.
- `-g=<gen>` — Override chunk generator.
- `-s=<seed>` — Seed (number, string hash, or `RANDOM`).
- `-m=<mode>` — `WorldFolderMode`: `VANILLA` (default) or `BUKKIT`.

---

## 8. World Creation & Persistence

### Creating a World

```
/mw create myWorld NORMAL -s=1234
  → CreateCommand.run()
    → MultiworldMod.createConfigAndWorld()
      → CreateCommand.makeConfigFile()   # writes multiworld-world.yml into world folder
      → ICreator.create_world()          # Fantasy: getOrOpenPersistentWorld()
```

### World Folder Layout (VANILLA mode, default)

```
<server-root>/world/dimensions/<namespace>/<worldname>/
  ├── multiworld-world.yml    # Multiworld metadata (environment, seed, gamerules, etc.)
  ├── level.dat               # Minecraft level data (managed by Fantasy/vanilla)
  ├── region/
  └── ...
```

`Utils.WORLD_YML_NAME = "multiworld-world.yml"` is the marker file used to identify a Multiworld-managed world during startup scans.

### `multiworld-world.yml` Fields

| Key | Type | Description |
|---|---|---|
| `namespace` | String | World identifier namespace (e.g. `multiworld`) |
| `path` | String | World identifier path (e.g. `myworld`) |
| `environment` | String | `NORMAL`, `NETHER`, `END`, `FLAT`, `VOID` |
| `seed` | long | World generation seed |
| `custom_generator` | String | (optional) custom generator name |
| `difficulty` | String | `EASY`, `NORMAL`, `HARD`, `PEACEFUL` |
| `worldFolderSaveMode` | String | `VANILLA` or `BUKKIT` |
| `isMultiworldWorld` | boolean | Always `true`; marks world as Multiworld-managed |
| `gamerule_<name>` | String/Boolean/Integer | Per-world gamerule overrides |

### Reloading on Server Start

`Utils.searchForWorlds()` recursively walks `world/dimensions/` looking for directories containing `multiworld-world.yml`. Each found path is passed to `Utils.loadSavedMultiworldWorld()`, which reads the YAML and recreates the world via `ICreator.create_world()`.

---

## 9. Permission System

Permission nodes follow the pattern `multiworld.<action>`:

| Node | Grants |
|---|---|
| `multiworld.admin` | All commands |
| `multiworld.cmd` | `/mw`, `/mw list`, `/mw version` |
| `multiworld.tp` | `/mw tp` |
| `multiworld.spawn` | `/mw spawn` |
| `multiworld.setspawn` | `/mw setspawn` |
| `multiworld.create` | `/mw create` |
| `multiworld.gamerule` | `/mw gamerule` |
| `multiworld.difficulty` | `/mw difficulty` |
| `multiworld.portal` | `/mw portal` |

Players with OP level ≥ 1, or the `multiworld.admin` node, bypass all individual permission checks.

---

## 10. Portal System

Portals are rectangular regions of `NETHER_PORTAL` blocks surrounded by `OBSIDIAN` frames, created with the **Portal Wand** item.

### Creating a Portal

1. Player runs `/mw portal wand` → receives the wand item.
2. Left-click block → sets first corner (`WandEventHandler`).
3. Right-click block → sets second corner.
4. Player runs `/mw portal create <name> <destination>` → `PortalCommand` calls `Portal.buildPortalArea()`.

### Destination Format (Multiverse-compatible)

| Format | Meaning |
|---|---|
| `myWorld` | Teleport to spawn of world `multiworld:myWorld` |
| `p:otherPortal` | Teleport through another named portal |
| `e:myWorld:x,y,z` | Teleport to exact coordinates in world |
| `w:myWorld:x,y,z` | (same as `e:`) |

### Persistence

Portals are saved to `config/multiworld/portals.yml` using `Portal.save()`.  
On server start, `Portal.reinit_portals_from_config()` reloads them and rebuilds the portal blocks in the world.

### Key Classes

| Class | Role |
|---|---|
| `Portal` | Data + logic for a single portal |
| `PortalUtil` | Geometry: min/max pos, center, safe exit finding |
| `WandEventHandler` | Player interaction events for the wand |
| `PortalCommand` | `/mw portal` subcommands; maintains `knownPortals` map |
| `MixinNetherPortalBlock` | Intercepts portal block interactions to trigger teleportation |

---

## 11. Config & I18n

### Config Files (runtime location: `config/multiworld/`)

| File | Purpose |
|---|---|
| `i18n.yml` | User-customisable message strings |
| `portals.yml` | All saved portals |
| `worlds/<ns>/<path>.yml` | **Legacy** per-world config (superseded by `multiworld-world.yml` inside world folder) |

### I18n

`I18n.java` reads `config/multiworld/i18n.yml` on startup and exposes static string fields:
- `I18n.TELEPORTING`
- `I18n.USAGE_CREATE`
- `I18n.CREATED_WORLD`
- `I18n.CMD_PORTAL_USAGE`
- etc.

If the file is missing, defaults are written on first run.

### Color Codes

`MultiworldMod.message(player, "&aGreen &4Red &r")` — uses Bukkit-style `&` color codes, translated to the Minecraft `§` format by `translate_alternate_color_codes()`.

---

## 12. Multi-Version Preprocessor

The root `build.gradle` registers a custom Gradle `preprocess` task that acts as a simple `#if/#elif/#else/#endif` preprocessor over Java source comments. This allows one source tree in `Multiworld-Common/` to target multiple Minecraft API versions.

### Syntax

```java
// #if mc218
// return (ServerWorld) plr.getWorld();       // ← uncommented for 1.21.8+
// #else
return (ServerWorld) plr.getEntityWorld();    // ← used for all other versions
// #endif
```

### Version Tags (set per subproject)

Each subproject declares its `targetVersion` in Gradle:
```kotlin
extensions.extraProperties["targetVersion"] = "mc211"  // for 1.21.1
```

Common tags:

| Tag | Minecraft Version |
|---|---|
| `mc182` | 1.18.2 |
| `mc1201` | 1.20.1 |
| `mc211` | 1.21.1 |
| `mc218` | 1.21.8+ |

### File Exclusions

Per-subproject, certain files can be excluded from preprocessing using:
```kotlin
extensions.extraProperties["excludedFiles"] = listOf("some/path/File.java")
```

For example, `GameruleCommand.java` is excluded on 1.21.11 builds.

---

## 13. Build System

### Building All Versions

```bash
./gradlew build
```

Built JARs are automatically copied to the `output/` directory by a `copyReport2` task in each subproject's `build.gradle.kts`.

### Building a Specific Version

```bash
./gradlew :Multiworld-Fabric-1.21.1:build
./gradlew :Multiworld-NeoForged-1.21.1:build
./gradlew :Multiworld-Forge-1.20.1:build
```

### Key Gradle Properties (`gradle.properties`)

| Property | Purpose |
|---|---|
| `mod_version` | The mod version applied to all artifacts |
| `forge_version`, `forge_version_1_20_1`, etc. | Forge loader versions |
| `neoforged_version_1_21_1`, etc. | NeoForge loader versions |

### Java Version Target

All subprojects use **Java 21** compiler with **jabel** (`com.pkware.jabel:jabel-javac-plugin`) to target **Java 17** bytecode, ensuring compatibility with older JVM installations while allowing modern language features during development.

---

## 14. Adding a New Feature — Checklist

### Adding a new `/mw` subcommand

1. Create `MyCommand.java` in `Multiworld-Common/src/main/java/me/isaiah/multiworld/command/`.
2. Add a permission node constant to `MultiworldMod.perms_list`.
3. Add the subcommand dispatch in `MultiworldMod.broadcast()` (follow the existing `if (args[0].equalsIgnoreCase(...))` pattern).
4. If the command needs console support, also handle it in `ConsoleCommand.broadcast_console()`.
5. Add a help line to `MultiworldMod.COMMAND_HELP`.
6. Add a message key to `I18n.java` if user-visible strings are needed.
7. Document the permission node in `README.md`.

### Adding a new platform/MC version (Fabric example)

1. Add the version string to `versions_fabric` in `settings.gradle`.
2. Copy an existing `fabric/Multiworld-Fabric-{close-version}/` directory to the new name.
3. Update `build.gradle.kts`: set `targetVersion`, update `minecraft(...)`, `mappings(...)`, `fabric-loader`, `fantasy`, and Fabric API version strings.
4. Adjust any `// #if mcXXX` blocks in Common source as needed for the new API changes.
5. Test that `./gradlew :Multiworld-Fabric-{new-version}:build` succeeds.

### Adding a new world generator type

1. Register it at startup with `CreateCommand.registerCustomGenerator(Identifier, ChunkGenerator)`.
2. It will automatically be usable via `/mw create myWorld <your-generator-id>`.

---

## 15. Known TODOs & Limitations

- **`/mw delete` is console-only** — by design, for safety.
- **Custom generator support** is scaffolded (`CreateCommand.registerCustomGenerator`) but not fully exposed via a public API.
- **`WorldFolderMode.BUKKIT`** path handling exists as an enum but most resolution code currently returns `VANILLA` unconditionally (several code paths are commented out).
- **`WandEventHandler.register()`** is called but the event registration is commented out in `MultiworldMod.init()` and `on_server_started()` — portal wand functionality may be partially disabled.
- **Portal delete command** is not yet implemented.
- **GameruleCommand** is excluded on MC 1.21.11 builds.
- The **NeoForge Fantasy bundle** is a manual vendor copy; it should be kept in sync with upstream [NucleoidMC/fantasy](https://github.com/NucleoidMC/fantasy) when upgrading NeoForge versions.
- **iCommon** is a changing dependency fetched from a personal Maven repo (`https://repo.codemc.io/repository/maven-releases/`); it provides cross-version helpers and is resolved with `isChanging = true`.
