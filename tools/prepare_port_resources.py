"""Translate the shared content pack to one modern Minecraft resource/data format.

Source textures, notation catalogs, block models and balance data remain shared.
Only generated build directories are written. Never modifies a Minecraft instance.
"""
import argparse
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources"
TARGETS = json.loads((ROOT / "ports/targets.json").read_text())


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def ingredient(value):
    if isinstance(value, list):
        return [ingredient(v) for v in value]
    if isinstance(value, dict):
        if set(value) == {"item"}:
            return value["item"]
        if set(value) == {"tag"}:
            return "#" + value["tag"]
    return value


def model(name):
    return {"type": "minecraft:model", "model": name}


def state_selector(entries, fallback):
    """Read the same saved BLOCK_STATE component used when placing the item."""
    if not entries[0][0]:
        return model(entries[0][1])
    prop = next(iter(entries[0][0]))
    cases = []
    for value in dict.fromkeys(state[prop] for state, _ in entries):
        children = [({k: v for k, v in state.items() if k != prop}, name)
                    for state, name in entries if state[prop] == value]
        cases.append({"when": value, "model": state_selector(children, fallback)})
    return {"type": "minecraft:select", "property": "minecraft:block_state",
            "block_state_property": prop, "cases": cases, "fallback": fallback}


def convert_items(out):
    assets = out / "assets/googology"
    portable = 0
    for path in sorted((assets / "models/item").glob("*.json")):
        old = read(path)
        rendered = model("googology:item/" + path.stem)
        overrides = old.pop("overrides", [])
        if overrides and "googology:appearance" in overrides[0].get("predicate", {}):
            variants = read(assets / "blockstates" / path.name)["variants"]
            entries = [(dict(p.split("=", 1) for p in key.split(",")), (value[0] if isinstance(value, list) else value)["model"])
                       for key, value in variants.items()]
            rendered = state_selector(entries, rendered)
            portable += len(entries)
        elif overrides and "angle" in overrides[0].get("predicate", {}):
            rendered = {"type": "minecraft:range_dispatch", "property": "minecraft:compass",
                        "target": "lodestone", "wobble": True,
                        "fallback": rendered,
                        "entries": [{"threshold": v["predicate"]["angle"], "model": model(v["model"])}
                                    for v in overrides]}
        elif overrides and "pulling" in overrides[0].get("predicate", {}):
            # Match the vanilla bow's modern item model. Custom BowItem subclasses
            # need no additional numeric item property registration on these targets.
            rendered = {"type": "minecraft:condition", "property": "minecraft:using_item",
                        "on_false": rendered,
                        "on_true": {"type": "minecraft:range_dispatch",
                                    "property": "minecraft:use_duration", "scale": .05,
                                    "fallback": model(overrides[0]["model"]),
                                    "entries": [{"threshold": v["predicate"]["pull"],
                                                 "model": model(v["model"])}
                                                for v in overrides if "pull" in v["predicate"]]}}
        write(path, old)
        write(assets / "items" / path.name, {"model": rendered})
    return portable


def convert_biome(data):
    effects = data["effects"]
    attributes = data.setdefault("attributes", {})
    for key in ("fog_color", "sky_color", "water_fog_color"):
        if key in effects:
            attributes["minecraft:visual/" + key] = f"#{effects.pop(key):06x}"
    for key in ("water_color", "grass_color", "foliage_color"):
        if key in effects and isinstance(effects[key], int):
            effects[key] = f"#{effects[key]:06x}"
    ambient = {}
    for old, new in (("ambient_sound", "loop"), ("mood_sound", "mood"), ("additions_sound", "additions")):
        if old in effects:
            ambient[new] = effects.pop(old)
    if ambient:
        attributes["minecraft:audio/ambient_sounds"] = ambient
    if "music" in effects:
        attributes["minecraft:audio/background_music"] = {"default": effects.pop("music")}
    if "particle" in effects:
        p = effects.pop("particle")
        attributes["minecraft:visual/ambient_particles"] = [{"particle": p["options"], "probability": p["probability"]}]
    if isinstance(data.get("carvers"), dict):
        data["carvers"] = [entry for values in data["carvers"].values() for entry in values]


def convert_dimension(data, underworld, target, world_lighting=True):
    for key in ("effects", "ultrawarm", "natural", "piglin_safe", "respawn_anchor_works", "bed_works", "has_raids", "fixed_time"):
        data.pop(key, None)
    data.update(has_fixed_time=True, skybox="none" if underworld else "overworld", timelines=[])
    data["attributes"] = {
        "minecraft:gameplay/bed_rule": {"can_set_spawn": "always", "can_sleep": "always"},
        "minecraft:gameplay/can_start_raid": False,
        "minecraft:gameplay/respawn_anchor_works": False,
        "minecraft:gameplay/sky_light_level": 1.5 if underworld else 15.0,
        "minecraft:visual/sky_light_factor": 0.12 if underworld else 1.0,
        "minecraft:visual/sun_angle": 180.0 if underworld else 0.0,
        "minecraft:visual/moon_angle": 0.0 if underworld else 180.0,
        "minecraft:visual/star_brightness": 0.0,
        "minecraft:visual/cloud_height": 420.0,
    }
    if underworld:
        data["attributes"].update({"minecraft:visual/fog_start_distance": 112.0,
                                   "minecraft:visual/fog_end_distance": 256.0,
                                   "minecraft:visual/sky_color": "#121829"})
    if target != "1.21.11":
        data["default_clock"] = "minecraft:overworld"
        data["has_ender_dragon_fight"] = False
        if world_lighting:
            # Since 26.1 the lightmap reads ambient_light_color, not the legacy
            # ambient_light brightness floor. Supply light independent of skylight
            # occlusion; this also reaches Sodium/Voxy through the shared lightmap.
            # At the user's normal gamma=0.5, unlit covered surfaces are roughly
            # half as bright as the same exposed surface. Keep Guogao cool/dim.
            data["attributes"]["minecraft:visual/ambient_light_color"] = "#151C25" if underworld else "#454545"
            data["attributes"]["minecraft:visual/sky_light_factor"] = 0.15 if underworld else 0.73


def convert_263_value(value, cell_xz=4, cell_y=8):
    if isinstance(value, list):
        return [convert_263_value(v, cell_xz, cell_y) for v in value]
    if not isinstance(value, dict):
        return value
    if 'Name' in value:
        properties = value.get('Properties', {})
        suffix = '[' + ','.join(k+'='+v for k,v in properties.items()) + ']' if properties else ''
        return value['Name'] + suffix
    converted = {k: convert_263_value(v, cell_xz, cell_y) for k,v in value.items()}
    if converted.get('type') == 'minecraft:interpolated':
        converted['input'] = converted.pop('argument')
        converted.update(cell_size_xz=cell_xz, cell_size_y=cell_y)
    return converted


def convert_263_noise(data, out, name):
    xz = data['noise'].pop('size_horizontal') * 4
    y = data['noise'].pop('size_vertical') * 4
    data = convert_263_value(data, xz, y)
    router = data['noise_router']
    router['chunk_surface_level'] = router.pop('preliminary_surface_level')
    for key in ('barrier','fluid_level_floodedness','fluid_level_spread','lava','vein_gap','vein_ridged','vein_toggle'):
        router.pop(key, None)
    rule = data.pop('surface_rule')
    write(out / 'data/googology/worldgen/material_rule' / name, rule)
    data['material_rule'] = 'googology:' + Path(name).stem
    data.pop('aquifers_enabled', None)
    data.pop('ore_veins_enabled', None)
    # Players spawn in the overworld; the two custom dimensions use explicit portals.
    data['spawn_target'] = []
    return data


def convert_263_misc(out):
    def loot(value):
        if isinstance(value,list):return [loot(v) for v in value]
        if not isinstance(value,dict):return value
        result={k:loot(v) for k,v in value.items()}
        for old in ('function','condition'):
            if old in result and isinstance(result[old],str):result['type']=result.pop(old)
        if 'functions' in result:result['modifier']=result.pop('functions')
        if 'conditions' in result:
            conditions=result.pop('conditions')
            assert len(conditions)<=1,'Review compound predicates before porting'
            if conditions:result['condition']=conditions[0]
        return result
    for path in (out / 'data/googology/loot_table').rglob('*.json'):
        write(path,loot(read(path)))
    for path in (out / 'data/googology/advancement').rglob('*.json'):
        data=read(path)
        for criterion in data.get('criteria', {}).values():
            if criterion.get('trigger') == 'minecraft:recipe_unlocked':
                conditions=criterion['conditions']
                conditions['recipes']=[conditions.pop('recipe')]
        write(path,data)
    for path in (out / 'data/googology/worldgen/noise').glob('*.json'):
        data=read(path)
        amplitudes=data.pop('amplitudes')
        assert amplitudes == [1.0], 'Review nontrivial octave weighting before porting'
        write(path,{'base_octave':data['firstOctave'],'base_amplitude':1.0,'octave_count':1})
    for path in (out / 'data/googology/worldgen/configured_feature').glob('*.json'):
        data = read(path)
        data.update(data.pop('config', {}))
        write(out / 'data/googology/worldgen/feature' / path.name, data)
        path.unlink()
    for name, chance in [('plant',65),('leaf',50)]:
        write(out / f'data/googology/context_int_provider/compostable/{name}.json', {
            'type':'minecraft:number_dispatcher',
            'cases':[{'condition':{'type':'minecraft:match_block','blocks':'minecraft:composter','state':{'level':'0'}},'value':1}],
            'default':{'type':'minecraft:weighted_list','distribution':[{'data':1,'weight':chance},{'data':0,'weight':100-chance}]}})


def prepare(target, release):
    config = TARGETS[target]
    out = (ROOT / "ports" / target / "build/generated/resources").resolve()
    expected = ROOT.resolve() / "ports" / target / "build/generated/resources"
    assert out == expected and out.is_relative_to(ROOT.resolve()), "Build output must remain inside this workspace"
    if out.exists():
        shutil.rmtree(out)
    shutil.copytree(SOURCE, out)
    shutil.copytree(ROOT / 'src/client/resources', out, dirs_exist_ok=True)
    # Imported world data is rebuilt directly from its source contracts below;
    # don't run the host's conversion pipeline over those already-converted files.
    from prepare_outer_resources import clear_generated
    clear_generated(out)
    metadata = read(out / "fabric.mod.json")
    metadata["version"] = release
    metadata.pop('jars',None)  # The legacy mapping bridge is only packaged for 1.21.1.
    metadata["depends"].update(minecraft=target, java=f">={config['java']}",
                              fabricloader=">=0.19.3", **{"fabric-api": ">=" + config["fabric"]})
    # Modern dimensions express their atmosphere through environment attributes.
    metadata["mixins"] = ["googology.mixins.json"]
    write(out / "fabric.mod.json", metadata)
    (out / "googology.client.mixins.json").unlink()
    # Fabric supplies metadata for the active pack type. A shared numeric range cannot
    # describe both resource 75 and data 94 without conflicting legacy-format rules.
    (out / "pack.mcmeta").unlink(missing_ok=True)
    for path in (out / "data/googology/recipe").glob("*.json"):
        data = read(path)
        for key in ("ingredient", "ingredients", "left", "right", "catalyst"):
            if key in data:
                data[key] = ingredient(data[key])
        if "key" in data:
            data["key"] = {k: ingredient(v) for k, v in data["key"].items()}
        write(path, data)
    for path in (out / "data/googology/worldgen/biome").glob("*.json"):
        data = read(path)
        convert_biome(data)
        if target == '26.3':
            for entries in data.get('spawners', {}).values():
                for entry in entries:
                    low, high = entry.pop('minCount'), entry.pop('maxCount')
                    entry['count'] = low if low == high else {'type':'minecraft:uniform','min_inclusive':low,'max_inclusive':high}
            data['attributes']['minecraft:gameplay/natural_mob_spawns'] = {
                'modifier':'overlay', 'argument':{'spawns_by_category':data.pop('spawners', {}), 'spawn_costs':data.pop('spawn_costs', {})}}
        write(path, data)
    for path in (out / "data/googology/dimension_type").glob("*.json"):
        data = read(path)
        convert_dimension(data, path.stem == "guogao", target)
        write(path, data)
    for path in (out / "data/googology/worldgen/noise_settings").glob("*.json"):
        data = read(path)
        router = data['noise_router']
        density = router.pop('initial_density_without_jaggedness')
        router['preliminary_surface_level'] = {
            'type': 'minecraft:find_top_surface', 'density': density,
            'upper_bound': 320.0, 'lower_bound': -64, 'cell_height': 8}
        if target == '26.3':
            data = convert_263_noise(data, out, path.name)
        write(path, data)
    if target == '26.3':
        convert_263_misc(out)
    portable = convert_items(out)
    from prepare_outer_resources import prepare as prepare_outer
    prepare_outer(target, out)
    print(f"Prepared {target} resources: {portable} portable decoration variants")
    return out


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("target", choices=TARGETS)
    parser.add_argument("release")
    args = parser.parse_args()
    prepare(args.target, args.release)
