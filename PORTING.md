# Porting BuildCraft 8.0.x (MC 1.12.2) → NeoForge 1.21.1

Target: **Minecraft 1.21.1, NeoForge 21.1.x, Java 21, ModDevGradle 2, Parchment mappings**.
Branch: `1.21.1-neoforge` (cut from `8.0.x-1.12.2` @ `7c86626`).

## How the port is laid out

The 1.12.2 tree is kept in place as a reference. It is **not compiled**. Code is ported file by file by
`git mv`-ing it into the new tree and then fixing it. Because the files are moved rather than copied,
`git log --follow` / blame keep working back to the 1.12 history.

| Path | Status |
|---|---|
| `src/main/java` | **compiled**: ported mod code |
| `src/api/java` | **compiled**: ported API (copied from the `BuildCraftAPI` submodule @ `b12940d`, since we can't change upstream) |
| `src/main/resources`, `src/generated/resources` | **compiled**: ported assets/data, and datagen output |
| `src/main/templates` | `neoforge.mods.toml` template (filled from `gradle.properties`) |
| `src/test/java` | **compiled**: ported tests |
| `sub_projects/expression` | **compiled into main**. MC-independent, also builds and tests standalone |
| `common/` | reference: 1.12.2 code still to port |
| `common_test/` | reference: 1.12.2 tests still to port |
| `BuildCraftAPI/` (submodule) | reference: 1.12.2 API |
| `buildcraft_resources/` | reference: 1.12.2 assets |
| `src_old_license/` | reference: BC 7.1 (1.7.10) code, **MMPL-licensed**, never compiled in 8.0 either |

Rules of thumb:
- A file that is in `src/` must compile. Anything that can't compile yet gets stubbed or commented with
  `// TODO(port): ...` (`grep -rn "TODO(port)" src` lists the outstanding hooks).
- When a file moves, its tests move from `common_test/` to `src/test/java` with it.
- Old Gradle/CI files (`Jenkinsfile`, `.travis.yml`, `license_checker/`, `testsuite/`, `test.py`, `misc/`) are
  stale and can be deleted or replaced as needed. The 1.12 `build.properties` has already been folded into
  `gradle.properties`.

## Mod structure

As in 1.12, each module is its own mod id (`buildcraftlib`, `buildcraftcore`, `buildcrafttransport`, …), but
they all ship in **one jar**: `neoforge.mods.toml` declares several `[[mods]]`. This keeps the original asset
namespaces and `BCModules.isLoaded()` checks working. When you port a module, add its `[[mods]]` block, its
`@Mod` class, and its id in the `data` run in `build.gradle`.

## Building

```
./gradlew build          # mod jar + api jar
./gradlew runClient      # dev client
./gradlew runData        # datagen into src/generated/resources
./gradlew :sub_projects:expression:test
```

## Status

- [x] Gradle: ModDevGradle 2.0.141, Gradle 8.14.3 wrapper, Java 21 toolchain
- [x] `expression` library: modern Gradle build, code generator runs, compiles on Java 21, 13/13 tests pass
- [x] `BCLib` / `BCCore`: NeoForge `@Mod` entry points, with every 1.12 lifecycle call left as a `TODO(port)`
- [x] API: `BCModules` (ModList / ResourceLocation 1.21), `BCLog`, `IBuildCraftMod`
- [x] 42 MC-independent `lib` classes moved (gui/pos, misc/collect, misc/data, script, …); `TypedMapTester` passes
- [ ] everything else (see order below)

> The NeoForge-facing code (`BCLib`, `BCCore`, `BCModules`) was written without access to the NeoForge maven.
> It has been type-checked against stubs only. Run `./gradlew build` first thing.

## Port order

Sizes are 1.12 line counts in `common/buildcraft/*`. Each step depends on the ones before it.

1. **lib** (55k), in this order:
   1. `misc` helpers → the 1.21 types (`Direction`, `CompoundTag`, `BlockState`, `Vec3`, `AABB`)
   2. `registry`: `RegistrationHelper` / `TagManager` → `DeferredRegister` + `DeferredHolder`
   3. `net`: `MessageManager`, `PacketBufferBC` → `CustomPacketPayload` + `StreamCodec`, `RegisterPayloadHandlersEvent`
   4. `tile`: `TileBC_Neptune` → `BlockEntity` base. Covers ticking, item/fluid/power capabilities, and sync (`getUpdateTag`/`ClientboundBlockEntityDataPacket`)
   5. `block` / `item` bases
   6. `gui`: `ContainerBC_Neptune` → `AbstractContainerMenu`, `GuiBC8` → `AbstractContainerScreen` with `GuiGraphics`
   7. `client`: models / render. This is the big one: ~50 files of `BufferBuilder`/GL11, TESRs, `IBakedModel`
   8. `client/guide`: the guide book. Markdown is loaded from `BuildCraftGuide` (submodule, not yet checked out)
   9. `misc/ExpressionCompat`, `script`: reloadable registries → datapack reload listeners
2. **api** (12k): port packages as lib/modules need them. `mj` (MJ power) is needed early
3. **core** (8.7k): wrench, markers, engines (redstone), paintbrush, list, gears, volume boxes, config
4. **energy** (5k): engines, oil/fuel fluids (`FluidType` + `BaseFlowingFluid`), oil worldgen (→ configured/placed features + biome modifiers, datapack-driven)
5. **transport** (17.6k): pipes (block entity + `PipeEventBus`), pluggables, wires, gates, facades
6. **factory** (6.8k): pumps, tanks, mining well, auto workbench, distiller, heat exchanger
7. **silicon** (8.8k): laser, assembly/integration tables, gates, chipsets
8. **builders** (14.3k): quarry (chunk loading → `TicketController`), filler, builder, architect, blueprints/schematics
9. **robotics** (2.2k in 8.0, mostly zone planner). Whether to port BC7 robots from `src_old_license` is undecided: that code is MMPL-licensed, not MPL

## 1.12.2 → 1.21.1 cheat sheet

| 1.12.2 (Forge 14.23) | 1.21.1 (NeoForge 21.1) |
|---|---|
| `@Mod(modid=…)` + `FMLPreInit/Init/PostInit` | `@Mod(id)` constructor `(IEventBus, ModContainer)`, `FMLCommonSetupEvent`, `FMLLoadCompleteEvent` |
| `@SidedProxy`, `@SideOnly` | `@Mod(value=id, dist=Dist.CLIENT)` class / `@EventBusSubscriber(value=Dist.CLIENT)`; no `@SideOnly` |
| `RegistryEvent.Register`, `GameRegistry` | `DeferredRegister.Blocks/Items/create(Registries.X, id)` |
| item metadata / subtypes | flattening: one item per variant, or data components |
| item `NBTTagCompound` | **data components** (`DataComponentType`), with NBT kept only in block entities |
| `NBTTagCompound`, `NBTTagList` | `CompoundTag`, `ListTag` (`saveAdditional/loadAdditional(tag, HolderLookup.Provider)`) |
| `IBlockState`, `IProperty` | `BlockState`, `Property` (`createBlockStateDefinition`) |
| `EnumFacing` | `Direction` |
| `TileEntity`, `ITickable` | `BlockEntity` + `BlockEntityType`, `EntityBlock#getTicker` |
| `Capability` / `@CapabilityInject` / `getCapability(cap, side)` | `BlockCapability` / `ItemCapability`, `RegisterCapabilitiesEvent`, `level.getCapability(...)`, `BlockCapabilityCache` |
| `IItemHandler`, `IFluidHandler`, `IEnergyStorage` | same names under `net.neoforged.neoforge.*` (`Capabilities.ItemHandler.BLOCK`, …) |
| `FluidStack` amount in mB | still mB; `FluidStack` is component-aware; fluids need a `FluidType` |
| `SimpleNetworkWrapper` / `IMessage` | `CustomPacketPayload` + `StreamCodec` + `RegisterPayloadHandlersEvent` |
| `IGuiHandler`, `GuiContainer` | `MenuType` (`IMenuTypeExtension.create`), `AbstractContainerScreen`, `RegisterMenuScreensEvent` |
| `GlStateManager`, `Tessellator`/`BufferBuilder` | `PoseStack`, `MultiBufferSource`/`VertexConsumer`, `RenderType`, `RenderSystem` |
| `TileEntitySpecialRenderer` / `FastTESR` | `BlockEntityRenderer` + `EntityRenderersEvent.RegisterRenderers` |
| `IBakedModel`, `ICustomModelLoader`, `ModelBakeEvent` | `BakedModel`, `IGeometryLoader` + `ModelEvent.RegisterGeometryLoaders`, `ModelEvent.ModifyBakingResult` |
| `OreDictionary` | item/block **tags** (`c:` common tags) |
| JSON recipes in `assets/` + `IRecipe` | `data/<ns>/recipe/` (1.21 format), `Recipe<RecipeInput>` + `RecipeSerializer` with `MapCodec` |
| `ForgeChunkManager` | `TicketController` + `RegisterTicketControllersEvent` |
| `Configuration` | `ModConfigSpec`, `container.registerConfig(...)` |
| `FMLInterModComms` | `InterModEnqueueEvent` / `InterModProcessEvent` |
| `CommandBase` | Brigadier, `RegisterCommandsEvent` |
| `.lang` files | `assets/<ns>/lang/en_us.json` |
| blockstates with `forge_marker` (36 files) | vanilla `variants`/`multipart`, or datagen via `BlockStateProvider` |
| `textures/blocks`, `textures/items` | `textures/block`, `textures/item` (or keep paths and point models at them) |

## Decisions still open

- **Power.** BC8 is MJ-only, and RF/FE came from a separate adapter mod. Do we keep MJ as the internal API and
  also expose the NeoForge `IEnergyStorage` capability, or MJ only? (`TileEngineRF`, `TileDynamoMJ` and
  `PipeFlowPower` already touch `IEnergyStorage`.)
- **Robots.** Port only BC8's robotics module, or also bring the BC7 robots in from the MMPL code?
- **World compatibility with 1.12 saves.** Probably not worth it; block/item ids change with flattening anyway.
- **Submodules.** `BuildCraft-Localization` and `BuildCraftGuide` point at upstream BuildCraft repos. Vendor
  them or fork them once the lang conversion (`.lang` → `.json`) and guide loader port start.
