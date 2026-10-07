package dev.guogaology.outer.registry;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
public record BlockItemId(ResourceKey<Block> block,ResourceKey<Item> item){
 public static BlockItemId create(Identifier block,Identifier item){return new BlockItemId(ResourceKey.create(Registries.BLOCK,block),ResourceKey.create(Registries.ITEM,item));}
}
