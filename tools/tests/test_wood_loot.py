"""Final-import guards for native wooden door/slab conservation and sign items."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from generate_wood_loot import FAMILIES, STRUCTURAL_SHAPES, generate, wood_loot
from package_multiversion import verify_wood_loot_policy
from prepare_outer_resources import prepare

ROOT = Path(__file__).resolve().parents[2]


def matches(value, block, state):
    if isinstance(value, list):
        return all(matches(term, block, state) for term in value)
    kind = value.get('condition', value.get('type'))
    if kind == 'minecraft:survives_explosion':
        return True  # Ordinary mining; native explosion randomness is unchanged.
    if kind == 'minecraft:all_of':
        return all(matches(term, block, state) for term in value['terms'])
    if kind in ('minecraft:block_state_property', 'minecraft:match_block'):
        return value.get('block', value.get('blocks')) == block and all(
            state.get(key) == expected for key, expected in value.get('properties', value.get('state', {})).items())
    raise AssertionError('Unexpected carpentry condition: ' + str(value))


def count(table, block, state):
    result = 0
    for pool in table['pools']:
        if not matches(pool.get('conditions', pool.get('condition', [])), block, state):
            continue
        for entry in pool['entries']:
            if not matches(entry.get('conditions', entry.get('condition', [])), block, state):
                continue
            amount = 1
            for function in entry.get('functions', entry.get('modifier', [])):
                if matches(function.get('conditions', function.get('condition', [])), block, state):
                    assert function.get('function', function.get('type')) == 'minecraft:set_count'
                    amount = function['count']
            result += amount * pool['rolls']
    return result


class WoodLootTests(unittest.TestCase):
    def test_committed_tables_match_the_shared_generator(self):
        self.assertEqual(generate(check=True), [])
        for shape in ('planks', 'stairs', 'fence', 'fence_gate', 'trapdoor', 'button', 'pressure_plate', 'sign', 'wall_sign'):
            actual = json.loads((ROOT / f'src/main/resources/data/guogaology/loot_table/blocks/dread_{shape}.json').read_text('utf8'))
            self.assertEqual(actual, wood_loot('dread', shape))

    def test_one_door_item_is_owned_only_by_the_lower_half(self):
        for family in FAMILIES:
            table = wood_loot(family, 'door')
            for half, expected in (('lower', 1), ('upper', 0)):
                for opened in ('false', 'true'):
                    with self.subTest(family=family, half=half, opened=opened):
                        self.assertEqual(count(table, f'guogaology:{family}_door', {'half': half, 'open': opened}), expected)

    def test_double_slabs_return_both_placed_items(self):
        for family in FAMILIES:
            for slab, expected in (('bottom', 1), ('top', 1), ('double', 2)):
                with self.subTest(family=family, slab=slab):
                    self.assertEqual(count(wood_loot(family, 'slab'), f'guogaology:{family}_slab', {'type': slab}), expected)

    def test_final_outer_import_preserves_rules_on_all_four_targets(self):
        (ROOT / 'build').mkdir(exist_ok=True)
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            with self.subTest(target=target), tempfile.TemporaryDirectory(dir=ROOT / 'build') as folder:
                root = Path(folder)
                prepare(target, root)
                class Archive:
                    def read(self, name):
                        return (root / name).read_bytes()
                verify_wood_loot_policy(Archive(), target)
                for family in FAMILIES:
                    for shape in STRUCTURAL_SHAPES:
                        block = f'guogaology:{family}_{shape}'
                        table = json.loads((root / f'data/guogaology/loot_table/blocks/{family}_{shape}.json').read_text('utf8'))
                        examples = (('half', 'lower', 1), ('half', 'upper', 0)) if shape == 'door' else (
                            ('type', 'bottom', 1), ('type', 'top', 1), ('type', 'double', 2))
                        for key, value, expected in examples:
                            self.assertEqual(count(table, block, {key: value}), expected)

    def test_packaging_rejects_unconditional_doors_and_lost_double_slabs(self):
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            for family in FAMILIES:
                for shape in STRUCTURAL_SHAPES:
                    tables = {f'{f}_{s}': wood_loot(f, s) for f in FAMILIES for s in STRUCTURAL_SHAPES}
                    table = tables[f'{family}_{shape}']
                    if shape == 'door':
                        table['pools'][0]['conditions'] = [{'condition': 'minecraft:survives_explosion'}]
                    else:
                        table['pools'][0]['entries'][0]['functions'] = []
                    if target == '26.3':
                        from prepare_port_resources import convert_263_loot
                        tables = {name: convert_263_loot(value) for name, value in tables.items()}
                    class Archive:
                        def read(self, name):
                            return json.dumps(tables[Path(name).stem]).encode()
                    with self.subTest(target=target, family=family, shape=shape), self.assertRaisesRegex(AssertionError, 'wooden door/slab loot'):
                        verify_wood_loot_policy(Archive(), target)

    def test_standing_and_wall_signs_return_one_matching_standing_item(self):
        for family in FAMILIES:
            for shape in ('sign', 'wall_sign'):
                name = f'{family}_{shape}'
                table = json.loads((ROOT / f'src/main/resources/data/guogaology/loot_table/blocks/{name}.json').read_text('utf8'))
                self.assertEqual(count(table, f'guogaology:{name}', {}), 1)
                self.assertEqual(table['pools'][0]['entries'][0]['name'], f'guogaology:{family}_sign')


if __name__ == '__main__':
    unittest.main()
