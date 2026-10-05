# Guogaology 验证记录

最新验证日期：2026-10-05。

## 0.3.3：跨平台发行校验修正

首次 GitHub Actions 四版编译均成功，1.21.1 在收集发行包时误报 `bms_aco.nbt` 不一致。已提交模板由 Windows 工具生成，CI 使用 Linux/Python 3.12；gzip 压缩头的 OS 字节可不同，不能用压缩流逐字节相等判断 NBT 是否相同。现代目标在 CI 当场转换模板，因此没有触发这一误报。

校验现改为解压后逐字节核对实际 NBT，不忽略方块、状态或坐标的差异，也不跳过 gzip CRC 验证。回归覆盖不同 OS 头、文件名／时间／压缩级别、错误方块调色板以及损坏的压缩校验和，并加入 CI。四个本地产物各 61 份模板的解压内容均与对应目标转换结果一致。仅发行工具和 CI 修改，游戏代码、贴图及已安装的 26.2 JAR 不变。

失败记录：[Actions 37261883631](https://github.com/hzyhhzy/minecraft-guogaology/actions/runs/37261883631)。

## 0.3.3：三层世界合并预览

四版独立编译与发行包检查通过：Minecraft 1.21.1、1.21.11、26.2、26.3。1.21.1 内嵌源码桥接模块的版本、入口和两处 Mixin 的 intermediary 目标已检查；现代版不包含该桥接模块。未启动 1.21.1、1.21.11 或 26.3。

运行验证使用隐藏独立 fo262 / Minecraft 26.2、Java 25、Fabric API 和 Sodium。不是整合包全部附加 Mod 的兼容性测试。正常游戏、配置、账户资料和存档未改动，窗口不捕获鼠标。最终测试正常保存并退出。

实际加载的服务端通过 181 项机制／模板断言，另通过真实致命伤害和三种景物实放检查：

- 主世界→表界→里界→地府及三次反向返回；六种祭品／门框组合实际生成完整门。
- 返程常用材料可徒手取得；破坏一块框架后完整门被移除。
- 强化台等级拒绝不适合的晶核、手稿可逆安装／拆出、飞行开启／卸除与安全缓降、创造飞行保留。
- 同一满配 Ω镐从表界 72 挖速切到里界 4608，回主世界恢复 72；没有携带深层倍率残留。
- 果糕手稿对真实致命伤害保命，背包两枚图腾变一枚；手稿和晶核仍保留。
- 61 份模板能解码；43 种 BMS 模板进一步确认含真实数字砖，防止“有尺寸却全是空气”的假通过。
- 在独立场地调用实际配置特征，确认 BMS 数字砖 13 格、紫菜桌木板 33 格、蘑菇云帽体 837 格。具体数值来自固定随机测试样本，不代表所有模板的大小。
- 七个表界群系的实际区块生成、七种普通生物模型和自然场景截图检查；BMS、紫菜树林、圣诞森林恢复可见景物。

实拍暴露并修复了 26.3 NBT 调色板 `id/properties` 与旧版 `Name/Properties` 的差异。61 份模板共 2,489 个位置条目保持原坐标；共用数字砖属性 `digit→number`、紫菜木材属性映射与状态格式按目标版本转换。两类模板原有的不同居中规则分别保留。日志未发现本模组的模型／配方／模板缺失、越界模板写入或注册错误；离线账户认证、系统性能计数器以及批量生成区块的超时 tick 警告单独记录，不作为完整性能基准。

资源审计：宿主 281 组中英文键、表界 137 组中英文键一致；主资源 1,893 个可达文件，表界 678 个可达文件，缺失引用和未引用资源均为零。73 条表界配方及其解锁奖励没有旧扽／斧铲锄／矿心引用。四个发行 JAR 逐项核对 28 款晶核模型、61 份按版本转换的模板、双语资源、许可文件和 Java 版本；不含测试辅助代码。

测试日志标记：`MERGE_MECHANICS_OK checks=181 templates=61`、`MERGE_TOTEM_OK`、`MERGE_FEATURES_OK`、`PRODUCTION_PORT_OK 26.2`。可复用辅助源码在 `ports/qa26`，运行范围和构建方式见 [BUILDING.md](BUILDING.md)。未进行旧六矿物品的存档迁移或长时间多人平衡测试；跨 Minecraft 版本的地形不承诺逐格一致。

All four targets compile and pass package checks. Only hidden Minecraft 26.2 is runtime tested. The final run validates three-layer travel, local-material return gates, passive Manuscripts, realm-dependent mining, inventory Totems, seven creatures, 61 templates, real BMS/Laver/cloud placements and clean shutdown. The original donor JAR is never embedded. See [the import architecture](OUTER-IMPORT.md) and [implemented choices for review](MERGE-REVIEW-CHECKLIST.md).

## 0.3.2：中英双语 / English and Simplified Chinese

285 项中英文键一一对应；163 个方块入口、51 个非方块物品入口、动态名称与 Java 界面引用均通过检查，没有缺译、格式参数不一致或硬编码中文提示。检查器接入四版批量构建、发行打包和 GitHub Actions。

在独立隐藏 fo262（Minecraft 26.2）中使用发行 JAR、Fabric API、Sodium 和已安装的 Mod Menu 实际切换 `en_us` → `zh_cn`。两种语言分别检查全部 285 条文案和 522 个创造栏变体，Mod Menu 名称／摘要／介绍正确切换；满 10 槽的强化界面显示正常，真实字体测量未发现面板文本溢出。已检查两种语言的创造物品栏与强化台原始截图。未操作正常实例、配置或玩家存档。

新增完整英文项目说明、生存指南和 142 条共享采集规则参考。英文创造栏标题缩短，避免覆盖翻页按钮；七个分类均检查真实字体宽度。动态数字、颜色、拼灯分片、门框名称和装备组件保留原行为。构建时保留 JSON 反斜杠转义，使双语元数据中的换行在四版 JAR 内仍是合法 JSON。

四版发行包构建与资源检查通过；与 0.3.1 逐项比较，只有两份语言文件和 `fabric.mod.json` 改变，业务字节码、模型、纹理及数据均一致。只有 26.2 启动客户端，其余三版未启动游戏。

All 285 English and Chinese entries, 163 block names, 51 non-block item names, and live UI references pass the localization audit. A hidden Minecraft 26.2 client reloads both languages and checks 522 creative variants, actual Mod Menu translations, enhancement-panel text widths, and screenshots. No normal game profile is used. Gameplay code, saved IDs, recipes, equipment balance, and world generation are unchanged.

## 0.3.1：GPL v3 许可更新

四版重新构建并通过元数据、完整 GPL 文本、Minecraft 链接例外及版权声明校验。二进制 JAR 与源码 JAR 都包含许可文件；资源引用检查通过。

四版业务 class、模型、纹理和游戏数据与 0.3.0 逐字节相同。本轮没有启动游戏，继续引用下面 0.3.0 的独立 fo262 运行验证。分发压缩包附带匹配的完整源码压缩包；0.3.0 的 MIT 标签及历史包保留，不被覆盖。

## 0.3.0 基线

## 四版发行包

| Minecraft | Java 字节码目标 | 独立源码目录构建 | 本轮运行验证 |
|---|---|---|---|
| 1.21.1 | 21 | 通过，发行 Mixin 引用检查通过 | 未启动 |
| 1.21.11 | 21 | 通过 | 未启动 |
| 26.2 | 25 | 通过 | 独立隐藏 fo262 通过 |
| 26.3 | 25 | 通过，含该版数据格式及 Sodium 朝向适配 | 未启动 |

四版从此仓库编译，不复制旧发行 JAR。打包检查验证版本、游戏依赖、Java 字节码、双语名称、图标、28 款晶核模型与资源一致性，以及发行包不含开发验证代码。四版全部业务类与原工作区已验证的 0.2.26 发行包逐字节一致；本次没有重新设计游戏机制。

运行验证按项目约定仅使用 Minecraft 26.2。其余三版的本轮证据是构建与静态发行检查，不能等同于在各自客户端完整游玩。GitHub Actions 四版工作流已配置，尚未在远端执行。

## 资源清理

删除 180 个无引用资源，共 45,307 字节。保留 1,955 个图检查可达资源，缺失引用为零。详情见 [RESOURCE-CLEANUP.json](RESOURCE-CLEANUP.json)。

- 保留所有在用数字、颜色、灯具分片与悲伤表情变体。
- 保留现代版和旧版护甲材质、GUI、记号目录及建筑数据。
- 旧展厅方案、临时飞行工具、试验、游戏存档、缓存和历史截图不进入源码仓库。

## fo262 实际发行包检查

使用独立世界与正常实例已安装的 Fabric API、Sodium、Iris 等图形组件。游戏窗口隐藏，不捕获鼠标，不复制正常账户资料或修改玩家存档。临时验证辅助程序在开发工作区，未打包进此仓库或发行 Mod。

通过内容：

- 全部 28 款晶核的放置模型、物品栏模型与动态渲染入口。
- 205 组珍藏材料的保存、玩家放置和生存回收，以及徒手序数晶体掉落。
- 两个维度的加载和实际帧缓冲截图。
- 四条传送门路径及大数世界坠入地府；准备完成的出口均在下一 tick 通过。
- 日志中无本 Mod 的模型、材质或 JSON 资源缺失；客户端正常退出。

离线验证账户产生的认证提示、系统性能计数器提示以及 Iris 在线更新超时不属于本 Mod 的资源故障。此次检查不是性能基准。

像素标识在运行验证后按最终要求缩为原表情的约 80%，居于 Ω 大圆环中央。表情借鉴终境巨树顶球的 `MosaicMotifs` 图案与配色，再按最终要求降低像素：30×30 采样、最近邻放大到图标中的 60×60，整体图标 128×128。该更改仅涉及图标；最终四版重新打包，并与已运行的 26.2 JAR 逐项比较，除图标外内容保持一致。

各版本的文件与 SHA-256 由 `tools/package_multiversion.py` 写入对应的 `build/releases/<版本>/`。
