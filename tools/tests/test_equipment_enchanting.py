"""Guard the native data categories needed by every enchantable gear family."""
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from generate_equipment_enchanting import generate, generated_tags


class EquipmentEnchantingTagsTests(unittest.TestCase):
    def test_every_mineral_and_numeric_tool_gets_its_native_category(self):
        tags = generated_tags()
        for material in ('number', 'omega', 'epsilon', 'gamma', 'true_omega'):
            for kind, category in [('pickaxe', 'mining'), ('sword', 'sword')]:
                item = f'guogaology:{material}_{kind}'
                self.assertIn(item, tags[f'enchantable/{category}'])
                self.assertIn(item, tags['enchantable/durability'])
            self.assertIn(f'guogaology:{material}_sword', tags['enchantable/melee_weapon'])

    def test_bows_and_all_four_armor_parts_are_covered_without_books(self):
        tags = generated_tags()
        for material in ('omega', 'epsilon', 'gamma', 'true_omega'):
            self.assertIn(f'guogaology:{material}_bow', tags['enchantable/bow'])
            self.assertIn(f'guogaology:{material}_bow', tags['enchantable/durability'])
            for kind, category in [('helmet', 'head'), ('chestplate', 'chest'),
                                   ('leggings', 'leg'), ('boots', 'foot')]:
                item = f'guogaology:{material}_{kind}'
                self.assertIn(item, tags['enchantable/armor'])
                self.assertIn(item, tags[f'enchantable/{category}_armor'])
                self.assertIn(item, tags['enchantable/durability'])
        self.assertFalse(any('manuscript' in item for values in tags.values() for item in values))

    def test_generation_extends_vanilla_and_roundtrips_checked_in_data(self):
        root = Path(__file__).resolve().parents[2] / 'src/main/resources'
        scratch = Path(__file__).resolve().parents[2] / 'build/test-equipment-enchanting'
        scratch.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as temp:
            generate(temp)
            for name, values in generated_tags().items():
                relative = f'data/minecraft/tags/item/{name}.json'
                data = json.loads((Path(temp) / relative).read_text())
                self.assertFalse(data['replace'], name)
                self.assertEqual(len(values), len(set(values)), name)
                self.assertEqual(json.loads((root / relative).read_text()), data, name)


if __name__ == '__main__':
    unittest.main()
