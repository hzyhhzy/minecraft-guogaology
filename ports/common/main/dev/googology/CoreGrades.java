package dev.googology;

import dev.googology.survival.BlockBalance;
import dev.googology.block.AnimatedCoreBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.*;

/** Crafted, portable grade forms. Wild scenery continues to place the original base materials. */
public final class CoreGrades {
    public static final List<String> ROOTS=List.of("sequence_core","power_tower_core","hydra_bud","lho_trace",
            "laver_core","astra_critical_core","boundary_core","guogao_heart","ordinal_crystal");
    private static final Map<String,List<Block>> FAMILIES=new LinkedHashMap<>();
    private static final Set<Block> UPPER=new HashSet<>();
    public static BlockEntityType<AnimatedCoreBlock.CoreEntity> ENTITY;
    private CoreGrades(){}
    public static void initialize(){
        if(!FAMILIES.isEmpty())return;
        for(var root:ROOTS){
            var levels=new ArrayList<Block>();levels.add(BuiltInRegistries.BLOCK.getValue(GoogologyMod.id(root)));
            if(levels.getFirst()==Blocks.AIR)throw new IllegalStateException("Missing base core: "+root);
            for(int level=2;level<=(root.equals("ordinal_crystal")?4:3);level++){
                var name=root+"_lv"+level;
                var block=new AnimatedCoreBlock(BlockBalance.apply(name,BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).noOcclusion()));
                Registry.register(BuiltInRegistries.BLOCK,GoogologyMod.id(name),block);
                Registry.register(BuiltInRegistries.ITEM,GoogologyMod.id(name),new BlockItem(block,
                        new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,GoogologyMod.id(name))).useBlockDescriptionPrefix().fireResistant().rarity(rarity(root,level))));
                GoogologyBlocks.ALL.add(block);GoogologyBlocks.TRANSLUCENT.add(block);UPPER.add(block);levels.add(block);
            }
            FAMILIES.put(root,List.copyOf(levels));
        }
        ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,GoogologyMod.id("animated_core"),
                FabricBlockEntityTypeBuilder.create(AnimatedCoreBlock.CoreEntity::new,UPPER.toArray(Block[]::new)).build());
    }
    /** Regional grades are yellow/cyan/purple; universal grades start at white. */
    public static Rarity rarity(String root,int level){
        if(!ROOTS.contains(root))return Rarity.RARE;
        int rank=level-(root.equals("ordinal_crystal")?1:0);
        return switch(rank){case 0->Rarity.COMMON;case 1->Rarity.UNCOMMON;case 2->Rarity.RARE;default->Rarity.EPIC;};
    }
    public static List<Block> levels(String root){return FAMILIES.getOrDefault(root,List.of());}
    public static boolean upper(Block block){return UPPER.contains(block);}
    public static List<Block> upgrades(Block root){
        var levels=levels(BuiltInRegistries.BLOCK.getKey(root).getPath());
        return levels.isEmpty()?List.of():levels.subList(1,levels.size());
    }
}
