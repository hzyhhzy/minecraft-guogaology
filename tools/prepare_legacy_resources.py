"""Adapt copied release resources to 1.21.1, leaving shared modern inputs intact.

Modern grass placements already supply the patch count/offset: inline only the
single-plant feature, never another random_patch (which would multiply density).
"""
import argparse
from collections import Counter
import json
from pathlib import Path


def plant(name):
    state = {"Name": "minecraft:" + name}
    if name in ("large_fern", "tall_grass"):
        state["Properties"] = {"half": "lower"}
    return {"type": "minecraft:simple_state_provider", "state": state}


PLANTS = {
    "minecraft:grass": plant("short_grass"),
    "minecraft:taiga_grass": {"type": "minecraft:weighted_state_provider", "entries": [
        {"data": {"Name": "minecraft:short_grass"}, "weight": 1},
        {"data": {"Name": "minecraft:fern"}, "weight": 4}]},
    "minecraft:tall_grass": plant("tall_grass"),
    "minecraft:large_fern": plant("large_fern"),
    # These two plants were introduced after 1.21.1; use the closest old foliage.
    "minecraft:bush": plant("fern"),
    "minecraft:dry_grass": plant("dead_bush"),
}


def adapt(value):
    if isinstance(value, list):
        return [adapt(v) for v in value]
    if not isinstance(value, dict):
        return "guogaology:patch_bush_on_soil" if value == "minecraft:patch_bush" else value
    result = {k: adapt(v) for k, v in value.items()}
    # Height providers share this type ID but have min/max_inclusive, not min/max.
    if (result.get("type") == "minecraft:trapezoid"
            and isinstance(result.get("min"), int) and isinstance(result.get("max"), int)):
        low, high, plateau = result["min"], result["max"], result.get("plateau", 0)
        assert 0 <= plateau <= high - low
        first = (high - low - plateau) // 2
        second = high - low - first
        weights = Counter(low + a + b for a in range(first + 1) for b in range(second + 1))
        return {"type": "minecraft:weighted_list", "distribution": [
            {"data": n, "weight": w} for n, w in sorted(weights.items())]}
    feature = result.get("feature")
    if isinstance(feature, str) and feature in PLANTS:
        result["feature"] = {"type": "minecraft:simple_block", "config": {"to_place": PLANTS[feature]}}
    if result.get("type") == "minecraft:block_blob":
        result["type"] = "guogaology:legacy_block_blob"
    if result.get("type") == "minecraft:vegetation_patch":
        tag = result["config"].get("replaceable")
        if isinstance(tag, str) and not tag.startswith("#"):
            result["config"]["replaceable"] = "#" + tag
    if "textures" in result:
        result["textures"] = {k: v["sprite"] if isinstance(v, dict) else v
                              for k, v in result["textures"].items()}
    return result


def prepare(root):
    changed = 0
    for folder in (root / "data/guogaology/worldgen", root / "assets/guogaology/models"):
        for path in folder.rglob("*.json"):
            before = json.loads(path.read_text("utf-8"))
            after = adapt(before)
            if before != after:
                path.write_text(json.dumps(after, ensure_ascii=False, indent=2) + "\n", "utf-8")
                changed += 1
    print(f"1.21.1 resource adapter: {changed} resources")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=Path)
    prepare(parser.parse_args().directory.resolve())
