package dev.guogaology.mergeqa;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import java.nio.file.*;
import java.util.*;
/** Small26.2 counterpart of the26.3 observation audit, enabled only by its runner. */
final class General068Focus {
    static boolean enabled(){return Boolean.getBoolean("guogaology.qa.general068");}
    private static Block block(String name){return BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+name));}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static void write(String name,JsonObject value)throws Exception{Files.writeString(Path.of(name),new GsonBuilder().setPrettyPrinting().create().toJson(value),StandardOpenOption.CREATE_NEW);}
    static void client(Minecraft client)throws Exception{
        var out=new JsonObject();var boats=new JsonArray();
        for(String name:List.of("laver_boat","dread_boat")){var row=new JsonObject();row.addProperty("id",name);row.addProperty("nativeHoverName",item(name).getHoverName().getString());String key="entity.guogaology."+name;row.addProperty("entityKey",key);row.addProperty("translationPresent",!I18n.get(key).equals(key));row.addProperty("nativeEntityName",BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("guogaology:"+name)).getDescription().getString());boats.add(row);}
        out.addProperty("completed",true);out.add("boats",boats);write("audit068-client-focus.json",out);
    }
    static void server(ServerPlayer p)throws Exception{
        CombatCooldownAudit.prepare(p.level());
        var out=new JsonObject();var facing=new JsonArray();var stripping=new JsonArray();var w=p.level();var pos=p.blockPosition().offset(12,0,12);var oldPos=p.position();var held=p.getMainHandItem();float yaw=p.getYRot(),pitch=p.getXRot();
        try{p.setPos(Vec3.atCenterOf(pos).add(0,1,3));w.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
            for(String id:List.of("server_rack","office_monitor","chair","nuke_chair","snake_head"))for(float turn:new float[]{0,90,180,270}){
                w.setBlock(pos,Blocks.AIR.defaultBlockState(),3);p.setYRot(turn);p.setXRot(0);p.setItemSlot(EquipmentSlot.MAINHAND,item(id));var used=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false)));
                var row=new JsonObject();row.addProperty("block",id);row.addProperty("yaw",turn);row.addProperty("action",used.toString());row.addProperty("placed",w.getBlockState(pos).is(block(id)));row.addProperty("state",w.getBlockState(pos).toString());facing.add(row);
            }
            for(String id:List.of("laver_vein","laver_wood","dread_log","dread_wood")){
                w.setBlock(pos,block(id).defaultBlockState(),3);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));var used=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,0,.5),Direction.SOUTH,pos,false)));
                var row=new JsonObject();row.addProperty("block",id);row.addProperty("action",used.toString());row.addProperty("result",BuiltInRegistries.BLOCK.getKey(w.getBlockState(pos).getBlock()).toString());stripping.add(row);
            }
        }finally{w.setBlock(pos,Blocks.AIR.defaultBlockState(),3);p.setPos(oldPos);p.setYRot(yaw);p.setXRot(pitch);p.setItemSlot(EquipmentSlot.MAINHAND,held);}
        out.addProperty("completed",true);out.add("placements",facing);out.add("axeStripping",stripping);write("audit068-server-focus.json",out);
    }
    static void combat(ServerPlayer p)throws Exception{var out=new JsonObject();out.add("nativeMeleeBurstCooldown",CombatCooldownAudit.run(p));out.addProperty("completed",true);write("audit068-combat-focus.json",out);}
}
