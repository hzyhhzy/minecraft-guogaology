"""Iron-density and mother-rock/biome scope regressions for Ordinal Crystal ore."""
from copy import deepcopy
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from generate_ordinal_crystal_ore import (ROOT, BIOMES, INNER_BIOMES, UNDERWORLD_BIOMES,
    BATCHES, HOSTS, FEATURES, LEGACY_FEATURES, RETIRED, ORE_IDS, add_biome_features, generate, ore_config, placed_config, owned_resources)


class OrdinalCrystalOreTests(unittest.TestCase):
    def test_uniform_all_elevations_and_native_small_veins(self):
        self.assertEqual(set(BATCHES), {'uniform', 'lho'})
        for batch in BATCHES:
            placement = placed_config(batch)['placement']
            height = placement[2]['height']
            self.assertEqual(height, {'type':'minecraft:uniform', 'min_inclusive':{'above_bottom':0}, 'max_inclusive':{'below_top':0}})
            self.assertEqual(placement[-1], {'type':'minecraft:biome'})
        config = ore_config(9)
        self.assertEqual(config['type'], 'minecraft:ore')
        self.assertEqual(config['config']['size'], 9)
        self.assertEqual(config['config']['discard_chance_on_air_exposure'], 0)

    def test_lho_vein_frequency_is_exactly_one_fifth(self):
        ordinary, lho = (placed_config(batch) for batch in ('uniform','lho'))
        self.assertEqual(ordinary['placement'][0]['count'], 5*lho['placement'][0]['count'])
        self.assertGreater(lho['placement'][0]['count'], 0)
        self.assertEqual(ordinary['feature'], lho['feature'])
        self.assertEqual(ordinary['placement'][1:], lho['placement'][1:])

    def test_only_twelve_exact_host_blocks_and_no_minerals_or_bedrock(self):
        hosts = [value for values in HOSTS.values() for value in values]
        expected = {'guogaology:ordinal_stone' + (f'_{i}' if i else '') for i in range(10)}
        expected.update(('guogaology:absence_glass', 'guogaology:rootbound_stone'))
        self.assertEqual(set(hosts), expected)
        targets = ore_config(9)['config']['targets']
        self.assertEqual([target['state']['Name'] for target in targets], ['guogaology:' + ore for ore in ORE_IDS])
        self.assertFalse(any('bedrock' in value or value.endswith('_ore') for value in hosts))

    def test_all_eleven_biomes_attach_one_correct_feature_without_legacy_bands(self):
        self.assertEqual(len(INNER_BIOMES), 7)
        self.assertEqual(len(UNDERWORLD_BIOMES), 4)
        self.assertEqual(len(set(BIOMES)), 11)
        for name in BIOMES:
            biome = json.loads((ROOT / f'src/main/resources/data/guogaology/worldgen/biome/{name}.json').read_text())
            expected = FEATURES[1 if name=='lho_absence' else 0]
            self.assertEqual([f for f in biome['features'][6] if f in FEATURES], [expected], name)
            for feature in FEATURES + LEGACY_FEATURES:
                self.assertEqual(sum(stage.count(feature) for stage in biome['features']), int(feature==expected), name)
        for relative in RETIRED:
            self.assertFalse((ROOT/'src/main/resources/data/guogaology'/relative).exists())

    def test_biome_update_preserves_other_features_and_is_idempotent(self):
        fixture = {'effects': {'sky_color': 123}, 'features': [[], [], [], [], [], [], ['minecraft:ore_coal', *LEGACY_FEATURES], [], [], ['guogaology:monumental_landmarks']]}
        before = deepcopy(fixture)
        updated = add_biome_features(fixture, 'lho_absence')
        self.assertEqual(fixture, before)
        self.assertEqual(updated['features'][6][0], 'minecraft:ore_coal')
        self.assertEqual(updated['features'][9], fixture['features'][9])
        self.assertEqual(add_biome_features(updated, 'lho_absence'), updated)

    def test_generated_configs_roundtrip_and_outer_biomes_are_untouched(self):
        scratch = ROOT / 'build/test-ordinal-crystal-ore'
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as temp:
            resources = Path(temp)
            folder = resources / 'data/guogaology/worldgen/biome'
            folder.mkdir(parents=True)
            for name in BIOMES:
                (folder / f'{name}.json').write_text(json.dumps({'features': [[] for _ in range(11)]}))
            sentinel = folder / 'bms_mountains.json'
            sentinel.write_text('{"outer":"untouched"}')
            generate(resources)
            self.assertEqual(sentinel.read_text(), '{"outer":"untouched"}')
            self.assertEqual(generate(resources, check=True), [])
            for relative, expected in owned_resources().items():
                committed = ROOT / 'src/main/resources/data/guogaology' / relative
                self.assertEqual(json.loads(committed.read_text()), expected)


if __name__ == '__main__':
    unittest.main()
