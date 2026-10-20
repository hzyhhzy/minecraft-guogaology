package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.mining.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import java.lang.reflect.Field;
import java.util.*;

/** Native damage plus the exact production melee burst, with a QA-only cooldown control. */
final class CombatCooldownAudit {
    private static final List<Cow> TARGETS=new ArrayList<>();
    static void prepare(ServerLevel world){
        world.setChunkForced(5,5,true);world.getChunk(5,5);
        for(double x:new double[]{80.5,82.5,88.5,90.5}){
            var cow=EntityTypes.COW.create(world,EntitySpawnReason.COMMAND);if(cow==null)throw new AssertionError("combat target creation");cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(x,290,80.5);if(!world.addFreshEntity(cow))throw new AssertionError("combat target insertion");TARGETS.add(cow);
        }
    }
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static Field cooldown(LivingEntity entity)throws Exception{
        for(String name:List.of("damageCooldownTime","invulnerableTime"))for(Class<?> type=entity.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(NoSuchFieldException ignored){}
        throw new AssertionError("Native hurt cooldown field unavailable");
    }
    static JsonArray run(ServerPlayer player)throws Exception{
        if(TARGETS.size()!=4)throw new AssertionError("four prepared combat targets");var result=new JsonArray();var held=player.getMainHandItem();var off=player.getOffhandItem();var position=player.position();var world=player.level();
        try{for(int i=0;i<2;i++){
            var target=TARGETS.get(i*2);var neighbor=TARGETS.get(i*2+1);if(!world.getEntitiesOfClass(Cow.class,target.getBoundingBox().inflate(3)).contains(target)||!world.getEntitiesOfClass(Cow.class,target.getBoundingBox().inflate(3)).contains(neighbor))throw new AssertionError("combat targets must already be query-visible");
            player.setPos(target.position().add(0,0,-3));var sword=item("true_omega_sword");GearData.setCores(sword,List.of(item("astra_critical_core_lv3")));GearData.refresh(sword);player.setItemSlot(EquipmentSlot.MAINHAND,sword);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
            float direct=(float)GearData.attackWithBook(sword,player);double extra=direct*EquipmentRules.criticalCoefficient(GearData.profile(sword),List.of(),false);var field=cooldown(target);boolean accepted=target.hurtServer(world,player.damageSources().playerAttack(player),direct);float afterDirect=target.getHealth();int timer=field.getInt(target);
            if(i==1)field.setInt(target,0); // Diagnostic control only: production is unchanged.
            MiningEffects.burst(sword,target,player);
            var row=new JsonObject();row.addProperty("controlClearsActualDamageCooldown",i==1);row.addProperty("actualNativeDirectAccepted",accepted);row.addProperty("nativeCooldownField",field.getName());row.addProperty("cooldownAfterDirect",timer);row.addProperty("cooldownAfterBurst",field.getInt(target));row.addProperty("directExpected",direct);row.addProperty("directObserved",100-afterDirect);row.addProperty("additionalExpected",extra);row.addProperty("additionalOnDirectTarget",afterDirect-target.getHealth());row.addProperty("additionalOnNeighbor",100-neighbor.getHealth());row.addProperty("targetsQueryVisible",true);result.add(row);
        }}finally{for(var target:TARGETS)target.discard();TARGETS.clear();player.setPos(position);player.setItemSlot(EquipmentSlot.MAINHAND,held);player.setItemSlot(EquipmentSlot.OFFHAND,off);}return result;
    }
}
