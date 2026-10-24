package dev.guogaology.releaseqa263;

import com.google.gson.*;
import dev.guogaology.mining.*;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Native player destruction of pre-loot Silk checks and block-entity loot; disposable world only. */
public final class NativeMechanism0614SilkChecks {
    private static final BlockPos TEST=new BlockPos(24,230,24);
    private static final AABB AREA=new AABB(TEST).inflate(8);
    private static int checks,breaks;
    private static boolean baseline;
    private static boolean silk(int mode){return mode==1||mode==3&&!baseline;}
    private static final JsonArray rows=new JsonArray();
    private record Result(List<ItemStack> drops,int xp,int bees,int silverfish,BlockState remainder){}
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError("Silk0614: "+why);}
    private static ItemStack own(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static PotDecorations decorations(){return new PotDecorations(Optional.of(new ItemStackTemplate(Items.ANGLER_POTTERY_SHERD)),Optional.of(new ItemStackTemplate(Items.ARCHER_POTTERY_SHERD)),Optional.of(new ItemStackTemplate(Items.ARMS_UP_POTTERY_SHERD)),Optional.of(new ItemStackTemplate(Items.BLADE_POTTERY_SHERD)));}
    private static ItemStack tool(ServerLevel world,int mode){
        var result=mode<2?new ItemStack(Items.NETHERITE_PICKAXE):own("true_omega_pickaxe");
        if(mode==1)result.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        if(mode>=2){GearData.setCores(result,List.of(own("hydra_bud")));GearData.setSilkTouch(result,mode==3);check(GearData.silkTouch(result)==(mode==3),"actual Branch toggle state");}
        return result;
    }
    private static void clear(ServerLevel level){
        level.setBlock(TEST,Blocks.AIR.defaultBlockState(),2);level.setBlock(TEST.below(),Blocks.AIR.defaultBlockState(),2);
        for(var entity:level.getEntitiesOfClass(Entity.class,AREA))if(entity instanceof ItemEntity||entity instanceof ExperienceOrb||entity instanceof Bee||entity.getType()==EntityTypes.SILVERFISH)entity.discard();
    }
    private static int count(List<ItemStack> drops,Item item){return drops.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    private static ItemStack only(List<ItemStack> drops,Item item){var all=drops.stream().filter(s->s.is(item)).toList();check(all.size()==1&&all.getFirst().getCount()==1,"one exact native "+BuiltInRegistries.ITEM.getKey(item));return all.getFirst();}
    @FunctionalInterface private interface Prepare{void run(ServerLevel level)throws Exception;}
    private static Result destroy(ServerPlayer player,Block block,int mode,Prepare prepare)throws Exception{
        var world=player.level();clear(world);world.setBlock(TEST.below(),Blocks.STONE.defaultBlockState(),2);world.setBlock(TEST,block.defaultBlockState(),2);prepare.run(world);
        var held=tool(world,mode);player.setItemSlot(EquipmentSlot.MAINHAND,held);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);player.setShiftKeyDown(false);MiningEffects.cancelStart(player);
        var before=held.copy();check(player.gameMode.destroyBlock(TEST),"native survival destruction "+BuiltInRegistries.BLOCK.getKey(block)+" mode"+mode);breaks++;
        var after=held.copy();after.setDamageValue(before.getDamageValue());check(ItemStack.matches(before,after),"real held components/cores/enchantments/count remain unchanged except wear");
        var drops=new ArrayList<ItemStack>();for(var entity:world.getEntitiesOfClass(ItemEntity.class,AREA)){drops.add(entity.getItem().copy());entity.discard();}
        int xp=0;var orbCount=ExperienceOrb.class.getDeclaredField("count");orbCount.setAccessible(true);for(var orb:world.getEntitiesOfClass(ExperienceOrb.class,AREA)){xp+=orb.getValue()*orbCount.getInt(orb);orb.discard();}
        int bees=world.getEntitiesOfClass(Bee.class,AREA).size(),silver=world.getEntitiesOfClass(Entity.class,AREA,e->e.getType()==EntityTypes.SILVERFISH).size();
        var row=new JsonObject();row.addProperty("block",BuiltInRegistries.BLOCK.getKey(block).toString());row.addProperty("mode",new String[]{"plain","nativeSilk","coreFortune","coreSilk"}[mode]);row.addProperty("xp",xp);row.addProperty("spawnedBees",bees);row.addProperty("spawnedSilverfish",silver);var amounts=new TreeMap<String,Integer>();for(var stack:drops)amounts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);row.add("drops",new Gson().toJsonTree(amounts));
        row.addProperty("baseline0613",baseline);row.addProperty("dimension",world.dimension().toString());row.addProperty("remainder",world.getBlockState(TEST).toString());row.addProperty("below",world.getBlockState(TEST.below()).toString());
        row.addProperty("belowIceMeltingTag",world.getBlockState(TEST.below()).is(net.minecraft.tags.TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:ice_melts_when_destroyed_above"))));
        row.addProperty("waterEvaporates",(Boolean)world.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES,TEST));
        row.addProperty("realCoreSilkToggle",GearData.silkTouch(before));row.addProperty("realNativeSilkLevel",EnchantmentHelper.getItemEnchantmentLevel(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),before));
        rows.add(row);var observation=new JsonObject();observation.addProperty("baseline0613",baseline);observation.addProperty("checksSoFar",checks);observation.add("rows",rows.deepCopy());
        Files.writeString(FabricLoader.getInstance().getGameDir().resolve("silk0614-observations.json"),new GsonBuilder().setPrettyPrinting().create().toJson(observation)+"\n");System.out.println("SILK0614_ROW "+row);
        return new Result(drops,xp,bees,silver,world.getBlockState(TEST));
    }
    private static void pots(ServerPlayer p)throws Exception{
        for(int mode=0;mode<4;mode++){
            var expected=decorations();var got=destroy(p,Blocks.DECORATED_POT,mode,level->{
                var pot=(DecoratedPotBlockEntity)level.getBlockEntity(TEST);var seed=DecoratedPotBlockEntity.createDecoratedPotInstance(expected);pot.applyComponentsFromItemStack(seed);pot.setTheItem(new ItemStack(Items.DIAMOND,7));check(pot.getDecorations().equals(expected),"actual native pot stores four authored sherds");
            });
            check(got.xp==0&&count(got.drops,Items.DIAMOND)==7,"pot contents exactly once with zero XP");
            if(silk(mode)){var full=only(got.drops,Items.DECORATED_POT);check(expected.equals(full.get(DataComponents.POT_DECORATIONS)),"Silk preserves all four decorations on intact pot");check(got.drops.size()==2,"only intact pot and its seven loose diamonds");}
            else{check(count(got.drops,Items.DECORATED_POT)==0,"non-Silk tool shatters native pot");for(var sherd:List.of(Items.ANGLER_POTTERY_SHERD,Items.ARCHER_POTTERY_SHERD,Items.ARMS_UP_POTTERY_SHERD,Items.BLADE_POTTERY_SHERD))check(count(got.drops,sherd)==1,"each native sherd once");check(got.drops.size()==5,"only four sherds and loose diamonds");}
        }
    }
    private static void ice(ServerPlayer p)throws Exception{
        for(int mode=0;mode<4;mode++){
            var got=destroy(p,Blocks.ICE,mode,level->{});
            if(silk(mode)){check(got.remainder.isAir(),"Silk ice never creates water");check(count(got.drops,Items.ICE)==1&&got.drops.size()==1,"one original ice block");}
            else{
                check(got.remainder.is(Blocks.WATER),"ordinary/old-core ice still melts on stone mode"+mode+" remainder="+got.remainder+" drops="+got.drops);
                if(mode==3&&baseline)check(count(got.drops,Items.ICE)==1&&got.drops.size()==1,"old virtualSilk already drops one ice block but fails to suppress melting");
                else check(got.drops.isEmpty(),"ordinary non-Silk ice produces no block mode"+mode+" drops="+got.drops);
            }
            check(got.xp==0,"ice has no XP");
        }
    }
    private static void hives(ServerPlayer p)throws Exception{
        for(int mode=0;mode<4;mode++){
            var got=destroy(p,Blocks.BEE_NEST,mode,level->{var nest=(BeehiveBlockEntity)level.getBlockEntity(TEST);var bee=EntityTypes.BEE.create(level,EntitySpawnReason.COMMAND);check(bee!=null,"native bee fixture created");nest.addOccupant(bee);check(nest.getOccupantCount()==1,"one actual bee stored in hive");});
            if(silk(mode)){var nest=only(got.drops,Items.BEE_NEST);var occupants=(net.minecraft.world.item.component.Bees)nest.get(DataComponents.BEES);check(occupants!=null&&occupants.bees().size()==1,"Silk nest carries exactly its stored bee");check(got.bees==0,"Silk does not release/anger stored bees");}
            else check(got.drops.isEmpty()&&got.bees==1,"non-Silk nest releases one native bee and no nest");
            check(got.xp==0,"nest has no XP");
        }
    }
    private static void controls(ServerPlayer p)throws Exception{
        for(int mode=0;mode<4;mode++){
            var shulker=destroy(p,Blocks.SHULKER_BOX,mode,level->{((ShulkerBoxBlockEntity)level.getBlockEntity(TEST)).setItem(0,new ItemStack(Items.EMERALD,11));});
            var box=only(shulker.drops,Items.SHULKER_BOX);var content=box.get(DataComponents.CONTAINER);check(content!=null,"native shulker retains container component");var stored=NonNullList.withSize(27,ItemStack.EMPTY);content.copyInto(stored);check(stored.stream().mapToInt(s->s.is(Items.EMERALD)?s.getCount():0).sum()==11&&shulker.drops.size()==1,"shulker contents stay inside once, no duplication/loss");
            var flower=destroy(p,Blocks.POTTED_DANDELION,mode,level->{});check(count(flower.drops,Items.FLOWER_POT)==1&&count(flower.drops,Items.DANDELION)==1&&flower.drops.size()==2,"native small flowerpot drops pot plus flower in every mode");
            var infested=destroy(p,Blocks.INFESTED_STONE,mode,level->{});check(infested.silverfish==((mode==1||mode==3)?0:1),"native infested-stone spawn follows same Silk view");
        }
        for(int mode:new int[]{1,3}){
            var spawner=destroy(p,Blocks.SPAWNER,mode,level->{});check(spawner.drops.isEmpty()&&spawner.xp>=15&&spawner.xp<=43,"native Silk spawner remains XP-only, never gives spawner");
        }
    }
    public static JsonObject run(ServerPlayer p)throws Exception{return run(p,false);}
    public static JsonObject run(ServerPlayer p,boolean old0613)throws Exception{
        baseline=old0613;checks=breaks=0;while(!rows.isEmpty())rows.remove(0);var level=p.level();level.getChunk(TEST);var saved=new LinkedHashMap<BlockPos,BlockState>();for(var at:List.of(TEST,TEST.below())){check(level.getBlockEntity(at)==null,"disposable fixture cells contain no old block entities");saved.put(at,level.getBlockState(at));}
        var mode=p.gameMode.getGameModeForPlayer();var pos=p.position();var velocity=p.getDeltaMovement();boolean gravity=p.isNoGravity(),shift=p.isShiftKeyDown(),ground=p.onGround();double fall=p.fallDistance;
        var inventory=new ArrayList<ItemStack>();for(int slot=0;slot<p.getInventory().getContainerSize();slot++)inventory.add(p.getInventory().getItem(slot));
        try{
            p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(TEST.getX()+.5,TEST.getY()+3,TEST.getZ()+.5);p.setDeltaMovement(Vec3.ZERO);p.getInventory().clearContent();pots(p);ice(p);hives(p);controls(p);
            var result=new JsonObject();result.addProperty("passed",true);result.addProperty("checks",checks);result.addProperty("nativePrimaryBreaks",breaks);result.addProperty("ordinaryNativeSilkPreserved",true);result.addProperty("virtualSilkUsesNativeTagsAndLoot",!baseline);result.addProperty("baseline0613",baseline);result.add("rows",rows.deepCopy());System.out.println("SILK0614_NATIVE_OK checks="+checks+" breaks="+breaks);return result;
        }finally{
            clear(level);saved.forEach((at,state)->level.setBlock(at,state,2));p.getInventory().clearContent();for(int slot=0;slot<inventory.size();slot++)p.getInventory().setItem(slot,inventory.get(slot));p.setGameMode(mode);p.setNoGravity(gravity);p.setPos(pos);p.setDeltaMovement(velocity);p.setShiftKeyDown(shift);p.setOnGround(ground);p.fallDistance=fall;MiningEffects.cancelStart(p);
        }
    }
}
