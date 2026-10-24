package dev.guogaology.releaseqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Actual registered native entityInside, scheduled-tick and piston block-event entries.
 * Synchronous to keep unrelated old-save fingerprints frozen: no fabricated world ticks.
 */
public final class NativeMechanism0614WoodChecks {
    private static final BlockPos POS=new BlockPos(40,246,40);
    private static final AABB AREA=new AABB(POS).inflate(6);
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError("Wood0614: "+why);}
    private static Block block(String name){return BuiltInRegistries.BLOCK.getValue(Identifier.parse(name));}
    private static String name(Block b){return BuiltInRegistries.BLOCK.getKey(b).toString();}
    private static final String[] SHAPES={"fence","fence_gate","door","trapdoor","button","pressure_plate"};
    private static void clear(ServerLevel world){
        for(var p:BlockPos.betweenClosed(POS.offset(-4,-2,-4),POS.offset(4,3,4))){
            world.removeBlockEntity(p);
            world.setBlock(p,Blocks.AIR.defaultBlockState(),18);
        }
    }
    private static void floor(ServerLevel world){
        clear(world);
        for(var p:BlockPos.betweenClosed(POS.offset(-3,-1,-3),POS.offset(3,-1,3)))
            world.setBlock(p,Blocks.STONE.defaultBlockState(),18);
    }
    private static void away(ServerPlayer p){p.setPos(POS.getX()+4.5,POS.getY()+2,POS.getZ()+4.5);}
    private static Entity entity(ServerLevel world,String kind,List<Entity> owned){
        Entity e;
        if(kind.equals("item"))e=new ItemEntity(world,POS.getX()+.5,POS.getY()+.03,POS.getZ()+.5,new ItemStack(Items.STICK));
        else{
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(kind.equals("mob")?"pig":"arrow"));
            e=type.create(world,EntitySpawnReason.COMMAND);
            check(e!=null,"registered "+kind+" created");
            e.setPos(POS.getX()+.5,POS.getY()+.03,POS.getZ()+.5);
        }
        e.setNoGravity(true);
        if(e instanceof Mob mob)mob.setNoAi(true);
        owned.add(e);
        check(world.addFreshEntity(e),"real registered "+kind+" inserted");
        return e;
    }
    private static JsonArray properties(ServerLevel world){
        var rows=new JsonArray();
        for(String family:List.of("laver","dread"))for(String shape:SHAPES){
            var own=block("guogaology:"+family+"_"+shape);
            var control=block("minecraft:"+(family.equals("dread")?"crimson":"oak")+"_"+shape);
            var ownStates=own.getStateDefinition().getPossibleStates();
            var nativeStates=control.getStateDefinition().getPossibleStates();
            check(ownStates.size()==nativeStates.size(),"unchanged native state cardinality "+name(own));
            var ownNames=own.getStateDefinition().getProperties().stream().map(p->p.getName()).collect(java.util.stream.Collectors.toSet());
            var nativeNames=control.getStateDefinition().getProperties().stream().map(p->p.getName()).collect(java.util.stream.Collectors.toSet());
            check(ownNames.equals(nativeNames),"unchanged native saved property keys "+name(own));
            for(var state:ownStates){
                var reference=control.withPropertiesOf(state);
                check(state.isSolid()==reference.isSolid(),"native solid predicate "+state);
                check(state.getPistonPushReaction()==reference.getPistonPushReaction(),"native piston reaction "+state);
                check(state.instrument()==reference.instrument(),"native note instrument "+state);
                check(state.getDestroySpeed(world,POS)==reference.getDestroySpeed(world,POS),"unchanged native hardness "+state);
                check(!Shapes.joinIsNotEmpty(state.getCollisionShape(world,POS),reference.getCollisionShape(world,POS),BooleanOp.NOT_SAME),"native collision "+state);
            }
            var row=new JsonObject();row.addProperty("block",name(own));row.addProperty("control",name(control));row.addProperty("states",ownStates.size());row.addProperty("physicalParity",true);row.addProperty("savedPropertiesPreserved",true);rows.add(row);
        }
        return rows;
    }
    private static JsonArray pressure(ServerPlayer player,List<Entity> owned,boolean observational){
        var world=player.level();var rows=new JsonArray();
        for(String id:List.of("minecraft:oak_pressure_plate","guogaology:laver_pressure_plate","minecraft:crimson_pressure_plate","guogaology:dread_pressure_plate")){
            var plate=block(id);
            for(String kind:List.of("item","player","mob","arrow")){
                var row=new JsonObject();row.addProperty("plate",id);row.addProperty("entrant",kind);
                try{
                away(player);floor(world);
                world.setBlock(POS,plate.defaultBlockState(),3);
                world.setBlock(POS.east(),Blocks.REDSTONE_WIRE.defaultBlockState(),3);
                check(world.getBlockState(POS.east()).is(Blocks.REDSTONE_WIRE),"native redstone fixture survives");
                check(!world.getBlockState(POS).getValue(BlockStateProperties.POWERED),"starts unpressed "+id);
                Entity entrant;
                if(kind.equals("player")){player.setPos(POS.getX()+.5,POS.getY()+.03,POS.getZ()+.5);entrant=player;}
                else entrant=entity(world,kind,owned);
                check(entrant.getBoundingBox().intersects(new AABB(POS.getX()+.0625,POS.getY(),POS.getZ()+.0625,POS.getX()+.9375,POS.getY()+.25,POS.getZ()+.9375)),"actual entrant intersects native pressure detection bounds");
                // Exercise the native entity->block intersection/dispatch gate as well.
                entrant.applyEffectsFromBlocks(entrant.position().add(0,0,-.45),entrant.position());
                var pressed=world.getBlockState(POS);
                check(pressed.getValue(BlockStateProperties.POWERED),"native entityInside activates "+id+" "+kind);
                check(world.getBlockState(POS.east()).getValue(BlockStateProperties.POWER)==15,"native neighbour propagation powers real wire15 "+id+" "+kind);
                check(pressed.getShape(world,POS).max(Direction.Axis.Y)==.5/16,"native pressed shape .5pixel");
                check(world.getBlockTicks().hasScheduledTick(POS,plate),"actual native20tick recheck scheduled");
                pressed.tick(world,POS,world.getRandom());
                check(world.getBlockState(POS).getValue(BlockStateProperties.POWERED),"native scheduled tick holds while entrant remains");
                if(entrant==player)away(player);else entrant.discard();
                world.getBlockState(POS).tick(world,POS,world.getRandom());
                check(!world.getBlockState(POS).getValue(BlockStateProperties.POWERED),"native scheduled tick releases after entrant leaves");
                check(world.getBlockState(POS.east()).getValue(BlockStateProperties.POWER)==0,"real wire unpowers after native release");
                check(world.getBlockState(POS).getShape(world,POS).max(Direction.Axis.Y)==1.0/16,"native released shape1pixel");
                row.addProperty("activated",true);row.addProperty("held",true);row.addProperty("released",true);row.addProperty("wirePressed",15);row.addProperty("wireReleased",0);row.addProperty("nativeScheduledTickRegistered",true);row.addProperty("matchesNativeActivation",true);
                }catch(AssertionError failure){
                    // Old production is an observation, not a current-value gate. Native
                    // controls must still pass, and all current mechanisms remain strict.
                    if(!observational||id.startsWith("minecraft:"))throw failure;
                    row.addProperty("matchesNativeActivation",false);row.addProperty("observedOldFailure",failure.getMessage());
                }finally{away(player);for(var e:owned)if(!e.isRemoved())e.discard();}
                rows.add(row);
            }
        }
        return rows;
    }
    private static JsonArray buttons(ServerPlayer player,List<Entity> owned){
        var world=player.level();var rows=new JsonArray();
        for(String id:List.of("minecraft:oak_button","guogaology:laver_button","minecraft:crimson_button","guogaology:dread_button")){
            away(player);floor(world);var b=block(id);
            var state=b.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
            world.setBlock(POS,state,3);
            var arrow=entity(world,"arrow",owned);
            var bounds=state.getShape(world,POS).bounds().move(POS);
            arrow.setPos((bounds.minX+bounds.maxX)/2,bounds.minY,(bounds.minZ+bounds.maxZ)/2);
            check(arrow.getBoundingBox().intersects(bounds),"native button arrow intersects real shape");
            state.entityInside(world,POS,arrow,InsideBlockEffectApplier.NOOP,true);
            check(world.getBlockState(POS).getValue(BlockStateProperties.POWERED),"native wooden button arrow activates "+id);
            check(world.getBlockTicks().hasScheduledTick(POS,b),"native wooden button30tick recheck registered");
            world.getBlockState(POS).tick(world,POS,world.getRandom());
            check(world.getBlockState(POS).getValue(BlockStateProperties.POWERED),"arrow holds native button");
            arrow.discard();world.getBlockState(POS).tick(world,POS,world.getRandom());
            check(!world.getBlockState(POS).getValue(BlockStateProperties.POWERED),"native button releases without arrow");
            var row=new JsonObject();row.addProperty("button",id);row.addProperty("arrowActivates",true);row.addProperty("holdsAndReleases",true);rows.add(row);
        }
        return rows;
    }
    private static JsonArray pistons(ServerPlayer player,List<Entity> owned,Set<UUID> original){
        var world=player.level();var rows=new JsonArray();
        for(String shape:List.of("pressure_plate","button"))for(String family:List.of("oak","laver","crimson","dread")){
            away(player);floor(world);
            String id=(family.equals("oak")||family.equals("crimson")?"minecraft:":"guogaology:")+family+"_"+shape;
            var wood=block(id);var woodState=wood.defaultBlockState();
            if(shape.equals("button"))woodState=woodState.setValue(BlockStateProperties.ATTACH_FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
            world.setBlock(POS,woodState,18);
            var base=POS.west();var piston=Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING,Direction.EAST);
            world.setBlock(base,piston,18);
            world.setBlock(base.north(),Blocks.REDSTONE_BLOCK.defaultBlockState(),18);
            var plan=new PistonStructureResolver(world,base,Direction.EAST,true);
            check(plan.resolve()&&plan.getToDestroy().contains(POS),"real native piston destroys rather than carries "+id);
            check(piston.triggerEvent(world,base,0,Direction.EAST.get3DDataValue()),"actual powered piston extension block event "+id);
            check(!world.getBlockState(POS).is(wood),"actual piston removed original mechanism "+id);
            var drops=new TreeMap<String,Integer>();
            for(var drop:world.getEntitiesOfClass(ItemEntity.class,AREA))if(!original.contains(drop.getUUID())){
                owned.add(drop);var stack=drop.getItem();drops.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);drop.discard();
            }
            check(drops.equals(Map.of(id,1)),"exact own-ID piston drop once "+id+" "+drops);
            var row=new JsonObject();row.addProperty("block",id);row.addProperty("actualPistonExtension",true);row.addProperty("drop",id);row.addProperty("count",1);rows.add(row);
        }
        return rows;
    }
    private static JsonArray miningDrops(ServerPlayer player,List<Entity> owned,Set<UUID> original){
        var world=player.level();var rows=new JsonArray();
        var forms=List.of("fence","fence_gate","door","trapdoor","button","pressure_plate","sign","wall_sign","hanging_sign","wall_hanging_sign");
        for(String family:List.of("oak","laver","crimson","dread"))for(String shape:forms){
            away(player);floor(world);
            String namespace=family.equals("oak")||family.equals("crimson")?"minecraft:":"guogaology:";
            String id=namespace+family+"_"+shape;
            String expected=namespace+family+"_"+(shape.equals("wall_sign")?"sign":shape.equals("wall_hanging_sign")?"hanging_sign":shape);
            var b=block(id);var state=b.defaultBlockState();
            if(shape.equals("button"))state=state.setValue(BlockStateProperties.ATTACH_FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
            world.setBlock(POS,state,18);
            if(shape.equals("door"))world.setBlock(POS.above(),state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),18);
            if(shape.equals("hanging_sign"))world.setBlock(POS.above(),Blocks.STONE.defaultBlockState(),18);
            if(shape.equals("wall_sign")||shape.equals("wall_hanging_sign"))
                for(var side:Direction.Plane.HORIZONTAL)world.setBlock(POS.relative(side),Blocks.STONE.defaultBlockState(),18);
            check(state.canSurvive(world,POS),"native-supported mechanism destruction fixture "+id);
            dev.guogaology.mining.MiningEffects.cancelStart(player);
            check(player.gameMode.destroyBlock(POS),"actual player destruction "+id);
            var drops=new TreeMap<String,Integer>();
            for(var drop:world.getEntitiesOfClass(ItemEntity.class,AREA))if(!original.contains(drop.getUUID())){
                owned.add(drop);var stack=drop.getItem();drops.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);drop.discard();
            }
            check(drops.equals(Map.of(expected,1)),"own family loot after full property copy, exactly once "+id+" "+drops);
            var row=new JsonObject();row.addProperty("block",id);row.addProperty("drop",expected);row.addProperty("count",1);row.addProperty("nativePlayerDestroy",true);rows.add(row);
        }
        return rows;
    }
    public static JsonObject run(ServerPlayer player)throws Exception{return run(player,false);}
    public static JsonObject runBaselinePressure(ServerPlayer player)throws Exception{return run(player,true);}
    private static JsonObject run(ServerPlayer player,boolean observational)throws Exception{
        checks=0;var out=new JsonObject();var world=player.level();
        world.getChunk(POS);
        var saved=new LinkedHashMap<BlockPos,BlockState>();
        for(var mutable:BlockPos.betweenClosed(POS.offset(-4,-2,-4),POS.offset(4,3,4))){
            var p=mutable.immutable();check(world.getBlockEntity(p)==null,"disposable fixture has no existing block entity");saved.put(p,world.getBlockState(p));
        }
        var original=world.getEntitiesOfClass(Entity.class,AREA).stream().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        check(original.isEmpty()||original.equals(Set.of(player.getUUID())),"fixture contains no unrelated existing entities");
        var mode=player.gameMode.getGameModeForPlayer();var pos=player.position();var velocity=player.getDeltaMovement();var yaw=player.getYRot();var pitch=player.getXRot();
        boolean gravity=player.isNoGravity(),ground=player.onGround(),shift=player.isShiftKeyDown(),sprint=player.isSprinting(),flying=player.getAbilities().flying,mayfly=player.getAbilities().mayfly;
        var fall=player.fallDistance;var main=player.getMainHandItem().copy();var off=player.getOffhandItem().copy();var inventory=new ArrayList<ItemStack>();
        for(int i=0;i<player.getInventory().getContainerSize();i++)inventory.add(player.getInventory().getItem(i).copy());
        var owned=new ArrayList<Entity>();
        try{
            player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);away(player);
            if(!observational)out.add("nativePropertyParity",properties(world));
            out.add("pressurePlateEntrants",pressure(player,owned,observational));
            if(!observational){
                out.add("woodenButtonArrows",buttons(player,owned));
                out.add("pistonExtensionsAndExactDrops",pistons(player,owned,original));
                out.add("playerDestructionOwnLoot",miningDrops(player,owned,original));
            }else{
                var observations=new JsonArray();
                for(String id:List.of("minecraft:oak_pressure_plate","guogaology:laver_pressure_plate","minecraft:crimson_pressure_plate","guogaology:dread_pressure_plate")){
                    var state=block(id).defaultBlockState();var row=new JsonObject();row.addProperty("plate",id);row.addProperty("solid",state.isSolid());row.addProperty("pistonReaction",state.getPistonPushReaction().toString());row.addProperty("instrument",state.instrument().toString());observations.add(row);
                }
                out.add("oldPhysicalObservations",observations);
            }
            player.setItemSlot(EquipmentSlot.MAINHAND,main.copy());player.setItemSlot(EquipmentSlot.OFFHAND,off.copy());
            for(int i=0;i<inventory.size();i++)check(ItemStack.matches(inventory.get(i),player.getInventory().getItem(i)),"inventory and components conserved slot"+i);
        }finally{
            for(var e:owned)e.discard();
            clear(world);
            for(var e:world.getEntitiesOfClass(Entity.class,AREA))if(e!=player&&!original.contains(e.getUUID()))e.discard();
            for(var e:saved.entrySet())world.setBlock(e.getKey(),e.getValue(),18);
            for(int i=0;i<inventory.size();i++)player.getInventory().setItem(i,inventory.get(i));
            player.setGameMode(mode);player.setPos(pos);player.setYRot(yaw);player.setXRot(pitch);player.setDeltaMovement(velocity);
            player.setNoGravity(gravity);player.setOnGround(ground);player.setShiftKeyDown(shift);player.setSprinting(sprint);player.fallDistance=fall;player.getAbilities().flying=flying;player.getAbilities().mayfly=mayfly;player.onUpdateAbilities();
        }
        for(var e:saved.entrySet())check(world.getBlockState(e.getKey()).equals(e.getValue()),"all fixture states restored "+e.getKey());
        for(int i=0;i<inventory.size();i++)check(ItemStack.matches(inventory.get(i),player.getInventory().getItem(i)),"finally inventory conservation slot"+i);
        out.addProperty("nativeEntryScope","actual entity collision dispatcher + registered entityInside + scheduledTick + powered piston blockEvent; no world tick advancement");
        out.addProperty("baselineObservational",observational);out.addProperty("pressureEntrantCases",16);out.addProperty("buttonArrowCases",observational?0:4);out.addProperty("actualPistonCases",observational?0:8);out.addProperty("actualPlayerDropCases",observational?0:40);out.addProperty("cleanupConserved",true);out.addProperty("checks",checks);out.addProperty("passed",true);
        return out;
    }
}

