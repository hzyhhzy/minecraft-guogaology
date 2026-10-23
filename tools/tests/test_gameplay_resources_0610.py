"""Protect two host policies from immutable donor re-imports and port formats."""
import copy
import json
from pathlib import Path
import tempfile
import unittest

from generate_gameplay_fixes_0610 import ROOT, RECIPE, LOOT, generate, verify_archive
from prepare_outer_resources import prepare


class DirectoryArchive:
    def __init__(self, root): self.root = root
    def read(self, name): return (self.root / name).read_bytes()


class SmallGameplayPolicyTests(unittest.TestCase):
    def test_committed_source_is_current(self):
        self.assertEqual(generate(check=True), [])

    def test_host_policies_survive_all_four_donor_imports_and_reimport(self):
        build = ROOT / 'build'
        build.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(prefix='small0610-', dir=build) as folder:
            root = Path(folder).resolve()
            self.assertTrue(root.is_relative_to(build.resolve()))
            for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
                with self.subTest(target=target):
                    out = root / target
                    prepare(target, out)
                    verify_archive(DirectoryArchive(out), target)
                    original = (out / RECIPE).read_bytes(), (out / LOOT).read_bytes()
                    prepare(target, out)
                    self.assertEqual(((out / RECIPE).read_bytes(), (out / LOOT).read_bytes()), original)
                    advancement = json.loads((out / 'data/guogaology/advancement/recipes/tnt_from_nuke_mushroom.json').read_text('utf-8'))
                    self.assertIn('#minecraft:sand', json.dumps(advancement))

    def test_final_guard_rejects_restored_sand_only_recipe_or_double_silk_drop(self):
        from generate_gameplay_fixes_0610 import resources
        for target in ('1.21.1', '1.21.11', '26.2', '26.3'):
            expected = {p.as_posix(): v for p, v in resources(target).items()}
            class MemoryArchive:
                def __init__(self, values): self.values = values
                def read(self, name): return json.dumps(self.values[name]).encode()
            for mutation in ('sand-only', 'double-drop'):
                broken = copy.deepcopy(expected)
                if mutation == 'sand-only':
                    broken[RECIPE.as_posix()]['key']['#'] = {'item': 'minecraft:sand'} if target == '1.21.1' else 'minecraft:sand'
                else:
                    pool = broken[LOOT.as_posix()]['pools'][1]
                    pool.pop('condition' if target == '26.3' else 'conditions')
                with self.subTest(target=target, mutation=mutation), self.assertRaises(AssertionError):
                    verify_archive(MemoryArchive(broken), target)


if __name__ == '__main__': unittest.main()
