package dev.guogaology;


import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
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
        var item=Registry.register(BuiltInRegistries.ITEM,GuogaologyMod.id(name),new Item(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,GuogaologyMod.id(name)))));
        ITEMS.put(name,item);return item;
    }
    public static void initialize(){
        for(var block:List.of(GuogaologyBlocks.Y_LOG,GuogaologyBlocks.LAVER_VEIN,GuogaologyBlocks.POWER_BRICKS)){
            FlammableBlockRegistry.getDefaultInstance().add(block,5,5);
        }
        DefaultItemComponentEvents.MODIFY.register(context->{
            for(var block:List.of(GuogaologyBlocks.Y_LOG,GuogaologyBlocks.LAVER_VEIN,GuogaologyBlocks.POWER_BRICKS,GuogaologyBlocks.LAVER_PLANKS))
                context.modify(block.asItem(),b->b.set(DataComponents.COOKING_FUEL,new CookingFuel(new ResolvableInt.Constant(300),new ResolvableFloat.Constant(1))));
            context.modify(GuogaologyBlocks.GIANT_LAVER.asItem(),b->b.set(DataComponents.COOKING_FUEL,new CookingFuel(new ResolvableInt.Constant(150),new ResolvableFloat.Constant(1))));
            for(var block:GuogaologyBlocks.ORDINAL_PLANTS)context.modify(block.asItem(),b->b.set(DataComponents.COMPOSTABLE,compost("plant")));
            for(var block:List.of(GuogaologyBlocks.DREAD_LEAVES,GuogaologyBlocks.Y_LEAVES,GuogaologyBlocks.GIANT_LAVER,GuogaologyBlocks.LAVER_MAT,GuogaologyBlocks.EPSILON_TURF,GuogaologyBlocks.EMOJI_FLOWER))context.modify(block.asItem(),b->b.set(DataComponents.COMPOSTABLE,compost("leaf")));
        });
        FlammableBlockRegistry.getDefaultInstance().add(GuogaologyBlocks.Y_LEAVES,10,15);
        for(var block:List.of(GuogaologyBlocks.LAVER_PLANKS,GuogaologyBlocks.BASIC_LAVER_PATTERN,GuogaologyBlocks.TIANYI_FIBER,GuogaologyBlocks.WHITE_FIBER))
            FlammableBlockRegistry.getDefaultInstance().add(block,5,10);
    }
    private static Compostable compost(String type){return new Compostable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.CONTEXT_INT_PROVIDER,GuogaologyMod.id("compostable/"+type)));}
    private WorldMaterials(){}
}
