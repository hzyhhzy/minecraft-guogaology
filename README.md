# 果糕逻辑 · Guogaology

![Guogaology](docs/branding/googology-mark.png)

**v0.3.1 · Minecraft Java / Fabric**

把大数构造、序数记号和社区梗变成可以探索、开采和建造的世界。两个维度、八种群系、八座巨型建筑，配有序数矿业、扽西装备、晶核融合和可逆强化。当前没有自定义生物或 Boss；代表建筑保留了后续战斗场地。

Explore worlds inspired by large numbers and ordinal notation: two dimensions, eight biomes, monumental landmarks, ordinal ores, Denxi equipment and reversible crystal upgrades.

## 支持版本

| Minecraft | Java | Fabric API（构建版本） |
|---|---|---|
| 1.21.1 | 21 | 0.116.17+1.21.1 |
| 1.21.11 | 21 | 0.141.6+1.21.11 |
| 26.2 | 25 | 0.153.0+26.2 |
| 26.3 | 25 | 0.161.0+26.3 |

四个版本分别生成 JAR。选择与游戏版本匹配的一份，与对应 Fabric API 放进 `mods`，客户端与服务器使用同一版本。模组内部 ID 保持 `googology`，因此现有方块和维度不会因改名丢失。

## 世界内容

| 群系 | 典型内容 | 代表建筑 |
|---|---|---|
| 矩阵山脉 | BMS 板、Y 序列、折面山脉、晶页 | 矩阵折峰堡 |
| 幂塔荒漠 | 幂塔、葛立恒树、TREE、SCG、古早记号水下遗迹 | 迭代圆木塔 |
| 序数花园 | 符号植物、Veblen 冠树、Hydra、ω树、Ω蘑菇 | 六枝冠庭 |
| LHO 空无之境 | 透明空岛、LHO 字母、会消失的记号碎片 | 缺页悬空回廊 |
| 紫菜桌高原 | 可演奏 iBLP 桌、紫菜、天依纱线、果糕软糖 | 紫菜三角宫 |
| Astra 推演原 | 明亮城市、机柜、扭结、蘑菇云 | Astra 推演中枢 |
| 界限高原 | 图灵机纸带、集合晶壳、证明与公式景观 | 界限演算庭 |
| 果糕冥林 | 阴森圣诞树、粗壮藤蔓、破碎地形与湖泊 | 果糕终境巨树 |

前七种在大数世界，果糕冥林位于独立的果糕地府。六阶矿石可制作镐、剑和护甲，九条晶核路线用于融合及强化。[生存指南](docs/SURVIVAL-GUIDE.md) · [方块采集表](docs/BLOCK-BALANCE.md) · [名称约定](docs/TERMINOLOGY.md)

## 进入维度

12 个完整蛋糕或任意颜色果糕，围成末地传送门形状。扔苹果开启大数世界门，扔果糕方块开启地府门。主世界、大数世界、果糕地府的水平距离比例为 1:4:16。

从大数世界坠入虚空会进入地府。地府重力为四分之一，无摔落伤害。`/googology` 显示帮助；管理员可用 `/googology return`。

## 构建

需要 JDK 21、JDK 25、Python 3.10+；普通构建只使用 Python 标准库。Gradle Wrapper 已包含，首次构建会下载依赖。

```powershell
$env:JAVA21_HOME = '你的 JDK 21 路径'
$env:JAVA25_HOME = '你的 JDK 25 路径'
./build-all.ps1
```

产物、版本清单与 SHA-256 位于 `build/releases/0.3.1/`。也可以逐版构建，详见[构建说明](docs/BUILDING.md)。源码中的模型、材质和记号数据已经齐备，不依赖原作者电脑上的表格或历史文件。

此仓库不包含临时晶核展厅、飞行调速工具、开发存档、缓存和历史备选设计。当前资源经过引用检查，检查器在 CI 中继续运行。[资源清理记录](docs/RESOURCE-CLEANUP.json) · [验证记录](docs/VALIDATION.md)

## 开发与许可

`src/main` / `src/client` 为 1.21.1 基础实现；`ports/common` 与目标覆盖负责现代 API，纯算法通过 `ports/shared-sources.txt` 共享。不要直接编辑 `build/generated`。像素 Logo 可用 `tools/generate_brand.py` 重建（可选安装 Pillow）。

本项目自 0.3.1 起采用 **GNU GPL v3（仅第 3 版）**，并附带 [Minecraft 链接许可](LICENSE-MINECRAFT-EXCEPTION)，允许模组与 Minecraft 游戏本体链接运行。该例外不改变本模组自身的 GPL 要求，也不授予 Minecraft 本体的再分发权。

向他人分发本模组或修改版时，需要按 GPL 提供相应源代码和许可声明。此前已按 MIT 分发的版本保留原有许可；Gradle Wrapper 等第三方文件仍遵循各自的许可证。

[GPL v3 全文](LICENSE) · [版权与适用范围](COPYRIGHT) · [第三方与数据说明](THIRD_PARTY.md)
