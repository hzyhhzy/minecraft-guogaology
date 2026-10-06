# 果糕逻辑 · Guogaology 0.4.0

三个相连的额外世界，九类晶核，以及可逆的装备与手稿强化。

- 实装完整九核机制、四档序数弓、图标等级与稀有度，以及八座巨构的新奖励。
- 副手手稿可从背包「手稿」按钮直接编辑，无需换手或快捷键；普通右键不被副手占用。关闭页面后才应用被动变化，编辑过程中死亡也不会重复掉落或吞掉晶核。
- 修复现代版本客户端飞行代码未被注册，以及与独立飞行调速工具的水平速度钩子冲突；三级手稿在里界／地府冲刺时，水平与竖直均为普通非冲刺飞行的8倍。
- 修复各级晶核破坏时的紫黑缺失贴图粒子。果糕之心物品图标保留原样，只压扁嘴形。
- 自然生成的「序数晶体」不标级；强化用「序数晶核」分Lv1～3，效果与配方用量不因显示改名而改变。
- 完整简体中文／英语，离线强化模拟器、作用表及生存指南包含在仓库中。

下载与游戏版本匹配的**一个**JAR：1.21.1、1.21.11、26.2或26.3。需要对应版本的Fabric Loader和Fabric API；客户端与服务器使用相同版本。1.21.x需要Java21，26.x需要Java25。无需安装独立展厅或飞行调速工具。

四个目标均构建及静态核验；实际游戏回归只在独立隐藏的fo262（26.2）进行。早期开发，未安排旧存档迁移。

---

Three connected extra worlds, nine core families, and reversible equipment/manuscript upgrades.

- Complete core effects, four ordinal bows, readable grade/rarity icons, and revised rewards in all eight monumental landmarks.
- Edit an offhand manuscript using the inventory **Book** button. No keybinding or offhand right-click interception. Passive changes apply on close; death during editing conserves the exact items and cores.
- Fix missing modern client flight hooks and a horizontal-speed hook conflict with the separate flight utility. Grade-3 manuscript sprint flight in Inner/Underworld scales horizontal and vertical movement to 8× normal non-sprint flight.
- Fix missing-texture core break particles. Preserve the Guogao Heart inventory design, with a flatter mouth.
- Natural Ordinal Crystals are ungraded; upgrade Ordinal Cores displayLv1–3 without changing effects or ingredient counts.
- Complete English/Simplified Chinese, with an offline enhancement simulator and reference guides in the repository.

Choose **one** JAR matching Minecraft1.21.1,1.21.11,26.2 or26.3. Install matching Fabric Loader and Fabric API on both client and server. Java21 is required for1.21.x and Java25 for26.x. The separate gallery and flight utility are not required.

All four targets are built and statically verified; in-game regression testing is limited to the isolated26.2 pack. Early development: no old-save migration is provided.

License: GPL-3.0-only WITH GPL-3.0-linking-exception; third-party notices remain included.
