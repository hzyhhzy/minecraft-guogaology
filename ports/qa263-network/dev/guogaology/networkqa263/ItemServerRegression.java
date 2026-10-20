package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Files;
import java.util.List;
import java.util.TreeMap;

/** Opt-in actual placement/destruction, with observations collected before any verify verdict. */
final class ItemServerRegression {
    private static final BlockPos TEST=new BlockPos(12,250,5);
    private static final AABB AREA=new AABB(TEST).inflate(4);
    private final JsonArray rows=new JsonArray(),issues=new JsonArray();
    private static final List<String> TOOLS=List.of("hand","axe","silk","fortune","creative");
    private static Block block(String id){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+id));}
    private static BlockEntityType<?> signType(){return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.parse("minecraft:sign"));}
    private static ItemStack item(String id){return new ItemStack(NetworkProbe.item("guogaology:"+id));}
    private void expect(boolean ok,String reason){if(!ok)issues.add(reason);}
    private static void clear(ServerPlayer p){
        var w=p.level();
        for(var pos:BlockPos.betweenClosed(TEST.offset(-1,0,-1),TEST.offset(1,3,1)))w.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        w.setBlock(TEST.below(),Blocks.SMOOTH_STONE.defaultBlockState(),3);
        for(var e:w.getEntitiesOfClass(ItemEntity.class,AREA))e.discard();
        p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setShiftKeyDown(false);p.setYRot(0);p.setXRot(0);
        p.setGameMode(GameType.SURVIVAL);
    }
    private static ItemStack tool(ServerPlayer p,String mode){
        if(mode.equals("hand")||mode.equals("creative"))return ItemStack.EMPTY;
        var stack=new ItemStack(Items.IRON_AXE);
        var data=new CompoundTag();data.putString("qa_owned_tool","must-not-copy-to-drops");
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        if(mode.equals("silk")||mode.equals("fortune"))
            stack.enchant(p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(mode.equals("silk")?Enchantments.SILK_TOUCH:Enchantments.FORTUNE),mode.equals("silk")?1:3);
        return stack;
    }
    private boolean place(ServerPlayer p,String id,BlockPos support,Direction face,Vec3 hit,int count){
        p.setItemSlot(EquipmentSlot.MAINHAND,item(id).copyWithCount(count));
        var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(hit,face,support,false)));
        return result.consumesAction();
    }
    private JsonObject harvest(ServerPlayer p,String id,String form,String mode,BlockPos target,String expectedItem,int expectedCount){
        var row=new JsonObject();row.addProperty("block",id);row.addProperty("form",form);row.addProperty("tool",mode);
        var held=tool(p,mode);p.setItemSlot(EquipmentSlot.MAINHAND,held);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        p.setGameMode(mode.equals("creative")?GameType.CREATIVE:GameType.SURVIVAL);
        var owned=held.copy();boolean destroyed=p.gameMode.destroyBlock(target);row.addProperty("actualPlayerDestroy",destroyed);
        expect(destroyed,id+" "+form+" "+mode+" actual player destruction");
        var drops=new TreeMap<String,Integer>();boolean plain=true;
        for(var entity:p.level().getEntitiesOfClass(ItemEntity.class,AREA)){
            var stack=entity.getItem();drops.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);
            plain&=stack.getComponentsPatch().isEmpty();entity.discard();
        }
        var counts=new JsonObject();drops.forEach(counts::addProperty);row.add("drops",counts);row.addProperty("plainDropComponents",plain);
        int count=mode.equals("creative")?0:expectedCount;
        boolean exact=count==0?drops.isEmpty():drops.size()==1&&drops.getOrDefault("guogaology:"+expectedItem,0)==count;
        row.addProperty("expectedCount",count);row.addProperty("expectedDropsMatch",exact);
        expect(exact,id+" "+form+" "+mode+" expected "+count+" "+expectedItem+", observed "+drops);
        expect(plain,id+" "+form+" "+mode+" drop components must not copy owned tool or placed text");
        var after=held.copy();if(!after.isEmpty())after.setDamageValue(owned.getDamageValue());
        boolean preserved=ItemStack.matches(owned,after);row.addProperty("ownedToolPreservedExceptWear",preserved);
        expect(preserved,id+" "+form+" "+mode+" owned tool components");
        return row;
    }
    private void door(ServerPlayer p,String wood,String half,String mode){
        String id=wood+"_door";clear(p);
        boolean placed=place(p,id,TEST.below(),Direction.UP,Vec3.atCenterOf(TEST.below()).add(0,.5,0),1);
        boolean complete=p.level().getBlockState(TEST).is(block(id))&&p.level().getBlockState(TEST.above()).is(block(id));
        expect(placed&&complete,id+" actual complete two-half item placement "+half+" "+mode);
        if(!complete){var row=new JsonObject();row.addProperty("block",id);row.addProperty("form",half);row.addProperty("tool",mode);row.addProperty("placementFailed",true);rows.add(row);return;}
        expect(p.level().getBlockState(TEST).getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.LOWER,id+" placed lower half");
        var row=harvest(p,id,half,mode,half.equals("upper")?TEST.above():TEST,id,1);
        boolean gone=p.level().getBlockState(TEST).isAir()&&p.level().getBlockState(TEST.above()).isAir();row.addProperty("bothHalvesRemoved",gone);expect(gone,id+" both halves removed after "+half+" destruction");rows.add(row);
    }
    private void slab(ServerPlayer p,String wood,String form,String mode){
        String id=wood+"_slab";clear(p);boolean placed;
        if(form.equals("top")){
            p.level().setBlock(TEST.south(),Blocks.SMOOTH_STONE.defaultBlockState(),3);
            placed=place(p,id,TEST.south(),Direction.NORTH,new Vec3(TEST.getX()+.5,TEST.getY()+.75,TEST.getZ()+1),1);
        } else {
            placed=place(p,id,TEST.below(),Direction.UP,Vec3.atCenterOf(TEST.below()).add(0,.5,0),form.equals("double")?2:1);
            if(form.equals("double")){
                var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST).add(0,0,0),Direction.UP,TEST,false)));
                placed&=result.consumesAction();
            }
        }
        var state=p.level().getBlockState(TEST);boolean right=state.is(block(id))&&state.getValue(BlockStateProperties.SLAB_TYPE)==SlabType.valueOf(form.toUpperCase());
        expect(placed&&right,id+" actual item placement "+form+" "+mode);
        if(!right){var row=new JsonObject();row.addProperty("block",id);row.addProperty("form",form);row.addProperty("tool",mode);row.addProperty("placementFailed",true);rows.add(row);return;}
        rows.add(harvest(p,id,form,mode,TEST,id,form.equals("double")?2:1));
    }
    private static List<String> lines(SignBlockEntity sign,SignTextSlot slot){return sign.getText(slot).getMessages(false).stream().map(value->value.getString()).toList();}
    private void sign(ServerPlayer p,String wood,boolean wall,String mode)throws Exception{
        String itemId=wood+"_sign",id=wood+(wall?"_wall_sign":"_sign");clear(p);
        boolean placed;
        if(wall){p.level().setBlock(TEST.south(),Blocks.SMOOTH_STONE.defaultBlockState(),3);placed=place(p,itemId,TEST.south(),Direction.NORTH,new Vec3(TEST.getX()+.5,TEST.getY()+.5,TEST.getZ()+1),1);}
        else placed=place(p,itemId,TEST.below(),Direction.UP,Vec3.atCenterOf(TEST.below()).add(0,.5,0),1);
        var state=p.level().getBlockState(TEST);boolean right=state.is(block(id));
        var row=new JsonObject();row.addProperty("block",id);row.addProperty("form",wall?"wall":"standing");row.addProperty("tool",mode);row.addProperty("actualItemPlacement",placed&&right);
        boolean supported=signType().isValid(state);row.addProperty("vanillaSignTypeSupportsBlock",supported);expect(placed&&right,id+" actual sign-item placement");expect(supported,id+" supported by native SIGN block-entity type");
        var entity=p.level().getBlockEntity(TEST);row.addProperty("actualSignBlockEntity",entity instanceof SignBlockEntity);
        if(entity instanceof SignBlockEntity sign){
            boolean nativeType=sign.getType()==signType();row.addProperty("nativeMinecraftSignBlockEntityType",nativeType);expect(nativeType,id+" uses native minecraft:sign block-entity type");
            boolean automaticEditor=p.getUUID().equals(sign.getPlayerWhoMayEdit());row.addProperty("automaticNativeEditorAuthorizedByItemPlacement",automaticEditor);expect(automaticEditor,id+" native item placement automatically authorizes its sign editor");
            var front=List.of("QA "+wood,"front "+mode,"third line","fourth line");var back=List.of("QA back",wood,mode,"persist both sides");
            for(var slot:SignTextSlot.values()){
                sign.setAllowedPlayerEditor(p.getUUID());sign.updateSignText(p,slot,(slot==SignTextSlot.FRONT?front:back).stream().map(FilteredText::passThrough).toList());
            }
            boolean edited=lines(sign,SignTextSlot.FRONT).equals(front)&&lines(sign,SignTextSlot.BACK).equals(back);row.addProperty("nativeAuthorizedTextEditBothSides",edited);expect(edited,id+" actual authorized native sign-text edit on both sides");
            var saved=sign.saveWithFullMetadata(p.level().registryAccess());var folder=NetworkProbe.directory().resolve("item-regression-nbt");Files.createDirectories(folder);var path=folder.resolve(id+"-"+mode+".nbt");
            NbtIo.writeCompressed(saved,path);var read=NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());var loaded=BlockEntity.loadStatic(TEST,state,read,p.level().registryAccess());
            boolean roundtrip=loaded instanceof SignBlockEntity restored&&lines(restored,SignTextSlot.FRONT).equals(front)&&lines(restored,SignTextSlot.BACK).equals(back);
            row.addProperty("nativeNbtDiskRoundtripBothSides",roundtrip);row.addProperty("nativeNbtFile",path.getFileName().toString());expect(roundtrip,id+" native compressed NBT disk load keeps both text sides");
        }else expect(false,id+" actual placed sign has a block entity");
        if(right){var harvest=harvest(p,id,wall?"wall":"standing",mode,TEST,itemId,1);row.add("harvest",harvest);}
        rows.add(row);
    }
    private void attempt(ServerPlayer p,String label,Checked action){
        try{action.run();}
        catch(Throwable error){var row=new JsonObject();row.addProperty("case",label);row.addProperty("error",error.toString());rows.add(row);issues.add(label+": "+error);}
        finally{
            // Old unsupported sign constructors can throw after the block state
            // was written. Remove that state before the next native broadcast.
            try{for(var pos:BlockPos.betweenClosed(TEST.offset(-1,0,-1),TEST.offset(1,3,1))){p.level().removeBlockEntity(pos);p.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);}}
            catch(Throwable error){issues.add(label+" cleanup: "+error);}
        }
    }
    private interface Checked{void run()throws Exception;}
    static JsonObject run(ServerPlayer p)throws Exception{
        var qa=new ItemServerRegression();
        p.teleport(new TeleportTransition(p.level(),new Vec3(12.5,250,2.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
        for(String wood:List.of("laver","dread"))for(String mode:TOOLS){
            for(String half:List.of("lower","upper"))qa.attempt(p,wood+" door "+half+" "+mode,()->qa.door(p,wood,half,mode));
            for(String form:List.of("bottom","top","double"))qa.attempt(p,wood+" slab "+form+" "+mode,()->qa.slab(p,wood,form,mode));
            for(boolean wall:new boolean[]{false,true})qa.attempt(p,wood+" sign "+wall+" "+mode,()->qa.sign(p,wood,wall,mode));
        }
        var out=new JsonObject();out.addProperty("mode",NetworkProbe.itemRegression());out.addProperty("actualServerPlayerPlacementAndDestruction",true);out.addProperty("caseCount",qa.rows.size());out.add("cases",qa.rows);out.add("issues",qa.issues);out.addProperty("expectedBehaviorMatches",qa.issues.isEmpty());
        var absent=new JsonArray();for(String wood:List.of("laver","dread"))for(String type:List.of("hanging_sign","wall_hanging_sign")){
            String id="guogaology:"+wood+"_"+type;if(!BuiltInRegistries.BLOCK.containsKey(Identifier.parse(id)))absent.add(id);
        }
        out.add("unregisteredHangingVariantsNotTested",absent);out.addProperty("signPersistenceBoundary","Native authorized SignBlockEntity text update plus compressed NBT write/read and BlockEntity.loadStatic; no claim of full-world reopen.");
        NetworkProbe.write("item-regression-server-cases.json",out.deepCopy());
        scene(p,out);return out;
    }
    private static void scene(ServerPlayer p,JsonObject out){
        clear(p);var w=p.level();
        for(var pos:BlockPos.betweenClosed(8,249,-3,22,255,12))w.setBlock(pos,pos.getY()==249?Blocks.SMOOTH_STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        var grahamPos=new BlockPos(11,250,5);w.setBlock(grahamPos,block("graham").defaultBlockState(),3);
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);boolean broken=p.gameMode.destroyBlock(grahamPos);
        var nativeDrops=w.getEntitiesOfClass(ItemEntity.class,new AABB(grahamPos).inflate(3));
        boolean exact=broken&&nativeDrops.size()==1&&nativeDrops.getFirst().getItem().is(NetworkProbe.item("guogaology:graham"))&&nativeDrops.getFirst().getItem().getCount()==1;
        var nativeBreak=new JsonObject();nativeBreak.addProperty("actualSurvivalPlayerDestroy",broken);nativeBreak.addProperty("actualItemEntityCount",nativeDrops.size());nativeBreak.addProperty("exactlyOneGrahamDrop",exact);
        ItemEntity grahamDrop=exact?nativeDrops.getFirst():null;
        if(grahamDrop!=null){nativeBreak.addProperty("entityId",grahamDrop.getId());nativeBreak.addProperty("componentsPatch",grahamDrop.getItem().getComponentsPatch().toString());}
        else {out.getAsJsonArray("issues").add("Graham native survival destruction did not produce exactly one original item entity");for(var e:nativeDrops)e.discard();out.addProperty("expectedBehaviorMatches",false);}
        out.add("nativeGrahamBreak",nativeBreak);
        var drops=new JsonArray();int index=0;
        for(String id:List.of("graham","graham_flower","graham_vine","dread_sapling","laver_sapling","loquat_sapling","hell_loquat_sapling","gummy_jar")){
            double x=11+(index%4)*2,z=5+(index/4)*2;
            boolean actualDrop=index==0&&grahamDrop!=null;var entity=actualDrop?grahamDrop:new ItemEntity(w,x,250.25,z,item(id));
            entity.setPos(x,250.25,z);entity.setNeverPickUp();entity.setUnlimitedLifetime();entity.setNoGravity(true);entity.setDeltaMovement(Vec3.ZERO);if(!actualDrop)w.addFreshEntity(entity);
            var row=new JsonObject();row.addProperty("item","guogaology:"+id);row.addProperty("entityId",entity.getId());row.addProperty("nativeSurvivalDrop",actualDrop);row.addProperty("x",x);row.addProperty("y",250.25);row.addProperty("z",z);drops.add(row);index++;
        }
        // Keep one final native sign alive so the remote client can observe its
        // real automatically opened editor after the batched destructive cases.
        var preview=new BlockPos(14,250,8);p.setItemSlot(EquipmentSlot.MAINHAND,item("dread_sign"));
        boolean previewSupported=signType().isValid(block("dread_sign").defaultBlockState());out.addProperty("previewNativeSignSupported",previewSupported);
        if(previewSupported){
            try{
                p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(preview.below()).add(0,.5,0),Direction.UP,preview.below(),false)));
                out.addProperty("previewNativeSignBlockEntity",w.getBlockEntity(preview) instanceof SignBlockEntity);
            }catch(Throwable error){out.addProperty("previewNativeSignPlacementError",error.toString());out.getAsJsonArray("issues").add("Preview native sign placement: "+error);out.addProperty("expectedBehaviorMatches",false);w.removeBlockEntity(preview);w.setBlock(preview,Blocks.AIR.defaultBlockState(),2);}
        }else out.addProperty("previewNativeSignBlockEntity",false);
        int slot=0;for(String id:List.of("graham","graham_flower","graham_vine","dread_sapling","laver_sapling","loquat_sapling","hell_loquat_sapling","gummy_jar"))p.getInventory().setItem(slot++,item(id));
        p.setGameMode(GameType.SURVIVAL);p.setItemSlot(EquipmentSlot.MAINHAND,item("graham"));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        p.teleport(new TeleportTransition(w,new Vec3(14,250,.5),Vec3.ZERO,0,18,TeleportTransition.DO_NOTHING));p.inventoryMenu.broadcastFullState();out.add("actualGroundItemEntities",drops);
    }
}
