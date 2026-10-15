"""Native gear enchantment categories, shared by legacy and modern resources.

Tag membership describes equipment, while runtime sockets govern eligibility.
Manuscripts deliberately have no native enchantment category or enchantability.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MATERIALS = ('omega', 'epsilon', 'gamma', 'true_omega')
ARMOR = ('helmet', 'chestplate', 'leggings', 'boots')


def generated_tags():
    tools = lambda kind: [f'guogaology:{m}_{kind}' for m in ('number', *MATERIALS)]
    pieces = lambda kind: [f'guogaology:{m}_{kind}' for m in MATERIALS]
    swords, picks, bows = tools('sword'), tools('pickaxe'), pieces('bow')
    armor = [item for kind in ARMOR for item in pieces(kind)]
    return {
        'swords': swords, 'pickaxes': picks,
        'enchantable/sword': swords,
        # Modern Sharpness uses melee_weapon; legacy still uses sword. Keeping
        # both data tags avoids changing vanilla enchantments in either target.
        'enchantable/melee_weapon': swords,
        'enchantable/mining': picks,
        'enchantable/bow': bows,
        'enchantable/durability': swords + picks + bows + armor,
        'enchantable/armor': armor,
        **{f'enchantable/{slot}_armor': pieces(kind) for slot, kind in
           zip(('head', 'chest', 'leg', 'foot'), ARMOR)},
    }


def generate(resource_root=None):
    target = Path(resource_root) if resource_root is not None else ROOT / 'src/main/resources'
    for name, values in generated_tags().items():
        path = target / f'data/minecraft/tags/item/{name}.json'
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps({'replace': False, 'values': values}, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    generate()
