"""Build a concise evidence-backed HTML report; never change production or QA data."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import hashlib
import html
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT.parent / 'outputs/20261023-repairs-0610'
VERSION = '0.6.10'
VISUAL = ROOT / 'build/runtime-qa263'
FUELS = {'laver_vein', 'laver_wood', 'laver_stripped_log', 'laver_stripped_wood',
         'laver_planks', 'laver_stairs', 'laver_slab', 'laver_fence', 'laver_fence_gate',
         'laver_trapdoor', 'laver_button', 'laver_pressure_plate', 'laver_door',
         'laver_sign', 'laver_hanging_sign', 'laver_sapling', 'laver_boat', 'laver_chest_boat'}
SCENES = ('drops-near', 'drops-wide', 'heart-nw', 'heart-ne', 'heart-se', 'heart-sw', 'heart-above', 'heart-below')


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def png(path):
    return path.is_file() and path.read_bytes()[:8] == b'\x89PNG\r\n\x1a\n'


def collect(directory, function, *args):
    try:
        return {'passed': True, 'directory': str(directory), 'data': function(directory, *args)}
    except (OSError, KeyError, TypeError, ValueError, AssertionError, StopIteration) as error:
        return {'passed': False, 'directory': str(directory), 'reason': str(error) or '缺少完整证据文件'}


def protection(directory):
    paths = (directory / 'normal-instance-after.json', directory / 'item068-normal-protection.json')
    record = read(next(path for path in paths if path.is_file()))
    require(record.get('unchanged') is True, '正常实例保护记录未通过')


def native(directory, target):
    proof = read(directory / 'repair0610-native-verification.json')
    require(proof.get('passed') is True and proof.get('version') == VERSION and proof.get('minecraft') == target,
            '原生验证版本或结果不符')
    server = read(directory / 'repair0610-server-ok.json')
    client = read(directory / 'repair0610-client-ok.json')
    for role in (server, client):
        require(role.get('success') is True and role.get('version') == VERSION, '原生客户端/服务端结果不完整')
        require(role['wood'].get('passed') is True, '木系原生断言未通过')
    wood = server['wood']
    require(len(wood['fuel']) == 18 and {row['item'] for row in wood['fuel']} == FUELS, '木系燃料覆盖不全')
    require(all(row['actualTicks'] == row['nativeTicks'] > 0 for row in wood['fuel']), '燃料与原生值不符')
    neighbors = {'laver_fence', 'dread_fence', 'minecraft:oak_fence', 'minecraft:nether_brick_fence'}
    require(len(wood['fences']) == 4 and {row['neighbor'] for row in wood['fences']} == neighbors, '栅栏对照覆盖不全')
    require(all(row['east'] == row['west'] == (row['neighbor'] != 'minecraft:nether_brick_fence')
                for row in wood['fences']), '栅栏连接与原版规则不符')
    require(wood['ordinarySignNoCollisionStates'] == 80, '普通告示牌无碰撞状态不足80')
    hanging = {f'{wood}_{form}' for wood in ('laver', 'dread') for form in ('hanging_sign', 'wall_hanging_sign')}
    require(len(wood['hangingSigns']) == 4 and {row['block'] for row in wood['hangingSigns']} == hanging,
            '四种吊牌形态未完整检查')
    require(all(all(row.get(key) is True for key in ('nativeItemPlacement', 'nativeBlockEntity',
                    'diskNbtBothSides', 'nativeCollisionMatches')) and row['collisionEmpty'] == row['nativeCollisionEmpty']
                    for row in wood['hangingSigns']), '吊牌放置/文字/原生碰撞对照未通过')
    require(len(wood['chestBoats']) == 2 and {row['wood'] for row in wood['chestBoats']} == {'laver', 'dread'},
            '两种箱船未完整检查')
    require(all(row['slots'] == 27 and all(row.get(key) is True for key in
                ('actualNativeMenu', 'diskNbtRoundtrip', 'destructionConserved')) for row in wood['chestBoats']),
            '原生箱船菜单/NBT/掉落守恒检查未通过')
    if target == '26.2':
        control = wood['nativeChestBoatControl']
        require(control['wood'] == 'oak' and control['nativeItem'] == 'minecraft:oak_chest_boat' and
                control['slots'] == 27 and all(control.get(key) is True for key in
                    ('actualNativeMenu', 'diskNbtRoundtrip', 'destructionConserved', 'actualEntityVisible')),
                '26.2同位置原版箱船可见性/掉落对照未通过')
    for family in ('laver', 'dread'):
        cargo = read(directory / f'wood0610-{family}-destroy.json')
        require(cargo['expectedCount'] == 17 and cargo['mutableSourceCountAfterNativeSplit'] == 0 and
                cargo['allCargoComponentsPreserved'] is True, '箱船缺少事前快照/逐片组件对照')
        totals = {}
        for row in cargo['actualDrops']:
            totals[row['item']] = totals.get(row['item'], 0) + row['count']
            if row['item'] == 'minecraft:stick':
                require(row['matchesExpectedCargoComponents'] is True and row['components'] == cargo['expectedComponents'],
                        '箱船某个实际掉落片的组件不符')
        require(totals == {f'guogaology:{family}_chest_boat': 1, 'minecraft:stick': 17, 'minecraft:diamond': 3},
                '箱船实际掉落总数不守恒')
    textures = client['wood']['textures']
    require(len(textures) == 2 and {row['wood'] for row in textures} == {'laver', 'dread'}, '木系客户端覆盖不足')
    require(all(row.get('wholeDoorIcon') is True and row['guiMatchesLightFacePixels'] == 288 for row in textures),
            '门图标或告示牌浅面不符')
    bows = read(directory / 'bow0610-server.json')
    draws = read(directory / 'bow0610-client.json')
    require(bows.get('passed') is True and len(bows['tiers']) == 4, '四档弓拾取检查不全')
    require(draws.get('passed') is True and len(draws['models']) == 280, '弓拉弦原生显示检查不全')
    require(len({(r['tier'], r['hand'], r['drawTicks'], r['context']) for r in draws['models']}) == 280,
            '弓拉弦显示存在重复/缺失用例')
    small = read(directory / 'small0610-server.json')
    require(small.get('passed') is True and small['tnt']['validSandMixtures'] == 16 and
            small['tnt']['rejectedMissingOrWrongGrids'] == 288, 'TNT正反配方检查不全')
    require(small['gummyJar']['actualPrimaryBreaks'] == 360 and small['gummyJar']['actualSpreadStoneBreaks'] == 56,
            '软糖罐原生破坏检查不全')
    require(small['coreShapes']['absenceStates'] == 12 and small['coreShapes']['neighborCoreStates'] == 49,
            '空无晶核与其他晶核碰撞对照不全')
    if target == '26.3':
        general = read(directory / 'audit068-client.json')
        suites = read(directory / 'audit068-server.json')
        require(general['items'] == 295 and general['modelContexts'] == 2065 and
                not general['missingTranslations'] and not general['modelFailures'], '全物品显示/翻译回归未通过')
        require(len(suites['suites']) == 6 and all(row['passed'] for row in suites['suites']) and
                not suites['numericMismatches'], '原生六组回归或数值对照未通过')
        require(all(row['matched'] and row['outputMatches'] and 'error' not in row
                    for row in suites['allNativeCraftingRecipes']), '存在未通过的工作台配方')
        staging = read(directory / 'staging.json')
        main = next(row for row in staging['mods'] if row['id'] == 'guogaology')
        sha = main['sha256']
    else:
        sha = read(directory / 'item068-run-input.json')['productionSha256']
    artifact = ROOT / f'build/releases/{VERSION}/guogaology-dimension-{target}-{VERSION}.jar'
    require(digest(artifact) == sha, '原生实测JAR与当前最终产物SHA不同')
    protection(directory)
    return {'target': target, 'sha256': sha, 'fuelItems': 18, 'signStates': 80,
            'hangingForms': 4, 'chestBoats': 2, 'bowModels': 280, 'primaryBreaks': 360}


def visual(directory, old=False, expected_sodium=None):
    result = read(directory / 'visual0610-results.json')
    runner = read(directory / 'visual0610-runner-ok.json')
    version = '0.6.9' if old else VERSION
    require(result.get('success') is True and result['productionVersion'] == version,
            '视觉结果未完成或版本不符')
    require(runner.get('success') is True and runner.get('exitCode') == 0, '视觉JVM未成功退出')
    if expected_sodium is not None:
        require(result['inputs'].get('sodium') is expected_sodium, '实际Sodium配置与本轮声称的渲染器不符')
        staging = read(directory / 'staging.json')
        require(any(row['id'] == 'sodium' for row in staging['mods']) is expected_sodium,
                '实际staging模组清单与渲染器不符')
    require(len(result['actualNativeEntityRenderer']) == 40 and len(result['serverItemEntities']) == 40,
            '真实掉落物对照不足40个')
    require(len({(r['item'], r['count']) for r in result['actualNativeEntityRenderer']}) == 40,
            '掉落物样本重复')
    require(result['creativeVariantCount'] > 0 and result['creativeVariantCount'] == len(result['allCreativeVariantDropExtractions']),
            '创造变体提取记录不完整')
    layers = result['nativeHeartChunkLayers']
    require((layers['solid'], layers['translucent'], layers['other']) == ((0, 850, 0) if old else (736, 114, 0)),
            '果糕之心实际渲染层与预期不符')
    expected = {f'visual0610-{scene}.png' for scene in SCENES}
    require(set(result['screenshots']) == expected and all(png(directory / 'screenshots' / name) for name in expected),
            '八张实际游戏截图不完整')
    source = ROOT / f'build/releases/{version}' / result['inputs']['productionJar']
    require(digest(source) == result['inputs']['productionSha256'], '视觉实测与最终产物SHA不同')
    protection(directory)
    return {'version': version, 'screenshots': sorted(expected), 'layers': layers,
            'creativeVariants': result['creativeVariantCount'], 'largeCandidates': result['largeCreativeCandidates']}


def save(directory):
    result = read(next(p for p in (directory / 'save0610-ok.json', directory / 'save069-ok.json') if p.is_file()))
    require(all(result.get(key) is True for key in ('success', 'serverStopped', 'nativeSaveCompleted', 'diskUpgradeVerified')),
            '旧档未完整升级保存并正常停服')
    require(result.get('version') == VERSION and result.get('minecraft') == '26.3', '旧档验证版本不符')
    artifact = ROOT / f'build/releases/{VERSION}/guogaology-dimension-26.3-{VERSION}.jar'
    require(result['inputs']['productionSha256'] == digest(artifact), '旧档实测JAR与当前最终产物SHA不同')
    counts = {'fixtureDimensions': 4, 'persistedBlockStates': 96, 'persistedChestStacks': 24,
              'persistedPlayerStacks': 3, 'persistedBoats': 8, 'rawLhoNativeControls': 16}
    require(all(result.get(key) == value for key, value in counts.items()), '旧方块/组件/船/LHO检查数量不足')
    require(read(directory / 'source-world-after.json').get('unchanged') is True, '原夹具发生变化')
    require(any((directory / 'saves').glob('*/level.dat')), '缺少实际保存的level.dat')
    require(any((directory / 'saves').glob('*/dimensions/*/*/region/*.mca')), '缺少实际保存的维度region')
    protection(directory)
    return {**counts, 'version': result['version'], 'mode': result.get('mode', ''), 'screenshot': result.get('screenshot')}


def copy_image(path, name):
    if not png(path):
        return ''
    destination = OUT / 'images' / name
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, destination)
    return 'images/' + name


def figure(src, caption):
    if not src:
        return ''
    return f'<figure><a href="{html.escape(src)}"><img loading="lazy" src="{html.escape(src)}" alt="{html.escape(caption)}"></a><figcaption>{html.escape(caption)}</figcaption></figure>'


def previews():
    artifact = ROOT / f'build/releases/{VERSION}/guogaology-dimension-26.3-{VERSION}.jar'
    if not artifact.exists():
        return '<p class="pending">最终JAR尚未生成，物品预览待补。</p>'
    rows = []
    with ZipFile(artifact) as jar:
        for family, name in [('laver', '紫菜木门'), ('dread', '冥杉木门')]:
            source = f'assets/guogaology/textures/item/{family}_door.png'
            target = OUT / 'images' / f'{family}-door.png'
            target.write_bytes(jar.read(source))
            rows.append(f'<tr><td>{name}</td><td class="sprites"><img src="images/{family}-door.png" alt="{name}"></td><td>完整门扇物品图标</td></tr>')
        for tier, label in [('omega', 'ω'), ('epsilon', 'ε'), ('gamma', 'Γ'), ('true_omega', 'Ω')]:
            icons = []
            for suffix in ('', '_pulling_0', '_pulling_1', '_pulling_2'):
                filename = f'{tier}_bow{suffix}.png'
                (OUT / 'images' / filename).write_bytes(jar.read('assets/guogaology/textures/item/' + filename))
                icons.append(f'<img src="images/{filename}" alt="{label}弓{suffix}">')
            rows.append(f'<tr><td>{label}弓</td><td class="sprites">{"".join(icons)}</td><td>待机／拉弦Ⅰ／Ⅱ／Ⅲ</td></tr>')
    return '<p>从当前26.3正式JAR直接提取，按原像素放大；下表是素材预览，后面才是游戏截图。</p><table><tr><th>物品</th><th>贴图</th><th>说明</th></tr>' + ''.join(rows) + '</table>'


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    for key, default in [('native263', 'repair0610-263-final-d'), ('native262', 'repair0610-262-final-b'),
                         ('baseline', 'visual0610-baseline069-d'), ('sodium', 'visual0610-fixed-b'),
                         ('vanilla', 'visual0610-fixed-vanilla-b')]:
        parser.add_argument('--' + key, default=default, help='Actual disposable run folder name')
    args = parser.parse_args(argv)
    (OUT / 'images').mkdir(parents=True, exist_ok=True)
    (OUT / 'evidence').mkdir(exist_ok=True)
    runs = {
        '26.3主回归': collect(VISUAL / args.native263, native, '26.3'),
        '26.2重点回归': collect(ROOT / 'build/runtime-qa' / args.native262, native, '26.2'),
        '0.6.9视觉对照': collect(VISUAL / args.baseline, visual, True),
        '26.3 Sodium视觉': collect(VISUAL / args.sodium, visual, False, True),
        '26.3原版渲染视觉': collect(VISUAL / args.vanilla, visual, False, False),
        '0.6.9旧档升级': collect(VISUAL / 'save0610-upgrade069-a', save),
        '升级保存后再重开': collect(VISUAL / 'save0610-reopen069-a', save),
    }
    n263, n262 = runs['26.3主回归']['passed'], runs['26.2重点回归']['passed']
    native_state = '已验证' if n263 and n262 else ('26.3已验证；26.2待验证' if n263 else '待验证')
    visual_ok = runs['26.3 Sodium视觉']['passed'] and runs['26.3原版渲染视觉']['passed']
    item_state, item_note = '待验证', '等待旧版与新版真实掉落物对照，不将素材大小推断为已复现。'
    if visual_ok and runs['0.6.9视觉对照']['passed']:
        before = len(runs['0.6.9视觉对照']['data']['largeCandidates'])
        after = len(runs['26.3 Sodium视觉']['data']['largeCandidates'])
        item_state = '本轮未复现' if before == after == 0 else '需逐项复核'
        item_note = f'旧版/新版创造变体大尺寸候选为{before}/{after}；另有40个实际掉落实体（单个/64堆叠）及原版对照。未复现的反馈不写成已修复。'
    entries = [
        (1, '创造模式下模组弓的箭不能捡回', native_state, '四档弓检查创造与生存的实际射击／拾取，保留原生对照。'),
        (2, '弓拉弦物品图标错误', native_state, '四档×双手×七个拉弦时间点×五种显示上下文，共280组原生模型检查。'),
        (3, '弓强度提升较低', '已撤回，不调整', '按后续确认保留原伤害、箭初速度、耐久与晶核强度；不把拾取或图标修正混入数值调整。'),
        (4, '核爆菇与红沙无法合成TNT', native_state, '原生工作台验证16种沙子混搭；拒绝288种缺料／错误组合。'),
        (5, '两种木门物品栏贴图错误', native_state, '改为完整门扇图标，并检查实际加载素材的非透明轮廓。'),
        (6, '两种木材缺少箱船', native_state, '新增原生27槽箱船；实际菜单、组件/NBT磁盘往返、破坏后船和货物数量守恒。'),
        (7, '紫菜木系列燃料不完整', native_state, '18种紫菜木物品与原版木制品时长对照；冥杉仍无燃料、不可燃。'),
        (8, '一级果糕之心部分角度渲染错误', '分层修复已验证' if visual_ok else '待验证', '确证并修复实体面误入透明层：736个实体面、114个玻璃面；六方向及双渲染器对照。0.6.9最小场景未明显重现原反馈的角度症状。'),
        (9, '紫菜木栅栏不连接', native_state, '实际放置与自身／冥杉／橡木互连；下界砖栅栏保持原版不互连。'),
        (10, '两种告示牌存在碰撞', native_state, '两种木材的立地／墙面告示牌共80种状态无碰撞。'),
        (11, '缺少悬挂式告示牌', native_state, '两种木材×天花／墙边四形态；实际放置、双面写字和原生保存。碰撞与对应原版一致，保留墙挂杆碰撞。'),
        (12, '告示牌编辑GUI使用深面', native_state, '两套GUI分别288像素精确对照朝外浅纹理。'),
        (13, '软糖罐支持精准采集', native_state, '360次原生主破坏与56次额外开采对照，覆盖精准采集与非精准采集。'),
        (14, '其他物品掉落外观过大', item_state, item_note),
        (15, '空无晶核三级均去掉碰撞', native_state, '12个空无晶核状态无碰撞；另49个晶核状态作对照。'),
    ]
    rows = ''.join('<tr>' + ''.join(f'<td>{html.escape(str(value))}</td>' for value in entry) + '</tr>' for entry in entries)
    run_rows = ''.join(f'<tr><td>{html.escape(label)}</td><td>{"通过" if record["passed"] else "待验证"}</td><td>{html.escape(record.get("reason", "结构化断言、产物身份与隔离保护通过"))}</td></tr>' for label, record in runs.items())
    pictures = []
    for label, folder, key in [('旧版0.6.9', args.baseline, '0.6.9视觉对照'), ('修复版0.6.10', args.sodium, '26.3 Sodium视觉')]:
        if runs[key]['passed']:
            src = copy_image(VISUAL / folder / 'screenshots/visual0610-drops-near.png', folder + '-drops-near.png')
            pictures.append(figure(src, label + '：真实掉落物近景，模组物品与原版控制相邻'))
    hearts = []
    if runs['26.3 Sodium视觉']['passed']:
        for scene, caption in [('heart-nw', '西北'), ('heart-ne', '东北'), ('heart-se', '东南'), ('heart-sw', '西南'), ('heart-above', '上方'), ('heart-below', '下方')]:
            src = copy_image(VISUAL / args.sodium / 'screenshots' / f'visual0610-{scene}.png', scene + '.png')
            hearts.append(figure(src, '修复版果糕之心：' + caption))
    if runs['26.3原版渲染视觉']['passed']:
        src = copy_image(VISUAL / args.vanilla / 'screenshots/visual0610-heart-nw.png', 'heart-vanilla-nw.png')
        hearts.append(figure(src, '原版渲染器对照：西北'))
    summary = {'version': VERSION, 'runs': runs, 'issues': [dict(zip(('number', 'issue', 'status', 'details'), row)) for row in entries],
               'scope': '26.3 primary,26.2 focused; report has no production/QA mutations; missing evidence remains pending'}
    (OUT / 'evidence/report-data.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    page = '''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>果糕逻辑0.6.10修复验证</title><style>
body{margin:0;background:#f5f7fa;color:#243447;font:15px/1.7 system-ui,"Microsoft YaHei",sans-serif}main{max-width:1160px;margin:auto;padding:30px 22px 55px}h1{font-size:30px}h2{font-size:22px;margin-top:30px}table{border-collapse:collapse;width:100%;background:white;font-size:14px}th,td{border:1px solid #dae2ea;padding:10px 12px;text-align:left;vertical-align:top}th{background:#e8eef4}td:first-child{white-space:nowrap}.scroll{overflow:auto}.note{padding:14px 18px;background:#e8f0f7;border-left:4px solid #5485ad}.grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}figure{margin:0}figure img{width:100%;border-radius:6px;border:1px solid #d6e0e9}figcaption,small{color:#607080;font-size:13px}.sprites img{image-rendering:pixelated;width:64px;height:64px;margin-right:12px;background:#e4e7eb}a{color:#246d9c}.pending{color:#8a6333}details{margin-top:22px}code{overflow-wrap:anywhere}@media(max-width:720px){main{padding:18px 12px}.grid{grid-template-columns:1fr}h1{font-size:25px}td,th{padding:8px}}
</style><main><small>2026-10-23 · 0.6.10 · 独立隐藏测试实例</small><h1>15项反馈：修复与验证</h1><p class="note">第3项按后续确认撤回。第14项区分“未复现”和“已修复”。下表只有结构化断言、实际运行产物身份和隔离保护记录齐全时才显示已验证；尚缺的记录保持待验证。</p><div class="scroll"><table><tr><th>#</th><th>反馈</th><th>状态</th><th>处理与证据</th></tr>''' + rows + '''</table></div><h2>门与弓物品预览</h2>''' + previews() + '''<h2>掉落物近景对照</h2><div class="grid">''' + (''.join(pictures) or '<p class="pending">最终实际截图待生成。</p>') + '''</div><p><small>Sodium的独立高空测试台画面仅用于核对物体与观察角度，不能据此推断正常地形的光照表现。</small></p><h2>果糕之心：实际六向近景</h2><div class="grid">''' + (''.join(hearts) or '<p class="pending">最终实际截图待生成。</p>') + '''</div><h2>旧存档与回归</h2><p>旧ID和原有方块状态保留，新增箱船及吊牌使用新ID。0.6.9旧夹具升级与再次重开分别在独立目录进行；原始存档不作测试材料。下面列出实际记录完整性，未完成不算通过。</p><div class="scroll"><table><tr><th>验证</th><th>结果</th><th>证据</th></tr>''' + run_rows + '''</table></div><p>范围：26.3主测试，26.2重点回归；1.21.x仅构建与静态适配检查。此报告不代表穷举所有资源包、任意其他模组组合或任意损坏旧存档。</p><p><a href="evidence/report-data.json">结构化结果与每轮路径</a> · 最终JAR：<code>Guogaology/build/releases/0.6.10</code></p></main></html>'''
    (OUT / '修复验证.html').write_text(page, encoding='utf-8')
    print(json.dumps({'report': str(OUT / '修复验证.html'), 'completeRuns': sum(r['passed'] for r in runs.values()),
                      'totalRuns': len(runs)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
