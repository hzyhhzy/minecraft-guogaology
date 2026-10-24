package dev.guogaology.mining;

import java.util.ArrayList;
import java.util.List;

/** Discrete equipment capabilities only; numeric bonuses belong in the station preview. */
public final class EquipmentTooltipRules {
    private EquipmentTooltipRules() {}

    /** Input is the carrier's effective profile, not its unvalidated saved socket list. */
    public static List<String> traitKeys(int kind,List<EquipmentRules.Core> profile,boolean silk,boolean socketTotem) {
        var active=profile.stream().filter(core->EquipmentRules.compatible(kind,core.type())).toList();
        var traits=new ArrayList<String>();
        int branch=EquipmentRules.highest(active,2),absence=EquipmentRules.highest(active,3);
        int water=EquipmentRules.highest(active,4),boundary=EquipmentRules.highest(active,6),guogao=EquipmentRules.highest(active,7);
        if(kind==0) {
            if(EquipmentRules.highest(active,0)>0)traits.add("mining.guogaology.tooltip.trait.pattern_mining");
            if(branch>0)traits.add(silk?"enchantment.minecraft.silk_touch":"enchantment.minecraft.fortune");
        } else if(kind==1&&branch>0)traits.add("enchantment.minecraft.looting");
        else if(kind==EquipmentRules.BOW) {
            if(branch>=1)traits.add("enchantment.minecraft.infinity");
            if(branch>=2)traits.add("enchantment.minecraft.multishot");
            if(branch>=3)traits.add("enchantment.minecraft.piercing");
        }
        if(kind==1||kind==EquipmentRules.BOW) {
            if(EquipmentRules.highest(active,5)>0)traits.add("mining.guogaology.tooltip.trait.impact_burst");
            if(guogao>0)traits.add("mining.guogaology.tooltip.trait.on_hit_debuffs");
        }
        if(kind==2&&water>0) {
            traits.add("enchantment.minecraft.respiration");
            traits.add("enchantment.minecraft.aqua_affinity");
        } else if(kind==5&&water>0)traits.add("enchantment.minecraft.depth_strider");
        if(kind==EquipmentRules.MANUSCRIPT) {
            if(absence>0)traits.add("mining.guogaology.tooltip.trait.stealth");
            if(absence>=2)traits.add("effect.minecraft.night_vision");
            if(water>0)traits.add("mining.guogaology.tooltip.trait.healing");
            if(water>=2)traits.add("mining.guogaology.tooltip.trait.food_floor");
            if(boundary>=2) {
                traits.add(boundary>=3?"mining.guogaology.tooltip.trait.sprint_flight":"mining.guogaology.tooltip.trait.flight");
                traits.add("mining.guogaology.tooltip.trait.fall_immunity");
                traits.add("effect.minecraft.fire_resistance");
            }
            if(boundary>=3)traits.add("mining.guogaology.tooltip.trait.collision_immunity");
            if(guogao>=2) {
                traits.add("mining.guogaology.tooltip.trait.debuff_immunity");
                traits.add("mining.guogaology.tooltip.trait.inventory_totems");
            }
            if(socketTotem)traits.add("mining.guogaology.tooltip.trait.socket_totems");
        }
        return List.copyOf(traits);
    }
}
