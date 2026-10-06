#!/usr/bin/env python3
"""Render the production revision47 audit as an offline bilingual report."""
from pathlib import Path
import base64
import html
import json
import math

ROOT = Path(__file__).resolve().parents[1]
DATA = json.loads((ROOT / "build/enhancement-simulator/proposal-audit.json").read_text(encoding="utf-8"))
if DATA["rules"]["revision"] != 47:
    raise SystemExit("Regenerate the production revision47 audit before rendering.")


def esc(value):
    return html.escape(str(value))


def number(value):
    if not isinstance(value, (int, float)) or not math.isfinite(value):
        return "—"
    return f"{value:,.3f}".rstrip("0").rstrip(".")


def link(record, lang, selected="mainhand", critical=False):
    config = {"version": 1, "lang": lang, "mode": "set", "selected": selected,
              "snapshot": record["configuration"], "combat": {"critical": critical, "cooldown": 1}}
    data = base64.b64encode(json.dumps(config, ensure_ascii=False, separators=(",", ":")).encode()).decode()
    return "enhancement-simulator.html#config=" + data


def configuration(record, lang, only_book=False):
    labels = ["剑／镐", "手稿", "头", "胸", "腿", "靴"] if lang == "zh" else ["Tool", "Book", "Head", "Chest", "Legs", "Feet"]
    names = DATA["rules"]["coreStageNames"][lang]
    parts = []
    for label, key in zip(labels, ["mainhand", "offhand", "helmet", "chestplate", "leggings", "boots"]):
        if only_book and key != "offhand":
            continue
        counts = {}
        for core in record["configuration"][key].get("cores", []):
            name = names[core["type"]][core["level"] - 1]
            counts[name] = counts.get(name, 0) + 1
        if counts:
            parts.append(label + ": " + "; ".join(f"{count}×{name}" for name, count in counts.items()))
    return "<br>".join(esc(part) for part in parts)


def table(headers, rows):
    return '<div class="table-wrap"><table><thead><tr>' + ''.join('<th>' + esc(v) + '</th>' for v in headers) + '</tr></thead><tbody>' + ''.join('<tr>' + ''.join('<td>' + str(v) + '</td>' for v in row) + '</tr>' for row in rows) + '</tbody></table></div>'


def section(lang):
    zh = lang == "zh"
    tr = lambda a, b: a if zh else b
    realms = ["normal", "deep"]
    realm_names = {"normal": tr("表界／原版维度", "Outer / vanilla realms"), "deep": tr("里界／地府", "Inner / Underworld")}
    metrics = [
        ("maximumHealth", tr("最大生命", "Maximum health"), "HP", "offhand", 1),
        ("maximumAttack", tr("最大直接攻击（无暴击、爆裂）", "Maximum direct attack (no crit / burst)"), "HP", "mainhand", 1),
        ("maximumHitWithBurst", tr("普通命中＋剑爆裂", "Ordinary hit + sword burst"), "HP", "mainhand", 1),
        ("maximumCriticalHitWithBurst", tr("暴击＋剑爆裂", "Critical hit + sword burst"), "HP", "mainhand", 1),
        ("maximumProtection", tr("最大额外防护 F", "Maximum extra protection F"), "×", "chestplate", 1),
        ("maximumProtection", tr("固定80%口径总防护 5F", "Total protection 5F at fixed80%"), "×", "chestplate", 5),
        ("maximumSingleHitThreshold", tr("生命＋防护联合致死阈值 5HF", "Joint lethal threshold 5HF"), "HP", "offhand", 1),
        ("maximumPeriodicHealing", tr("每4秒紫菜周期治疗", "Laver healing every4 seconds"), "HP", "offhand", 1),
        ("maximumGuogaoDuration", tr("果糕剑负面状态最长持续时间", "Longest Guogao sword debuff duration"), tr("秒", "s"), "offhand", 1),
        ("maximumWearProtection", tr("单件省耐久因子 W", "Single-item wear factor W"), "×", "mainhand", 1),
        ("maximumExtraBlocks", tr("每次额外开采", "Extra blocks per operation"), tr("格", "blocks"), "mainhand", 1),
        ("maximumReach", tr("额外触距", "Additional reach"), tr("格", "blocks"), "offhand", 1),
        ("maximumMiningSpeed", tr("匹配方块挖速系数", "Matching-block mining coefficient"), "", "mainhand", 1),
    ]
    metric_rows = []
    for key, label, unit, selected, multiplier in metrics:
        cells = [esc(label)]
        for realm in realms:
            record = DATA[realm][key]
            cells.append(f'<a href="{esc(link(record, lang, selected, key == "maximumCriticalHitWithBurst"))}">{number(record["value"] * multiplier)} {esc(unit)}</a>')
        metric_rows.append(cells)
    config_rows = []
    for key, label in [("maximumAttack", tr("最大直接攻击", "Direct attack")), ("maximumProtection", tr("最大防护", "Protection")), ("maximumSingleHitThreshold", tr("最大承伤", "Single-hit capacity")), ("maximumPeriodicHealing", tr("最大周期治疗", "Healing"))]:
        config_rows.append([label] + [configuration(DATA[r][key], lang) for r in realms])

    objective_rows = []
    for realm in realms:
        for key, label in [("attackDefense", "A×F"), ("attackDefenseHealth", "A×F×H")]:
            r = DATA["objectives"][realm][key]
            objective_rows.append([realm_names[realm], label, f'<a href="{esc(link(r, lang))}">{configuration(r, lang, True)}</a>', number(r["effects"]["attack"]), number(r["effects"]["protectionFactor"]), number(5 * r["effects"]["protectionFactor"]), number(r["effects"]["actualMaxHealth"]), number(r["value"])])
    pvp_rows = []
    for example in DATA.get("balancedPvp", []):
        pvp_rows.append([realm_names["deep" if example["deep"] else "normal"], f'<a href="{esc(link(example, lang))}">{configuration(example, lang, True)}</a>', number(example["hitDamage"]), str(example["noHealingHits"]), number(example["criticalHitDamage"]), str(example["criticalWithPeriodicHealingHits"])])

    lower_rows = []
    for realm in realms:
        high = DATA[realm]["maximumProtection"]
        low = DATA["lowerGradeProtection"][realm]
        lower_rows.append([realm_names[realm], number(5 * low["value"]), configuration(low, lang, True), number(5 * high["value"]), configuration(high, lang, True)])

    native_rows = []
    for row in DATA["nativeArmorTable"]:
        native_rows.append([["ω", "ε", "Γ", "Ω"][row["tier"] - 1], str(row["armor"]), str(row["toughness"])] + [number((1 - d["hp"] / d["raw"]) * 100) + "%" for d in row["damage"][:4]])

    book_rows = []
    for tier in range(1, 5):
        r = DATA["rules"]
        book_rows.append([["ω", "ε", "Γ", "Ω"][tier - 1], number(r["bookInnateAttackHp"][tier]) + "HP", number(100 * r["bookInnateAttackProtection"][tier]) + "%", number(100 * r["bookInnateHealing"][tier]) + "%", number(100 * r["bookInnateDuration"][tier]) + "%"])
    ordinal_rows = []
    r = DATA["rules"]
    for i in range(4):
        ordinal_rows.append([r["coreStageNames"][lang][8][i], number(r["normalOrdinalAttackGrades"][i]) + "HP", number(r["ordinalPercentGrades"][i] * 100) + "%", number(r["ordinalHealingGrades"][i] * 100) + "%", number(r["ordinalDurationGrades"][i] * 100) + "%"])
    ranged_rows = []
    for example in DATA["rangedExamples"]:
        shot = example["shot"]
        ranged_rows.append([realm_names["deep" if example["deep"] else "normal"], ["ω", "ε", "Γ", "Ω"][example["tier"] - 1],
                            number(shot["launchSpeed"]), str(shot["vanillaBase"]),
                            f'<a href="{esc(link(example, lang))}">{number(shot["directMinimum"])}–{number(shot["directMaximum"])} HP</a>', str(shot["durability"])])
    examples = DATA.get("protectionCounterexamples", [])
    monotonic_rows = []
    for example in examples[:8] + [e for e in examples if e["targetPiece"] == "offhand"]:
        before, after = example["emptySocket"], example["addBoundary"]
        monotonic_rows.append([realm_names["deep" if example["deep"] else "normal"], esc(example["targetPiece"]), f'<a href="{esc(link(before, lang))}">{number(before["protectionFactor"])}</a>', f'<a href="{esc(link(after, lang))}">{number(after["protectionFactor"])}</a>', tr("通过", "Pass")])

    mining_rows = []
    for tier in range(1, 5):
        cells = [["ω", "ε", "Γ", "Ω"][tier - 1]]
        for realm in realms:
            record = next(v for v in DATA["miningTierComparisons"] if v["configuration"]["deep"] == (realm == "deep") and v["detail"]["toolTier"] == tier and v["detail"]["manuscriptTier"] == tier)
            cells.append(f'<a href="{esc(link(record, lang))}">{number(record["value"])}</a>')
        mining_rows.append(cells)
    wear_rows = []
    for key in ["helmet", "chestplate", "leggings", "boots"]:
        cells = [key]
        for realm in realms:
            record = DATA["jointArmorWear"][realm][key]
            cells.append(f'<a href="{esc(link(record, lang, key))}">{number(record["value"])}×</a><br>F={number(record["detail"]["protectionFactor"])}; W={number(record["detail"]["ownWearFactor"])}')
        wear_rows.append(cells)
    v = DATA["vanillaComparison"]
    compare_rows = [[tr("直接攻击／下界合金锋利V", "Direct attack / Netherite SharpnessV"), number(v["attackRatio"]["normal"]) + "×", number(v["attackRatio"]["deep"]) + "×"], [tr("固定80%最大承伤／四件保护IV", "Fixed80% hit capacity / four ProtectionIV"), number(v["fixed80ThresholdRatio"]["normal"]) + "×", number(v["fixed80ThresholdRatio"]["deep"]) + "×"]]
    return f'''<section class="language" data-lang="{lang}">
<h1>{tr("强化系统 · 数值审计", "Enhancement system · Numerical audit")}</h1>
<p class="notice">{tr("0.3.12生产规则revision47。每行独立求最优，不能把不同配装的上限相乘。攻击上限表审计剑；弓另用速度伤害示例，不假定固定基础HP。", "Production0.3.12 rules, revision47. Each row is independently optimal; peaks from different loadouts cannot be multiplied. Attack maxima use swords; bows have separate velocity-based examples with no fixed baseHP.")}</p>
<nav><a href="enhancement-simulator.html">{tr("强化模拟器", "Simulator")}</a><a href="core-effects-reference.html?lang={lang}&realm=both&levels=1,2,3,4">{tr("九核作用与逐级数值", "Core effects and grade values")}</a></nav>
<article><h2>{tr("最高档合法配装的上限", "Legal highest-tier maxima")}</h2>
<p>{tr("Ω装备每件8槽、手稿6槽；群系晶核Lv3／序数晶核Lv3。序数晶体与晶核只能放手稿。攻击值为原始HP；防护F仅是晶核与手稿的额外因子，不包含裸甲。", "Omega gear has8 sockets each; manuscript6, regional Core Lv3 / Ordinal Core Lv3. Ordinal is manuscript-only. Attack is rawHP. F is only extra core/manuscript protection, excluding bare armor.")}</p>
{table([tr("指标", "Metric"), realm_names["normal"], realm_names["deep"]], metric_rows)}
<p>{tr("固定80%口径是用户指定的数学对比：裸甲减伤80%相当于5倍，所以总防护为5F，单次原始致死阈值为5HF。等于阈值会耗尽生命，必须略小才存活。真实模拟器仍按原版护甲与韧性计算，不能把此假设当作所有伤害的实际减伤；虚空、摔落等不吃原版护甲的伤害也没有这个5倍。", "The requested fixed80% convention is a comparison assumption: bare armor contributes5×, total protection is5F, and lethal input is5HF. Equality exhausts health; survival needs less. The simulator still calculates native armor/toughness. This assumption is not actual mitigation for all hits, or for armor-bypassing sources such as void/fall.")}</p>
<details><summary>{tr("最优配装详情", "Optimal loadout details")}</summary>{table([tr("目标", "Metric"), realm_names["normal"], realm_names["deep"]], config_rows)}</details>
<h3>{tr("仅使用群系晶核Lv2／序数晶核Lv2", "Using regional Core Lv2 / Ordinal Core Lv2 only")}</h3>
<p>{tr("此表保留Ω装备和Ω手稿的8／6槽，只降低镶嵌晶核等级，并非ε档装备。", "This keepsOmega gear/book with8 /6 sockets and lowers only the socketed core grades; it does not useEpsilon gear.")}</p>
{table([tr("环境", "Realm"), tr("二阶总防护5F", "Lv2 total5F"), tr("二阶手稿", "Lv2 book"), tr("三阶总防护5F", "Lv3 total5F"), tr("三阶手稿", "Lv3 book")], lower_rows)}
</article>
<article><h2>{tr("攻击、防护与生命联合最优", "Joint attack, protection and health optima")}</h2>
<p>{tr("共同装备为剑8幂塔、各件甲8界限，仅枚举手稿的幂塔／界限／序数／果糕配比。目标里的F是额外防护；固定80%总因子使目标乘5，不改变最优配比。A×F×H不包含治疗、爆裂、负面状态或功能性价值。", "Shared gear is a sword with8 Power and each armor piece with8 Boundary. Enumerate manuscript Power / Boundary / Ordinal / Guogao. ObjectiveF is extra protection; fixed80% multiplies the objective by5 without changing the optimum. A×F×H excludes healing, bursts, status effects and utility.")}</p>
{table([tr("环境", "Realm"), tr("目标", "Objective"), tr("手稿", "Book"), "A", "F", "5F", "H", tr("目标值", "Objective value")], objective_rows)}
<h3>{tr("联合最优配装同配互攻", "Mirrored joint-optimal loadouts")}</h3>
{table([tr("环境", "Realm"), tr("手稿", "Book"), tr("普通命中损血HP", "HP lost ordinary"), tr("普通致死刀数", "Ordinary lethal hits"), tr("暴击损血HP", "HP lost critical"), tr("暴击致死刀数", "Critical lethal hits")], pvp_rows)}
<p>{tr("这里按固定80%裸甲、不计回血、爆裂、盾牌、图腾、走位或网络因素，不是实际游戏TTK。并列最优可能偏攻击或偏防御；上表已列出并列方案。", "Fixed80% bare armor, without healing, bursts, shields, totems, movement or networking. This is not in-gameTTK. Tied optima can favor attack or defense; ties are listed above.")}</p>
</article>
<article><h2>{tr("当前公式与逐级输入", "Current formulas and per-grade inputs")}</h2>
<p>{tr("同一件内同种攻防贡献先平方求和开根号。武器与手稿分开算；四件甲各算RSS后按20%／40%／25%／15%加权相加，禁止跨甲整体RSS或逐件相乘。", "Same-family attack/defense contributions use per-item root-sum-square. Weapon and book remain separate. ComputeRSS per armor piece, then weighted-sum20% /40% /25% /15%; never global armorRSS or per-piece multiplication.")}</p>
<code>{tr("表攻击", "Outer attack")}: A=B+a+P剑HP+P书HP+U书HP<br>{tr("里攻击", "Inner attack")}: A=B(1+b)(1+P剑%)(1+P书%)(1+U书%)<br>S甲=Σwᵢ RSS(界限ᵢ%)<br>F甲=1+2S甲<br>{tr("表手稿防护", "Outer book protection")}: F书=1+b+Q书+U书<br>{tr("里手稿防护", "Inner book protection")}: F书=(1+b)(1+Q书)(1+U书)<br>F额外=F甲×F书; H=20+4RSS(果糕等级)</code>
<p>{tr("表攻击幂塔单颗加1／2／3HP，表攻击序数单颗加0.5／1／1.5／2.5HP；里攻击幂塔为25%／50%／100%，序数为5%／10%／20%／40%。界限防护两界单颗都是25%／50%／100%。没有旧的裸甲本体倍率，没有装备序数乘区。", "Outer Power adds1 /2 /3HP per core; Ordinal adds0.5 /1 /1.5 /2.5HP. Inner Power contributes25% /50% /100%, Ordinal5% /10% /20% /40%. Boundary protection is25% /50% /100% in both realms. Old bare-armor multipliers and gear Ordinal channels are removed.")}</p>
<h3>{tr("四档裸手稿（里外一样的输入）", "Four bare manuscripts (identical inputs across realms)")}</h3>
{table([tr("档位", "Tier"), tr("表攻击a", "Outer attack a"), tr("里攻击／防护b", "Inner attack / protection b"), tr("治疗放大", "Healing amplification"), tr("果糕剑延时", "Guogao extension")], book_rows)}
<h3>{tr("仅手稿序数晶体与晶核", "Manuscript-only Ordinal Crystal and Cores")}</h3>
{table([tr("等级", "Level"), tr("表攻击", "Outer attack"), tr("里攻击／防护", "Inner attack / protection"), tr("治疗量", "Healing amount"), tr("果糕剑延时", "Guogao extension")], ordinal_rows)}
<p><code>{tr("紫菜每4秒治疗", "Laver healing every4s")}=RSS(紫菜等级)×(1+治疗基础+RSS(序数治疗%))<br>{tr("果糕剑秒数", "Guogao sword seconds")}=2×(1+延时基础+RSS(序数延时%))</code></p>
<p>{tr("没有紫菜核就不回血；没有果糕剑就不产生负面状态。序数只保留攻、防、治疗乘算和状态延时，不加生命、挖速、耐久或触距。延时不提高状态等级或失明触发率。", "No Laver means no healing; no Guogao sword means no debuff. Ordinal retains only attack, protection, multiplicative healing and debuff duration. It grants no health, mining, wear or reach. Extension changes neither status potency nor blindness chance.")}</p>
</article>
<article><h2>{tr("弓：速度相关的伤害范围", "Bows: velocity-dependent damage ranges")}</h2>
<p>{tr("示例为同档裸弓＋裸手稿、满弦、碰撞速度等于初速、目标无防护；未加入任何晶核。保留原版箭系数2、ceil取整及满弦随机加伤，飞行阻力和射手速度可在模拟器的碰撞速度字段输入。表界固定HP加成随蓄力比例缩放，里界乘区施加于原版实际箭伤。", "Examples use matching bare bow/manuscript tiers, full draw, impact speed equal to launch speed and an unprotected target; no cores. Vanilla coefficient2, ceil and full-draw random damage remain. Enter impact speed in the simulator to account for drag or shooter motion. Outer fixedHP bonuses scale with charge; inner factors multiply actual native arrow damage.")}</p>
{table([tr("环境", "Realm"), tr("档位", "Tier"), tr("初速 格/刻", "Launch blocks/tick"), tr("原版取整伤害", "Native ceil damage"), tr("含手稿直击范围", "Direct range with manuscript"), tr("基础耐久", "Base durability")], ranged_rows)}
<p>{tr("分枝Lv1无限，Lv2三箭，Lv3每箭最多命中两个生物。三箭不把单目标伤害乘3；同轮同目标最多承受一次直击和一次爆裂。每箭仅首次有效生物命中产生爆裂，第二目标仍可获得果糕状态。特殊箭每轮消耗一支，仅中央保留特殊效果，侧箭不能回收；生存普通箭无限仍须背包有箭，创造空背包可发普通箭。临界核晶禁止放入手稿，防火由界限手稿Lv2／3提供。", "BranchLv1 Infinity, Lv2 three arrows, Lv3 up to two living targets per arrow. Three arrows never triple one target's damage; each target gets at most one direct hit and one burst per volley. Each arrow bursts only on its first valid living hit; its second target can receive Guogao effects. Special ammo consumes one per volley; only the centre keeps special effects and side arrows cannot be picked up. Survival Infinity still requires an arrow; Creative may fire ordinary arrows from an empty inventory. Criticality cannot fit manuscripts; Boundary manuscriptsLv2/3 provide fire resistance.")}</p>
</article>
<article><h2>{tr("真实裸甲与原版对比", "Actual bare armor and vanilla comparison")}</h2>
{table([tr("档位", "Tier"), tr("整套护甲", "Set armor"), tr("整套韧性", "Set toughness"), "5HP", "10HP", "15HP", "20HP"], native_rows)}
<p>{tr("表中百分比仅原版护甲减伤，不含任何晶核或手稿。原版护甲条最多画20点，但超额护甲仍参与公式。实际受伤会随攻击大小变化：原版先将护甲属性总和向下取整，再算有效甲=min(20,max(整数护甲×0.2,整数护甲−伤害/(2+韧性/4)))，甲后伤害=伤害×(1−有效甲/25)，再应用额外F。", "Percentages are native armor mitigation only, without cores or manuscripts. The HUD displays at most20 armor points; extra armor still affects damage. Native armor=floor(total armor attribute); effective armor=min(20,max(nativeArmor×0.2,nativeArmor−damage/(2+toughness/4))); post-armor damage=damage×(1−effectiveArmor/25), then apply extraF.")}</p>
{table([tr("比较指标", "Comparison"), realm_names["normal"], realm_names["deep"]], compare_rows)}
<p>{tr("通用目标基线：下界合金锋利V剑普通攻击11HP、暴击15HP；四件保护IV为EPF16，保护附魔后保留36%伤害。统一按固定80%裸甲口径，原版总防护5/0.36≈13.889，20HP原始致死阈值约277.778HP。不是原版任意重击的真实阈值，也不考虑亡灵杀手专用目标、药水、盾牌或图腾。", "Generic-target baseline: Netherite SharpnessV ordinary11HP / critical15HP. Four ProtectionIV pieces giveEPF16 and retain36% damage after native armor. With fixed80% armor, vanilla protection=5/0.36≈13.889 and20HP lethal input≈277.778HP. This is not the actual threshold for arbitrary heavy hits; specialized Smite targets, potions, shields and totems are excluded.")}</p>
</article>
<article><h2>{tr("合法性与单调性复核", "Legality and monotonicity checks")}</h2>
<p>{tr(f"全部审计候选禁止装备序数材料；共{len(examples)}项加核单调性检查，均只填空槽，不替换别的晶核。每件甲分别计算再加权，手稿与套装分开，添正贡献不会反而降低防护。以下显示各部位示例与手稿项。", f"All audited candidates prohibit gear Ordinal. {len(examples)} empty-socket monotonicity checks add a core without replacing another. Per-piece weighted armor and independent book/set contributions cannot reduce defense when a positive core is added. Slot/book examples follow.")}</p>
{table([tr("环境", "Realm"), tr("部位", "Slot"), tr("加核前F", "F before"), tr("加核后F", "F after"), tr("结果", "Result")], monotonic_rows)}
<details><summary>{tr("防护与省耐久联合最优", "Joint protection and wear optima")}</summary><p>{tr("每行是单独最优配装，不能一套甲同时取得各行。F×W是相对原版耐久损耗预算的期望缩小倍数，不是固定免疫时间。", "Each row has its own optimal loadout; one set cannot simultaneously realize all rows. F×W reduces expected native wear budget, not guaranteed invulnerability time.")}</p>{table([tr("部位", "Slot"), realm_names["normal"], realm_names["deep"]], wear_rows)}</details>
<details><summary>{tr("同档镐＋手稿挖速", "Matching pick/book tier mining")}</summary>{table([tr("档位", "Tier"), realm_names["normal"], realm_names["deep"]], mining_rows)}<p>{tr("序列手稿效率II／IV／VI加5／17／37，重复取最高。序数不再参与挖速，所有矿硬度、采集权限、水下与站稳状态仍另算。", "Sequence manuscript EfficiencyII /IV /VI adds5 /17 /37 and uses max. Ordinal no longer boosts mining; hardness, permissions, water and airborne penalties remain separate.")}</p></details>
<p>{tr("省耐久由空集装备承担；触距由分枝手稿承担。重复效率、时运／抢夺、隐蔽与布尔免疫取最高，可能浪费槽位。周期治疗与饥饿保障可能叠加原版自然恢复，本页最优不包含食物回血。最终玩法需另测爆裂频率、特殊索敌和多人表现。", "Absence gear owns wear protection; Branch manuscripts own reach. Repeated Efficiency, Fortune/Looting, stealth and boolean immunities use max and can waste sockets. Periodic healing/food floors may coexist with native regeneration, excluded here. Gameplay will need tests for burst rate, special targeting and multiplayer.")}</p>
<details><summary>{tr("可复现数据", "Reproducibility")}</summary><code>{DATA['evaluations']} evaluations<br>engine SHA-256: {esc(DATA['inputSha256']['engine'])}<br>rules SHA-256: {esc(DATA['inputSha256']['rules'])}<br>node tools/tests/audit_enhancement_proposal.js<br>python tools/generate_enhancement_audit.py</code></details>
</article></section>'''


page = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>果糕逻辑 · 数值审计</title><style>
:root{color-scheme:light dark;--bg:#f3f5fa;--card:white;--ink:#202638;--muted:#627084;--line:#dfe5ef;--accent:#5145c5}*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.7 system-ui,'Microsoft YaHei',sans-serif}main{max-width:1180px;margin:auto;padding:28px 24px}h1,h2,h3{line-height:1.35}h1{font-size:28px;margin:0}h2{font-size:21px}h3{font-size:17px}p{margin:10px 0;color:var(--muted)}a{color:var(--accent)}nav{display:flex;gap:20px;flex-wrap:wrap;margin:18px 0}article{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:20px;margin:18px 0}.notice{border-left:4px solid var(--accent);padding:12px 16px;background:#5145c514;color:var(--ink)}.table-wrap{overflow-x:auto}table{width:100%;border-collapse:collapse;text-align:left;font-size:14px}td,th{padding:12px 14px;border-bottom:1px solid var(--line);vertical-align:top}thead{background:var(--bg)}td{min-width:145px}button{float:right;padding:7px 13px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--ink);cursor:pointer}details{margin:16px 0}summary{cursor:pointer;color:var(--accent)}code{overflow-wrap:anywhere;font-size:12px}li{margin:10px 0}.language{display:none}html[data-language="zh"] [data-lang="zh"],html[data-language="en"] [data-lang="en"]{display:block}@media(prefers-color-scheme:dark){:root{--bg:#151923;--card:#1e2431;--ink:#edf0f7;--muted:#abb6c8;--line:#343e51;--accent:#aca3ff}}@media(max-width:540px){main{padding:18px 12px}article{padding:14px}h1{font-size:23px}td,th{padding:10px}}
p{overflow-wrap:anywhere}
</style><main><button id="language">English</button>''' + section("zh") + section("en") + '''</main><script>
let lang=new URLSearchParams(location.search).get('lang')==='en'?'en':'zh';function render(){document.documentElement.dataset.language=lang;document.documentElement.lang=lang==='zh'?'zh-CN':'en';document.title=lang==='zh'?'果糕逻辑 · 数值审计':'Guogaology · Numerical audit';document.getElementById('language').textContent=lang==='zh'?'English':'中文';try{history.replaceState(null,'','?lang='+lang)}catch{}}document.getElementById('language').onclick=()=>{lang=lang==='zh'?'en':'zh';render()};render();
</script></html>'''
target = ROOT / "docs/enhancement-numeric-audit.html"
target.write_text(page, encoding="utf-8", newline="\n")
print("Generated", target)
