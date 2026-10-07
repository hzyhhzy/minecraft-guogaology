package dev.googology.mergeqa;

import dev.googology.GoogologyBlocks;
import dev.googology.block.AnchoredBlock;
import dev.googology.block.PortableRelicBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Native 26.2 state/tool/drop paths; never ships in the release JAR. */
public final class CoreHarvest043Checks {
    private static int checks;
    private static final String[] REGIONAL={"sequence_core","power_tower_core","hydra_bud","lho_trace",
            "laver_core","astra_critical_core","boundary_core","guogao_heart"};
    private static final String[] AUXILIARIES={"tree_node_red","tree_node_green","tree_node_blue",
            "fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z"};
    private CoreHarvest043Checks(){}
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:"+id));}
    private static void intact(List<ItemStack> drops,Block block,String label){
        check(drops.size()==1&&drops.getFirst().is(block.asItem())&&drops.getFirst().getCount()==1,label);
    }
    public static void run(ServerPlayer p){
        checks=0;
        var world=p.level();var center=p.blockPosition().offset(8,0,8);var anchor=center.offset(2,2,2);
        var saved=new LinkedHashMap<BlockPos,BlockState>();
        var main=p.getMainHandItem();var off=p.getOffhandItem();var mode=p.gameMode();
        var oldEntities=new HashSet<UUID>();
        for(var e:world.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(5)))oldEntities.add(e.getUUID());
        try{
            for(var pos:BlockPos.betweenClosed(center.offset(-3,-3,-3),center.offset(3,3,3))){
                saved.put(pos.immutable(),world.getBlockState(pos));world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
            p.setGameMode(GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            var expected=new LinkedHashMap<String,Float>();
            for(String root:REGIONAL)for(int grade=1;grade<=3;grade++)expected.put(root+(grade==1?"":"_lv"+grade),(float)(25<<(grade-1)));
            for(int stage=2;stage<=4;stage++)expected.put("ordinal_crystal_lv"+stage,(float)(25<<(stage-2)));
            for(String name:AUXILIARIES)expected.put(name,25f);
            expected.put("ordinal_crystal",2f);
            for(var entry:expected.entrySet()){
                String name=entry.getKey();Block block=block(name);var state=block.defaultBlockState();
                check(block!=Blocks.AIR,"registered core "+name);
                check(state.getDestroySpeed(world,center)==entry.getValue(),"exact hardness "+name);
                check(!state.requiresCorrectToolForDrops(),"no copied correct-tool flag "+name);
                check(state.is(BlockTags.MINEABLE_WITH_PICKAXE),"pickaxe acceleration tag "+name);
                check(!state.is(BlockTags.NEEDS_STONE_TOOL)&&!state.is(BlockTags.NEEDS_IRON_TOOL)&&!state.is(BlockTags.NEEDS_DIAMOND_TOOL),"no tier tag "+name);
                if(block instanceof AnchoredBlock){
                    check(Block.getDrops(state,world,center,null,p,ItemStack.EMPTY).isEmpty(),"unanchored natural LHO remains empty "+name);
                    intact(Block.getDrops(state.setValue(PortableRelicBlock.PLAYER_PLACED,true),world,center,null,p,ItemStack.EMPTY),block,"player-placed LHO stays recoverable without anchor "+name);
                    intact(Block.getDrops(state.setValue(AnchoredBlock.STABLE,true),world,center,null,p,ItemStack.EMPTY),block,"stable natural LHO accepts hand "+name);
                }
                world.setBlock(anchor,GoogologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),2);
                for(var tool:List.of(ItemStack.EMPTY,new ItemStack(Items.STICK),new ItemStack(Items.WOODEN_PICKAXE),new ItemStack(Items.IRON_PICKAXE))){
                    p.setItemSlot(EquipmentSlot.MAINHAND,tool);
                    check(p.hasCorrectToolForDrops(state),"native player harvest permission "+name+" / "+tool);
                    var drops=Block.getDrops(state,world,center,null,p,tool);
                    if(name.equals("ordinal_crystal"))shards(drops,2,4,"ordinary crystal tool-independent shards");
                    else intact(drops,block,"natural intact drop "+name+" / "+tool);
                }
                check(new ItemStack(Items.WOODEN_PICKAXE).getDestroySpeed(state)>ItemStack.EMPTY.getDestroySpeed(state),"wood pick speeds up "+name);
                if(state.hasProperty(PortableRelicBlock.PLAYER_PLACED)){
                    intact(Block.getDrops(state.setValue(PortableRelicBlock.PLAYER_PLACED,true),world,center,null,p,ItemStack.EMPTY),block,"placed core hand return "+name);
                }
                world.setBlock(anchor,Blocks.AIR.defaultBlockState(),2);
            }
            check(expected.size()==35,"all27 graded cores plus seven auxiliaries and natural crystal");
            ordinalDrops(p,center);
            // Exercise the native survival break path too, including provenance and anchored LHO.
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            for(String name:new String[]{"sequence_core","laver_core","ordinal_crystal_lv4","tree_node_red","lho_trace","ordinal_crystal"}){
                world.setBlock(anchor,GoogologyBlocks.ORDINAL_CRYSTAL.defaultBlockState(),2);
                var state=block(name).defaultBlockState();world.setBlock(center,state,2);
                var prior=new HashSet<UUID>();for(var e:world.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(3)))prior.add(e.getUUID());
                check(p.gameMode.destroyBlock(center),"actual survival hand break "+name);
                var drops=world.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(3),e->!prior.contains(e.getUUID())).stream().map(ItemEntity::getItem).toList();
                if(name.equals("ordinal_crystal"))shards(drops,2,4,"actual hand break shard count");else intact(drops,block(name),"actual hand break emits intact item "+name);
            }
            check(block("astra_compute_crystal").defaultBlockState().requiresCorrectToolForDrops(),"unrelated landmark relic retains its tool gate");
            check(block("omega_ore").defaultBlockState().requiresCorrectToolForDrops(),"ordinary ore retains its tool gate");
            System.out.println("CORE_HARVEST_043_OK checks="+checks+" 35 materials / hand+stick+wood+iron / LHO anchoring / native survival drops / Fortune+Silk");
        }finally{
            for(var e:world.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(5)))if(!oldEntities.contains(e.getUUID()))e.discard();
            for(var entry:saved.entrySet())world.setBlock(entry.getKey(),entry.getValue(),2);
            p.setItemSlot(EquipmentSlot.MAINHAND,main);p.setItemSlot(EquipmentSlot.OFFHAND,off);p.setGameMode(mode);
        }
    }
    private static void shards(List<ItemStack> drops,int min,int max,String label){
        check(drops.size()==1&&drops.getFirst().is(BuiltInRegistries.ITEM.getValue(Identifier.parse("googology:ordinal_shard")))
                &&drops.getFirst().getCount()>=min&&drops.getFirst().getCount()<=max,label);
    }
    private static void ordinalDrops(ServerPlayer p,BlockPos pos){
        var world=p.level();var state=GoogologyBlocks.ORDINAL_CRYSTAL.defaultBlockState();
        var registry=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var fortune=new ItemStack(Items.WOODEN_PICKAXE);fortune.enchant(registry.getOrThrow(Enchantments.FORTUNE),10);
        var silk=new ItemStack(Items.WOODEN_PICKAXE);silk.enchant(registry.getOrThrow(Enchantments.SILK_TOUCH),1);
        int low=4,high=0;
        for(int sample=0;sample<96;sample++){
            var drops=Block.getDrops(state,world,pos,null,p,ItemStack.EMPTY);shards(drops,2,4,"glowstone base count");
            low=Math.min(low,drops.getFirst().getCount());high=Math.max(high,drops.getFirst().getCount());
            shards(Block.getDrops(state,world,pos,null,p,fortune),2,4,"Fortune remains capped at four shards");
        }
        check(low==2&&high==4,"natural ordinal includes both count endpoints");
        intact(Block.getDrops(state,world,pos,null,p,silk),GoogologyBlocks.ORDINAL_CRYSTAL,"Silk Touch returns original ungraded crystal");
        boolean decayed=false;
        for(int sample=0;sample<96;sample++){
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(world)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.atCenterOf(pos))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL,ItemStack.EMPTY)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS,4f);
            var drops=state.getDrops(params);int count=drops.stream().mapToInt(ItemStack::getCount).sum();
            check(count>=0&&count<=4,"explosion preserves four-shard upper bound");decayed|=count<2;
        }
        check(decayed,"actual ordinal loot still applies explosion decay");
    }
}
