"""Collect final0.6.9 native, save, package and loopback proofs without changing JARs."""
from pathlib import Path
import hashlib
import html
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / 'build/repair-069'
OUT = ROOT.parent / 'outputs/20261020-repairs-069'


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def main():
    runtime = read(BUILD / 'runtime-summary.json')
    assert runtime['success'] and len(runtime['cases']) == 13
    assert all(row['exitCode'] == 0 for row in runtime['cases'])
    package = read(BUILD / 'package-save-compat.json')
    assert package['success']
    saves = []
    for old in ('068', '060'):
        for stage in ('baseline', 'upgrade', 'reopen'):
            directory = ROOT / f'build/runtime-qa263/save069-{stage}{old}-a'
            result = read(directory / 'save069-ok.json')
            assert result['success'] and result['serverStopped'] and result['nativeSaveCompleted']
            assert result['persistedBlockStates'] == 96 and result['persistedBoats'] == 8
            assert result['persistedChestStacks'] == 24 and result['persistedPlayerStacks'] == 3
            assert result['rawLhoNativeControls'] == 16
            assert result['diskUpgradeVerified'] is (stage != 'baseline')
            if stage != 'baseline':
                assert read(directory / 'source-world-after.json')['unchanged']
            assert read(directory / 'normal-instance-after.json')['unchanged']
            saves.append({'label': directory.name, **{k: v for k, v in result.items() if k != 'inputs'}})
    native = []
    for label, parent in [('repair069-263-final-a', 'runtime-qa263'), ('repair069-263-final-b', 'runtime-qa263'), ('repair069-262-final-a', 'runtime-qa')]:
        directory = ROOT / 'build' / parent / label
        result = read(directory / 'repair069-native-verification.json')
        assert result['passed']
        native.append({'label': label, **result})
    network = []
    for mode in ('none', 'client', 'server', 'both'):
        directory = ROOT / ('build/network-qa263/repair069-network-' + mode)
        result = read(directory / 'network-verification-ok.json')
        raw = read(directory / 'network-result.json')
        assert result['nativeVerificationComplete'] and result['runnerProtectionComplete'] and result['normalInstanceUnchanged']
        assert result['clientExit'] == result['serverExit'] == 0 and result['portClosedAfterShutdown']
        assert raw['success'] and raw['bind'] == '127.0.0.1'
        network.append({'placement': mode, **result})
    hashes = {row['minecraft']: row['sha256'] for row in package['targets']}
    for target, digest in hashes.items():
        assert hashlib.sha256((ROOT / f'build/releases/0.6.9/guogaology-dimension-{target}-0.6.9.jar').read_bytes()).hexdigest() == digest
    independent_save = read(BUILD / 'save-independent-review.json')
    independent_native = read(BUILD / 'native-independent-review.json')
    assert independent_save['success'] and independent_native['passed']
    installation = read(ROOT / 'build/install-069.json')
    assert installation['status'] == 'installed' and installation['sha256'] == hashes['26.2']
    assert installation['mainMods'] == 1 and installation['fo263_2Unchanged']
    protection = read(BUILD / 'normal-after-install.json')
    assert protection['success'] and protection['fo263_2Unchanged']
    summary = {'version': '0.6.9', 'sixConfirmedIssueFamiliesFixed': True, 'package': package,
               'native': native, 'saveRuns': saves, 'network': network, 'runtime': runtime,
               'independentSave': independent_save, 'independentNative': independent_native,
               'installation': installation, 'installationProtection': protection,
               'scope': '26.3 primary,26.2 focused;1.21.x compiled/static only;no main merge/push/Release'}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'images').mkdir(exist_ok=True)
    (OUT / 'evidence').mkdir(exist_ok=True)
    for destination in (BUILD / 'final-verification.json', OUT / 'evidence/summary.json'):
        destination.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    pictures = [('save069-baseline068-a', 'save069-baseline068-world.png', 'old068.png'),
                ('save069-upgrade068-a', 'save069-upgrade-world.png', 'upgrade068.png'),
                ('save069-reopen060-a', 'save069-reopen-world.png', 'reopen060.png')]
    for label, source, name in pictures:
        shutil.copyfile(ROOT / f'build/runtime-qa263/{label}/screenshots/{source}', OUT / 'images' / name)
    server = read(ROOT / 'build/runtime-qa263/repair069-263-final-a/audit068-server.json')
    assert not server['numericMismatches'] and all(row['passed'] for row in server['suites'])
    combat = server['nativeMeleeBurstCooldown'][0]
    assert abs(combat['additionalOnDirectTarget'] - 4.8) < 1e-4
    shutil.copyfile(ROOT / 'build/runtime-qa263/repair069-263-final-a/audit068-server.json', OUT / 'evidence/native263.json')
    rows = [
        ('26.3核爆追加伤害', '已修复', '剑直击16后主目标及邻居都追加4.8；弓原来两项数值失败已清零，冷却恢复正确。'),
        ('紫菜船实体名称', '已修复', '补中英文实体翻译，库存物品名保持原样。'),
        ('五种家具朝向', '已修复', '机柜、终端、椅子、震惊瘫坐椅、蛇头：实际5×4方向放置、CW90旋转、FRONT_BACK镜像通过。'),
        ('紫菜木去皮', '已修复', '原木／木块接原版斧头；紫菜和冥杉共4×3轴向检查通过。'),
        ('1.21.1船摔毁木材', '已修复，构建／字节码核验', '保留原版摔毁条件及三木板两木棍，只换为自身木材。本轮未运行1.21.1。'),
        ('1.21.1改名船放置', '已修复，构建／字节码核验', '复用原版放置配置与权限。以前已丢失的名字无法恢复，本轮未运行1.21.1。'),
    ]
    table = ''.join('<tr>' + ''.join('<td>' + html.escape(cell) + '</td>' for cell in row) + '</tr>' for row in rows)
    save_table = ''.join(f"<tr><td>{html.escape(r['label'])}</td><td>通过</td><td>{r['persistedBlockStates']}方块状态／{r['persistedBoats']}船／{r['persistedChestStacks']}箱内物品／{r['persistedPlayerStacks']}玩家物品</td></tr>" for r in saves)
    page = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>果糕逻辑0.6.9修复与旧档验证</title><style>body{margin:0;background:#f4f7fa;color:#1c3043;font:16px/1.75 system-ui,"Microsoft YaHei",sans-serif}main{max-width:1100px;margin:auto;padding:38px 24px 60px}h1{font-size:32px}h2{font-size:23px;margin-top:35px}table{border-collapse:collapse;width:100%;background:white;font-size:14px}td,th{border:1px solid #d6e1eb;padding:12px;text-align:left;vertical-align:top}th{background:#e5edf5}.note{background:#e8f4ee;border-left:4px solid #378063;padding:15px 20px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}figure{margin:0}img{width:100%;border:1px solid #d6e1eb;border-radius:8px}figcaption,small{color:#52677a;font-size:13px}a{color:#216994}.scroll{overflow:auto}code{overflow-wrap:anywhere}@media(max-width:700px){.grid{grid-template-columns:1fr}main{padding:20px 14px}h1{font-size:27px}}</style><main><small>2026-10-20 · codex/0.6.9-audit-fixes · 未合并main／未推送／未发布</small><h1>0.6.9：确定问题修复完成</h1><p class="note">六类确定问题已修复。四版构建、125项Python检查、26.3主要实测和26.2回归通过；0.6.0与0.6.8正式JAR创建的测试存档均完成升级、保存及再次重开。</p><div class="scroll"><table><tr><th>项目</th><th>状态</th><th>证据与边界</th></tr>''' + table + '''</table></div><h2>旧存档兼容</h2><p>不改注册ID、组件codec、维度、模型本体、贴图、配方或战利品。旧机柜／终端没有facing时默认north，对应原先未旋转的模型；已有椅子、蛇头四向状态不变。</p><p>两条测试链都由旧版真实JAR先写磁盘并正常停服，之后复制到新目录升级0.6.9，保存退出，再在第三个JVM重开。每轮核对四个维度、96个旧状态、24个复杂组件箱内物品、3个玩家物品、8艘有名称的船和16项原LHO稳定／来源控制。原夹具逐文件SHA未变，正常存档没有被用作测试材料。另独立解析真实Anvil及player NBT，累计核对576坐标、144箱内物品、18玩家物品及48船实体的UUID和名称。</p><div class="scroll"><table><tr><th>独立运行</th><th>结果</th><th>核对数据</th></tr>''' + save_table + '''</table></div><h2>原生回归</h2><p>26.3简中＋Sodium、英文＋原版渲染两轮，检查291种物品×7种原生显示上下文，共2037次、2722状态模型与NBT往返、145个工作台配方。菜单、附魔互斥、编辑中真实死亡守恒、弓实际发射命中和生存破坏的掉落／经验检查通过。破坏测试不代表按住鼠标计时。</p><p>26.2完成569项木制品／告示牌／物品显示重点回归，另有本轮新增的朝向、三轴去皮、名称和伤害对照。联机四组JEI配置（均不装／仅客户端／仅服务端／双方）全部通过首次捡枝石和数字刻石、标签／配方同步；均仅监听127.0.0.1，客户端及服务端正常退出，端口关闭。</p><h2>原始画面对照</h2><div class="grid"><figure><a href="images/old068.png"><img src="images/old068.png" alt="0.6.8旧存档"></a><figcaption>正式0.6.8写出的独立旧存档</figcaption></figure><figure><a href="images/upgrade068.png"><img src="images/upgrade068.png" alt="升级0.6.9"></a><figcaption>同一存档副本升级0.6.9，旧家具保持原方向</figcaption></figure><figure><a href="images/reopen060.png"><img src="images/reopen060.png" alt="0.6.0存档升级再重开"></a><figcaption>正式0.6.0存档升级保存后再次重开</figcaption></figure></div><h2>范围与记录</h2><p>本轮没有运行1.21.x客户端、Connector或Voxy，也未穷举任意损坏存档和其他模组组合。旧存档验证使用独立真实旧版夹具，不代表修复已经损坏、缺少其他模组或更早命名空间的任意存档。联机双方应使用相同主Mod版本，旧存档兼容不是任意不同版本联机认证。</p><p><a href="evidence/summary.json">完整结构化记录与SHA</a> · <a href="evidence/native263.json">26.3原生检查记录</a></p><p>JAR目录：<code>Guogaology/build/releases/0.6.9</code>。正常fo262已安装0.6.9并备份原0.6.8，核对唯一主Mod及55个其他Mod不变，369个保护文件哈希和22489个存档文件大小／mtime未变；fo263_2没有替换。详细开发记录：<code>Guogaology/docs/WORK-069.md</code>。</p></main></html>'''
    (OUT / '修复验证.html').write_text(page, encoding='utf-8')
    print('REPAIR069_FINAL_VERIFICATION_OK')


if __name__ == '__main__':
    main()
