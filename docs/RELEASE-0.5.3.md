# 果糕逻辑 · Guogaology v0.5.3

## 相对 v0.5.0 的变化

- 巨型无穷降链增加沿下降方向排列的大于号装饰；恢复八座巨构主、副箱子的原定晶核及特色产物数量。
- 修复 Minecraft 1.21.1 通过 Sinytra Connector 加载时的飞行、耐久和表界生成兼容问题，并补齐1.21.1的资源适配。
- 摔落保护覆盖原版摔落类伤害，包括落到石笋尖端与末影珍珠伤害。界限晶核装在手稿中时，Lv1减半并与里界减半独立相乘；Lv2／3免疫，即使关闭飞行也生效。原版摔落保护、抗性提升仍正常叠加。
- 修复 NeoForge 忽略旧减伤方法返回值造成的静默失效。掉落的钟乳石砸人仍属于另一类伤害，不会误获摔落免疫。

## 下载与安装

| Minecraft | Java | JAR |
| --- | --- | --- |
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.5.3.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.5.3.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.5.3.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.5.3.jar` |

只安装对应游戏版本的一个JAR，升级时移走旧版主模组。

- **原生 Fabric**：安装对应版本的 Fabric Loader 与 Fabric API。
- **NeoForge＋Sinytra Connector，仅1.21.1**：使用上面同一份1.21.1 JAR，搭配 NeoForge21.1.248、Connector2.0.0-beta.16+1.21.1 和 Forgified Fabric API0.116.15+2.3.1+1.21.1。该环境不要再装普通 Fabric API。
- Connector不是必需依赖；26.2本轮测试使用原生 Fabric。无需晶核展厅或创造飞行调速附加模组。
- 客户端与服务端使用相同游戏版本及主模组版本。0.5.0之前的命名空间不做存档迁移；旧世界继续用其原版本。

## 验证

- 同一1.21.1发行JAR在原生Fabric与NeoForge＋Connector beta.16各通过544项实际客户端／集成服务端检查。
- 26.2独立fo262通过705项落地专项与8,932项手稿主回归，包含96组持续键盘飞行场景，同时加载Sodium、Voxy和独立飞行工具。
- 四个版本均完成构建和发行校验，43项Python测试通过。**1.21.11和26.3仅完成编译／资源检查，本轮没有运行测试。**
- 这些检查不等同于专用服务器多人压力测试。Connector beta.17曾在0.5.2通过检查；0.5.3重新验证的是beta.16。

附件包含完整对应源码 `Guogaology-v0.5.3-source.zip`、SHA-256、依赖清单与安装说明。许可为 `GPL-3.0-only WITH GPL-3.0-linking-exception`，第三方内容保留各自许可。

## English

Since v0.5.0, giant descending chains gain repeated greater-than decorations and landmark chest rewards return to their original quantities. Minecraft1.21.1 now supports the tested Sinytra Connector setup, with fixes for fall/flight handling, durability, world-generation injection and legacy resources.

Landing protection now follows vanilla's fall-damage tag, including stalagmites and ender-pearl fall damage. A Boundary core in a manuscript halves this damage at Lv1 (multiplying with the Inner realm's independent half), and grants immunity at Lv2/3 even with flight switched off. Native Feather Falling and Resistance still stack. A separate fix ensures NeoForge actually applies the reduction instead of silently discarding its return value. Falling stalactites remain a different damage family.

Choose one matching JAR for **1.21.1, 1.21.11, 26.2 or26.3**. Java21+ is required for1.21.x and Java25+ for26.x. Native Fabric needs matching Fabric API. **The same1.21.1 JAR** also runs with NeoForge21.1.248, Connector2.0.0-beta.16 and Forgified Fabric API0.116.15+2.3.1+1.21.1; do not install ordinary Fabric API alongside Forgified Fabric API. Connector is optional, and26.2 was tested on native Fabric.

Native Fabric1.21.1 and Connector1.21.1 each pass544 actual checks. Native26.2 passes705 landing checks and8,932 main manuscript checks, including96 sustained keyboard-flight scenarios. All four targets pass build/package validation and43 Python tests pass. **1.21.11 and26.3 are compile/resource checked only.** Runtime checks use disposable clients with integrated servers, not dedicated-server multiplayer stress tests. Connector beta.17 was previously checked with0.5.2;0.5.3 reruns beta.16.

Matching complete source, checksums, dependencies, installation instructions and license notices are attached. Source is `GPL-3.0-only WITH GPL-3.0-linking-exception`; third-party licenses remain applicable.
