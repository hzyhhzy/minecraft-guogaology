package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;

/** Native wooden fuel durations; Dread Fir deliberately has no fuel entries. */
public final class ModWoodFuels {
    public static final Map<String,Integer> LAVER = Map.ofEntries(
        Map.entry("laver_vein",300),Map.entry("laver_wood",300),Map.entry("laver_stripped_log",300),Map.entry("laver_stripped_wood",300),
        Map.entry("laver_planks",300),Map.entry("laver_stairs",300),Map.entry("laver_slab",150),Map.entry("laver_fence",300),
        Map.entry("laver_fence_gate",300),Map.entry("laver_trapdoor",300),Map.entry("laver_pressure_plate",300),Map.entry("laver_button",100),
        Map.entry("laver_door",200),Map.entry("laver_sign",200),Map.entry("laver_hanging_sign",800),Map.entry("laver_sapling",100),
        Map.entry("laver_boat",1200),Map.entry("laver_chest_boat",1200));
    public static void initialize() {
        LAVER.forEach((name,ticks) -> FuelRegistry.INSTANCE.add(BuiltInRegistries.ITEM.get(GuogaologyMod.id(name)), ticks));
    }
    private ModWoodFuels() {}
}
