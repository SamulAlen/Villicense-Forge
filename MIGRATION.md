# Villicense-Forge — 1.20.1 Forge 版实现说明

本文档记录 Villicense（村民证件）模组的 **Minecraft 1.20.1 / Forge** 版本实现要点与踩坑。
本版复用 1.20.1 原版的可复用资产（贴图 / 模型 / 语言文件），并参照同系列的 NeoForged(1.21.1)
与 Fabric(1.21.1) 版本的功能需求，用 Forge API 独立实现。

---

## 1. 构建系统

| 项 | 值 | 说明 |
| --- | --- | --- |
| 加载器 | Forge（MinecraftForge） | 支持 **`[47.4.10,)`**（即 47.4.10 及以上） |
| Minecraft | 1.20.1 | |
| Forge（编译目标） | 47.4.10 | `forge_version=47.4.10` |
| 映射 | official 1.20.1 | Mojang 官方映射 |
| Gradle 插件 | `net.minecraftforge.gradle` 6.x + `org.spongepowered.mixin` (mixingradle 0.7-SNAPSHOT) | |
| JDK | **17** | 1.20.1 需要 Java 17（不是 21） |

关键文件：
- `build.gradle`：`apply plugin: 'net.minecraftforge.gradle'` + `apply plugin: 'org.spongepowered.mixin'`；
  `mixin { add sourceSets.main, 'villicense.refmap.json'; config 'villicense.mixins.json' }`；
  runs 里加 `-mixin.config=villicense.mixins.json` 和 `mixin.env.remapRefMap`。
- `settings.gradle`：pluginManagement 里加了 `maven.minecraftforge.net` 与 sponge 仓库。

> **注意**：构建前需把 `JAVA_HOME` 切到 JDK 17，否则 toolchain 会因找不到 17 而失败或用到错误的 JDK。

---

## 2. Mixin（与 NeoForge 1.21 的差异，关键）

NeoForge 1.21 在 `neoforge.mods.toml` 里写 `[[mixins]] config="xxx.mixins.json"` 即可自动加载；
**Forge 1.20.1 不识别该段**，改用传统 MixinGradle 方式：

1. `buildscript` 里加 `classpath 'org.spongepowered:mixingradle:0.7-SNAPSHOT'`；
2. `apply plugin: 'org.spongepowered.mixin'`；
3. `mixin { ... }` 块生成 refmap；
4. 各 run 配置加 `-mixin.config=<modid>.mixins.json`。

`villicense.mixins.json` 里 `compatibilityLevel` 用 **`JAVA_17`**，并声明 `refmap`。

Mixin 注入点：`Villager#mobInteract(Player, InteractionHand)`，HEAD 处，当玩家主手持证件时
`cir.setReturnValue(InteractionResult.PASS)`。

---

## 3. 为什么需要 Mixin（交互顺序，1.20.1 已确认）

在 1.20.1 的 `Player.interact` 中，交互顺序是：
1. 先触发 Forge 的 `PlayerInteractEvent.EntityInteract`；
2. 然后调用 `entity.mobInteract(...)`（对村民就是**打开交易界面**）；
3. **仅当 `mobInteract` 返回 `PASS`**，才会走到物品的 `ItemStack.interactLivingEntity(...)`。

因此若不拦截，手持证件右键村民会直接开交易界面，物品逻辑根本不会执行。所以必须用 Mixin 让
`mobInteract` 在持证件时返回 `PASS`，物品的 `interactLivingEntity` 才能接管。

（参考 MinecraftForge 1.20.1 `Player.java` 的 Forge patch。）

---

## 4. 物品创造标签页

Forge 1.20.1 的事件类在 **`net.minecraftforge.event.BuildCreativeModeTabContentsEvent`**
（不是 1.21 NeoForge 那个同名包在 neoforge.event，也不是 `CreativeModeTabEvent`）。
它是 `IModBusEvent`，注册在 **mod 总线**（构造器注入的 `IEventBus`，用 `addListener`）。
判断用 `event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES`（`getTab()` 返回的是具体 tab 对象）。

---

## 5. 注册与数据存储

- 注册：`DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID)` + `RegistryObject`。
- 数据存储：1.20.1 用物品 **NBT**（Data Components 到 1.20.5 才有），字段：
  - `pos`（`long`，`BlockPos.asLong()`）
  - `dim`（`String`，维度 `ResourceLocation`）
  - `block`（`String`，方块注册名，用于 tooltip 显示）
  - `entity_id`（`int`）
  - `confirm`（`boolean`）
- `appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)` —— 1.20.1 仍有 `Level` 参数
  （与 1.21.1 的 `Item.TooltipContext` 不同）。

---

## 6. Villager / POI API（1.20.1）

- 注册表：用 `net.minecraft.core.registries.BuiltInRegistries` 的
  `BLOCK` / `VILLAGER_PROFESSION` / `POINT_OF_INTEREST_TYPE`。
  - 取方块：`BuiltInRegistries.BLOCK.get(ResourceLocation)`（`DefaultedRegistry` 没有 `getValue`）。
- `PoiTypes.forState(BlockState)` → `Optional<Holder<PoiType>>`。
- `VillagerProfession#heldJobSite()` → `Predicate<Holder<PoiType>>`。
- 遍历所有有效工作站点 POI：`BuiltInRegistries.POINT_OF_INTEREST_TYPE.holders()` + `unwrapKey()`。
- `setVillagerData` / `setProfession` / `refreshBrain(ServerLevel)` / `Brain#setMemory` 与 1.21.1 基本一致。

---

## 7. 配方

- 目录：**`data/villicense/recipes/`**（**复数 `recipes/`**，1.21.1 才改成单数 `recipe/`）。
- 格式：1.20.1 的 `result` 用 **`"item"`**：
  ```json
  "result": { "item": "villicense:villager_license", "count": 1 }
  ```
  （`"item"`→`"id"` 是 1.20.5+/1.21 引入 Data Components 之后才变的，1.20.1 仍是 `item`。）
- 写了 16 个「纸 + 每种染料」的明确无序配方（不用 `c:dyes` 标签，规避环境差异）。
- `pack.mcmeta`：`pack_format` 为 **15**（1.20.1）。

---

## 8. 验证情况

- `gradlew build` **BUILD SUCCESSFUL**：编译、Mixin 注解处理（生成 `villicense.refmap.json`）、
  资源处理、reobf 打包全部通过。
- 产物 `build/libs/villicense-1.0.0.jar` 结构已验证：含全部 class、`villicense.mixins.json`、
  `villicense.refmap.json`、16 个 `data/villicense/recipes/*.json`、assets 与 `pack.mcmeta`。
- **运行时（进游戏）**：本沙箱因下载客户端 asset 走代理超时而无法完整启动客户端，请在本机
  JDK 17 + Forge ≥47.4.10 环境里用 `runClient` 或实际 mods 目录实测合成与交互。
