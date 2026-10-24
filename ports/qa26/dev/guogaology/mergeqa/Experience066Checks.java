package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.mining.*;
import dev.guogaology.world.WorldNoise;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

/** Real survival destruction, including the AFTER event and merged experience-orb counts. */
public final class Experience066Checks {
    private static final BlockPos TEST=new BlockPos(8,220,8);
    private static final AABB AREA=new AABB(TEST).inflate(10);
    private static int checks,breaks;
    private static final JsonArray rows=new JsonArray();
    private record Harvest(int experience,Map<String,Integer> drops){}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError("Experience066: "+message);}
    private static Block block(String name){return BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+path)));}
    private static ItemStack tool(ServerLevel world,int mode,boolean extra){
        var tool=mode>=3?item("true_omega_pickaxe"):new ItemStack(Items.NETHERITE_PICKAXE);
        if(mode==1||mode==2)tool.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode==1?Enchantments.SILK_TOUCH:Enchantments.FORTUNE),mode==1?1:3);
        if(mode>=3){var cores=new ArrayList<ItemStack>();cores.add(item("hydra_bud"));if(extra)cores.add(item("sequence_core"));GearData.setCores(tool,cores);GearData.setSilkTouch(tool,mode==4);check(GearData.silkTouch(tool)==(mode==4),"actual Branch toggle mode"+mode);}
        return tool;
    }
    private static void clear(ServerLevel world){
        for(var e:world.getEntitiesOfClass(ItemEntity.class,AREA))e.discard();
        for(var e:world.getEntitiesOfClass(ExperienceOrb.class,AREA))e.discard();
        world.setBlock(TEST,Blocks.AIR.defaultBlockState(),2);world.setBlock(TEST.west(),Blocks.AIR.defaultBlockState(),2);world.setBlock(TEST.below(),Blocks.AIR.defaultBlockState(),2);
    }
    private static Harvest harvest(ServerPlayer player,Block type,ItemStack tool,boolean extra,long seed)throws Exception{
        var world=player.level();clear(world);world.getRandom().setSeed(WorldNoise.mix(seed));
        player.setItemSlot(EquipmentSlot.MAINHAND,tool);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);player.setShiftKeyDown(false);player.setYRot(extra?180:0);player.setXRot(0);
        world.setBlock(TEST,type.defaultBlockState(),2);
        if(extra){world.setBlock(TEST.west(),type.defaultBlockState(),2);world.setBlock(TEST.below(),type.defaultBlockState(),2);}
        if(extra)GearData.setMiningPattern(tool,new MiningPattern(12,1<<11));var before=tool.copy();if(extra)MiningEffects.captureStart(player,TEST);check(player.gameMode.destroyBlock(TEST),"native survival destruction "+BuiltInRegistries.BLOCK.getKey(type));breaks++;
        check(world.getBlockState(TEST).isAir(),"primary block actually removed");
        if(extra){check(world.getBlockState(TEST.west()).isAir(),"actual AFTER event excavates one extra neighbor");check(world.getBlockState(TEST.below()).is(type),"unselected lower ore remains intact");}
        var after=tool.copy();after.setDamageValue(before.getDamageValue());check(ItemStack.matches(before,after),"owned tool components/cores/enchantments/count unchanged except ordinary wear");
        int xp=0;var countField=ExperienceOrb.class.getDeclaredField("count");countField.setAccessible(true);
        for(var e:world.getEntitiesOfClass(ExperienceOrb.class,AREA)){xp+=e.getValue()*countField.getInt(e);e.discard();}
        var drops=new TreeMap<String,Integer>();for(var e:world.getEntitiesOfClass(ItemEntity.class,AREA)){drops.merge(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString(),e.getItem().getCount(),Integer::sum);e.discard();}
        return new Harvest(xp,drops);
    }
    private static void sample(ServerPlayer p,String name,int mode,boolean baseline,boolean xpBlock)throws Exception{
        var type=block(name);int total=0;var counts=new TreeMap<String,Integer>();
        for(int i=0;i<16;i++){
            var got=harvest(p,type,tool(p.level(),mode,false),false,666000L+name.hashCode()*31L+i);total+=got.experience();got.drops().forEach((k,n)->counts.merge(k,n,Integer::sum));
            if(mode==1||mode==4)check(got.drops().size()==1&&got.drops().getOrDefault(name,0)==1,"Silk returns exactly one original block "+name+" mode"+mode+" got"+got.drops());
            if(mode==1||mode==4&&!baseline||!xpBlock)check(got.experience()==0,"Silk/non-XP block never gives XP "+name+" mode"+mode+" got"+got.experience());
        }
        if(xpBlock&&(mode==0||mode==2||mode==3||mode==4&&baseline))check(total>0,"normal/fortune or old bug has actual XP "+name+" mode"+mode);
        var row=new JsonObject();row.addProperty("block",name);row.addProperty("mode",new String[]{"plain","vanillaSilk","vanillaFortuneIII","BranchFortuneII","BranchSilk"}[mode]);row.addProperty("samples",16);row.addProperty("xpTotal",total);row.add("drops",new Gson().toJsonTree(counts));rows.add(row);
        System.out.println("EXPERIENCE066_ROW "+row);
    }
    public static void run(ServerPlayer p,boolean baseline)throws Exception{
        var oldMode=p.gameMode.getGameModeForPlayer();var oldPosition=p.position();var held=p.getMainHandItem();var off=p.getOffhandItem();
        p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(.5,220,.5);
        try{
            var xpNames=new ArrayList<String>();for(var mineral:EquipmentRules.MINERALS)for(var prefix:new String[]{"","nether_"})xpNames.add("guogaology:"+prefix+mineral+"_ore");
            xpNames.addAll(List.of("minecraft:coal_ore","minecraft:lapis_ore","minecraft:diamond_ore","minecraft:redstone_ore","minecraft:emerald_ore","minecraft:nether_quartz_ore","minecraft:nether_gold_ore","minecraft:sculk"));
            for(var name:xpNames)for(int mode=0;mode<5;mode++)sample(p,name,mode,baseline,true);
            for(var name:List.of("guogaology:ordinal_crystal_ore","guogaology:empty_crystal_ore","guogaology:dread_crystal_ore","guogaology:ordinal_crystal","minecraft:iron_ore"))for(int mode:new int[]{0,1,4})sample(p,name,mode,baseline,false);
            for(int mode:new int[]{3,4}){
                int xp=0;for(int i=0;i<8;i++){var got=harvest(p,block("guogaology:omega_ore"),tool(p.level(),mode,true),true,66011+i);xp+=got.experience();if(mode==4){check(got.drops().equals(Map.of("guogaology:omega_ore",2)),"extra Silk has exactly2ore blocks");if(!baseline)check(got.experience()==0,"extra Silk awards zero XP");}}
                check(mode==4&&!baseline?xp==0:xp>0,"extra mining XP expected mode"+mode);var row=new JsonObject();row.addProperty("extraMode",mode);row.addProperty("xpTotal",xp);rows.add(row);
            }
            int cycleXp=0;for(int i=0;i<8;i++){var got=harvest(p,block("guogaology:true_omega_ore"),tool(p.level(),4,false),false,66999+i);check(got.drops().equals(Map.of("guogaology:true_omega_ore",1)),"repeated ore refill preserves single block");cycleXp+=got.experience();}
            check(baseline?cycleXp>0:cycleXp==0,"repeated Silk harvest cannot create XP after fix");
            for(int mode:new int[]{1,4}){var got=harvest(p,Blocks.SPAWNER,tool(p.level(),mode,false),false,6669+mode);check(got.experience()>=15&&got.experience()<=43&&got.drops().isEmpty(),"vanilla spawner remains XP-only even with Silk mode"+mode);}
            var out=new JsonObject();out.addProperty("status","passed");out.addProperty("mode",baseline?"baseline065-reproduced":"fixed066");out.addProperty("checks",checks);out.addProperty("actualPrimaryBreaks",breaks);out.addProperty("repeatedSilkOreXp",cycleXp);out.add("rows",rows);
            Files.writeString(Path.of("experience066-native.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\n");
            System.out.println("EXPERIENCE066_OK mode="+(baseline?"baseline065":"fixed066")+" checks="+checks+" actualBreaks="+breaks+" cycleXp="+cycleXp);
        }finally{clear(p.level());p.setItemSlot(EquipmentSlot.MAINHAND,held);p.setItemSlot(EquipmentSlot.OFFHAND,off);p.setGameMode(oldMode);p.setPos(oldPosition);}
    }
}
