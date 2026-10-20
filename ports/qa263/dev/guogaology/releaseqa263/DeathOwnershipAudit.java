package dev.guogaology.releaseqa263;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.guogaology.mining.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import java.util.*;

/** Actual lethal damage while core ownership is split between inventory, book and cursor. */
final class DeathOwnershipAudit {
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static void tally(Map<String,Integer> counts,ItemStack stack){if(stack.isEmpty())return;counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);for(var core:GearData.cores(stack))tally(counts,core);}
    private static Map<String,Integer> inventory(Player player){var counts=new TreeMap<String,Integer>();for(int i=0;i<player.getInventory().getContainerSize();i++)tally(counts,player.getInventory().getItem(i));tally(counts,player.containerMenu.getCarried());return counts;}
    static JsonArray run(ServerPlayer real){
        var rows=new JsonArray();var level=real.level();var rules=level.getGameRules();boolean oldKeep=rules.get(GameRules.KEEP_INVENTORY);var area=real.getBoundingBox().inflate(5);
        try{for(boolean keep:new boolean[]{false,true})for(boolean off:new boolean[]{false,true}){
            rules.set(GameRules.KEEP_INVENTORY,keep,level.getServer());var profile=new GameProfile(UUID.randomUUID(),"GeneralDeathAudit");var player=new ServerPlayer(level.getServer(),level,profile,ClientInformation.createDefault());
            player.connection=new ServerGamePacketListenerImpl(level.getServer(),new Connection(PacketFlow.SERVERBOUND),player,CommonListenerCookie.createInitial(profile,false));player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());player.setGameMode(GameType.SURVIVAL);player.setPos(real.position());player.getInventory().setSelectedSlot(2);
            var book=item("true_omega_manuscript");GearData.setCores(book,List.of(item("sequence_core"),item("sequence_core")));int slot=off?40:2;player.getInventory().setItem(slot,book);player.getInventory().setItem(9,item("laver_core").copyWithCount(4));var expected=inventory(player);
            var menu=new ManuscriptMenu(168,player.getInventory(),level,slot,book);player.containerMenu=menu;menu.quickMoveStack(player,11);menu.clicked(1,0,ContainerInput.PICKUP,player);
            var row=new JsonObject();row.addProperty("keepInventory",keep);row.addProperty("offhand",off);row.addProperty("cursorHasOneRemovedCore",menu.getCarried().getCount()==1);row.addProperty("newCoreInactive",ManuscriptEffects.level(ManuscriptEffects.held(player),4)==0);
            var before=new HashSet<UUID>();for(var drop:level.getEntitiesOfClass(ItemEntity.class,area))before.add(drop.getUUID());
            player.hurtServer(level,player.damageSources().generic(),10000);row.addProperty("actualDeath",!player.isAlive());player.closeContainer();var actual=inventory(player);var spawned=new ArrayList<ItemEntity>();
            for(var drop:level.getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(drop.getUUID())){tally(actual,drop.getItem());spawned.add(drop);}
            row.addProperty("exactMultisetConserved",actual.equals(expected));row.add("expected",new Gson().toJsonTree(expected));row.add("actual",new Gson().toJsonTree(actual));
            var prior=new TreeMap<>(actual);menu.removed(player);actual=inventory(player);for(var drop:spawned)tally(actual,drop.getItem());row.addProperty("duplicateCloseConserved",actual.equals(prior));for(var drop:spawned)drop.discard();rows.add(row);
        }}finally{rules.set(GameRules.KEEP_INVENTORY,oldKeep,level.getServer());}return rows;
    }
}
