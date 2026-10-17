# 正式名称与词根 · 0.6.0

| 中文 | 英文 | 约定 |
|---|---|---|
| 果糕逻辑 | Guogaology | 模组正式中英文名称 |
| 大数表界 | Outer Guogaology | 自然尺度世界；四档装备矿物与普通生物 |
| 大数里界 | Inner Guogaology | 巨构世界；群系原料与序数晶矿；内部 ID `guogaology:guogaology` |
| 果糕地府 | Guogao Underworld | 第三层世界；也有群系原料和序数晶矿，与表界的地下群系区分 |
| 果糕 | Guogao | 词根 guogao，不再写成 fruit cake 或 jelly |
| 扽西 | Denxi | 词根 denxi；保留数字工具与被动副手手稿，四档矿物装备为「ω镐、ε剑、Γ胸甲、Ω靴子」等，不带「石」 |
| 矩阵数字砖 | Matrix Number Bricks | 宿主字形、Alice 白底；右键循环0～15 |
| 表界归途框 | Outer Return Frame | 表界→主世界的固定十二格框架 |
| 里界归途框 | Inner Return Frame | 里界→表界的固定十二格框架 |
| 冥林归途框 | Dread Forest Return Frame | 地府→里界的固定十二格框架 |
| 归途签 | Return Token | 一块任意木板放木棍上方合成，统一返程激活物 |
| 空境玻璃 | Void Glass | 统一原空境／空无玻璃，使用宿主 absence_glass 材质 |
| 紫菜原木／木板／叶 | Laver Log / Planks / Leaves | 两层世界共用 Alice 外观和宿主物品 |
| 序数 | Ordinal | 词根 ordinal |
| 序列 | Sequence | 数学数列、Y序列及数列灯的用词；晶核显示名改为「矩阵晶核」 |
| 矩阵晶核 | Matrix Core | 原序列晶核的现行名称；保留 `sequence_core` 及各阶 ID |
| 幂塔 | Power Tower | 各阶晶核统一称幂塔晶核 |
| 分枝 | Branch / Hydra | 中文统一用「分枝」；Hydra 专指多级分枝树及芽核 |
| 空集 | Empty Set | 数学符号与数学概念；晶核显示名改为「空无晶核」 |
| 空无晶核 | Absence Core | 原空集晶核的现行名称；合成Lv1／2／3，保留 `lho_trace` 及各阶 ID；不再作为野外碎片生成 |
| 紫菜 / Laver | Laver | 景观叫紫菜，记号及贵重材料用 Laver；iBLP 固定大小写 |
| 推演 / Astra | Inference / Astra | 群系、主建筑使用「推演」；震惊瘫坐椅保留既定名称 |
| 核爆之心 | Heart of Nuclear Awe | 原临界核晶的现行名称；保留 `astra_critical_core` 及各阶 ID |
| 界限 | Boundary | 界限高原、界限晶核一致 |
| ω、ε、Γ、Ω序数结晶 | Ordinal Mineral | 四档表界矿材；ψ、层峦、证界旧矿材已删除 |
| 序数晶屑 | Ordinal Shard | 三种序数晶矿共用的产物，四片与一块普通序数晶体可双向转换 |
| 序数晶体 | Ordinal Crystal | 无等级的基础方块；采集掉本体，提供LHO稳定场；不是表界四档装备矿材 |
| 序数晶核 | Ordinal Core | 显示Lv1／2／3，分别对应 `ordinal_crystal_lv2`／`_lv3`／`_lv4`；只用于手稿镶嵌 |
| 果糕灯塑形台 | Guogao Lamp Shaper | 两种表情各27种形态，同色一换一；不加工数字灯和无字彩灯 |
| 冥杉 | Dread Fir | 自然树木及木材保留；巨构叫果糕终境巨树 |

| 自然原料 | 英文 | ID |
|---|---|---|
| 矩阵薄片 | Matrix Sheet | `matrix_sheet` |
| SCG结点 | SCG Junction | `scg_node` |
| Hydra芽穗 | Hydra Sprig | `hydra_sprig` |
| 核爆晶块 | Nuclear Blast Block | `cloud_pearl`；完整方块 |
| 集合膜 | Set Membrane | `set_membrane` |
| 馃槹 | Guogao | `guogao_drop`；亮度15、轻圆角，六面原生32×32纹理、约28×28表情区 |
| 图灵终符 | Turing Terminus | `turing_endcap` |

| 群系 | 代表建筑 |
|---|---|
| 矩阵山脉 | 矩阵折峰堡 |
| 幂塔荒漠 | 迭代圆木塔 |
| 序数花园 | 六枝冠庭 |
| LHO 空无之境 | 缺页悬空回廊 |
| 紫菜桌高原 | 紫菜三角宫 |
| Astra 推演原 | Astra 推演中枢 |
| 界限高原 | 界限演算庭 |
| 果糕冥林（果糕地府维度） | 果糕终境巨树 |

修订源为 `tools/copy_catalog.json`，`tools/copy_catalog.py` 可同步语言资源。资源生成器写语言文件时也执行这份规则；新增或更名须同步中英文和当前指南。

巨构与晶核的 ID 路径部分保留，命名空间统一为 `guogaology`；`epsilon_meadow` 对应序数花园。四档装备矿及其 `nether_*_ore` 外观仅在表界自然生成。里界、地府另外生成序数晶矿：数字母岩款 `ordinal_crystal_ore`、空境玻璃款 `empty_crystal_ore`、冥岩款 `dread_crystal_ore`，均掉同一种序数晶屑。野外成品晶核与普通序数晶体不再自然摆放，巨构仅保留有限晶核珍藏。

四档装备矿物名称为ω／ε／Γ／Ω，不带数字0；美术沿用 Alice 0.5.4.1 的矿物资源。数学内容中的 ε₀ 仍表示真实的序数概念，不因矿物名称而改写。

门面和 `guogao_portal_frame` 均用 `style=0/1/2` 区分表界／归家、里界、地府三种目的地。所有导入注册、生成及美术资源统一到 `guogaology`；表界维度／类型／噪声配置为 `outer`。按用户要求不提供旧存档迁移。

晶核当前显示名为「紫菜凝核 / Laver Condensation Core」；数学记号、桌子与群系的 Laver / iBLP 名称继续保留。
