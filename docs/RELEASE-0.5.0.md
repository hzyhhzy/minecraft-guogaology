# 果糕逻辑 · Guogaology v0.5.0

## 安装与兼容性

- 提供 Minecraft **1.21.1、1.21.11、26.2、26.3** 的四个独立 Fabric JAR，请只安装与游戏版本对应的一个；另需对应版本的 Fabric API。
- 1.21.x 使用 Java 21+，26.x 使用 Java 25+。客户端与服务端须使用相同版本。
- **本版 Mod ID、注册 ID、资源命名空间改为 `guogaology`，不提供旧存档迁移。请新建世界，旧世界继续使用旧版；升级时移走旧版 `googology-dimension` JAR，不能两个同时加载。**
- 独立晶核展厅与创造飞行调速工具不是主模组的必要依赖，本次发行不包含它们。

## 相对 v0.4.0 的主要变化

- 品牌和内部标识统一为 Guogaology / `guogaology`。学科名称 googology、原作者及历史来源信息保留。
- 命令整理为 `/guogaology help`、`tp outer|inner|underworld|overworld`、`up`、`kit portal <目标>`、`kit return`。帮助对所有玩家开放，传送与材料包需要管理员权限。
- 果糕地府加入错层崖地与静默沼泽等群系、普通地下下降洞穴，以及从 Y250+ 崖顶连接谷底的巨型斜向降链。
- 恢复八座巨构的完整内饰与晶核、序数晶体陈列；里界巨构密度约1座/km²，地府约0.5座/km²。幂塔战场保留在真正的最高层。
- 传送先显示原生地形加载界面，目的地异步准备、有界搜索和失败回退；落点优先实际地面。激活门框硬度200、破坏不掉落。
- 手稿编辑和强化界面的操作与效果预览改进，手稿可镶嵌并真实消耗不死图腾；手稿本身不提供耐久效果。
- 手稿挖掘速度同时提供工具加项和急迫式乘项；额外开采不挖原目标下方。四档矿物装备与手稿均可接受最高阶晶核，槽位数保持原设定。
- 手稿飞行与创造飞行调速独立。最高阶冲刺在里界及地府水平、垂直均4倍，在普通维度2倍；二阶无冲刺。
- 表界掉落到 Y≤−500 进入里界。里界摔落与鞘翅撞击伤害降低，相关晶核效果叠加。
- 四矿尝试数翻倍，均采用接触空气50%舍弃；Γ、Ω矿增加偏向底层的高度分布。
- 晶核物品栏保留简洁等级图标，手持与掉落外观使用静态立体模型；创造分类采用代表物品。空白彩灯与数字彩灯分离，表情灯变体名称可区分。

## 验证与源码

- 四个版本独立编译、资源与命名空间审计通过，39项 Python 测试通过。
- Minecraft 26.2 的独立隐藏 fo262 新世界通过26,431项原生断言，并检查了实际创造分类和命令帮助截图；同时加载 Sodium、Voxy 和独立飞行工具。
- **其余三个版本仅完成编译和静态检查，未做游戏运行测试。**
- 附件含四版 JAR、SHA-256 清单、依赖 manifest、安装说明、许可文件及完整对应源码 `Guogaology-v0.5.0-source.zip`。源码压缩包取自本次发布标签，含构建脚本和在用资源。
- 许可：GPL-3.0-only WITH GPL-3.0-linking-exception；第三方素材保留各自版权与许可。

## English

Four separate Fabric builds are provided for **Minecraft 1.21.1, 1.21.11, 26.2 and 26.3**. Use exactly one matching JAR with the matching Fabric API; Java 21+ is required for 1.21.x and Java 25+ for 26.x.

**Breaking identifier change:** the Mod ID, registry IDs and resource namespace now use `guogaology`. There is no old-save migration: start a new world, keep old worlds with their previous build, and remove the old `googology-dimension` JAR when upgrading.

Highlights since v0.4.0 include the reorganized `/guogaology` commands, new Underworld biomes and enormous descending cliff chains, restored landmark interiors with lower landmark density, grounded asynchronous portal exits, manuscript UI and totem support, independent manuscript flight, mining and ore-distribution updates, and improved item/creative-tab presentation. Highest-grade manuscript sprint is 4× on both axes in the Inner world and Underworld, and 2× elsewhere.

All four builds and package audits passed, alongside 39 Python tests. Runtime validation is limited to Minecraft 26.2: 26,431 native assertions passed in an isolated fo262 instance with Sodium, Voxy and the standalone flight utility. The other three targets were compiled and statically checked, not playtested.

The release includes matching complete source, checksums, dependency metadata and license notices. Source is licensed under GPL-3.0-only WITH GPL-3.0-linking-exception; third-party notices remain applicable.
