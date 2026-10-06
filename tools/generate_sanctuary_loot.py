"""Generate the sixteen current giant-landmark chest tables.

All rewards use existing survival materials. No retired content, equipment,
foreign-family cores or mineral-tier shortcuts enter these tables. Appearance-
dependent bricks/lamps are deliberately omitted instead of inventing components.
"""
from pathlib import Path
import argparse
import json
import math
import random

ROOT = Path(__file__).resolve().parents[1]
TABLES = ROOT / 'src/main/resources/data/googology/loot_table/chests'

# Entries are (registered item path, minimum count, maximum count). Every listed
# theme product is guaranteed once; integer uniform counts supply the variety.
THEMES = {
    'matrix': {
        'core': 'sequence_core',
        'main': [('matrix_archive_ceramic',8,16),('ridge_lamina',24,48),
                 ('projection_glass',24,48),('y_log',24,48),('y_leaves',16,32)],
        'side': [('ridge_lamina',4,12),('y_sequence_stone',4,12),('y_log',4,8)],
    },
    'power': {
        'core': 'power_tower_core',
        'main': [('recursive_bronze',8,16),('power_bricks',32,64),
                 ('tree_node_red',4,8),('tree_node_green',4,8),('tree_node_blue',4,8)],
        'side': [('power_bricks',8,16),('power_sand',8,16),('amber_inlay',2,6)],
    },
    'hydra': {
        'core': 'hydra_bud',
        'main': [('hydra_jade',8,16),('veblen_petal',24,48),('epsilon_turf',32,64),
                 ('omega_symbol',8,16),('phi_symbol',8,16)],
        'side': [('epsilon_turf',8,16),('psi_fern',2,6),('omega_bloom',2,6)],
    },
    'absence': {
        'core': 'lho_trace',
        'main': [('absence_glass',32,64),('lho_letter_l',12,24),('lho_letter_h',12,24),
                 ('lho_letter_o',12,24),('fffz_trace',4,8),('fos_trace',4,8)],
        'side': [('absence_glass',8,16),('lho_letter_l',2,6),
                 ('lho_letter_h',2,6),('lho_letter_o',2,6)],
    },
    'weaver': {
        'core': 'laver_core',
        'main': [('resonant_silk',8,16),('laver_planks',32,64),
                 ('basic_laver_pattern',16,32),('lty_yarn',8,16),('tianyi_fiber',8,16)],
        'side': [('laver_planks',8,16),('white_fiber',2,6),('giant_laver',4,8)],
    },
    'astra': {
        'core': 'astra_critical_core',
        'main': [('astra_compute_crystal',8,16),('compute_chip',16,32),
                 ('astra_marble',32,64),('astra_mint',16,32),('astra_light',8,16)],
        'side': [('astra_marble',8,16),('astra_mint',4,12),('compute_chip',2,6)],
    },
    'guogao': {
        'core': 'guogao_heart',
        'main': [('guogao_heart_resin',8,16),('rootbound_stone',32,64),
                 ('dread_log',32,64),('guogao_loam',24,48),
                 ('plain_amber_guogao',4,8),('plain_berry_guogao',4,8),
                 ('plain_lime_guogao',4,8),('plain_azure_guogao',4,8)],
        'side': [('guogao_loam',8,16),('dread_log',8,16),('plain_berry_guogao',2,6)],
    },
    'frontier': {
        'core': 'boundary_core',
        'main': [('axiom_porcelain',8,16),('set_jade',24,48),('logic_ivory',24,48),
                 ('rank_amber',16,32),('proof_stone',24,48)],
        'side': [('limit_stone',8,16),('formula_stone',4,12),('proof_stone',4,12)],
    },
}


def pool(name, minimum, maximum):
    count = minimum if minimum == maximum else {
        'type': 'minecraft:uniform', 'min': minimum, 'max': maximum}
    return {'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'googology:'+name,
            'functions': [{'function': 'minecraft:set_count', 'count': count}]}]}


def table(theme, main):
    spec = THEMES[theme]
    rewards = ([(spec['core']+'_lv3',1,1),('ordinal_crystal_lv3',1,1)] if main else
               [(spec['core'],6,12),('ordinal_crystal',30,60)])
    rewards += spec['main' if main else 'side']
    return {'type': 'minecraft:chest', 'pools': [pool(*entry) for entry in rewards]}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf8')


def audit():
    """Check the actual written JSON, exact guarantees, ranges and stack capacity."""
    rng = random.Random(312)
    reports = []
    models = ROOT / 'src/main/resources/assets/googology/models/item'
    blockstates = ROOT / 'src/main/resources/assets/googology/blockstates'
    known = {p.stem for p in models.glob('*.json')} | {p.stem for p in blockstates.glob('*.json')}
    for theme,spec in THEMES.items():
        for main in (True,False):
            name = theme + ('_sanctum' if main else '_ruin')
            data = json.loads((TABLES / (name+'.json')).read_text('utf8'))
            assert data == table(theme,main), name+' has drifted from the generator'
            bounds = {}
            for p in data['pools']:
                assert p['rolls'] == 1 and len(p['entries']) == 1
                entry = p['entries'][0]; item = entry['name'].split(':',1)[1]
                assert item in known, 'Unregistered inventory/block entrypoint: '+item
                assert entry['type'] == 'minecraft:item'
                assert len(entry['functions']) == 1
                count = entry['functions'][0]['count']
                lo,hi = (count,count) if isinstance(count,int) else (count['min'],count['max'])
                assert 1 <= lo <= hi <= 64, (name,item,lo,hi)
                assert item not in bounds, 'Duplicate guaranteed item '+item
                bounds[item] = (lo,hi)
            core = spec['core'] + ('_lv3' if main else '')
            ordinal = 'ordinal_crystal_lv3' if main else 'ordinal_crystal'
            assert bounds[core] == ((1,1) if main else (6,12))
            assert bounds[ordinal] == ((1,1) if main else (30,60))
            maximum_stacks = sum(math.ceil(hi/64) for lo,hi in bounds.values())
            assert maximum_stacks <= 27, name+' can overflow the chest'
            expected_core_roots = {core,ordinal}
            all_core_roots = {x['core'] for x in THEMES.values()} | {'ordinal_crystal'}
            for item in bounds:
                if any(item == root or item.startswith(root+'_lv') for root in all_core_roots):
                    assert item in expected_core_roots, 'Foreign/extra core '+item
                assert not item.endswith(('_ore','_material')), 'Mineral shortcut '+item
            seen_core=set(); seen_ordinal=set()
            for _ in range(10000):
                sample = {item:rng.randint(lo,hi) for item,(lo,hi) in bounds.items()}
                assert sum(math.ceil(count/64) for count in sample.values()) <= 27
                seen_core.add(sample[core]); seen_ordinal.add(sample[ordinal])
            assert seen_core == set(range(bounds[core][0],bounds[core][1]+1))
            assert seen_ordinal == set(range(bounds[ordinal][0],bounds[ordinal][1]+1))
            reports.append({'table':name,'guaranteed_core':core,'core_count':list(bounds[core]),
                            'ordinal_count':list(bounds[ordinal]),'maximum_initial_stacks':maximum_stacks,
                            'samples':10000,'theme_products':{k:list(v) for k,v in bounds.items() if k not in (core,ordinal)}})
    result={'tables':len(reports),'sampled_chests':sum(x['samples'] for x in reports),
            'maximum_initial_stacks':max(x['maximum_initial_stacks'] for x in reports),'results':reports,
            'notes':['All rewards are stackable existing materials with individual counts <=64.',
                     'Vanilla chest shuffle may split stacks into available empty slots; initial stacks remain <=27.',
                     'No appearance-bearing bricks/lamps, foreign cores/minerals or legacy table IDs.']}
    write(ROOT/'build/loot-0312.json',result)
    return result


def generate():
    for theme in THEMES:
        for main in (True,False):
            write(TABLES / (theme+('_sanctum' if main else '_ruin')+'.json'),table(theme,main))
    result = audit()
    print(f"Generated/audited {result['tables']} tables; {result['sampled_chests']} sampled chests; "
          f"at most {result['maximum_initial_stacks']} initial stacks / 27 slots")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true',help='Audit committed tables without rewriting them')
    args = parser.parse_args()
    if args.check:
        result = audit()
        print(f"PASS: {result['tables']} tables, {result['sampled_chests']} samples, "
              f"at most {result['maximum_initial_stacks']} initial stacks")
    else:
        generate()
