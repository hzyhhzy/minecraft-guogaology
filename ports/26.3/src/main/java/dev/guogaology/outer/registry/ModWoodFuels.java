package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
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
        DefaultItemComponentEvents.MODIFY.register(context -> LAVER.forEach((name,ticks) -> context.modify(
            BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(name)), builder -> builder.set(DataComponents.COOKING_FUEL,
                new CookingFuel(new ResolvableInt.Constant(ticks), new ResolvableFloat.Constant(1))))));
    }
    private ModWoodFuels() {}
}
