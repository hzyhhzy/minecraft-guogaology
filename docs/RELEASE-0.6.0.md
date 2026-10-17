# 果糕逻辑 · Guogaology v0.6.0

本次发行汇总自公开版 **v0.5.3** 以来的更新，包含 Alice 0.5.4.1 的原始修改，以及后续晶核资源循环、生存引导、演奏、景观和美术调整。

## 主要变化

- **Alice 0.5.4.1 内容整合。** 表界螺旋竖井、可转向康威链、软糖罐、紫菜掉落与果糕配方调整、可攀爬葛立恒藤蔓、矿物美术及相关26.3适配修正，原始修改均由 **Alice（AliceLdl）** 制作。本仓库完成源码还原、四版本适配与后续整合；还原工作不改变原作者归属。螺旋竖井保留原设计的开放底部，直达世界最低处。详见[贡献者](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/AUTHORS.md)和[来源记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/docs/WORK-0541-IMPORT.md)。
- **晶核改为采料合成。** 里界、地府野外的成品晶核和普通序数晶体由主题原料及三种母岩外观的序数晶矿取代。晶矿在整个岩体高度生成，LHO较稀疏；晶屑可合成普通序数晶体。八座巨构保留固定设计的本系一级晶核装置与家具，主箱提供本系Lv2和序数晶核Lv1，副箱提供一级配方原料；终境巨树八系展台为Lv1。
- **统一固定中央的合成配方。** 八系Lv1采用对称的八材料环，中央为一个普通序数晶体；Lv2为外围八个本系Lv1、中央ε结晶块，Lv3为外围八个本系Lv2、中央Γ结晶块。分枝芽核的四个枝石格可独立选用六种枝石，允许混搭或重复；空无晶核的四个碎片格也可独立混用fffZ/FOS。序数晶核仍是纯九合一及反向拆分。完整材料与位置见[生存指南](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/docs/SURVIVAL-GUIDE.md#晶核获取与融合)。
- **矿业与强化规则调整。** 四档护甲/韧性总和为15/0、20/8、24/12、30/16，装备矿物分布和结晶块采集要求同步调整。九类显示Lv1/2/3的晶核固定挖掘100/200/400游戏tick（20TPS时5/10/20秒），不受工具、附魔或手稿加速；无等级序数晶体仍可用镐加速，任意工具均完整掉落。数字及序数装备支持适用的原版附魔，但实际附魔与晶核/图腾镶嵌互斥，已有晶核不会被静默删除。
- **完整生存引导。** 新世界自动显示模组目录；真实首次入界、材料、晶核融合、强化、食物、八群系与巨构探索分别有引导。八群系路线按材料→Lv1→Lv2→Lv3展开，使用代表景物图标，完整保留原版进度。中英文名称、提示和指南同步。
- **紫菜桌逐行弹奏。** 右键镶边石，读取六个正交方向中最长的“连续桌格→矩阵数字砖”行，每格5tick；空白格是休止，改变终点数字会改变旋律，可手持物品操作。遇到未加载区块时取消，断行、编辑或卸载会安全停止。宫殿原有图案桌和晶核位置保留。
- **果糕灯塑形与掉落。** 塑形台提供54种形态；普通掉落保留颜色和馃槹/馃槬表情、清除分块状态，精准采集保留完整形态。右键只切换表情。新生成的3×3×3灯中心为空气，外侧26块不变；大型马赛克球保留。

## 景观与美术

- 核爆原料改为带清晰核爆纹样的青绿/象牙白完整方块，名为“核爆晶块”；保留`cloud_pearl`注册ID。Astra蘑菇云完全包裹的内部方块按1%机会生成该原料，扭结中心也提供原料；普通办公室和路灯分别采用震惊瘫坐椅、星芒琉璃。
- 一级核爆之心及同系三阶背包图标恢复0.5.12重做前的原始造型；二三级世界模型保留。SCG结点采用鲜明玫紫与金白三叉纹样，和青绿核爆原料清楚区分。编织晶瓷保留现有64阶循环渐变。
- 新增原料“馃槹”，藏于果糕树星星内部和终境巨树外挂表情球中心。它使用手绘原生32×32纹理、六面完整表情、轻微单级切角和原生亮度15。集合膜则采用实际立体的三组紫白正交膜及匹配纹理。
- Astra正长及云下倒挂圣诞树固定直轴、不歪斜、不分叉，材质与灯饰沿用地府系列；地府原有树形保留。葛立恒植株可在里界、地府自然生长或用骨粉催生成原有葛立恒树。
- 野外LHO的ψ/Z树及fffZ/FOS碎片提高至约原先两倍密度，L/H/O、其他景观及特殊机制保留。普通序数晶体的5×5×5稳定范围、玩家放置保护及天然材料的消失/连锁规则仍有效；新建筑中的天然样品不再预设永久稳定。
- 移除后续开发版新增的箭纹结、紫菜胶质和冥杉树脂三种原料，新的布景改用现有主题材料；原有`guogao_heart_resin`装饰不在移除范围。三者均不存在于Alice原始0.5.4.1中。

## 下载与安装

| Minecraft | Java | 独立Fabric JAR |
| --- | --- | --- |
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.6.0.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.6.0.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.6.0.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.6.0.jar` |

只安装对应游戏版本的一个主模组JAR，并使用对应版本的Fabric Loader与Fabric API。升级时移走旧版主模组；客户端、服务端须使用相同版本。Voxy、分页进度、独立晶核展厅和飞行调速工具均不是必要依赖。附件包括四版JAR、SHA-256清单、依赖说明及完整对应源码。

## 存档兼容与验证边界

- **兼容审计的明确目标是Minecraft26.3的Alice 0.5.4.1存档。** 已核对该原始包的注册ID、维度、结构、装备组件及持久化格式；后续删除的三种原料并非其内容。静态审计预计可直接升级，但没有用真实26.3旧档完成加载、保存和重进验证，不能视为已确认的旧档迁移。升级前保留完整备份；不支持把26.3世界降到26.2打开。详见[兼容审计](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/docs/SAVE-COMPAT-0541-0510.md)及[后续补充](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/docs/SAVE-COMPAT-0541-0512.md)。
- 旧区域不会自动补矿、换原料、重建景观或清理灯芯；新生成内容采用新规则。已经保存的箱内物品保留，尚未生成内容的战利品箱使用当前奖励。现有装备属性、合成、采集和新材质会按当前规则更新，部分新进度需要再次触发。中间开发版被移除的三种原料不提供自动转换。
- 0.5.0以前的旧命名空间不提供迁移；若要回退版本，请恢复升级前的整份世界备份，而非只换回旧JAR。
- 本开发周期的实际游戏与画面检查限于隔离的**Minecraft26.2原生Fabric**，包括Sodium/Voxy及原版/分页进度界面。其他三版以编译和资源检查为范围。没有为0.6.0重新进行1.21.1 Connector运行测试，历史0.5.3测试不代表本版已重新验证。
- 0.5.5曾完成26.2本机回环专用服务器、真实客户端连接、保存重启与Voxy WorldGen功能检查；这些是当时的有限场景记录，不是0.6.0新完成的专服认证，也不代表多人容量、外网或Linux性能保证。

### 0.6.0最终验证 / Final release validation

四个版本均通过构建与打包检查，74项Python测试通过；2,378项资产无缺失或孤立引用，每种语言的642个键无翻译或参数错误。逐项递归比较发行JAR（包括1.21.1嵌套桥接包），确认相对0.5.16仅版本元数据和桥接包文件名/引用变化，所有class、assets、data内容一致。

最终0.6.0 JAR在隐藏独立fo262中搭配Sodium/Voxy通过Astra、紫菜演奏、景观及原生碰撞/光照定向检查，记录`ASTRA516_RUNTIME_OK`与`PRODUCTION_PORT_OK 26.2`；SCG、直立树和倒挂树三张原生截图均已检查。这不是全量生存流程或新的专服、Connector验证。

All four targets pass build/package validation, alongside 74 Python tests. The 2,378 assets have no missing/orphan references, and all 642 keys per locale pass translation/parameter checks. Recursive release-JAR comparison, including the nested 1.21.1 bridge, confirms that only version metadata and bridge filenames/references differ from 0.5.16; all class, asset and data contents are identical.

The final 0.6.0 JAR passes focused Astra, Laver playback, scenery and native collision/light checks in an isolated hidden Fabric 26.2 client with Sodium/Voxy, recording `ASTRA516_RUNTIME_OK` and `PRODUCTION_PORT_OK 26.2`. All three native screenshots of SCG, upright and hanging firs were inspected. This is not a complete survival playthrough or a new dedicated-server/Connector validation.

许可为`GPL-3.0-only WITH GPL-3.0-linking-exception`；Alice及其他第三方内容保留各自版权和许可声明。

## English

This release collects the changes since public **v0.5.3**, including the original 0.5.4.1 work by **Alice (AliceLdl)** and the subsequent resource, survival-guide, scenery and artwork updates.

### Highlights

- **Alice 0.5.4.1 integration:** Outer spiral shafts, directional Conway Links, Gummy Jars, revised Laver drops and Guogao recipes, climbable Graham Vines, mineral artwork and 26.3 compatibility corrections. Original authorship remains with Alice; this repository reconstructed maintainable source and adapted it to four Minecraft versions. Shafts retain their original open bottom at the world minimum. See [contributors](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/AUTHORS.md) and [provenance](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/docs/WORK-0541-IMPORT.md).
- **Gather materials and craft cores.** Wild finished regional cores and ordinary Ordinal Crystals in the Inner world and Underworld are replaced by thematic materials and Ordinal Ore with three host-rock appearances. Ore spans the rock's height range and is rarer in LHO; shards craft ordinary crystals. Landmarks retain authored Lv1 core devices and furnishings. Main chests give a regional Lv2 and Ordinal Core Lv1, secondary chests give Lv1 ingredients, and the final eight-family displays contain Lv1 cores.
- **Centered recipes:** every regional Lv1 uses eight symmetrically arranged materials around one ordinary crystal. Lv2 uses eight matching Lv1 cores around an Epsilon Block; Lv3 uses eight Lv2 cores around a Gamma Block. Each of the four Branch-stone slots independently accepts any of six stones, including repeats; each of the four Absence-fragment slots independently accepts fffZ or FOS. Ordinal fusion and splitting remain pure ninefold recipes. See the [survival guide](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.0/docs/SURVIVAL-GUIDE.en.md).
- **Mining and equipment:** armor/toughness totals are 15/0, 20/8, 24/12 and 30/16. Ore distribution and storage-block mining requirements are adjusted. Displayed Lv1/2/3 cores always take 100/200/400 game ticks to mine; ordinary ungraded crystals retain pickaxe acceleration and drop intact with any tool. Applicable native enchantments are supported on numeric and ordinal equipment, but enchantments and core/totem sockets are mutually exclusive without silently deleting existing cores.
- **Survival guidance:** an automatically visible directory leads through actual dimension entry, materials, crafting, socketing, food, all eight biome routes and landmark exploration. Vanilla advancements remain intact; Chinese and English guides and text are synchronized.
- **Laver row playback:** right-click an Inlay Stone to play the longest straight row ending in a Matrix Number Brick, in any of six orthogonal directions. Each cell lasts 5 ticks; blank cells are rests and the final number changes the melody. Held items are allowed. Unloaded chunks cancel attempts without loading them; edits, breaks and unloading stop playback safely. Palace tables and core positions are preserved.
- **Lamp shaping and drops:** 54 forms, color/expression-preserving ordinary drops, and exact-form Silk Touch drops. Right-click changes only the Guogao/Guogua expression. Newly generated 3×3×3 lamps have an air center and the same 26 exterior blocks; large mosaic globes remain.

### Scenery and art

The Nuclear Blast Block is a full teal/ivory radiation-emblem cube retaining the `cloud_pearl` ID. It occurs in 1% of fully enclosed eligible Astra cloud voxels and in knot centers. Ordinary offices use Shock-Slump Chairs and streetlights use Starburst Glass. Astra Lv1 and all three family inventory icons restore the original pre-0.5.12 design; higher-grade world models remain. SCG Junction now uses vivid rose-purple with gold/ivory, clearly distinct from the nuclear material. Braided Crystal Porcelain retains its existing 64-step cyclic gradient.

The new Guogao material is concealed inside Guogao tree stars and the giant exterior emoji globes. It has authored 32×32 artwork, six complete faces, a slight single-step corner cut and native light 15. Set Membranes retain three-dimensional violet/ivory orthogonal panels with matching textures. Upright and cloud-hanging Astra firs have a fixed straight axis without crooked or forked forms; Underworld tree variants remain unchanged. Graham plants can grow naturally or with bone meal into their existing trees in the Inner world and Underworld.

Wild LHO ψ/Z trees and fffZ/FOS fragments have roughly doubled density. L/H/O, other scenery, the crystal's 5×5×5 stabilization field, player-placement protection and natural disappearance/chaining rules remain. New building specimens no longer receive forced permanent stability. The later development materials Arrowgrain Knot, Laver Gel and Dread Fir Resin are removed, with new scenery using existing thematic replacements. None existed in Alice's original 0.5.4.1; the separate old `guogao_heart_resin` decoration is preserved.

### Installation and compatibility

Use exactly one matching **Fabric JAR for 1.21.1, 1.21.11, 26.2 or 26.3**, with the matching Fabric Loader and Fabric API. Java 21+ is required for 1.21.x and Java 25+ for 26.x. Remove the previous main-Mod JAR; clients and servers must match. Voxy, Paginated Advancements and the standalone gallery/flight tools are optional. The release includes four binaries, checksums, dependency information and matching complete source.

The explicit compatibility target is **Alice's Minecraft 26.3 / 0.5.4.1 save format**. Static audits preserve its IDs, dimensions, structures and persistent equipment data, but no real 26.3 old save has been loaded, saved and reopened in this development cycle. Back up the whole world first; do not downgrade a 26.3 world to 26.2. Existing chunks are not regenerated or automatically converted. Generated chest contents remain, while unopened loot tables use current rewards; equipment, crafting, mining and appearances follow current rules. New advancements may require another trigger. Removed intermediate-development materials have no automatic conversion. Pre-0.5.0 namespaces have no migration, and rollback requires restoring the complete pre-upgrade backup.

Actual gameplay and visual checks in this development cycle are limited to isolated **native Fabric 26.2**, including Sodium/Voxy and native/paginated advancement interfaces. The other targets are limited to build/resource checks. **No new 1.21.1 Connector runtime test is claimed for 0.6.0.** Local 26.2 dedicated-server, real-client, restart and Voxy WorldGen checks were performed with 0.5.5; they are historical functional evidence, not a fresh 0.6.0 certification or a multiplayer, Internet or Linux performance guarantee.

License: `GPL-3.0-only WITH GPL-3.0-linking-exception`. Alice's attribution and all applicable third-party notices remain in force.
