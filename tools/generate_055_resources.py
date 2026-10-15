"""Regenerate the current acquisition pack after older broad art/content generators.

Committed assets remain build inputs; regular builds do not require Pillow.
"""
from generate_core_resources import generate as raw_resources
from generate_lamp_shaper import generate as lamp_shaper
from generate_lho_anchor_art import generate as anchor_art
from generate_equipment_enchanting import generate as equipment_enchanting
from generate_ordinal_crystal_ore import generate as ordinal_ore
from generate_sanctuary_loot import generate as landmark_loot

def generate():
    raw_resources()
    lamp_shaper()
    anchor_art()
    equipment_enchanting()
    ordinal_ore()
    landmark_loot()

if __name__=='__main__':generate()
