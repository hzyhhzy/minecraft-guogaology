"""Assemble the read-only 0.6.8 audit from immutable native observations.

This does not rebuild or repair the Mod. A successful runner only means that
observations were collected; the bow suite intentionally remains FAILED.
"""
from pathlib import Path
import hashlib
import html
import json
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / "build/audit-068-general"
OUT = ROOT.parent / "outputs/20261020-general-audit-068"
R263 = ROOT / "build/runtime-qa263/audit068-general-263-d"
R262 = ROOT / "build/runtime-qa/audit068-focus-262-b"


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT, text=True).strip()


def main():
    expected = {
        "1.21.1": "d4c7e13b4a7d7bab78421adb8e82f7bb9c759765b5c2cb2a67413b445c7fda92",
        "1.21.11": "699465d5f0427f57a3e8dc9c3a394376831e9b4d4f1f2e9cfe9fc6cfd1d2c1c9",
        "26.2": "cdbf300554851ccee6aba13fcd55210f1688a40da01a0b6914cc0f5b5b4b74b4",
        "26.3": "c15eb5f74ee645ab6253ec5333fd35fd9760cd97faf5e9ed408ac0d85beeabff",
    }
    hashes = {}
    for target, digest in expected.items():
        jar = ROOT / f"build/releases/0.6.8/guogaology-dimension-{target}-0.6.8.jar"
        hashes[target] = hashlib.sha256(jar.read_bytes()).hexdigest()
        assert hashes[target] == digest, f"Released artifact changed: {target}"
    production_diff = git("diff", "--name-only", "c122ac5", "--", "src", "ports/common", "ports/outer-1.21.1", "ports/1.21.11/src", "ports/26.2/src", "ports/26.3/src", "tools/prepare_port_sources.py", "tools/prepare_outer_legacy.py", "gradle.properties")
    assert not production_diff, production_diff
    assert git("branch", "--show-current") == "codex/audit-0.6.8-general"
    assert git("rev-parse", "main") == "11360c85d0149b9e1e16cfa2cef7a9e53eba2c6a"

    server = read(R263 / "audit068-server.json")
    client = read(R263 / "audit068-client.json")
    focus = read(R262 / "audit068-combat-focus.json")
    xp = read(R263 / "experience066-native.json")
    native263 = read(R263 / "release-263-ok.json")
    assert server["completed"] and client["completed"]
    assert len(server["numericMismatches"]) == 2
    assert next(s for s in server["suites"] if s["suite"] == "bow-launch-hit-reload")["passed"] is False
    assert all(s["passed"] for s in server["suites"] if s["suite"] != "bow-launch-hit-reload")
    assert len(server["allNativeCraftingRecipes"]) == 145
    assert all(r["matched"] and r["outputMatches"] for r in server["allNativeCraftingRecipes"])
    assert not client["modelFailures"]
    assert not any(r["empty"] for r in client["models"])
    assert not server["stateCodecRoundTrips"]["failures"]
    assert all(r["exactMultisetConserved"] and r["duplicateCloseConserved"] for r in server["deathDuringManuscriptEdit"])
    assert xp["repeatedSilkOreXp"] == 0
    assert read(R263 / "normal-instance-after.json")["unchanged"]
    assert read(R262 / "item068-normal-protection.json")["unchanged"]
    runs263 = []
    for suffix in "abcd":
        directory = ROOT / f"build/runtime-qa263/audit068-general-263-{suffix}"
        completion = read(directory / "release-263-ok.json")
        protection = read(directory / "normal-instance-after.json")
        assert completion["success"] and protection["unchanged"]
        runs263.append({"label": directory.name, "seed": completion["seed"], "checks": completion["checks"], "normalInstanceUnchanged": True})
    assert len({r["seed"] for r in runs263}) == 4
    network_summary = read(EVIDENCE / "remaining-runtime-summary.json")
    assert network_summary["success"] and len(network_summary["cases"]) == 6
    network = []
    for case in network_summary["cases"][1:]:
        directory = ROOT / "build/network-qa263" / case["label"]
        result = read(directory / "network-result.json")
        guard = read(directory / "network-verification-ok.json")
        assert result["success"] and case["exitCode"] == 0
        assert guard["runnerProtectionComplete"] and guard["nativeVerificationComplete"]
        assert guard["portClosedAfterShutdown"] and guard["clientExit"] == guard["serverExit"] == 0
        assert read(directory / "normal-instance-unchanged.json")["unchanged"]
        network.append({"label": case["label"], "seconds": case["elapsedSeconds"], "result": result, "guard": guard})

    findings = [
        {"id": "A01", "priority": "P2", "title": "26.3 核爆追加伤害被受伤冷却拦截", "targets": "26.3", "proof": "原生复现＋正确字段对照", "detail": "剑直击16HP后，应再追加4.8HP，主目标实际追加0；旁边目标正常追加4.8。弓两项实际16.5，预期21.45。只清除正确冷却字段的QA对照恢复正常；26.2同场景直接目标与邻居均正常追加4.8。已有受伤冷却时，较弱追加会被拒绝，较强追加也可能只算差额；不是所有核爆溅射都失效。", "cause": "26.3的真实受伤冷却已移到LivingEntity.damageCooldownTime；当前两个适配点清理的仍是Entity.invulnerableTime。", "fix": "仅改26.3适配器的两处字段，保留保存、置零、finally恢复及lastHurt恢复。修复后重新做剑／弓原生回归。", "sources": ["tools/prepare_port_sources.py:176", "tools/prepare_port_sources.py:178", "ports/common/main/dev/guogaology/mining/MiningEffects.java:68", "ports/common/main/dev/guogaology/mining/BowEffects.java:80"]},
        {"id": "A02", "priority": "P3", "title": "紫菜木船实体名称显示翻译键", "targets": "四版；26.3／26.2原生确认", "proof": "原生中英＋四版包核对", "detail": "缺entity.guogaology.laver_boat。库存里的船物品名正常，错误发生于船实体名称，不能把两者混为一谈。冥杉船正常。", "cause": "item键有翻译，entity键遗漏；现有静态语言审计没有枚举动态注册实体。", "fix": "补中英实体名称及copy_catalog，并把注册实体加入翻译覆盖检查。", "sources": ["src/main/resources/assets/guogaology/lang/zh_cn.json", "src/main/resources/assets/guogaology/lang/en_us.json", "tools/audit_localization.py"]},
        {"id": "A03", "priority": "P2", "title": "五种方向性方块放置朝向固定", "targets": "四版；26.3／26.2原生确认", "proof": "26.3、26.2各20次；其余两版静态核对", "detail": "Astra机柜server_rack、终端office_monitor没有facing状态；椅子chair、震惊瘫坐椅nuke_chair、蛇头snake_head虽有四向模型，普通放置仍固定north。分别按玩家四个方向放置，结果均不变。", "cause": "前两种注册为普通Block；后三种缺少按玩家方向选择放置状态的实现。", "fix": "添加原版式水平放置／旋转／镜像。新状态默认方向应保持现有外观，以免旧建筑整体转向。", "sources": ["ports/common/main/dev/guogaology/GuogaologyBlocks.java:167", "ports/common/main/dev/guogaology/outer/block/ChairBlock.java:22", "ports/common/main/dev/guogaology/outer/block/SnakeHeadBlock.java:22"]},
        {"id": "A04", "priority": "P2", "title": "紫菜原木和紫菜木不能用斧头去皮", "targets": "四版；26.3／26.2原生确认", "proof": "原生铁斧useOn＋冥杉对照", "detail": "laver_vein、laver_wood返回Pass并保持原块；对应去皮方块已存在。冥杉原木和木块同样操作都成功。这里说的是右键去皮，不是之前排查的斧头挖速。", "cause": "四版都只注册了冥杉的两条去皮映射。", "fix": "把紫菜两条映射接入各版本原版去皮接口，保留木纹轴向。", "sources": ["ports/26.3/src/main/java/dev/guogaology/outer/registry/ModBlocks.java:115", "ports/26.3/src/main/java/dev/guogaology/outer/registry/ModBlocks.java:116"]},
        {"id": "A05", "priority": "P2", "title": "自定义船摔毁掉橡木板", "targets": "仅1.21.1", "proof": "源码＋对应原版字节码；未启动1.21.1", "detail": "两种船继承默认OAK变体，满足原版落地摔毁条件时掉3橡木板＋2木棍。现代三版使用带对应掉落供应器的原版Boat实体，不属于这一路径。", "cause": "仅覆写普通船物品掉落，未覆盖摔毁时按木材变体拆板的路径。", "fix": "使该路径使用对应木材，并补旧版原生摔毁测试。", "sources": ["ports/outer-1.21.1/src/main/java/dev/guogaology/outer/registry/ModBoats.java"]},
        {"id": "A06", "priority": "P3", "title": "改名船放置后丢失名称", "targets": "仅1.21.1", "proof": "源码＋对应原版字节码；未启动1.21.1", "detail": "紫菜／冥杉船的自定义OuterBoatItem漏掉原版放置配置步骤。1.21.11、26.2、26.3使用原版BoatItem，不受此遗漏影响。", "cause": "缺少EntityType.createDefaultStackConfig应用物品自定义名称／实体数据。", "fix": "复用原版配置步骤，并验证改名→放置→回收。", "sources": ["ports/outer-1.21.1/src/main/java/dev/guogaology/outer/item/OuterBoatItem.java"]},
    ]
    risks = [
        ["超容量核心组件", "外部命令／模组可构造超过当前装备容量的合法组件；打开菜单同步可能截断尾槽。正常镶嵌流程不能产生它，未证实0.6.0正常存档受影响。", "保留或返还溢出槽，不应静默丢弃。"],
        ["传送门断线重连", "pending请求按UUID查当前玩家，没有验证新旧ServerPlayer是否同会话；极窄同UUID重连场景尚未网络复现。", "断线取消，或检查玩家对象身份。"],
        ["弓命中账本／跨MC迁移", "损坏账本和跨Minecraft版本数据路径未实测；正常同版本流程未发现复制或串玩家。", "独立做损坏记录恢复和跨MC迁移测试。"],
        ["额外物品展示上下文", "新版本ON_SHELF等额外上下文不在本轮七种原生上下文里；不能用2037项检查覆盖它。", "后续按该功能专测，不计为当前确证bug。"],
    ]
    summary = {"baseline": git("rev-parse", "HEAD"), "branch": git("branch", "--show-current"), "productionChanged": False,
               "version": "0.6.8", "hashes": hashes, "findings": findings, "risks": risks,
               "native263": {"client": {k: v for k, v in client.items() if k not in ("models", "blockStateProperties")},
                             "suites": server["suites"], "numericMismatches": server["numericMismatches"],
                             "combat": server["nativeMeleeBurstCooldown"], "death": server["deathDuringManuscriptEdit"],
                             "crafts": len(server["allNativeCraftingRecipes"]), "states": server["stateCodecRoundTrips"],
                             "xp": {k: v for k, v in xp.items() if k != "rows"}},
               "native262Combat": focus, "network": network, "observationRuns263": runs263,
               "auditOutcome": "confirmed defects remain; successful observation runner is not an all-tests-pass result"}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "evidence").mkdir(exist_ok=True)
    (OUT / "images").mkdir(exist_ok=True)
    for name in ["wood-localization.json", "orientation-models.json", "dynamic-localization.json", "mechanics-wood-followup.json", "bow-ownership.json", "effects-audit.json", "damage-cooldown-263.json", "data-audit.json", "remaining-runtime-summary.json"]:
        if (EVIDENCE / name).exists():
            shutil.copyfile(EVIDENCE / name, OUT / "evidence" / name)
    for source, name in [(R263 / "audit068-server.json", "native263-server.json"), (R263 / "audit068-client.json", "native263-client.json"), (R262 / "audit068-combat-focus.json", "native262-combat.json")]:
        shutil.copyfile(source, OUT / "evidence" / name)
    for source, name in [(R263 / "screenshots/release263-02-core-item-gui.png", "263-cores.png"), (R263 / "screenshots/release263-03-outer.png", "263-outer.png"), (R263 / "screenshots/release263-05-underworld.png", "263-underworld.png"), (R262 / "screenshots/item068-ground-eight-items.png", "262-drops.png"), (R262 / "screenshots/item068-sign-laver_sign.png", "262-sign.png")]:
        shutil.copyfile(source, OUT / "images" / name)
    for destination in [EVIDENCE / "summary.json", OUT / "evidence/summary.json"]:
        destination.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    sections = []
    for finding in findings:
        refs = " · ".join(f'<code>{html.escape(s)}</code>' for s in finding["sources"])
        sections.append(f'''<article data-target="{'legacy' if finding['targets'].startswith('仅1.') else 'modern'}"><div class="eyebrow">{finding['id']} · {finding['priority']} · {html.escape(finding['proof'])}</div><h2>{html.escape(finding['title'])}</h2><p class="muted">{html.escape(finding['targets'])}</p><p>{html.escape(finding['detail'])}</p><p><b>原因：</b>{html.escape(finding['cause'])}</p><p><b>修复建议：</b>{html.escape(finding['fix'])}</p><small>{refs}</small></article>''')
    network_rows = "".join(f"<tr><td>{html.escape(case['label'].replace('audit068-network-', ''))}</td><td>通过</td><td>{case['seconds']:.1f}秒</td><td>仅127.0.0.1，正常退出、端口关闭</td></tr>" for case in network)
    risk_rows = "".join("<tr>" + "".join(f"<td>{html.escape(cell)}</td>" for cell in row) + "</tr>" for row in risks)
    document = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>果糕逻辑0.6.8综合审计</title><style>
    :root{color-scheme:light;--ink:#192a3a;--muted:#516174;--line:#dce4eb}*{box-sizing:border-box}body{margin:0;background:#f3f6fa;color:var(--ink);font:16px/1.75 system-ui,"Microsoft YaHei",sans-serif}main{max-width:1120px;margin:auto;padding:42px 24px 70px}h1{font-size:36px;margin:5px 0 15px;line-height:1.35}h2{font-size:22px;line-height:1.5;margin:5px 0 10px}h3{font-size:24px;margin-top:42px}.eyebrow{color:#526b85;font-size:13px;font-weight:650}.lead{font-size:19px}.muted,small{color:var(--muted)}.note{padding:15px 20px;border-left:4px solid #bd7923;background:#fff7e7;border-radius:4px}article,.panel{background:#fff;border:1px solid var(--line);border-radius:12px;padding:24px;margin:20px 0}article p{margin:9px 0}code{font:12px/1.5 ui-monospace,Consolas,monospace;overflow-wrap:anywhere}table{width:100%;border-collapse:collapse;background:white;font-size:14px}td,th{border:1px solid var(--line);padding:12px;text-align:left;vertical-align:top}th{background:#eaf0f6}.scroll{overflow:auto}button{font:inherit;border:1px solid #aebdcb;background:white;border-radius:7px;padding:8px 14px;margin-right:8px;cursor:pointer}button[aria-pressed=true]{background:#214764;color:white}.grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.grid figure{margin:0}.grid img{width:100%;border-radius:7px;border:1px solid var(--line)}figcaption{font-size:13px;color:var(--muted)}a{color:#16639a}li{margin:7px 0}[hidden]{display:none!important}@media(max-width:700px){main{padding:24px 15px}.grid{grid-template-columns:1fr}h1{font-size:29px}article{padding:18px}}
    </style><main><div class="eyebrow">2026-10-20 · 正式0.6.8原包 · 独立审计分支</div><h1>果糕逻辑综合审计</h1><p class="lead">当前主测版本确认4类问题，另有2类仅涉及1.21.1的源码确定问题。最值得优先处理的是26.3核爆追加伤害。</p><p class="note">这次只做审计和测试，没有修改正式机制、版本或发行JAR，没有替换游戏，没有合并main或发布。下方“测试运行完成”不代表所有功能通过：弓套件保留了2项真实数值失败。</p>
    <div><button aria-pressed="true" data-filter="all">全部问题</button><button aria-pressed="false" data-filter="modern">26.3／26.2相关</button><button aria-pressed="false" data-filter="legacy">仅1.21.1</button></div>''' + "".join(sections) + '''
    <h3>本轮实际检查了什么</h3><div class="scroll"><table><tr><th>范围</th><th>实际覆盖与结果</th></tr>
    <tr><td>26.3资源和保存</td><td>291个注册物品 × 7种原生模型上下文＝2037次解析，无空模型／异常；2716种方块状态的原生模型查询与NBT编解码往返无失败。另审查四版最终包模型、翻译、标签与引用。</td></tr>
    <tr><td>26.3合成与镶嵌</td><td>145条实际工作台配方各取一组真实材料，匹配与产物ID／数量通过。菜单119项、附魔与晶核互斥234项、手稿与效果169项通过。没有穷举每个tag的所有材料组合。</td></tr>
    <tr><td>死亡物品守恒</td><td>主手／副手 × keepInventory开／关，共4次真实致死。核心同时分布于书、背包和光标；死亡与重复关闭后精确物品多重集合均守恒。不能替代所有网络时序测试。</td></tr>
    <tr><td>挖掘与经验</td><td>1546次生存模式服务端实际破坏方块、6788项经验断言通过（检查掉落／经验，不是按住鼠标计时）；原版精采／晶核虚拟精采／时运／额外开采路径与刷怪笼对照。反复精采矿石的循环经验为0。</td></tr>
    <tr><td>弓与近战</td><td>发射、弹药、快照、命中、重载、多重射击／穿透与归属流程运行完成，弓2项伤害断言失败，见A01。近战有正确冷却字段的QA单变量对照。</td></tr>
    <tr><td>26.2重点回归</td><td>两次独立新世界，中英测试；每轮569项既有木制品／告示牌／物品显示检查通过。另复现船名、朝向和去皮问题。近战核爆对照正常。</td></tr>
    <tr><td>三个维度</td><td>4个种子的隔离26.3新世界，各访问表界、里界、地府并返回；抽样区块生成、碰撞／高度图与28款晶核／序数晶体渲染通过。未重新穷举8座巨构内饰，也不是传送门仪式耗时／成功率的新统计。</td></tr>
    <tr><td>离线检查</td><td>111个Python检查通过；2378条在用资源引用无缺失，644条中英键一致。静态语言审计存在实体动态键盲点，已由原生检查发现A02，不能以静态全绿排除缺译。</td></tr>
    </table></div><h3>26.3真实联机矩阵</h3><p>本轮重新启动独立服务端和隐藏客户端，使用正式0.6.8原包；JEI为31.16.0.86。包含自然首次捡枝石／数字刻石、标签／配方同步与查询。旧服对照为正式0.6.0服务端。旧服自身没有的新配方仍不能凭客户端创造出来。</p><div class="scroll"><table><tr><th>组合</th><th>结果</th><th>整轮时间</th><th>隔离保护</th></tr>''' + network_rows + '''</table></div><p class="muted">时间包含进程启动、加载、测试及退出，不是传送门耗时或服务器性能指标。与前次发布测试分开计数；这次没有新增Connector／Voxy认证。</p>
    <h3>未证实为正常玩法Bug的边界</h3><div class="scroll"><table><tr><th>边界</th><th>证据限度</th><th>建议</th></tr>''' + risk_rows + '''</table></div>
    <h3>选取的原生截图</h3><p>截图来自独立测试实例；只展示已选取检查的画面，不把模型解析等同于逐个物品截图检查。</p><div class="grid"><figure><a href="images/263-cores.png"><img src="images/263-cores.png" alt="26.3晶核物品栏"></a><figcaption>26.3：28款晶核／序数晶体物品栏</figcaption></figure><figure><a href="images/262-drops.png"><img src="images/262-drops.png" alt="26.2掉落物"></a><figcaption>26.2：前次修复的8类物品掉落显示</figcaption></figure><figure><a href="images/263-outer.png"><img src="images/263-outer.png" alt="26.3表界"></a><figcaption>26.3：新生成表界抽样</figcaption></figure><figure><a href="images/263-underworld.png"><img src="images/263-underworld.png" alt="26.3地府"></a><figcaption>26.3：新生成地府抽样</figcaption></figure><figure><a href="images/262-sign.png"><img src="images/262-sign.png" alt="26.2告示牌"></a><figcaption>26.2：原版告示牌编辑器</figcaption></figure></div>
    <h3>验证边界与证据</h3><p>未对所有外部模组、所有伤害源、所有旧存档、长时间多人负载、持续键盘飞行、区块强退时序作穷举。没有在已覆盖场景中发现新的崩溃、复制或读档失败，不等于这些情况已被普遍排除。1.21.1两项问题是源码与对应原版字节码结论，本轮没有启动其游戏。</p><p>早期a／b试验中的过时弓等级断言、早期未满足驻留／实体查询前置条件的夹具失败属于QA夹具问题，均排除；最终d轮等待真实tick并预先创建、确认目标可查询后，仍保留A01。不能把被排除的经验夹具失败写成经验回归。</p><p><a href="evidence/summary.json">结构化总证据</a> · <a href="evidence/native263-server.json">26.3原生服务端记录</a> · <a href="evidence/native263-client.json">26.3原生客户端记录</a> · <a href="evidence/native262-combat.json">26.2伤害对照</a> · <a href="evidence/damage-cooldown-263.json">伤害字段字节码证据</a> · <a href="evidence/wood-localization.json">木制品与旧版船证据</a></p><p class="muted">修复顺序建议：A01战斗结算 → A03方向与A04去皮 → A02船名与检查盲点 → 旧版船及数据加固。正式修复需另做回归；本报告没有声称这些问题已经修好。</p></main><script>document.querySelectorAll('[data-filter]').forEach(b=>b.addEventListener('click',()=>{document.querySelectorAll('[data-filter]').forEach(x=>x.setAttribute('aria-pressed',x===b?'true':'false'));document.querySelectorAll('article[data-target]').forEach(a=>a.hidden=b.dataset.filter!=='all'&&a.dataset.target!==b.dataset.filter)}));</script></html>'''
    (OUT / "审计.html").write_text(document, encoding="utf-8")
    markdown = "# Guogaology 0.6.8 综合审计（2026-10-20）\n\n"
    markdown += "本轮为审计，不是修复发布。正式源码、四版JAR、正常实例、存档与配置未改；未合并main、推送或发布。基线 `c122ac5`，审计分支 `codex/audit-0.6.8-general`。\n\n"
    markdown += "## 已确认问题\n\n| 编号 | 优先级 | 问题 | 影响与证据 |\n|---|---|---|---|\n"
    for finding in findings:
        markdown += f"| {finding['id']} | {finding['priority']} | {finding['title']} | {finding['targets']}；{finding['proof']} |\n"
    for finding in findings:
        markdown += f"\n### {finding['id']} {finding['title']}\n\n{finding['detail']}\n\n原因：{finding['cause']}\n\n建议：{finding['fix']}\n\n来源：" + "、".join(f"`{s}`" for s in finding["sources"]) + "。\n"
    markdown += "\n## 本轮实际验证\n\n"
    markdown += "- 26.3：4个种子独立新世界，三个自定义维度抽样与返回、28晶核／序数晶体模型；最终d轮291注册物品×7上下文、2716状态原生模型查询及NBT往返、145真实工作台配方均无结构错误。模型解析不能代替逐个截图审美检查；tag配方各取一组材料，未穷举所有组合。\n"
    markdown += "- 26.3菜单119项、附魔互斥234项、手稿169项通过；4次真实编辑中死亡及重复关闭均精确守恒。1546次生存模式服务端实际破坏／6788项经验断言通过（不代表按住鼠标计时），精采循环经验0。弓运行完成但**2项数值失败**，不得将观察收集器的正常退出当作全绿。\n"
    markdown += "- 26.2：中英两轮重点回归，每轮569项；包括50个木制品实际采集案例、4种原生告示牌编辑与文本保存、56显示上下文。另有A01伤害对照及船名／去皮／20次四向放置观察。\n"
    markdown += "- 四版最终包静态审查、111个Python检查通过；2378在用资源无缺失引用、644中英键一致。额外动态语言审查揭示原静态检查的实体键盲点；进度249项（76可见）、148配方、248战利品表的引用与父链补充审计见data-audit.json。后者不等于逐一实际解锁所有成就／抽取全部loot。\n"
    markdown += "\n### 26.3真实TCP矩阵\n\nJEI31.16.0.86，Loader0.20.0／API0.163.0+26.3。每组自然首次捡枝石和数字刻石；核对服务端权威配方与标签。旧服为正式0.6.0。所有监听仅127.0.0.1、双方正常退出、端口关闭、正常实例哈希保护通过。\n\n| 组合 | 结果 | 整轮秒数 |\n|---|---|---|\n"
    for case in network:
        markdown += f"| {case['label'].replace('audit068-network-', '')} | 通过 | {case['seconds']:.1f} |\n"
    markdown += "\n整轮时间包含启动、加载、操作和退出，不是传送门性能指标。旧服不具备的新配方仍不存在；本轮未新增Connector／Voxy测试。\n\n## 风险与未覆盖\n\n"
    for name, detail, suggestion in risks:
        markdown += f"- **{name}：**{detail} 建议：{suggestion}\n"
    markdown += "\n本轮未穷举八巨构内饰、所有外部模组、所有伤害源、持续键盘飞行、长时间多人负载、所有旧存档或强退时序；不能声称没有其他Bug。早期过时弓断言与早期未满足驻留／实体查询前置条件的QA失败已排除，最终d轮预热且验证实体可查询后仍确认A01。\n\n## 证据与复现\n\n"
    markdown += f"- 可视报告：`{OUT / '审计.html'}`\n- 结构化汇总：`build/audit-068-general/summary.json`\n- 最终263：`build/runtime-qa263/audit068-general-263-d/`\n- 最终262：`build/runtime-qa/audit068-focus-262-b/`\n- 网络：`build/network-qa263/audit068-network-*/`，每组的`network-verification-ok.json`和`network-result.json`共同确认结果。\n- 木制品／方向／动态语言／战斗／数据／所有权只读子审计：`build/audit-068-general/*.json`。\n"
    markdown += "\nQA均在独立source set；新观察套件、263适配准备器和报告生成器在`ports/qa263`、`ports/qa26`和`tools/audit-support`，不进入正式JAR。原始测试日志包含已知失败，不删改。修复后必须重新生成证据，不能复用本轮失败结果认证修复。\n\n四版正式SHA256未变：\n\n| MC | SHA256 |\n|---|---|\n"
    markdown += "".join(f"| {target} | `{digest}` |\n" for target, digest in hashes.items())
    (ROOT / "docs/AUDIT-068-GENERAL.md").write_text(markdown, encoding="utf-8")
    print(f"Audit report: {OUT / '审计.html'}")
    print("Six confirmed issue families; production unchanged; bow failure retained.")


if __name__ == "__main__":
    main()
