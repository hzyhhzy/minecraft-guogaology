"""The 1.21.1 adapter must preserve patch densities and unrelated providers."""
import copy
import json
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from prepare_legacy_resources import adapt


class LegacyResources(unittest.TestCase):
    def test_triangle_distribution(self):
        source = {"type": "minecraft:trapezoid", "min": -3, "max": 3, "plateau": 0}
        result = adapt(source)
        self.assertEqual([e["data"] for e in result["distribution"]], list(range(-3, 4)))
        self.assertEqual([e["weight"] for e in result["distribution"]], [1, 2, 3, 4, 3, 2, 1])

    def test_float_and_height_providers_unchanged(self):
        for source in (
            {"type": "minecraft:trapezoid", "min": 0.0, "max": 6.0, "plateau": 2.0},
            {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": -64}, "max_inclusive": {"absolute": 50}},
        ):
            self.assertEqual(adapt(source), source)

    def test_shared_placements_keep_attempt_counts_and_predicates(self):
        root = Path(__file__).resolve().parents[2] / "src/main/resources/data/guogaology/worldgen/placed_feature"
        for path in root.glob("patch_*_on_*.json"):
            source = json.loads(path.read_text("utf8"))
            snapshot = copy.deepcopy(source)
            output = adapt(source)
            self.assertEqual(source, snapshot, "authoring data must not mutate")
            self.assertEqual(len(source["placement"]), len(output["placement"]))
            for old, new in zip(source["placement"], output["placement"]):
                if old["type"] != "minecraft:random_offset":
                    self.assertEqual(old, new)
            self.assertEqual(output["feature"]["type"], "minecraft:simple_block")
            self.assertEqual(adapt(output), output, "incremental builds must be idempotent")

    def test_boulder_retains_substrate_predicate(self):
        source = {"type": "minecraft:block_blob", "config": {"state": {"Name": "minecraft:cobblestone"}, "can_place_on": {"type": "minecraft:matching_block_tag", "tag": "guogaology:ordinal_surface_replaceables"}}}
        self.assertEqual(adapt(source)["config"], source["config"])


if __name__ == "__main__":
    unittest.main()
