package dev.guogaology.releaseqa263;

import com.google.gson.*;
import dev.guogaology.CoreGrades;
import dev.guogaology.mining.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.nio.file.Files;
import java.util.*;

/** Native recipe matching, survival harvesting and shape controls for this repair only. */
public final class Small0610Checks {
    private static final BlockPos TEST=new BlockPos(12,240,12);
    private static final List<BlockPos> CELLS=List.of(TEST,TEST.west(),TEST.east(),TEST.below());
    private static final AABB AREA=new AABB(TEST).inflate(3);
    private static int checks,breaks,extraBreaks;
    private static void check(boolean yes,String label){checks++;if(!yes)throw new AssertionError("Small0610: "+label);}
    private static Block block(String name){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+name));}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static ItemStack recipe(ServerLevel level,List<ItemStack> cells){
        var input=CraftingInput.of(3,3,cells);var match=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        return match.isEmpty()?ItemStack.EMPTY:match.orElseThrow().value().assemble(input);
    }
    private static JsonObject recipes(ServerLevel level){
        var rows=new JsonArray();int rejected=0;
        for(int mask=0;mask<16;mask++){
            var cells=new ArrayList<ItemStack>();int sand=0;
            for(int i=0;i<9;i++)cells.add(i%2==0?item("nuke_mushroom"):new ItemStack((mask&(1<<sand++))==0?Items.SAND:Items.RED_SAND));
            var out=recipe(level,cells);check(out.is(Items.TNT)&&out.getCount()==1,"all sand/red-sand mixtures craft exactly one TNT mask="+mask+" got="+out);
            for(int i=0;i<9;i++){
                var missing=new ArrayList<>(cells);missing.set(i,ItemStack.EMPTY);check(!recipe(level,missing).is(Items.TNT),"TNT rejects missing cell "+i+" mask="+mask);rejected++;
                var wrong=new ArrayList<>(cells);wrong.set(i,new ItemStack(i%2==0?Items.BROWN_MUSHROOM:Items.SOUL_SAND));check(!recipe(level,wrong).is(Items.TNT),"TNT rejects wrong item in cell "+i+" mask="+mask);rejected++;
            }
            var row=new JsonObject();row.addProperty("redSandMask",mask);row.addProperty("count",out.getCount());row.addProperty("output","minecraft:tnt");rows.add(row);
        }
        var out=new JsonObject();out.addProperty("validSandMixtures",16);out.addProperty("rejectedMissingOrWrongGrids",rejected);out.add("rows",rows);return out;
    }
    private static ItemStack tool(ServerLevel world,int mode){
        var tool=mode<3?new ItemStack(Items.NETHERITE_PICKAXE):item("true_omega_pickaxe");
        if(mode==1||mode==2)tool.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode==1?Enchantments.SILK_TOUCH:Enchantments.FORTUNE),mode==1?1:3);
        if(mode>=3){var cores=new ArrayList<ItemStack>();cores.add(item("hydra_bud"));if(mode>=5)cores.add(item("sequence_core"));GearData.setCores(tool,cores);GearData.setSilkTouch(tool,mode==4||mode==5);GearData.refresh(tool);if(mode>=5)GearData.setMiningPattern(tool,new MiningPattern(12,1<<13));check(GearData.silkTouch(tool)==(mode==4||mode==5),"actual Branch silk toggle mode="+mode);}
        return tool;
    }
    private static Map<String,Integer> harvest(ServerPlayer player,ItemStack tool,boolean extra){
        var level=player.level();var beforeEntities=new HashSet<UUID>();for(var e:level.getEntitiesOfClass(ItemEntity.class,AREA))beforeEntities.add(e.getUUID());
        var beforeOrbs=new HashSet<UUID>();for(var e:level.getEntitiesOfClass(ExperienceOrb.class,AREA))beforeOrbs.add(e.getUUID());
        var made=new ArrayList<Entity>();
        try{
            for(var pos:CELLS)level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            player.setItemSlot(EquipmentSlot.MAINHAND,tool);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);player.setShiftKeyDown(false);player.setYRot(extra?180:0);player.setXRot(0);
            level.setBlock(TEST,block("gummy_jar").defaultBlockState(),2);
            if(extra){level.setBlock(TEST.west(),block("gummy_jar").defaultBlockState(),2);level.setBlock(TEST.below(),block("gummy_jar").defaultBlockState(),2);level.setBlock(TEST.east(),Blocks.STONE.defaultBlockState(),2);}
            var original=tool.copy();if(extra)MiningEffects.captureStart(player,TEST);check(player.gameMode.destroyBlock(TEST),"actual survival destroyBlock of gummy jar");breaks++;check(level.getBlockState(TEST).isAir(),"jar primary really removed");
            if(extra){check(level.getBlockState(TEST.west()).is(block("gummy_jar")),"spread cannot harvest a second jar");check(level.getBlockState(TEST.below()).is(block("gummy_jar")),"unselected lower jar is preserved");check(level.getBlockState(TEST.east()).isAir(),"spread AFTER hook really ran and harvested eligible adjacent stone");extraBreaks++;}
            var after=tool.copy();after.setDamageValue(original.getDamageValue());check(ItemStack.matches(original,after),"owned tool components, enchantments and silk toggle unchanged except wear");
            var drops=new TreeMap<String,Integer>();for(var e:level.getEntitiesOfClass(ItemEntity.class,AREA))if(!beforeEntities.contains(e.getUUID())){made.add(e);drops.merge(BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).toString(),e.getItem().getCount(),Integer::sum);}
            for(var e:level.getEntitiesOfClass(ExperienceOrb.class,AREA))if(!beforeOrbs.contains(e.getUUID()))made.add(e);
            check(made.stream().noneMatch(e->e instanceof ExperienceOrb),"jar and adjacent stone do not manufacture XP");return drops;
        }finally{
            // Only new drops from this test are removed; pre-existing entities are untouched.
            for(var e:level.getEntitiesOfClass(ItemEntity.class,AREA))if(!beforeEntities.contains(e.getUUID()))e.discard();
            for(var e:level.getEntitiesOfClass(ExperienceOrb.class,AREA))if(!beforeOrbs.contains(e.getUUID()))e.discard();
            for(var pos:CELLS)level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static JsonObject jars(ServerPlayer p){
        var rows=new JsonArray();String[] names={"plainVanilla","vanillaSilk","vanillaFortuneIII","branchFortune","branchSilk","branchSilkAndSpread","branchFortuneAndSpread"};int[] samples={128,24,64,64,24,24,32};
        for(int mode=0;mode<names.length;mode++){
            var histogram=new TreeMap<Integer,Integer>();int total=0;
            for(int i=0;i<samples[mode];i++){
                var drops=harvest(p,tool(p.level(),mode),mode>=5);boolean silk=mode==1||mode==4||mode==5;
                if(mode>=5){String stone=silk?"minecraft:stone":"minecraft:cobblestone";check(drops.getOrDefault(stone,0)==1,"one spread stone drops once mode="+names[mode]+" got="+drops);drops.remove(stone);}
                if(silk)check(drops.equals(Map.of("guogaology:gummy_jar",1)),"Silk returns one jar and no gummy mode="+names[mode]+" got="+drops);
                else{int gummy=drops.getOrDefault("guogaology:gummy",0);check(drops.size()==1&&gummy>=1&&gummy<=3,"ordinary jar gives only1-3 gummy mode="+names[mode]+" got="+drops);histogram.merge(gummy,1,Integer::sum);total+=gummy;}
            }
            if(mode==0){check(histogram.keySet().equals(Set.of(1,2,3)),"ordinary native sample retains all three drop counts");double mean=total/(double)samples[mode];check(mean>=1.5&&mean<=2.5,"ordinary native sample remains around uniform1-3 mean, got="+mean);}
            var row=new JsonObject();row.addProperty("mode",names[mode]);row.addProperty("samples",samples[mode]);row.addProperty("silk",mode==1||mode==4||mode==5);row.add("gummyCountHistogram",new Gson().toJsonTree(histogram));row.addProperty("gummyTotal",total);rows.add(row);
        }
        var out=new JsonObject();out.addProperty("actualPrimaryBreaks",breaks);out.addProperty("actualSpreadStoneBreaks",extraBreaks);out.add("rows",rows);return out;
    }
    private static JsonObject collision(ServerPlayer p){
        var level=p.level();var context=CollisionContext.of(p);var rows=new JsonArray();int absenceStates=0,otherStates=0;
        for(var root:CoreGrades.ROOTS)for(var type:CoreGrades.levels(root)){
            boolean absence=root.equals("lho_trace");int count=0;
            for(var state:type.getStateDefinition().getPossibleStates()){
                level.setBlock(TEST,state,2);
                check(state.getCollisionShape(level,TEST).isEmpty()==absence,"core collision scope default context "+state);
                check(state.getCollisionShape(level,TEST,context).isEmpty()==absence,"core collision scope player context "+state);
                check(!state.getShape(level,TEST,context).isEmpty(),"all cores remain selectable through nonempty outline "+state);
                count++;
            }
            if(absence)absenceStates+=count;else otherStates+=count;var row=new JsonObject();row.addProperty("block",BuiltInRegistries.BLOCK.getKey(type).toString());row.addProperty("states",count);row.addProperty("noCollision",absence);row.addProperty("selectable",true);rows.add(row);
        }
        var out=new JsonObject();out.addProperty("absenceStates",absenceStates);out.addProperty("neighborCoreStates",otherStates);out.add("rows",rows);return out;
    }
    public static void run(ServerPlayer p)throws Exception{
        checks=0;breaks=0;extraBreaks=0;var level=p.level();check(level.hasChunkAt(TEST),"test chunk was already loaded by isolated runner");
        var oldBlocks=new LinkedHashMap<BlockPos,BlockState>();for(var pos:CELLS){check(level.getBlockEntity(pos)==null,"temporary cell contains no existing block entity");oldBlocks.put(pos,level.getBlockState(pos));}
        var oldMode=p.gameMode.getGameModeForPlayer();var oldPosition=p.position();var oldVelocity=p.getDeltaMovement();var oldYaw=p.getYRot();var oldPitch=p.getXRot();var oldFall=p.fallDistance;boolean oldGravity=p.isNoGravity(),oldGround=p.onGround(),oldShift=p.isShiftKeyDown(),oldFlying=p.getAbilities().flying,oldMayfly=p.getAbilities().mayfly;
        var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i));var food=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());p.getFoodData().addAdditionalSaveData(food);var oldFood=food.buildResult();
        try{
            p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.setPos(TEST.getX()+.5,TEST.getY()+2,TEST.getZ()+.5);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.getInventory().clearContent();
            var out=new JsonObject();out.add("tnt",recipes(level));out.add("gummyJar",jars(p));out.add("coreShapes",collision(p));out.addProperty("passed",true);out.addProperty("checks",checks);
            Files.writeString(FabricLoader.getInstance().getGameDir().resolve("small0610-server.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\n");System.out.println("SMALL0610_NATIVE_OK checks="+checks+" breaks="+breaks+" extra="+extraBreaks);
        }finally{
            for(var entry:oldBlocks.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
            p.getInventory().clearContent();for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.setGameMode(oldMode);p.setNoGravity(oldGravity);p.setPos(oldPosition);p.setYRot(oldYaw);p.setXRot(oldPitch);p.setDeltaMovement(oldVelocity);p.fallDistance=oldFall;p.setOnGround(oldGround);p.setShiftKeyDown(oldShift);p.getAbilities().flying=oldFlying;p.getAbilities().mayfly=oldMayfly;p.getFoodData().readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),oldFood));ManuscriptEffects.tick(p);
        }
    }
}
