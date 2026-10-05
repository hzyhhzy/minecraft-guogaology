# 表界内容移植与维护 · 0.3.3

基底始终是本项目。获授权的 `googology-dimension-1.0.0.jar` 内容作为表界模块加入；不加载或嵌入对方原 JAR，不运行其同名入口。来源、SHA-256、作者与许可声明见 [THIRD_PARTY.md](../THIRD_PARTY.md)。用户已明确说明取得作者许可。

## 保留范围

- 七个群系的气候分区、地表规则、植物与矿物分布参数；主世界噪声委托和 LHO 岛体密度场。
- 61 份原始 NBT 景物模板，包括 43 种 BMS、五种紫菜桌、三种紫菜树及蘑菇云等。坐标、体积、模板权重与旋转规则保留，只映射共用方块 ID 和属性。
- 圣诞／枇杷／紫菜／地狱圣诞／地狱枇杷五套木材，以及船、牌子、座椅、植物和食物。
- 蛇、DeepSeek 鲸、忙碌海狸、飞行 Y、果糕史莱姆、水果史莱姆、邪恶猪七类普通生物的行为和模型。仅表界的生成表引用它们。
- ω、ε、Γ、Ω矿物、材料、工具和护甲贴图采用授权原图。注册、配方和强化规则由本项目的四档装备系统统一管理。

九头蛇生物、Boss、独立扽工具、旧矿心系统、旧入口物品和与共用系统冲突的配方不移植。巨型 Hydra 植物并非被删除的九头蛇生物。

## 目录与修改入口

| 路径 | 用途 |
|---|---|
| `content/outer-1.0.0` | 导入时的原始数据与模板；仅供重建和比较，不整目录打包 |
| `ports/26.3/src/main/java/dev/googology/outer` | 从原 JAR 重建的原生 26.3 模块 |
| `ports/common/main/dev/googology/outer` | 1.21.11／26.2 的世界、景物、注册和生物适配 |
| 对应 `client` 目录 | 生物、船、牌子渲染；不进入专用服务器加载路径 |
| `ports/outer-1.21.1` | 1.21.1 源码桥接，构建为宿主内嵌的 intermediary JAR |
| `tools/prepare_outer_legacy.py` | 1.21.1 API 转换；有语义差异时使用明确源码覆盖 |
| `tools/prepare_outer_resources.py` | 原始数据到各版数据格式的可重复转换 |
| `tools/import_outer_content.py` | 可选的原 JAR 导入工具和共用 ID 映射；普通构建无需原 JAR |
| `tools/generate_merge_progression.py` | 共用四矿、装备、手稿、配方和文案生成 |
| `tools/audit_outer_content.py` | 导入资源、语言、配方解锁和物品引用检查 |

表界独有 ID 位于 `googology_outer`。数字砖、紫菜叶脉／木板、果糕、灯以及四套矿材装备使用 `googology` 的共用 ID。`outer/GoogologyMod.id` 是 Java 映射入口；Python 映射在 `import_outer_content.ALIASES`。修改别名要同步两处，并检查 NBT 调色板、JSON 和 Java 三种来源。

`src/main/resources/data/googology_outer` 是可直接构建的 1.21.1 数据。修改原始参数后运行：

```sh
python tools/prepare_outer_resources.py 1.21.1 src/main/resources
python tools/audit_outer_content.py
```

现代目标会在构建时自动从原始数据重新转换。不得修改 `build/generated`；否则下次构建会覆盖。资源与模型已提交，不需要作者私有文件。可选重新导入原 JAR 后，需要运行 `normalize_outer_assets.py`、共用配方生成器、上述转换和资源审计；不能直接发布未过滤的导入目录。

## 有意适配的地方

26.3 的 Feature/Carver、方块状态、物品模型、环境属性和战利品 JSON 与旧引擎不同。NBT 调色板也必须把 `id/properties` 转成旧版的 `Name/Properties`，否则模板会无声地变成空气。旧版本委托本版原版噪声地形，保留原模组的群系参数及 LHO 算法；地下洞穴额外保留原版 26.3 配置的次数、宽度和偏置。因引擎噪声与植被实现跨版本变化，**不承诺四个 Minecraft 版本逐格一致**。

26.3 普通模板中心使用半尺寸偏移，旧版桥接保持这个语义；作者自定义的紫菜桌／树则保留其整尺寸偏移。没有统一改成另一种居中方式。随机状态必须使用表界真实噪声配置，不能退回默认配置。生物出生规则通过可重映射的 Invoker 注册，不使用只在开发映射中有效的私有反射方法名。

表界普通紫菜桌的羊毛图案保持作者版本；里界 iBLP 桌仍是原核驱动的可演奏桌。它们共用紫菜木材，功能定位没有强行合并。两种表界矿石底材沿用作者美术；里界地貌方块未整体重绘。

合成解锁提示按实际保留的配方重建，取得任一相关材料即可解锁；不带入已删除的扽／斧铲锄／矿心奖励。树苗、叶片的行为以导入源码为准，不额外承诺自动成长等作者未实现的系统。

## 验证边界

四版执行编译和资源检查；运行测试按用户要求仅在隐藏独立 fo262 / Minecraft 26.2。运行辅助代码在 `ports/qa26`，只由 `-PmergeQa` 构建为独立辅助 JAR，不能进入发行包。详细结果见 [VALIDATION.md](VALIDATION.md)。

这是实验分支。原有里界与地府的维度 ID 保留，但删去的六矿物品没有旧存档迁移；验证使用新测试世界。对方原模组与此整合版不能同时加载。
