# 26.3／0.6.1 首次拾取枝石反馈排查

2026-10-20用户报告：联机，客户端安装JEI，果糕逻辑0.6.1，第一次捡到枝石“会崩”。JEI版本、服务器的主Mod／JEI版本，以及断连或完整进程崩溃尚待确认。没有本次反馈的具体堆栈，不能把此前数字刻石的报错直接当作这次证据。

基线为已发布0.6.7／`11360c8`；独立分支`codex/investigate-061-branch-pickup`，本轮没有发布授权。首先审计精确发布0.6.1 JAR（SHA-256 `1cf73cbd73b65bb04870f4b0c0dc5d772573a3d486196acaacc2c8b2e5b0ea30`），主Mod版本及生产代码暂不改。

## 已核对

- 061的`hydra_bud`配方使用4芽穗、4个六选一枝石和1序数晶体；首次取得任一种枝石即可能解锁这一配方。网络显示依赖`#guogaology:ordinal_branch_stones`。
- 061包的六个枝石成员和标签完整，没有引用065才新增的`ordinal_flowers`标签。
- 061的可选JEI保护、Mixin及RecipeSyncCompat与067逐字节相同；兼容保护仅对JEI元数据版本`31.14.0.72`启用。
- JEI的原问题需要进入缺少服务器完整配方快照的fallback路径；仅JEI版本不同不足以证明这次原因。两端正确061按设计会同步完整快照。
- 061此前真实联机四安装组合覆盖首次数字刻石及全部标签指纹，未逐个实测枝石首拾。原证据见`docs/WORK-061.md`。

## 已完成的联机对照

独立QA使用精确061主包、真实ItemEntity拾取、原生配方解锁和TCP包解码。服务端只绑定127.0.0.1，禁止开放给其他电脑；每次新建目录／世界／玩家，不复制或修改正常存档、配置、账号，不抢占鼠标。服务端不安装JEI。

| 测试目录 | 客户端Mod | 服务端Mod | 客户端JEI | 首拾ε枝石结果 |
|---|---|---|---|---|
| `branch061-jei72-epsilon-a` | 0.6.1 | 0.6.1 | 31.14.0.72 | 通过 |
| `branch061-jei75-epsilon-a` | 0.6.1 | 0.6.1 | 31.15.0.75 | 通过 |
| `branch061-jei86-epsilon-a` | 0.6.1 | 0.6.1 | 31.16.0.86 | 通过 |
| `branch061-jei86-oldserver-epsilon-a` | 0.6.1 | 精确0.6.0 | 31.16.0.86 | 复现原生`recipe_book_add`断线 |
| `branch061-jei72-oldserver-epsilon-a` | 0.6.1 | 同一精确0.6.0 | 31.14.0.72 | 通过；fallback保护仍生效 |

双方061的正向测试同时验证解锁前配方不存在、拾取后自然advancement完成、客户端收到实际原生display815、4个TagSlotDisplay各可解析6种枝石、JEI实际查询及打开分枝芽核页面。全457个方块标签／18953成员与523个物品标签／5561成员的客户端／服务端指纹一致。原始证据在`build/network-qa263/`各同名目录中。

旧060服务端对照只替换JEI版本：72能够保留完整标签和收到原生分枝配方，86丢失标签并断线，说明单版本保护范围确实存在缺口。72对照没有服务器完整Fabric配方快照，JEI仍仅提供原版查询；不能将“未断线”解释为旧服务端也能提供全部自定义配方数据。

负对照中服务器仍有完整枝石标签，但客户端拾取前已丢失该标签，Fabric同步配方数为0；JEI明确进入原版配方fallback。实际拾取后原生断连报告包含`NoSuchElementException → Optional.orElseThrow → ByteBufCodecs$34.decode:734`，报文为`clientbound/minecraft:recipe_book_add`。客户端报告失败，不能记为“测试通过”；其价值是精确复现缺标签的故障。服务器干净退出、端口关闭、正常实例不变。它证明存在一组同症状兼容边界，不证明反馈者一定使用了0.6.0服务器；仍缺反馈者日志和服务器版本。

## JEI版本边界

官方[Maven版本清单](https://maven.blamejared.com/mezz/jei/jei-26.3-fabric/maven-metadata.xml)列出70个历史版本。本轮选择正式26.3／Fabric的71、72、73、75、76、77、78、79、81、82、83、84、85、86共14个候选，下载真实成品，以官方Modrinth发布元数据及Maven哈希交叉核对，未伪造JAR版本。

所选71～85的13包`VanillaClientRecipeLoader`整类字节相同；86编译体不同，但仍保留静态注册表标签的fallback覆盖路径。纯Python解析class文件，未把静态字节码对照当作14次实际运行。来源、版本、依赖和SHA在`build/branch-pickup-061/official-candidates.json`，逐指令证据为`bytecode-audit.json`。

## 补充首拾与依赖环境

两端精确061、客户端单装JEI、每轮全新世界／玩家，六种枝石首次拾取均通过；下表含与上表重合的两个分组，用于明确覆盖范围，不重复累计运行数。

| 测试目录 | 实际首次拾取 | JEI版本 |
|---|---|---|
| `branch061-jei72-epsilon-a` | ε | 31.14.0.72 |
| `branch061-jei71-omega-a` | ω | 31.13.0.71 |
| `branch061-jei73-greatomega-a` | Ω | 31.15.0.73 |
| `branch061-jei82-zeta-a` | ζ | 31.15.0.82 |
| `branch061-jei85-psi-a` | ψ | 31.16.0.85 |
| `branch061-jei86-api163-phi-a` | φ | 31.16.0.86 |

φ组使用两端实际Fabric Loader0.20.0、API0.163.0+26.3，其余默认Loader0.19.5、API0.161.0+26.3，均Java25。新依赖只从build测试缓存装入新目录，未改正常实例。所有正例均验证拾取前目标配方未解锁、实际ItemEntity消失且库存收到、原生解锁和客户端网络display、完整标签指纹、JEI真实运行／页面、隐藏窗口／不捕获鼠标、完整停服及端口关闭。已检查代表正例与负例的原生截图。

共10次实际独立联机运行：9次正向通过，1次原生缺标签断线复现；实际运行7个JEI版本，静态审计14个，没有宣称运行全部70历史版本。独立结果核验`build/branch-pickup-061/runtime-summary.json`。原生失败报告正确保留`success=false`，不计作功能通过。

仍无法确定反馈者实际服务器版本、JEI版本及完整异常；因此不能把混合版本复现直接认定为其根因。两端正确061的本轮测试未复现问题。当前067的三个兼容class与061字节相同，单纯升级客户端到067也不会扩大旧保护的JEI版本范围；两端都更新可使用正式同步，避免本轮已复现的旧服务端fallback情形。

## 其他维度Mod的官方实现

没有找到可直接安装或照搬的“维度Mod通用JEI冲突补丁”。可借鉴的共同点是服务端提供权威配方，JEI插件负责展示；这与保护实时物品／方块标签是不同层面的工作。

| 官方实现与核查版本 | 做法与适用范围 |
|---|---|
| BetterEnd 26.301.2／Fabric26.3 | [JEI插件](https://github.com/quiqueck/BetterEnd/blob/a3240014c07575f75143a4d43d37ceee35ae6b18/src/main/java/org/betterx/betterend/integration/jei/JeiPlugin.java)通过WorldWeaver读同步配方；[底层实现](https://github.com/quiqueck/WorldWeaver/blob/36ee9d8b697a777fbd9f51e7a18259c12598fe14/wover-recipe-api/src/main/java/de/ambertation/wover/recipe/impl/SyncedRecipesImpl.java#L35-L43)也调用Fabric `RecipeSynchronization.synchronizeRecipeSerializer`，与我们的061正式同步方向一致 |
| 暮色森林4.9／NeoForge26.1.2 | [服务端](https://github.com/TeamTwilight/twilightforest/blob/f8cf92fa1852daae49906413f43a1eb97b8d7808/src/main/java/twilightforest/compat/RecipeViewerSync.java#L13-L20)用OnDatapackSyncEvent发送配方，[客户端](https://github.com/TeamTwilight/twilightforest/blob/f8cf92fa1852daae49906413f43a1eb97b8d7808/src/main/java/twilightforest/compat/RecipeViewerRecipes.java#L17-L27)缓存后供JEI读；平台API不能直接移植到Fabric |
| 天境1.5.11／Fabric1.21.1 | [插件](https://github.com/The-Aether-Team/The-Aether/blob/0febf5a1e95c880372e1101f9fad28f0c185e898/src/main/java/com/aetherteam/aether/integration/jei/AetherJEIPlugin.java#L36-L110)从现有RecipeManager读取配方、登记分类和工作站，核查入口没有26.3实时标签保护，不能视为本问题的现成修复 |
| Bumblezone7.16.1／1.21.1 | [插件](https://github.com/TelepathicGrunt/Bumblezone/blob/c9224bb971f6191abedf6d3f218aa6381b381ff4/common/src/main/java/com/telepathicgrunt/the_bumblezone/modcompat/JEIIntegration.java)登记分类／交易展示／催化剂并读取现有配方；核查范围未见标签回退隔离 |

直接相关但根因不同的案例是[JEI官方#4542](https://github.com/mezz/JustEnoughItems/issues/4542)：BetterNether作者报告Fabric26.3在ClientboundUpdateRecipesPacket阶段断线，因JEI重建原版注册表看不到模组群系，上游标注31.10.0.63修复。这不是本次缺少物品标签的recipe_book_add故障，不能把该修复视为已经解决本次问题。[JEI官方26.3源码](https://github.com/mezz/JustEnoughItems/blob/26.3/Common/src/main/java/mezz/jei/common/recipes/VanillaClientRecipeLoader.java#L45-L73)仍有`basePendingTags`的应用；正式成品以本轮精确JAR字节码核对为准。

对当前架构的建议：保留Fabric正式完整配方同步，不依赖服务器必须装JEI；对于确实进入JEI fallback的旧服务端连接，兼容层保护实时注册表标签，且以明确核验的调用结构／版本范围启用，避免只认单个JEI版本的覆盖缺口。不通过删枝石配方、取消解锁、重建客户端假配方或改成显式物品列表掩盖标签丢失。当前未修改生产代码，也未发布新版本。

本轮仅提交可复用QA和排查记录到独立分支；主Mod代码／数据／版本号与已发布067保持不变，无正常实例替换，无main合并、push或Release。
