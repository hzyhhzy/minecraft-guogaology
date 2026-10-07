"""Maintain only core harvesting profiles, tool tags, and existing reference rows.

Run with --check in audits. This deliberately does not regenerate world art,
recipes, ores, loot, or other materials. Raw Ordinal stages remain 1..4.
"""
import argparse
import copy
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
REGIONAL = ('sequence_core', 'power_tower_core', 'hydra_bud', 'lho_trace',
            'laver_core', 'astra_critical_core', 'boundary_core', 'guogao_heart')
AUXILIARIES = ('tree_node_red', 'tree_node_green', 'tree_node_blue',
               'fffz_trace', 'fos_trace', 'lho_hydra_psi', 'lho_hydra_z')
ANCHORED = frozenset(('lho_trace', 'fffz_trace', 'fos_trace', 'lho_hydra_psi', 'lho_hydra_z'))
HARDNESS = {root + ('' if grade == 1 else f'_lv{grade}'): 25 * 2 ** (grade - 1)
            for root in REGIONAL for grade in range(1, 4)}
HARDNESS.update({f'ordinal_crystal_lv{stage}': 25 * 2 ** (stage - 2) for stage in range(2, 5)})
HARDNESS.update({name: 25 for name in AUXILIARIES})
HARDNESS['ordinal_crystal'] = 2
IDS = frozenset('googology:' + name for name in HARDNESS)


def read(path):
    return json.loads(path.read_text(encoding='utf8'))


def profile_updates(profiles):
    result = copy.deepcopy(profiles)
    for name, hardness in HARDNESS.items():
        p = result[name]
        p.update(hardness=hardness, tool='pickaxe', tier=0)
        if name in ANCHORED:
            p['use'] = '序数晶体锚定或已稳定后，徒手／任意工具完整采集；镐加速；放置后永久稳定'
        elif name == 'ordinal_crystal':
            p['use'] = '徒手／任意工具掉2～4晶屑，镐加速；时运上限4；精准采集完整方块；爆炸衰减'
        else:
            p['use'] = re.sub(r'自然方块铁镐采集|铁镐采集完整方块|铁镐采集',
                              '徒手／任意工具完整采集，镐加速', p['use'])
    return result


def tag_updates(data, name):
    result = copy.deepcopy(data)
    if name == 'mineable/pickaxe':
        result['values'] += sorted(IDS - set(result['values']))
    else:
        result['values'] = [v for v in result['values'] if (v if isinstance(v, str) else v.get('id')) not in IDS]
    return result


def reference_updates(source, profiles, english):
    lines = source.splitlines()
    for i, line in enumerate(lines):
        if not line.startswith('| '):
            continue
        columns = [c.strip() for c in line.split('|')[1:-1]]
        if len(columns) != 9:
            continue
        name = columns[1].strip('`')
        if name not in HARDNESS:
            continue
        columns[2] = str(profiles[name]['hardness'])
        columns[4] = 'Pickaxe' if english else '镐 / 无'
        if english:
            columns[5] = 'None'
        else:
            columns[8] = profiles[name]['use']
        lines[i] = '| ' + ' | '.join(columns) + ' |'
    return '\n'.join(lines) + '\n'


def generate(check=False):
    changed = []
    def save(path, value):
        text = json.dumps(value, ensure_ascii=False, indent=2) + '\n' if not isinstance(value, str) else value
        if path.read_text(encoding='utf8') != text:
            changed.append(str(path.relative_to(ROOT)))
            if not check:
                path.write_text(text, encoding='utf8', newline='\n')
    path = RES / 'googology/block_balance.json'
    profiles = profile_updates(read(path))
    save(path, profiles)
    tags = RES / 'data/minecraft/tags/block'
    for path in tags.rglob('*.json'):
        name = path.relative_to(tags).with_suffix('').as_posix()
        if name.startswith(('mineable/', 'needs_', 'incorrect_for_')):
            before = read(path)
            after = tag_updates(before, name)
            if before != after:
                save(path, after)
    for english in (False, True):
        path = ROOT / 'docs' / ('BLOCK-BALANCE.en.md' if english else 'BLOCK-BALANCE.md')
        save(path, reference_updates(path.read_text(encoding='utf8'), profiles, english))
    if check and changed:
        raise SystemExit('Core harvest generated files need updates: ' + ', '.join(changed))
    print(f'CORE_HARVEST_043_OK profiles={len(HARDNESS)} changed={len(changed)} check={check}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    generate(parser.parse_args().check)
