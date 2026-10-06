"""Revision47 bilingual core wording, applied after historical generators.

These strings match the checked 0.3.12 copy catalog. Keep this final overlay
current when changing core roles or their UI; older progression generators may
still emit historical roles before calling generate(). --check never writes.
"""
from pathlib import Path
import argparse
import json

ROOT = Path(__file__).resolve().parents[1]

GEAR = [
    ('额外开采', 'Extra mining'),
    ('提高攻击力', 'Attack damage'),
    ('时运／抢夺；弓箭能力', 'Fortune / Looting; bow abilities'),
    ('降低耐久消耗', 'Reduced durability consumption'),
    ('水域适应', 'Aquatic adaptation'),
    ('命中爆裂', 'Impact burst'),
    ('伤害防护', 'Damage protection'),
    ('施加负面状态', 'On-hit debuffs'),
    ('仅供手稿：攻防、恢复与延时', 'Manuscript only: combat, healing and duration'),
]
BOOK = [
    ('工具效率', 'Tool efficiency'),
    ('提高攻击力', 'Attack damage'),
    ('延长挖掘与攻击触距', 'Longer block and entity reach'),
    ('减少普通索敌；高阶夜视', 'Reduced ordinary targeting; high-grade night vision'),
    ('治疗；高阶饥饿保障', 'Healing; high-grade food floor'),
    ('不能镶入手稿', 'Cannot be socketed into manuscripts'),
    ('防护、跳跃、飞行与防火', 'Protection, jumping, flight and fire resistance'),
    ('生命、状态免疫与图腾', 'Health, debuff immunity and inventory totems'),
    ('攻防、放大治疗、延长果糕状态', 'Combat, amplified healing and longer Guogao debuffs'),
]
EXTRA = {
    'manuscript.passive': ('持于主手或副手被动生效，副手优先', 'Passive while held; offhand takes priority'),
    'manuscript.open': ('右键：直接镶嵌晶核', 'Right-click: socket cores directly'),
    'manuscript_label': ('扽西手稿', 'Denxi Manuscript'),
    'manuscript_drag_hint': ('直接取放晶核 · 手稿保持在手中', 'Move cores directly · keep this manuscript held'),
    'status_attack': ('攻击：%s HP', 'Attack: %s HP'),
    'status_mining': ('挖速：×%s', 'Mining: ×%s'),
    'status_mining_speed': ('基础挖速：%s', 'Base mining speed: %s'),
    'status_yield': ('增产等级：%s', 'Yield level: %s'),
    'status_range': ('额外开采：%s 格', 'Extra mining: %s blocks'),
    'status_reach': ('触距：+%s 格', 'Reach: +%s blocks'),
    'status_healing': ('治疗：%s HP / 4 秒', 'Healing: %s HP / 4 s'),
    'status_health': ('生命上限：+%s HP', 'Max health: +%s HP'),
    'status_defense': ('承伤：×%s', 'Damage taken: ×%s'),
    'status_wear': ('耐久保护：×%s', 'Wear protection: ×%s'),
    'status_jump': ('跳高：+%s 格', 'Jump height: +%s blocks'),
    'status_flight': ('飞行：%s', 'Flight: %s'),
    'flight_slow': ('乐魂式慢速飞行', 'Slow flight'),
    'flight_normal': ('创造式飞行', 'Creative-style flight'),
    'flight_deep': ('创造式飞行 · 冲刺八倍', 'Creative-style flight · 8× sprint'),
    'native_armor': ('护甲 %s · 韧性 %s', 'Armor %s · Toughness %s'),
    'silk_on': ('精准采集：开启', 'Silk Touch: ON'),
    'silk_off': ('精准采集：关闭', 'Silk Touch: OFF'),
    'bow_speed': ('箭初速度：×%s', 'Arrow launch speed: ×%s'),
    'bow_native_damage': ('箭伤随实际速度与蓄力变化', 'Arrow damage varies with speed and charge'),
    'status_stealth': ('普通索敌抑制：Lv%s', 'Ordinary targeting suppression: Lv%s'),
    'status_food': ('饥饿下限：%s/20', 'Food floor: %s/20'),
    'status_efficiency': ('效率：%s级', 'Efficiency: level %s'),
    'status_burst': ('爆裂伤害：%s%%', 'Burst damage: %s%%'),
    'status_duration': ('果糕状态：%s秒', 'Guogao debuffs: %s s'),
    'status_oxygen': ('氧气消耗：×%s', 'Oxygen consumption: ×%s'),
    'status_water': ('水下行走：%s级', 'Depth Strider: level %s'),
    'bow_infinity': ('无限：保留一支普通箭即可', 'Infinity: keep one ordinary arrow'),
    'bow_multishot': ('多重射击：三箭，同目标不重复直击', 'Multishot: three arrows, one direct hit per target'),
    'bow_pierce': ('贯穿：每箭最多命中两个生物', 'Piercing: up to two creatures per arrow'),
    'bow_speed_tooltip': ('箭初速度：原版的%s倍', 'Arrow launch speed: %s× vanilla'),
    'bow_ammo': ('特殊箭每轮消耗一个，仅中央箭保留效果', 'One special arrow per volley; only the center retains its effect'),
}


def core_copy(index):
    values = {}
    # Registry/material stages stay 1..4. The natural base crystal is ungraded;
    # its three crafted upgrades display as Ordinal Core grades 1..3.
    for material_stage in range(1, 5):
        suffix = '' if material_stage == 1 else f'_lv{material_stage}'
        values[f'block.googology.ordinal_crystal{suffix}'] = (
            ('序数晶体' if index == 0 else 'Ordinal Crystal') if material_stage == 1
            else ('序数晶核' if index == 0 else 'Ordinal Core') + f' · Lv{material_stage - 1}')
    for level in range(1, 4):
        suffix = '' if level == 1 else f'_lv{level}'
        values[f'block.googology.laver_core{suffix}'] = (
            '紫菜凝核' if index == 0 else 'Laver Condensation Core') + f' · Lv{level}'
    for i in range(9):
        values[f'mining.googology.effect.{i}'] = GEAR[i][index]
        values[f'mining.googology.manuscript.effect.{i}'] = BOOK[i][index]
    values.update({'mining.googology.' + key: pair[index] for key, pair in EXTRA.items()})
    return values


def generate(root=ROOT, *, check=False):
    """Restore current copy without deleting unrelated or future UI fields.

    An alternate root lets tests exercise historical inputs in temporary copies.
    Already-current files are left byte-for-byte untouched, even in write mode.
    """
    root = Path(root)
    path = root / 'tools/copy_catalog.json'
    catalog = json.loads(path.read_text(encoding='utf8'))
    for locale, index in [('zh_cn', 0), ('en_us', 1)]:
        catalog['translations'][locale].update(core_copy(index))
    expected = {path: catalog}
    for locale in ('zh_cn', 'en_us'):
        path = root / f'src/main/resources/assets/googology/lang/{locale}.json'
        values = json.loads(path.read_text(encoding='utf8'))
        for key in catalog['removed']:
            values.pop(key, None)
        values.update(catalog['translations'][locale])
        expected[path] = values
    changed = []
    for path, values in expected.items():
        if json.loads(path.read_text(encoding='utf8')) == values:
            continue
        changed.append(path)
        if not check:
            path.write_text(json.dumps(values, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    action = 'Checked' if check else 'Updated'
    print(f'{action} revision47 bilingual core copy; {len(changed)} files ' +
          ('would change' if check else 'changed'))
    return changed

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='verify current copy without writing')
    args = parser.parse_args()
    changed = generate(check=args.check)
    if args.check and changed:
        for path in changed:
            print(f'Outdated copy: {path.relative_to(ROOT)}')
        raise SystemExit(1)
