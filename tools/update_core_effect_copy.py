"""Revision48 bilingual core wording, applied after historical generators.

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
    ('提高基础挖速与急迫乘区', 'Mining speed and Haste bonus'),
    ('提高攻击力', 'Attack damage'),
    ('延长挖掘与攻击触距', 'Longer block and entity reach'),
    ('减少普通索敌；高阶夜视', 'Reduced ordinary targeting; high-grade night vision'),
    ('治疗；高阶饥饿保障', 'Healing; high-grade food floor'),
    ('不能镶入手稿', 'Cannot be socketed into manuscripts'),
    ('防护、步行跑步、跳跃与飞行', 'Protection, walking/sprinting, jumping and flight'),
    ('生命、状态免疫与图腾', 'Health, debuff immunity and inventory totems'),
    ('攻防、放大治疗、延长果糕状态', 'Combat, amplified healing and longer Guogao debuffs'),
]
EXTRA = {
    'slots': ('晶核槽位：%s / %s', 'Core slots: %s / %s'),
    'manuscript.passive': ('持于主手或副手被动生效，副手优先', 'Passive while held; offhand takes priority'),
    'manuscript.open': ('背包手稿按钮：镶嵌；主手持有时也可右键', 'Inventory Book button: socket cores; main-hand right-click also works'),
    'manuscript.button': ('手稿', 'Book'),
    'manuscript.button_hint': ('编辑副手手稿；关闭页面后生效', 'Edit offhand manuscript; changes activate when closed'),
    'manuscript.preview': ('效果预览', 'Effect preview'),
    'manuscript.socket_totem': ('致命伤害时消耗一个；关闭镶嵌页后生效', 'Consumed on lethal damage; activates when the socket screen closes'),
    'manuscript.totem_slots': ('手稿内图腾：%s', 'Socketed totems: %s'),
    'manuscript_label': ('扽西手稿', 'Manuscript'),
    'manuscript_drag_hint': ('手稿留在手中 · 拖动晶核镶嵌', 'Keep held · drag cores'),
    'status_attack': ('攻击：%s HP', 'Attack: %s HP'),
    'status_attack_factor': ('攻击：%s', 'Attack: %s'),
    'status_base_attack': ('基础攻击：%s HP', 'Base attack: %s HP'),
    'status_mining': ('挖速：×%s', 'Mining: ×%s'),
    'status_mining_speed': ('基础挖速：%s', 'Base mining speed: %s'),
    'status_enchantment': ('%s：%s', '%s: %s'),
    'status_range': ('额外开采：+%s 格', 'Extra mining: +%s blocks'),
    'status_reach': ('触距：+%s 格', 'Reach: +%s blocks'),
    'status_healing': ('治疗：+%s HP / 4 秒', 'Healing: +%s HP / 4 s'),
    'status_health': ('生命上限：+%s HP', 'Max health: +%s HP'),
    'status_defense': ('防护：×%s', 'Protection: ×%s'),
    'status_armor_protection': ('本件防护贡献：+%s%%', 'Armor contribution: +%s%%'),
    'status_duration_factor': ('果糕持续时间：×%s', 'Guogao duration: ×%s'),
    'status_wear': ('耐久保护：×%s', 'Wear protection: ×%s'),
    'status_walking': ('步行/跑步：+%s%%', 'Walk/sprint: +%s%%'),
    'status_landing': ('摔落/撞墙：×%s / ×%s', 'Fall/wall: ×%s / ×%s'),
    'status_safe_fall': ('安全落差：×%s', 'Safe fall: ×%s'),
    'status_jump': ('跳高：+%s 格', 'Jump height: +%s blocks'),
    'status_flight': ('飞行：%s', 'Flight: %s'),
    'flight_slow': ('缓速飞行', 'Slow flight'),
    'flight_normal': ('自由飞行 · 冲刺两倍', 'Free · sprint ×2'),
    'flight_deep': ('自由飞行 · 冲刺四倍', 'Free · sprint ×4'),
    'native_armor': ('护甲 +%s · 韧性 +%s', 'Armor +%s · Toughness +%s'),
    'silk_on': ('精准采集：开启', 'Silk Touch: ON'),
    'silk_off': ('精准采集：关闭', 'Silk Touch: OFF'),
    'bow_speed': ('箭初速度：×%s', 'Arrow launch speed: ×%s'),
    'bow_native_damage': ('箭伤随实际速度与蓄力变化', 'Arrow damage varies with speed and charge'),
    'status_stealth': ('普通索敌抑制：Lv%s', 'Mob targeting: Lv%s'),
    'status_food': ('饥饿下限：%s/20', 'Food floor: %s/20'),
    'status_mining_flat': ('挖速加值：+%s', 'Mining speed: +%s'),
    'status_mining_rate': ('挖速倍率：×%s', 'Mining multiplier: ×%s'),
    'status_burst': ('额外爆裂：+%s%%', 'Extra burst: +%s%%'),
    'status_duration': ('果糕状态：%s秒', 'Guogao debuffs: %s s'),
    'status_oxygen': ('氧气消耗：×%s', 'Oxygen consumption: ×%s'),
    'status_water': ('水下行走：%s级', 'Depth Strider: level %s'),
    'bow_infinity': ('无限：保留一支普通箭即可', 'Infinity: keep one ordinary arrow'),
    'bow_multishot': ('多重射击：三箭，同目标不重复直击', 'Multishot: three arrows, one direct hit per target'),
    'bow_pierce': ('穿透：每箭最多命中两个生物', 'Piercing: up to two creatures per arrow'),
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
    retired_keys = {'mining.googology.status_yield', 'mining.googology.status_efficiency', 'mining.googology.grade_limit'}
    catalog['removed'] = sorted(set(catalog['removed']) | retired_keys)
    for locale, index in [('zh_cn', 0), ('en_us', 1)]:
        for retired in retired_keys:
            catalog['translations'][locale].pop(retired, None)
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
    print(f'{action} revision48 bilingual core copy; {len(changed)} files ' +
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
