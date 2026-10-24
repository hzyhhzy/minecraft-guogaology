# 果糕逻辑 · Guogaology v0.6.11

更新弓、幂塔晶核与手稿的攻击数值，并包含0.6.10的13项修复／采集规则调整。强化界面、模拟器及晶核说明表同步更新。本版从`codex/0.6.11-combat-balance`发布，未合并到`main`。

感谢@8266等人帮忙测试

## 攻击调整

以下为各级单颗数值。「普通维度」包括主世界、下界、末地和表界；「深层」为里界与地府。

| 项目 | 普通维度 | 深层 |
|---|---|---|
| 四档弓箭初速 | 原版的1／1.3／1.6／2倍 | 相同 |
| 四档弓基础箭伤系数 | 均为2 | 均为2 |
| 弓幂塔 Lv1／2／3 | 箭伤系数＋0.5／1／1.5 | 系数增幅＋50%／100%／200% |
| 剑幂塔 Lv1／2／3，含数字剑 | 攻击力＋3／6／9 HP | 攻击增幅＋100%／200%／400% |
| 手稿幂塔 Lv1／2／3 | 攻击力＋2／4／6 HP | 攻击增幅＋50%／100%／200% |
| 四档裸手稿攻击 | ＋1／2／3／4 HP | 增幅＋10%／20%／30%／40% |

同件同类按平方求和开根号；深层对「增幅」计算后再加1。不同来源仍按普通维度加算、深层乘算。**裸手稿只翻倍攻击基础项**，防护、治疗、采掘等基础项不变；序数及其它晶核、弓耐久和附魔／镶嵌互斥规则不变。

弓自身幂塔进入原生箭伤系数，再由实际命中速度、向上取整和原生暴击决定伤害；手稿最后加算／乘算。预览和模拟器对齐原版浮点取整。修正深层满配手稿的攻击上限补偿，避免截断或重复放大，同时保留其它来源原有的裁切。

## 包含0.6.10的修复与规则调整

1. 创造模式可回收免费箭；生存模式仍不能回收无限箭或免费副箭，正常付费主箭保持可回收。
2. 修正四档弓12张蓄力贴图的箭头／箭羽方向。
3. 核爆菇TNT配方接受普通沙、红沙及两者混搭，材料数量不变。
4. 紫菜、冥杉木门使用完整木门物品图标。
5. 新增紫菜、冥杉箱船，使用原版27格容器、保存与毁坏掉落规则。
6. 补齐18项紫菜木燃料时间；冥杉继续不可燃。
7. 一级果糕之心的不透明内容与玻璃分别渲染，修正错误的透明排序通道，保留原几何、贴图和颜色。
8. 紫菜木栅栏接入原版木栅栏连接标签。
9. 两木系普通告示牌无物理碰撞，仍可选中；原版墙挂吊牌横杆的碰撞保留。
10. 新增两木系原版悬挂式／墙挂告示牌，支持正常放置、文字编辑和保存。
11. 告示牌编辑界面采用与正面一致的浅色木纹。
12. 软糖罐精准采集掉落一罐，普通开采仍掉1～3个软糖。
13. 空无晶核三级均可穿过、仍能选中开采；一级原已有此行为，二／三级补齐。

0.6.10中「弓强度偏低」的处理当时被撤回，本版攻击调整来自随后确认的新方案。树苗／藤等掉落物过大的反馈在精确0.6.9与0.6.10原生对照中未复现，没有盲目缩放，也不称为已修复。一级果糕之心确认修正了渲染通道，未声称复现所有用户渲染环境。

## 下载与安装

| Minecraft | Java | 文件 |
|---|---|---|
| 1.21.1 | 21+ | `guogaology-dimension-1.21.1-0.6.11.jar` |
| 1.21.11 | 21+ | `guogaology-dimension-1.21.11-0.6.11.jar` |
| 26.2 | 25+ | `guogaology-dimension-26.2-0.6.11.jar` |
| 26.3 | 25+ | `guogaology-dimension-26.3-0.6.11.jar` |

选择与游戏版本匹配的一份JAR，并安装对应Fabric Loader和Fabric API；客户端与服务器同步更新。JEI可选。附件包含四版JAR、对应标签的完整源码ZIP、manifest、SHA-256校验和及许可声明。

## 验证与兼容边界

四版构建和保存面审计、131项Python及2321项纯Java检查通过。最终26.3主测试和26.2重点回归各通过1590项战斗检查：144次真实发箭、90次近战、48次原生暴击及四维度数值控制，并回归0.6.10修复。模拟器Java／JS对照与77项真实浏览器检查通过。

相较0.6.10，全部230个方块状态、配方、战利品、世界生成和模型贴图保持一致；旧ID、组件codec、箭矢保存格式、LHO稳定和JEI保护类不变。已发出的旧箭保留原伤害快照。0.6.11进行了构造的旧装备／旧箭压缩NBT往返，**未新跑完整旧世界升级**。真实旧世界升级与再次重开、JEI双方不装／双方安装31.14.0.72的两组联机，是0.6.10的历史验证，不计作本版重测。

本轮1.21.x仅构建和静态检查，未重测Connector、Voxy、混版本联机或任意未来JEI结构，不承诺修复任意已损坏存档。所有原生测试使用隔离副本，没有对局域网开放。精确范围、最终JAR哈希和中间失败记录见[0.6.11验证记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.11/docs/WORK-0611.md)；继承修复的证据见[0.6.10记录](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.11/docs/WORK-0610.md)。

许可仍为`GPL-3.0-only WITH GPL-3.0-linking-exception`，保留Minecraft链接例外、第三方许可与来源说明。**0.5.4.1的原始修改由Alice（AliceLdl）完成**，原作者修改与本仓库移植／适配工作分别署名。

## English

Version 0.6.11 updates bow, Power Tower and manuscript attack values, with matching equipment previews, simulator and core reference. It also includes the thirteen repair/policy changes from 0.6.10. This release comes from `codex/0.6.11-combat-balance`; it is **not merged into main**.

### Attack values

Values below are per core/grade. Ordinary dimensions are the Overworld, Nether, End and Outer Guogaology; deep dimensions are Inner Guogaology and the Underworld.

| Channel | Ordinary | Deep |
|---|---|---|
| Four bow launch speeds | 1 / 1.3 / 1.6 / 2 × native bow speed | Same |
| Native arrow damage coefficient | 2 for every bow tier | Same |
| Bow Tower Lv1 / 2 / 3 | Coefficient +0.5 / 1 / 1.5 | Coefficient increase +50% / 100% / 200% |
| Sword Tower, including Numeric Swords | Attack +3 / 6 / 9 HP | Attack increase +100% / 200% / 400% |
| Manuscript Tower Lv1 / 2 / 3 | Attack +2 / 4 / 6 HP | Attack increase +50% / 100% / 200% |
| Four bare manuscript tiers | Attack +1 / 2 / 3 / 4 HP | Attack increase +10% / 20% / 30% / 40% |

Repeated cores within one item use root-sum-of-squares; deep aggregation applies to increases before adding 1. Separate sources retain ordinary addition/deep multiplication. Only innate manuscript **attack** doubles: protection, healing, mining and other innate channels stay unchanged. Ordinal/other cores, bow durability and the enchantment/socket exclusion remain unchanged.

Bow Tower modifies the native arrow coefficient before impact speed, rounding and criticals; manuscripts apply afterward. Preview/simulator rounding matches native float behavior. Local melee overflow handling restores clipped manuscript contributions without double compensation or removing other sources' existing clamps.

### Included 0.6.10 changes

1. Creative players can recover free arrows; survival players cannot recover infinite/free side arrows, while paid central arrows remain recoverable.
2. Correct arrowhead/fletching direction in all twelve bow draw textures.
3. Nuclear Mushroom TNT accepts sand, red sand and mixtures at unchanged quantities.
4. Both custom wooden doors use whole-door inventory icons.
5. Add Laver and Dread Fir chest boats with native 27-slot containers, persistence and destruction drops.
6. Add eighteen Laver fuel entries; Dread Fir remains nonflammable.
7. Separate Guogao Heart Lv1's opaque content from glass rendering, retaining its geometry, pixels and colors.
8. Connect Laver fences through native wooden-fence tags.
9. Ordinary custom signs have no physical collision but remain selectable; native wall-hanging support-bar collision stays.
10. Add both woods' native hanging/wall-hanging signs with text editing and persistence.
11. Match the sign editor to the lighter front-face wood grain.
12. Silk Touch collects one Gummy Jar; ordinary mining still drops one to three gummies.
13. All Absence Core grades are pass-through and selectable; Lv1 already behaved this way, and Lv2/3 now match.

Bow balance was deferred in 0.6.10; the new values implement the later approved proposal. Oversized sapling/vine drops were not reproduced in exact 0.6.9/0.6.10 native controls and were not blindly resized or declared fixed. The confirmed Heart render-channel defect was corrected without claiming reproduction of every reported rendering environment.

### Installation and validation

Use one JAR from the four-version table above: Java 21+ for 1.21.1/1.21.11, Java 25+ for 26.2/26.3, with matching Fabric Loader/API. Update both client and server; JEI is optional. Attachments include all four JARs, complete source from the release tag, manifests, checksums and license notices.

Four builds/save-surface audits, 131 Python and 2,321 pure Java checks pass. Final native 26.3 and focused 26.2 runs each pass 1,590 combat checks, including 144 real arrows, 90 melee attacks and 48 native criticals, plus the 0.6.10 repair regressions. Simulator Java/JS comparisons and 77 real-browser checks pass.

All 230 blockstates, recipes, loot, worldgen, models and textures remain identical to 0.6.10; saved IDs, codecs, arrow payloads, LHO stability and JEI guard classes remain unchanged. Existing arrows retain their saved damage snapshots. This version tests constructed old-item/arrow compressed NBT roundtrips, **not a new whole old-world upgrade**. Real old-world upgrade/reopen and the two neither-end/both-ends JEI 31.14.0.72 runs are historical 0.6.10 results, not repeated 0.6.11 certification.

The 1.21.x targets have build/static coverage only this round. Connector, Voxy, mixed Mod-version networking and arbitrary future JEI rewrites were not retested; arbitrary damaged saves are not promised recoverable. Native checks use isolated instances, never exposed to the LAN. See the [0.6.11 validation record](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.11/docs/WORK-0611.md) and [0.6.10 historical evidence](https://github.com/hzyhhzy/minecraft-guogaology/blob/v0.6.11/docs/WORK-0610.md).

License: `GPL-3.0-only WITH GPL-3.0-linking-exception`, retaining the Minecraft linking permission and third-party/provenance notices. **The original 0.5.4.1 changes were made by Alice (AliceLdl)**; original authorship and this repository's reconstruction/adaptation work remain credited separately.
