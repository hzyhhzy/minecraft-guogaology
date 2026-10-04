package dev.googology;

import dev.googology.survival.BlockBalance;
import dev.googology.block.AnimatedCoreBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Rarity;
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
            var levels=new ArrayList<Block>();levels.add(Registries.BLOCK.get(GoogologyMod.id(root)));
            if(levels.getFirst()==Blocks.AIR)throw new IllegalStateException("Missing base core: "+root);
            for(int level=2;level<=(root.equals("ordinal_crystal")?4:3);level++){
                var name=root+"_lv"+level;
                var block=new AnimatedCoreBlock(BlockBalance.apply(name,AbstractBlock.Settings.copy(Blocks.AMETHYST_BLOCK).nonOpaque()));
                Registry.register(Registries.BLOCK,GoogologyMod.id(name),block);
                Registry.register(Registries.ITEM,GoogologyMod.id(name),new BlockItem(block,
                        new Item.Settings().fireproof().rarity(level>=3?Rarity.EPIC:Rarity.RARE)));
                GoogologyBlocks.ALL.add(block);GoogologyBlocks.TRANSLUCENT.add(block);UPPER.add(block);levels.add(block);
            }
            FAMILIES.put(root,List.copyOf(levels));
        }
        ENTITY=Registry.register(Registries.BLOCK_ENTITY_TYPE,GoogologyMod.id("animated_core"),
                FabricBlockEntityTypeBuilder.create(AnimatedCoreBlock.CoreEntity::new,UPPER.toArray(Block[]::new)).build());
    }
    public static List<Block> levels(String root){return FAMILIES.getOrDefault(root,List.of());}
    public static boolean upper(Block block){return UPPER.contains(block);}
    public static List<Block> upgrades(Block root){
        var levels=levels(Registries.BLOCK.getId(root).getPath());
        return levels.isEmpty()?List.of():levels.subList(1,levels.size());
    }
}
