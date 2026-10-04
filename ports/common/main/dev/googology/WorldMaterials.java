package dev.googology;

import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Collectible materials and ordinary block behavior; equipment progression is reserved for a redesign. */
public final class WorldMaterials {
    public static final Map<String,Item> ITEMS=new LinkedHashMap<>();
    public static final Item LAVER_RIBBON=item("laver_ribbon");
    public static final Item COMPUTE_CHIP=item("compute_chip");
    private static Item item(String name){
        var item=Registry.register(BuiltInRegistries.ITEM,GoogologyMod.id(name),new Item(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,GoogologyMod.id(name)))));
        ITEMS.put(name,item);return item;
    }
    public static void initialize(){
        for(var block:List.of(GoogologyBlocks.Y_LOG,GoogologyBlocks.LAVER_VEIN,GoogologyBlocks.POWER_BRICKS)){
            FlammableBlockRegistry.getDefaultInstance().add(block,5,5);
        }
        FuelRegistryEvents.BUILD.register((builder,context)->{
            for(var block:List.of(GoogologyBlocks.Y_LOG,GoogologyBlocks.LAVER_VEIN,GoogologyBlocks.POWER_BRICKS,GoogologyBlocks.LAVER_PLANKS))builder.add(block,300);
            builder.add(GoogologyBlocks.GIANT_LAVER,150);
        });
        FlammableBlockRegistry.getDefaultInstance().add(GoogologyBlocks.Y_LEAVES,10,15);
        for(var block:List.of(GoogologyBlocks.LAVER_PLANKS,GoogologyBlocks.BASIC_LAVER_PATTERN,GoogologyBlocks.TIANYI_FIBER,GoogologyBlocks.WHITE_FIBER))
            FlammableBlockRegistry.getDefaultInstance().add(block,5,10);
        for(var block:GoogologyBlocks.ORDINAL_PLANTS)CompostingChanceRegistry.INSTANCE.add(block,.65f);
        for(var block:List.of(GoogologyBlocks.DREAD_LEAVES,GoogologyBlocks.Y_LEAVES,GoogologyBlocks.GIANT_LAVER,GoogologyBlocks.LAVER_MAT,GoogologyBlocks.EPSILON_TURF,GoogologyBlocks.EMOJI_FLOWER))
            CompostingChanceRegistry.INSTANCE.add(block,.5f);
    }
    private WorldMaterials(){}
}
