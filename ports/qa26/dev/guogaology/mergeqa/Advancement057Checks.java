package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.advancement.GuideProgress;
import dev.guogaology.mining.GearData;
import dev.guogaology.portal.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.*;

/** Native criterion/loot checks; never grants vanilla progress by commands or award(). */
public final class Advancement057Checks {
    public static final String[] ROOTS={"minecraft:story/root","minecraft:adventure/root","minecraft:husbandry/root","minecraft:nether/root","minecraft:end/root","guogaology:arrival"};
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    private static AdvancementHolder holder(ServerPlayer p,String id){var a=p.level().getServer().getAdvancements().get(Identifier.parse(id));check(a!=null,"loaded advancement "+id);return a;}
    private static boolean done(ServerPlayer p,String id){return p.getAdvancements().getOrStartProgress(holder(p,id)).isDone();}
    private static void complete(ServerPlayer p,String id){check(done(p,id),"criterion completed "+id);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"guogaology:"+id)));}
    private static void inventory(ServerPlayer p,String... names){
        p.getInventory().clearContent();for(int i=0;i<names.length;i++)p.getInventory().setItem(i,item(names[i]));
        for(String name:names)CriteriaTriggers.INVENTORY_CHANGED.trigger(p,p.getInventory(),item(name));
    }
    public static void run(ServerPlayer p,boolean baseline)throws Exception{
        checks=0;var manager=p.level().getServer().getAdvancements();
        long vanilla=manager.getAllAdvancements().stream().filter(a->a.id().getNamespace().equals("minecraft")).count();
        check(vanilla>1200,"vanilla advancement/recipe definitions retained: "+vanilla);
        for(String root:ROOTS)check(holder(p,root).value().display().isPresent(),"visible root "+root);
        check(!done(p,"minecraft:story/root"),"fresh world has no vanilla Story root before acquiring a crafting table");
        // The actual registered vanilla predicates, not manual awards.
        inventory(p,"minecraft:crafting_table");complete(p,"minecraft:story/root");
        inventory(p,"minecraft:cobblestone");complete(p,"minecraft:story/mine_stone");
        inventory(p,"minecraft:stone_pickaxe");complete(p,"minecraft:story/upgrade_tools");
        CriteriaTriggers.CONSUME_ITEM.trigger(p,new ItemStack(Items.APPLE));complete(p,"minecraft:husbandry/root");
        var zombie=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:zombie")).create(p.level(),EntitySpawnReason.COMMAND);check(zombie!=null,"native zombie");
        CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(p,zombie,p.damageSources().playerAttack(p));
        complete(p,"minecraft:adventure/root");complete(p,"minecraft:adventure/kill_a_mob");zombie.discard();
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,Level.OVERWORLD,Level.NETHER);complete(p,"minecraft:nether/root");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,Level.NETHER,Level.END);complete(p,"minecraft:end/root");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,GuogaologyMod.OUTER,GuogaologyMod.DIMENSION);complete(p,"guogaology:arrival");
        if(!baseline)guide(p);
        p.getInventory().clearContent();p.getAdvancements().flushDirty(p,true);
        System.out.println("ADVANCEMENT057_SERVER_OK checks="+checks+" vanilla="+vanilla+" baseline="+baseline+"; native inventory/food/kill/travel predicates, all five vanilla roots retained");
    }
    private static void guide(ServerPlayer p)throws Exception{
        var manager=p.level().getServer().getAdvancements();
        check(manager.getAllAdvancements().stream().filter(a->a.id().getNamespace().equals("guogaology")&&a.value().display().isPresent()).count()==39,"all39guide definitions decoded");
        check(!done(p,"guogaology:guide/portal_outer"),"travel does not fake a player-built gate");
        inventory(p,"omega_material");complete(p,"guogaology:guide/first_ore");
        inventory(p,"omega_pickaxe");complete(p,"guogaology:guide/first_tool");
        inventory(p,"omega_manuscript");complete(p,"guogaology:guide/first_book");
        inventory(p,"omega_helmet","epsilon_chestplate","gamma_leggings");check(!done(p,"guogaology:guide/full_set"),"three armor pieces do not complete a set");
        inventory(p,"omega_helmet","epsilon_chestplate","gamma_leggings","true_omega_boots");complete(p,"guogaology:guide/full_set");
        inventory(p,"number_pickaxe");complete(p,"guogaology:guide/number_tools");
        inventory(p,"return_token");check(!done(p,"guogaology:guide/return_ready"),"return token alone is insufficient");
        inventory(p,"return_token","inner_return_frame");complete(p,"guogaology:guide/return_ready");
        String[][] goals={{"matrix_sheet","raw_material"},{"ordinal_shard","shards"},{"ordinal_crystal","crystal"},{"sequence_core","first_core"},{"enhancement_table","station"},{"enhancement_table_2","higher_station"},{"enhancement_table_3","ultimate_station"},{"sequence_core_lv2","core2"},{"sequence_core_lv3","core3"},{"ordinal_crystal_lv4","ordinal3"},{"guogao_lamp_shaper","lamp_shaper"},{"server_rack","compute_setup"}};
        for(var goal:goals){inventory(p,goal[0]);complete(p,"guogaology:guide/"+goal[1]);}
        var raw=holder(p,"guogaology:guide/raw_material");p.getAdvancements().revoke(raw,"collected");
        inventory(p,"fos_trace");complete(p,"guogaology:guide/raw_material");
        var blank=item("omega_manuscript");GuideProgress.edited(p,blank);check(!done(p,"guogaology:guide/socketed"),"empty editor does not complete sockets");
        GearData.setCores(blank,List.of(item("sequence_core")));GuideProgress.edited(p,blank);complete(p,"guogaology:guide/socketed");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,Level.OVERWORLD,GuogaologyMod.OUTER);complete(p,"guogaology:guide/enter_outer");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,GuogaologyMod.DIMENSION,GuogaologyMod.GUOGAO);complete(p,"guogaology:guide/enter_underworld");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,GuogaologyMod.OUTER,Level.OVERWORLD);complete(p,"guogaology:guide/return_home");
        CriteriaTriggers.CHANGED_DIMENSION.trigger(p,GuogaologyMod.GUOGAO,GuogaologyMod.DIMENSION);complete(p,"guogaology:guide/return_up");
        // Use real lazy chest loot generation, including a negative ordinary chest.
        var world=p.level();var pos=p.blockPosition().above(6);var original=world.getBlockState(pos);
        try{
            world.setBlock(pos,Blocks.CHEST.defaultBlockState(),2);var chest=(ChestBlockEntity)world.getBlockEntity(pos);
            check(chest!=null,"real loot chest");
            chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:chests/simple_dungeon")),57);chest.unpackLootTable(p);
            check(!done(p,"guogaology:guide/landmark_first"),"ordinary loot is not a giant landmark");
            String[] themes={"matrix","power","hydra","absence","weaver","astra","frontier","guogao"};
            for(int i=0;i<themes.length;i++){
                chest.clearContent();chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("guogaology:chests/"+themes[i]+(i%2==0?"_sanctum":"_ruin"))),570+i);chest.unpackLootTable(p);
                complete(p,"guogaology:guide/landmark_first");complete(p,"guogaology:guide/landmark_"+themes[i]);
                if(i<7)check(!done(p,"guogaology:guide/landmarks_all"),"all-eight requires every theme");
            }
            complete(p,"guogaology:guide/landmarks_all");
        }finally{world.setBlock(pos,original,2);}
        portals(p);
        for(var a:manager.getAllAdvancements())if(a.id().getNamespace().equals("guogaology")&&a.value().display().isPresent())check(p.getAdvancements().getOrStartProgress(a).isDone(),"all guide routes exercised "+a.id());
    }
    private static void portals(ServerPlayer p){
        var server=p.level().getServer();
        var sources=List.of(Level.OVERWORLD,GuogaologyMod.OUTER,GuogaologyMod.DIMENSION,GuogaologyMod.OUTER,GuogaologyMod.DIMENSION,GuogaologyMod.GUOGAO);
        String[] goals={"portal_outer","portal_inner","portal_underworld","portal_return","portal_return","portal_return"};
        String[] offerings={"minecraft:apple","true_omega_material","amber_guogao","return_token","return_token","return_token"};
        String[] frames={"minecraft:cake","true_omega_block","amber_guogao","outer_return_frame","inner_return_frame","guogao_return_frame"};
        for(int i=0;i<sources.size();i++){
            var world=server.getLevel(sources.get(i));check(world!=null,"portal source exists");var center=new BlockPos(8+i*16,250,8);var saved=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)world.getChunk((center.getX()>>4)+x,(center.getZ()>>4)+z);
            try{
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                    var pos=center.offset(x,0,z);saved.put(pos,world.getBlockState(pos));world.setBlock(pos,PortalRitual.isFrameOffset(x,z)?Block.byItem(item(frames[i]).getItem()).defaultBlockState():Blocks.AIR.defaultBlockState(),2);
                }
                var offering=new ItemEntity(world,center.getX()+.5,250.5,8.5,item(offerings[i]));offering.setThrower(p);
                if(i==0){
                    var missing=center.offset(2,0,0);world.setBlock(missing,Blocks.AIR.defaultBlockState(),2);
                    check(!PortalRitual.tryActivate(world,offering)&&!done(p,"guogaology:guide/portal_outer"),"incomplete ring gives no progress");
                    world.setBlock(missing,Blocks.CAKE.defaultBlockState(),2);
                }
                check(PortalRitual.tryActivate(world,offering),"real activation route "+i);complete(p,"guogaology:guide/"+goals[i]);
            }finally{for(var e:saved.entrySet())world.setBlock(e.getKey(),e.getValue(),2);}
        }
    }
}
