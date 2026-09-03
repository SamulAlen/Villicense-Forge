# Villicense（村民证件）— Forge 1.20.1

为 **Minecraft 1.20.1 / Forge**（支持 **Forge 47.4.10 及以上**）开发的原生 Forge 模组。手持「村民证件」可以对村民进行绑定、查看与重新安排工作/床等操作。

> 想跑 1.21.1？同系列的其它版本：
> - [Villicense-NeoForged](https://github.com/SamulAlen/Villicense-NeoForged)（原生 NeoForge 1.21.1）
> - [Villicense-Fabric](https://github.com/SamulAlen/Villicense-Fabric)（Fabric 1.21.1，经 Sinytra Connector）

## 功能

- **合成**：**纸 + 任意一种染料**（无序，16 种颜色都支持），得到「村民证件」`villicense:villager_license`。
- **潜行 + 右键村民**：若证件上没记方块 → 选定该村民；若已记方块 → 把该方块绑定到当前村民。
- **潜行 + 右键方块**（床 / 有效工作站点）：若证件上没记村民 → 选定该方块；若已记村民 → 把村民绑定到该方块。
- **绑定效果**：床 → 设置村民的床；工作站点 → 把村民职业改成匹配该工作方块的职业，并绑定工作站点。
- **右键村民**（不潜行）：查看标题、UUID、职业、工作站点与床位置（坐标可点击 `/tp`）。
- **右键任意方块两次**（不潜行）：擦除证件记录。
- 全部交互仅主手生效；副手忽略。

## 环境准备

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | **17**（必须，1.20.1 要求） | 建议用 Temurin / Corretto / Oracle JDK 17 |
| Gradle | 8.x（wrapper 自带） | 无需手动安装，用项目里的 `gradlew` |

### 安装 JDK 17

- **Windows**：从 [Adoptium Temurin 17](https://adoptium.net/temurin/releases/?version=17) 下载安装，或 `winget install EclipseAdoptium.Temurin.17.JDK`。
- **macOS**：`brew install --cask temurin@17`。
- **Linux**：`sudo apt install openjdk-17-jdk`。

> 若 `JAVA_HOME` 指向的是 JDK 21，构建前请切到 17，例如 Windows：
> ```bat
> set JAVA_HOME=D:\Program Files\Java\jdk-17.0.16
> set PATH=%JAVA_HOME%\bin;%PATH%
> ```

安装后确认：

```bash
java -version   # 应输出 java version "17.x"
```

## 编译

在 `Villicense-Forge` 目录下：

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

产物：`build/libs/villicense-<版本>.jar`。

> 首次构建会联网下载 ForgeGradle、Mixingradle、Forge 47.4.10 与 Minecraft 1.20.1，需保持网络畅通。国内网络可参考 `settings.gradle` 里的阿里云 / Lss233 镜像。

## 使用

1. 把 `build/libs/villicense-*.jar` 放进 `.minecraft/mods/`（或整合包的 `mods/`）。
2. 启动游戏（Forge 1.20.1，版本 ≥ 47.4.10）。
3. `/give @s villicense:villager_license` 或从创造模式「工具与实用物品」标签页拿取。
4. 按上文「功能」操作即可。

## 技术说明

- **Mixin**：Forge 1.20.1 用 **MixinGradle（`org.spongepowered.mixin`）** 加载 Mixin（不同于 NeoForge 1.21 在 `mods.toml` 里用 `[[mixins]]`）。Mixin 注入 `Villager#mobInteract`，当玩家主手持证件时返回 `PASS`，避免村民打开交易界面拦截交互。
- **配方**：`data/villicense/recipes/*.json`（**复数 `recipes/`** 目录）。1.20.1 的 `result` 用 **`"item"`**（不是 1.21.1 的 `"id"`）。
- **数据存储**：1.20.1 用物品 **NBT**（`pos` / `dim` / `block` / `entity_id` / `confirm`），因为 Data Components 要到 1.20.5+ 才有。

## 文档

- 实现决策与坑：见 [`MIGRATION.md`](MIGRATION.md)。
- 许可证：LGPL-3.0（见 [`LICENSE.txt`](LICENSE.txt)）。

## 致谢

- 原版 1.20.1 Forge / Fabric 版作者：Tapio、Kennrou_Holo。
- 本 1.20.1 Forge 重写版作者：SamulAlen。
