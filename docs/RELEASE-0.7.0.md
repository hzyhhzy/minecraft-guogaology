# 果糕逻辑 · Guogaology v0.7.0

汇总自已发布的v0.6.11以来，0.6.12～0.6.16完成的挖掘图案、外观与原生行为修复、说明精简和iBLP裁库。本版以合并后的真实`main`主分支为发布来源；0.7.0是在已验证0.6.16内容上的版本重建，没有额外玩法或数值调整。

感谢@8266等人帮忙测试

## 更新内容

- **可编辑连锁挖掘**：强化台可设置竖直5×5图案，指定一个参考格和额外开采格。上下始终对应世界高度，左右取开挖时的水平朝向；只尝试选中位置，不寻找替补。矩阵晶核单颗Lv1／2／3提供1／2／4个额外预算，同镐平方求和开根号取整，表里一致；额度变化自动换用对应默认图案，额度不变保留自定义。保留潜行跳过、权限、掉落、经验与耐久判定。[0.6.12记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0612.md)
- **弓与塑形台外观**：修正序数弓蓄力图案的对称性、箭尾移动及第一人称另一只手的显隐；第三人称继续使用原版拉弓动作。果糕灯塑形台右侧模具改为正方形并清理内部叠面；移除冗余潜行物品提示。[0.6.13记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0613.md)
- **连续灯带与地府底座**：Astra和地府自然树按整段安排连续灯带，与其他灯带、挂饰留出间距，保留合法数字序列与树形。新地府区块采用类似主世界的五层随机基岩底座，并连接上方母岩。[0.6.14记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0614.md)
- **精准采集与木制机关**：晶核精准现在统一陶罐完整掉落、冰不融水、蜂巢保留住蜂的原版判据；真实精准附魔和容器、经验规则保持原版。紫菜木／冥杉机关补齐原版形态属性、支持与活塞行为、乐器和标签；保留自身物品ID，冥杉仍不可燃。
- **装备说明精简**：显示非定量能力、已用／总槽位和每行两颗晶核；重复晶核按原槽位顺序保留，序数晶核等级正确显示，图腾不标等级。长能力摘要按字体自动换行；详细数值留在强化台，原版附魔、自定义名称与物品数据不改。[0.6.15记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0615.md)
- **iBLP精选1000种**：从14661条中按尺寸与形态选留完整记录，覆盖1～69行；12种小图和988种野外候选，步长、标记和表达式不改。1070条BMS和四种固定宫殿图案保持不变。目录JSON约18.0MB降至1.86MB，其压缩项比裁库前减少926503字节；完整母本保留于源码离线参考，不进入游戏JAR。[0.6.16记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0616.md)

## 下载与安装

| Minecraft | Java | 文件 |
|---|---|---|
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.7.0.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.7.0.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.7.0.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.7.0.jar` |

选择匹配游戏版本的一份JAR，搭配对应Fabric Loader和Fabric API；客户端与服务器同步更新。附件包含四版JAR、对应发布标签的完整源码、版本清单和SHA-256校验和。JEI不是必需依赖。

## 验证与兼容边界

0.6.12～0.6.16各阶段均已完成四版构建／包检查；原生实测以26.3为主、26.2作重点回归，覆盖挖掘与菜单所有权、实际弓姿态和塑形台模型、灯带与4096列地府底座、精准与木机关、完整装备提示，以及iBLP实际生成／播放和旧桌子开档。旧区块和旧桌子的磁盘升级检查是限定样例，不是任意完整存档认证。**这些是对应候选版本的已完成验证，不计作0.7.0全套重测。**

本轮另以精确0.7.0 JAR作26.3／26.2全新世界重点回归，范围为记号库、自然桌生成与现场演奏，结果见[0.7.0发布记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-070.md)。本轮不重跑其余功能整套或旧档升级；1.21.x为构建／静态覆盖，未新增Connector、Voxy或混版本联机认证。

- 保留现有注册ID、旧数据读取规则及fffZ／FOS、ψ／Z稳定机制；已生成区块不重写。
- 新地府基岩底座与新树灯带只影响新生成内容，不会自动改造旧地形。
- iBLP同尺寸的变体选择会变化。完整旧桌子的原有方块和现场演奏保留；若一张桌子横跨升级前后才生成的区块，接缝两边的点阵、标记或步长可能不同。

许可仍为`GPL-3.0-only WITH GPL-3.0-linking-exception`，保留Minecraft链接例外、第三方许可与来源说明。**0.5.4.1的原始修改由Alice（AliceLdl）完成**；其原始工作与本仓库还原、移植和适配分别署名。

## English

Version 0.7.0 collects the completed 0.6.12–0.6.16 changes since the published v0.6.11. The release source is the merged `main` branch. It rebuilds the verified 0.6.16 content under the new version number, without additional gameplay or balance changes.

Thanks to @8266 and others for helping test.

### Changes

- **Editable mining patterns:** configure a vertical 5×5 pattern in the enhancement station, with one reference cell and selected extra cells. World-up stays vertical; horizontal orientation is locked when mining starts. Only selected positions are attempted. Matrix grades 1/2/3 grant 1/2/4 extra-cell budget in every realm; repeated cores use root-sum-of-squares rounded down. A changed budget selects its default pattern; an unchanged budget preserves custom edits. Sneak bypass, permissions, drops, experience and wear checks remain.
- **Bow and Lamp Shaper visuals:** repair ordinal-bow draw symmetry, arrow movement and native first-person hand selection. Third-person poses continue to use native bow animation. Make the Shaper's right die square, remove overlapping internal faces and drop the redundant sneak tooltip.
- **Tree lights and Underworld bedrock:** plan whole continuous strands on natural Astra/Underworld trees with spacing between strands and ornaments. Preserve legal number sequences and tree geometry. Newly generated Underworld chunks receive a five-layer, Overworld-style randomized bedrock floor connected to motherrock.
- **Silk Touch and wooden mechanisms:** align core Silk mode with native decorated-pot, ice and occupied-hive decisions. Actual native enchantments, container contents and experience rules remain intact. Restore native shape properties, support/piston behavior, instruments and tags for Laver/Dread Fir mechanisms; retain their item identities and nonflammable Dread Fir behavior.
- **Compact equipment tooltips:** show discrete abilities, used/total sockets and two installed cores per row. Preserve duplicates and order, correct Ordinal Core grades and ungraded totems. Long capability summaries wrap using the native font. Detailed numeric previews remain in the station; actual enchantments, custom names and saved components are unchanged.
- **1,000 selected iBLP patterns:** select whole records by size and shape from the 14,661-entry pool, covering 1–69 rows with 12 small patterns and 988 wild candidates. Keep their cells, marks, steps and expressions. All 1,070 BMS records and four fixed palace patterns remain unchanged. Catalog JSON falls from about 18.0 MB to 1.86 MB; its compressed entry saves 926,503 bytes. The full source pool remains an offline reference in source, outside runtime JARs.

### Installation and scope

Use one matching 0.7.0 JAR from the table above: Java 21+ for 1.21.1/1.21.11, Java 25+ for 26.2/26.3, with matching Fabric Loader/API. Update both client and server; JEI is optional. Release assets include four binaries, complete corresponding source, a manifest and SHA-256 checksums.

Each 0.6.12–0.6.16 stage passed four-target build/package checks. Native testing primarily used 26.3 with focused 26.2 regressions: mining/menu ownership, bow poses and Shaper geometry, lights and 4,096 Underworld floor columns, Silk/wood behavior, complete tooltips, and actual iBLP generation/playback plus a saved-table upgrade. These are bounded historical candidate results, **not a full 0.7.0 rerun**. See [0.6.12](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0612.md), [0.6.13](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0613.md), [0.6.14](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0614.md), [0.6.15](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0615.md) and [0.6.16](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-0616.md) for exact evidence.

The separate exact-0.7.0 native check uses fresh 26.3/26.2 worlds and is limited to the catalog, natural-table generation and live playback; see the [0.7.0 publication record](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.7.0/docs/WORK-070.md) for results. Other feature suites and old-save upgrades are not repeated this round. The 1.21.x targets have build/static coverage; no new Connector, Voxy or mixed-version network certification is claimed.

- Existing registered identities, saved-data reading and fffZ/FOS/ψ/Z stability remain. Generated chunks are not rewritten.
- New bedrock and tree-light placement affect newly generated content only.
- iBLP variants within a size change. Complete old tables keep their physical blocks and live playback. A partly generated table crossing an old/new chunk frontier can acquire different cells, marks or steps across that seam. Focused upgrade samples are not whole-save certification.

License remains `GPL-3.0-only WITH GPL-3.0-linking-exception`, retaining the Minecraft linking permission and third-party/provenance notices. **The original 0.5.4.1 changes were made by Alice (AliceLdl)**; original authorship and this repository's reconstruction/adaptation remain credited separately.
