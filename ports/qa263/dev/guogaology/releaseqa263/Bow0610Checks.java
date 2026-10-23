package dev.guogaology.releaseqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.guogaology.mining.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.util.RandomSource;

import java.nio.file.Files;
import java.util.*;

/** Opt-in native pickup and actual baked draw-model regression, never production. */
public final class Bow0610Checks {
    private static final String[] NAMES={"omega","epsilon","gamma","true_omega"};
    private static int checks;
    private static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);checks++;}
    private static ItemStack core(int grade){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:hydra_bud"+(grade==1?"":"_lv"+grade))));}
    private static List<AbstractArrow> fire(ServerPlayer p,ItemStack bow,ItemStack ammo,List<Entity> cleanup){
        p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.MAINHAND,bow);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.getInventory().setItem(19,ammo);
        var before=new HashSet<UUID>();for(var e:p.level().getEntitiesOfClass(AbstractArrow.class,p.getBoundingBox().inflate(16)))before.add(e.getUUID());
        p.startUsingItem(InteractionHand.MAIN_HAND);var item=(BowItem)bow.getItem();boolean fired=item.releaseUsing(bow,p.level(),p,item.getUseDuration(bow,p)-20);p.stopUsingItem();
        var arrows=p.level().getEntitiesOfClass(AbstractArrow.class,p.getBoundingBox().inflate(16),e->!before.contains(e.getUUID()));cleanup.addAll(arrows);
        check(fired&&!arrows.isEmpty(),"native release spawned arrows");return arrows;
    }
    private static void touch(AbstractArrow arrow,ServerPlayer p){arrow.setNoPhysics(true);arrow.shakeTime=0;arrow.playerTouch(p);}
    private static int count(ServerPlayer p,Item item){int amount=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))amount+=s.getCount();}return amount;}
    private static void clear(List<Entity> entities){for(var e:entities)e.discard();entities.clear();}
    public static void run(ServerPlayer p)throws Exception{
        checks=0;var oldMode=p.gameMode();var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i));var cleanup=new ArrayList<Entity>();var rows=new JsonArray();
        try{
            // Native bow establishes the creative pickup contract independently.
            p.setGameMode(GameType.CREATIVE);var vanilla=fire(p,new ItemStack(Items.BOW),ItemStack.EMPTY,cleanup);check(vanilla.size()==1&&vanilla.getFirst().pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"vanilla creative pickup policy");touch(vanilla.getFirst(),p);check(vanilla.getFirst().isRemoved(),"native creative bow arrow picked up");clear(cleanup);
            for(int tier=0;tier<4;tier++){
                p.setGameMode(GameType.CREATIVE);var arrows=fire(p,new ItemStack(MiningContent.BOWS[tier]),ItemStack.EMPTY,cleanup);var a=arrows.getFirst();check(arrows.size()==1&&a.pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"creative ordinal arrow same policy as vanilla");int before=count(p,Items.ARROW);touch(a,p);check(a.isRemoved()&&count(p,Items.ARROW)==before,"creative ordinal arrow really collected without manufacturing ammo");clear(cleanup);
                p.setGameMode(GameType.SURVIVAL);var paid=new ItemStack(Items.ARROW,2);arrows=fire(p,new ItemStack(MiningContent.BOWS[tier]),paid,cleanup);a=arrows.getFirst();check(paid.getCount()==1&&a.pickup==AbstractArrow.Pickup.ALLOWED,"survival central arrow spends one ammo and stays recoverable");touch(a,p);check(a.isRemoved()&&count(p,Items.ARROW)==2,"paid survival arrow actually recovered once");clear(cleanup);
                for(int grade=1;grade<=3;grade++){
                    var bow=new ItemStack(MiningContent.BOWS[tier]);GearData.setCores(bow,List.of(core(grade)));GearData.refresh(bow);var ammo=new ItemStack(Items.ARROW);arrows=fire(p,bow,ammo,cleanup);check(arrows.size()==(grade==1?1:3)&&ammo.getCount()==1,"Infinity/multishot ammo conserved");
                    for(var arrow:arrows){check(arrow.pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"free arrow is creative-only, including side arrows");touch(arrow,p);check(!arrow.isRemoved()&&count(p,Items.ARROW)==1,"survival cannot recover free arrows");}
                    p.setGameMode(GameType.CREATIVE);for(var arrow:arrows){touch(arrow,p);check(arrow.isRemoved()&&count(p,Items.ARROW)==1,"creative can clear free arrows without survival ammo gain");}p.setGameMode(GameType.SURVIVAL);clear(cleanup);
                }
                for(var special:List.of(Items.TIPPED_ARROW,Items.SPECTRAL_ARROW)){
                    var bow=new ItemStack(MiningContent.BOWS[tier]);GearData.setCores(bow,List.of(core(3)));GearData.refresh(bow);var ammo=new ItemStack(special,2);arrows=fire(p,bow,ammo,cleanup);check(arrows.size()==3&&ammo.getCount()==1,"special-ammo volley still pays once");int paidCount=0;for(var arrow:arrows){if(arrow.pickup==AbstractArrow.Pickup.ALLOWED){paidCount++;touch(arrow,p);check(arrow.isRemoved(),"paid special arrow collected");}else{check(arrow.pickup==AbstractArrow.Pickup.CREATIVE_ONLY,"special-ammo side arrow creative-only");touch(arrow,p);check(!arrow.isRemoved(),"special-ammo side arrow not recoverable in survival");}}check(paidCount==1&&count(p,special)==2&&count(p,Items.ARROW)==0,"recover only the one consumed special arrow, no side-arrow duplication");clear(cleanup);
                }
                var row=new JsonObject();row.addProperty("tier",tier+1);row.addProperty("creativePickup",true);row.addProperty("paidSurvivalPickup",true);row.addProperty("infiniteSurvivalPickupBlocked",true);row.addProperty("specialAmmoConserved",true);rows.add(row);
            }
            var out=new JsonObject();out.addProperty("passed",true);out.addProperty("checks",checks);out.add("tiers",rows);write("bow0610-server.json",out);System.out.println("BOW0610_NATIVE_PICKUP_OK checks="+checks);
        }finally{clear(cleanup);p.stopUsingItem();p.getInventory().clearContent();for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.setGameMode(oldMode);ManuscriptEffects.tick(p);}
    }
    public static void client(Minecraft client)throws Exception{
        checks=0;var p=client.player;check(p!=null,"client player exists");var main=p.getMainHandItem();var off=p.getOffhandItem();var oldUse=p.getUseItem();int oldRemaining=p.getUseItemRemainingTicks();var oldHand=p.getUsedItemHand();var remaining=LivingEntity.class.getDeclaredField("useItemRemaining");remaining.setAccessible(true);var rows=new JsonArray();
        try{
            for(int tier=0;tier<4;tier++)for(var hand:InteractionHand.values()){
                p.stopUsingItem();var stack=new ItemStack(MiningContent.BOWS[tier]);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setItemInHand(hand,stack);
                for(int draw:new int[]{-1,0,12,13,17,18,20}){
                    p.stopUsingItem();if(draw>=0){p.startUsingItem(hand);remaining.setInt(p,stack.getUseDuration(p)-draw);}
                    String suffix=draw<0?"":draw<13?"_pulling_0":draw<18?"_pulling_1":"_pulling_2";String expected="guogaology:item/"+NAMES[tier]+"_bow"+suffix;
                    for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND)){
                        var state=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(state,stack,context,p);check(!state.isEmpty(),"draw-state model not empty");String actual=state.pickParticleMaterial(RandomSource.create(0)).sprite().contents().name().toString();check(actual.equals(expected),"actual native draw texture: tier="+(tier+1)+" hand="+hand+" ticks="+draw+" context="+context+" expected="+expected+" actual="+actual);
                        var row=new JsonObject();row.addProperty("tier",tier+1);row.addProperty("hand",hand.toString());row.addProperty("drawTicks",draw);row.addProperty("context",context.toString());row.addProperty("sprite",actual);rows.add(row);
                    }
                }
            }
            var out=new JsonObject();out.addProperty("passed",true);out.addProperty("checks",checks);out.add("models",rows);write("bow0610-client.json",out);System.out.println("BOW0610_NATIVE_DRAW_MODELS_OK cases="+rows.size());
        }finally{p.stopUsingItem();p.setItemSlot(EquipmentSlot.MAINHAND,main);p.setItemSlot(EquipmentSlot.OFFHAND,off);if(!oldUse.isEmpty()){p.startUsingItem(oldHand);remaining.setInt(p,oldRemaining);}}
    }
    private static void write(String name,JsonObject value)throws Exception{Files.writeString(FabricLoader.getInstance().getGameDir().resolve(name),value.toString());}
}
