package dev.guogaology.releaseqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/** Native APIs, real item placements and disk NBT; only called by isolated QA. */
public final class Wood0610Checks {
    private static final BlockPos POS=new BlockPos(54,287,54);
    private static int checks;
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError("Wood0610: "+why);checks++;}
    private static Identifier id(String path){return Identifier.fromNamespaceAndPath("guogaology",path);}
    private static Block block(String path){return BuiltInRegistries.BLOCK.getValue(id(path));}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.getValue(id(path)));}
    private static final class FuelProbe extends FurnaceBlockEntity {
        FuelProbe(){super(POS,Blocks.FURNACE.defaultBlockState());}
        int duration(ServerLevel world,ItemStack fuel){return getBurnDuration(world,fuel);}
    }
    private static boolean place(ServerPlayer player,String name,BlockPos support,Direction face){
        player.setItemSlot(EquipmentSlot.MAINHAND,item(name));
        Vec3 hit=Vec3.atCenterOf(support).add(face.getStepX()*.5,face.getStepY()*.5,face.getStepZ()*.5);
        return player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(hit,face,support,false))).consumesAction();
    }
    // A fresh disposable QA world owns this volume; no normal save is ever passed here.
    private static void clear(ServerLevel world){for(var pos:BlockPos.betweenClosed(POS.offset(-3,-2,-3),POS.offset(4,3,3)))world.setBlock(pos,Blocks.AIR.defaultBlockState(),3);}
    // NBT clones are never added to the world, but native discard still drops their cargo.
    // Only fixture cleanup clears them; the actual destroy assertion above remains untouched.
    private static void discardFixture(net.minecraft.world.entity.Entity entity){if(entity instanceof Container cargo)cargo.clearContent();entity.discard();}
    private static final String[][] FUELS={
        {"laver_vein","oak_log"},{"laver_wood","oak_wood"},{"laver_stripped_log","stripped_oak_log"},{"laver_stripped_wood","stripped_oak_wood"},
        {"laver_planks","oak_planks"},{"laver_stairs","oak_stairs"},{"laver_slab","oak_slab"},{"laver_fence","oak_fence"},
        {"laver_fence_gate","oak_fence_gate"},{"laver_trapdoor","oak_trapdoor"},{"laver_button","oak_button"},{"laver_pressure_plate","oak_pressure_plate"},
        {"laver_door","oak_door"},{"laver_sign","oak_sign"},{"laver_hanging_sign","oak_hanging_sign"},{"laver_sapling","oak_sapling"},
        {"laver_boat","oak_boat"},{"laver_chest_boat","oak_chest_boat"}};
    public static JsonObject run(ServerPlayer player)throws Exception{
        checks=0;var result=new JsonObject();var fuels=new JsonArray();var fences=new JsonArray();var signs=new JsonArray();var boats=new JsonArray();
        var world=player.level();var oldMain=player.getMainHandItem().copy();var oldOff=player.getOffhandItem().copy();var oldPos=player.position();var oldMode=player.gameMode.getGameModeForPlayer();float oldYaw=player.getYRot(),oldPitch=player.getXRot();
        var oldVelocity=player.getDeltaMovement();var oldFall=player.fallDistance;boolean oldGravity=player.isNoGravity(),oldGround=player.onGround(),oldShift=player.isShiftKeyDown(),oldSprint=player.isSprinting(),oldFlying=player.getAbilities().flying,oldMayfly=player.getAbilities().mayfly;
        var testArea=new AABB(POS).inflate(6);var existingIds=world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,testArea).stream().map(net.minecraft.world.entity.Entity::getUUID).collect(java.util.stream.Collectors.toSet());
        var createdEntities=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        world.getChunk(POS);player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        try{
            var furnace=new FuelProbe();
            for(var pair:FUELS){
                int actual=furnace.duration(world,item(pair[0]));
                int nativeTicks=furnace.duration(world,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(pair[1]))));
                var row=new JsonObject();row.addProperty("item",pair[0]);row.addProperty("actualTicks",actual);row.addProperty("nativeItem",pair[1]);row.addProperty("nativeTicks",nativeTicks);
                check(actual>0&&actual==nativeTicks,"native fuel duration "+row);fuels.add(row);
                String dread=pair[0].equals("laver_vein")?"dread_log":pair[0].replace("laver_","dread_");
                check(furnace.duration(world,item(dread))==0,"Dread Fir must not burn "+dread);
            }
            for(String neighbor:List.of("laver_fence","dread_fence","minecraft:oak_fence","minecraft:nether_brick_fence")){
                clear(world);world.setBlock(POS.below(),Blocks.STONE.defaultBlockState(),3);world.setBlock(POS.east().below(),Blocks.STONE.defaultBlockState(),3);player.setPos(POS.getX()+.5,POS.getY()+.1,POS.getZ()+2.5);player.setYRot(180);
                check(place(player,"laver_fence",POS.below(),Direction.UP),"actual primary fence placement");
                if(neighbor.startsWith("minecraft:"))player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(neighbor))));else player.setItemSlot(EquipmentSlot.MAINHAND,item(neighbor));
                var clicked=POS.east().below();var response=player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(clicked).add(0,.5,0),Direction.UP,clicked,false)));
                check(response.consumesAction(),"actual neighbor fence placement");
                boolean east=world.getBlockState(POS).getValue(FenceBlock.EAST),west=world.getBlockState(POS.east()).getValue(FenceBlock.WEST),expected=!neighbor.contains("nether_brick");
                check(east==expected&&west==expected,"native reciprocal fence connectivity "+neighbor);
                var row=new JsonObject();row.addProperty("neighbor",neighbor);row.addProperty("east",east);row.addProperty("west",west);row.addProperty("expected",expected);fences.add(row);
            }
            int noCollision=0;
            for(String family:List.of("laver","dread"))for(String form:List.of("sign","wall_sign"))for(var state:block(family+"_"+form).getStateDefinition().getPossibleStates()){
                check(state.getCollisionShape(world,POS).isEmpty(),"ordinary sign remains pass-through "+state);noCollision++;
            }
            for(String family:List.of("laver","dread"))for(boolean wall:new boolean[]{false,true}){
                clear(world);player.setPos(POS.getX()+.5,POS.getY(),POS.getZ()-2.5);player.setYRot(0);player.setXRot(0);player.setShiftKeyDown(false);
                var support=wall?POS.south():POS.above();var face=wall?Direction.NORTH:Direction.DOWN;world.setBlock(support,Blocks.STONE.defaultBlockState(),3);
                check(place(player,family+"_hanging_sign",support,face),"native hanging-sign item placement "+family+" "+wall);
                String name=family+(wall?"_wall_hanging_sign":"_hanging_sign");var state=world.getBlockState(POS);check(state.is(block(name)),"correct ceiling/wall hanging form "+name+" "+state);
                check(world.getBlockEntity(POS) instanceof SignBlockEntity,"native hanging sign block entity");var sign=(SignBlockEntity)world.getBlockEntity(POS);
                check(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(sign.getType()).toString().equals("minecraft:hanging_sign"),"native supported hanging-sign type");
                for(var side:SignTextSlot.values()){var expectedLines=List.of("QA "+name,"front or back",side.toString(),"preserve text");sign.setAllowedPlayerEditor(player.getUUID());sign.updateSignText(player,side,expectedLines.stream().map(FilteredText::passThrough).toList());check(sign.getText(side).getMessages(false).stream().map(Component::getString).toList().equals(expectedLines),"actual authorized hanging-sign text edit "+name+" "+side);}
                var saved=sign.saveWithFullMetadata(world.registryAccess());var folder=Path.of("wood0610-nbt");Files.createDirectories(folder);var file=folder.resolve(name+".nbt");NbtIo.writeCompressed(saved,file);
                var loaded=BlockEntity.loadStatic(POS,state,NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()),world.registryAccess());
                check(loaded instanceof SignBlockEntity&&loaded.saveWithFullMetadata(world.registryAccess()).equals(saved),"both hanging-sign text sides native disk NBT roundtrip");
                var collision=state.getCollisionShape(world,POS);var nativeState=(wall?Blocks.OAK_WALL_HANGING_SIGN:Blocks.OAK_HANGING_SIGN).withPropertiesOf(state);var nativeCollision=nativeState.getCollisionShape(world,POS);boolean nativeCollisionMatches=!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(collision,nativeCollision,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME);
                check(nativeCollisionMatches,"hanging-sign collision matches actual native oak shape "+name);
                var row=new JsonObject();row.addProperty("block",name);row.addProperty("nativeItemPlacement",true);row.addProperty("nativeBlockEntity",true);row.addProperty("diskNbtBothSides",true);row.addProperty("collisionEmpty",collision.isEmpty());row.addProperty("nativeCollisionEmpty",nativeCollision.isEmpty());row.addProperty("nativeCollisionMatches",nativeCollisionMatches);signs.add(row);
            }
            clear(world);player.setPos(POS.getX(),POS.getY()+3,POS.getZ()-4);
            for(String family:List.of("laver","dread")){
                var type=BuiltInRegistries.ENTITY_TYPE.getValue(id(family+"_chest_boat"));var entity=type.create(world,EntitySpawnReason.COMMAND);if(entity!=null)createdEntities.add(entity);
                check(entity instanceof Container,"native chest boat container");var inventory=(Container)entity;check(inventory.getContainerSize()==27,"native27chest slots");
                entity.setPos(POS.getX()+.5,POS.getY()+.1,POS.getZ()+.5);entity.setNoGravity(true);entity.setCustomName(Component.literal("Saved QA "+family+" chest boat"));
                var owned=new ItemStack(Items.STICK,17);owned.set(DataComponents.CUSTOM_NAME,Component.literal("Owned cargo"));var unknown=new net.minecraft.nbt.CompoundTag();unknown.putString("qa_keep_unknown","preserve");owned.set(DataComponents.CUSTOM_DATA,CustomData.of(unknown));
                var expectedOwned=owned.copy();var expectedDiamonds=new ItemStack(Items.DIAMOND,3);inventory.setItem(0,owned);inventory.setItem(26,expectedDiamonds.copy());
                var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,world.registryAccess());entity.saveWithoutId(save);var nbt=save.buildResult();var folder=Path.of("wood0610-nbt");Files.createDirectories(folder);var file=folder.resolve(family+"_chest_boat.nbt");NbtIo.writeCompressed(nbt,file);
                var restored=type.create(world,EntitySpawnReason.COMMAND);if(restored!=null)createdEntities.add(restored);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,world.registryAccess(),NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())));
                check(restored instanceof Container&&ItemStack.matches(((Container)restored).getItem(0),expectedOwned)&&((Container)restored).getItem(13).isEmpty()&&ItemStack.matches(((Container)restored).getItem(26),expectedDiamonds),"native chest cargo NBT preserves slots/components/count");
                check(restored.getCustomName()!=null&&restored.getCustomName().equals(entity.getCustomName()),"named chest boat NBT preserves name");
                var menu=((net.minecraft.world.MenuProvider)entity).createMenu(73,player.getInventory(),player);check(menu!=null&&menu.slots.size()==63,"native27slot chest menu plus36player slots");
                check(world.addFreshEntity(entity),"actual chest boat registered in world");
                ((net.minecraft.world.entity.vehicle.boat.AbstractChestBoat)entity).destroy(world,world.damageSources().playerAttack(player));
                var drops=new TreeMap<String,Integer>();boolean cargoKept=true;int cargoPieces=0;var actualStacks=new JsonArray();
                for(var drop:world.getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(3))){
                    var stack=drop.getItem();String stackId=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();drops.merge(stackId,stack.getCount(),Integer::sum);
                    var actual=new JsonObject();actual.addProperty("item",stackId);actual.addProperty("count",stack.getCount());actual.addProperty("components",stack.getComponentsPatch().toString());
                    if(stack.is(Items.STICK)){cargoPieces++;boolean matches=ItemStack.isSameItemSameComponents(stack,expectedOwned);cargoKept&=matches;actual.addProperty("matchesExpectedCargoComponents",matches);}
                    actualStacks.add(actual);drop.discard();
                }
                var cargoProof=new JsonObject();cargoProof.addProperty("expectedCount",expectedOwned.getCount());cargoProof.addProperty("expectedComponents",expectedOwned.getComponentsPatch().toString());cargoProof.addProperty("mutableSourceCountAfterNativeSplit",owned.getCount());cargoProof.addProperty("cargoPieces",cargoPieces);cargoProof.addProperty("allCargoComponentsPreserved",cargoKept&&cargoPieces>0);cargoProof.add("actualDrops",actualStacks);
                Files.writeString(Path.of("wood0610-"+family+"-destroy.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(cargoProof)+"\n");
                check(drops.equals(Map.of("guogaology:"+family+"_chest_boat",1,"minecraft:stick",17,"minecraft:diamond",3))&&cargoKept&&cargoPieces>0,"native chest boat destruction drops exact boat and cargo once "+drops);
                var row=new JsonObject();row.addProperty("wood",family);row.addProperty("slots",27);row.addProperty("actualNativeMenu",true);row.addProperty("diskNbtRoundtrip",true);row.addProperty("destructionConserved",true);boats.add(row);discardFixture(entity);discardFixture(restored);
            }
            result.addProperty("ordinarySignNoCollisionStates",noCollision);
        }finally{
            try{
                clear(world);for(var entity:createdEntities)discardFixture(entity);
                for(var entity:world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class,testArea))if(entity!=player&&!existingIds.contains(entity.getUUID()))discardFixture(entity);
            }finally{
                player.closeContainer();player.setItemSlot(EquipmentSlot.MAINHAND,oldMain);player.setItemSlot(EquipmentSlot.OFFHAND,oldOff);player.setGameMode(oldMode);player.setPos(oldPos);player.setYRot(oldYaw);player.setXRot(oldPitch);
                player.setNoGravity(oldGravity);player.setOnGround(oldGround);player.setShiftKeyDown(oldShift);player.setSprinting(oldSprint);player.setDeltaMovement(oldVelocity);player.fallDistance=oldFall;player.getAbilities().mayfly=oldMayfly;player.getAbilities().flying=oldFlying;player.onUpdateAbilities();
            }
        }
        result.add("fuel",fuels);result.add("fences",fences);result.add("hangingSigns",signs);result.add("chestBoats",boats);result.addProperty("checks",checks);result.addProperty("passed",true);return result;
    }
    public static JsonObject client(Minecraft client)throws Exception{
        var out=new JsonObject();var rows=new JsonArray();
        for(String family:List.of("laver","dread")){
            var model=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(model,item(family+"_door"),ItemDisplayContext.GUI,client.player);check(!model.isEmpty(),"door GUI model exists");
            var doorResource=client.getResourceManager().getResourceOrThrow(id("textures/item/"+family+"_door.png"));java.awt.image.BufferedImage door;
            try(var stream=doorResource.open()){door=ImageIO.read(stream);}
            int minX=door.getWidth(),minY=door.getHeight(),maxX=-1,maxY=-1;
            for(int y=0;y<door.getHeight();y++)for(int x=0;x<door.getWidth();x++)if((door.getRGB(x,y)>>>24)!=0){minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
            int alphaWidth=maxX-minX+1,alphaHeight=maxY-minY+1;
            check(alphaWidth>0&&alphaHeight>=12&&alphaHeight>alphaWidth*1.3,"whole tall door sprite silhouette "+family+" "+alphaWidth+"x"+alphaHeight);
            var source=client.getResourceManager().getResourceOrThrow(id("textures/block/"+family+"_sign.png"));var gui=client.getResourceManager().getResourceOrThrow(id("textures/gui/signs/"+family+".png"));
            java.awt.image.BufferedImage atlas,image;try(var stream=source.open()){atlas=ImageIO.read(stream);}try(var stream=gui.open()){image=ImageIO.read(stream);}
            for(int y=0;y<12;y++)for(int x=0;x<24;x++)check(atlas.getRGB(x,y+2)==image.getRGB(x,y),"editor uses exact light/front sign face");
            var row=new JsonObject();row.addProperty("wood",family);row.addProperty("wholeDoorIcon",true);row.addProperty("guiMatchesLightFacePixels",288);rows.add(row);
        }
        if(client.gui.screen() instanceof AbstractSignEditScreen)client.gui.setScreen(null);
        out.add("textures",rows);out.addProperty("passed",true);return out;
    }
}
