package dev.googology.mergeqa;

import dev.googology.mining.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.phys.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.scores.PlayerTeam;
import java.util.*;
import java.lang.reflect.*;

/** Development-only real release/impact checks. Never packaged into the main Mod. */
public final class Bow047Checks {
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void near(double actual,double expected,double tolerance,String label){check(Math.abs(actual-expected)<tolerance,label+": "+actual+" != "+expected);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("googology:"+id)));}
    private static ItemStack bow(String... cores){var s=item("true_omega_bow");GearData.setCores(s,Arrays.stream(cores).map(Bow047Checks::item).toList());GearData.refresh(s);return s;}
    private static List<AbstractArrow> fire(ServerPlayer p,ItemStack bow,ItemStack ammo,int ticks,List<Entity> cleanup){
        return fire(p,bow,ammo,ItemStack.EMPTY,ticks,cleanup);
    }
    private static List<AbstractArrow> fire(ServerPlayer p,ItemStack bow,ItemStack ammo,ItemStack book,int ticks,List<Entity> cleanup){
        var world=p.level();p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.MAINHAND,bow);p.setItemSlot(EquipmentSlot.OFFHAND,book);p.getInventory().setItem(19,ammo);
        var before=new HashSet<UUID>();for(var e:world.getEntitiesOfClass(AbstractArrow.class,p.getBoundingBox().inflate(16)))before.add(e.getUUID());
        p.startUsingItem(InteractionHand.MAIN_HAND);var type=(OrdinalBowItem)bow.getItem();boolean released=type.releaseUsing(bow,world,p,type.getUseDuration(bow,p)-ticks);p.stopUsingItem();
        var arrows=world.getEntitiesOfClass(AbstractArrow.class,p.getBoundingBox().inflate(16),e->!before.contains(e.getUUID()));cleanup.addAll(arrows);
        check(released&&!arrows.isEmpty(),"real BowItem release spawned vanilla arrows");return arrows;
    }
    private static void clear(List<Entity> entities){for(var e:entities)e.discard();entities.clear();}
    public static void run(ServerPlayer p){
        checks=0;var world=p.level().getServer().overworld();var oldWorld=p.level();var oldPosition=p.position();var oldVelocity=p.getDeltaMovement();var oldMode=p.gameMode();float oldYaw=p.getYRot(),oldPitch=p.getXRot();boolean oldGround=p.onGround();
        var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i));
        var entities=new ArrayList<Entity>();
        try{
            p.teleport(new TeleportTransition(world,new Vec3(8.5,290,24.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            p.setOnGround(true);p.setGameMode(GameType.SURVIVAL);p.setDeltaMovement(Vec3.ZERO);
            int[] durability={768,1152,1536,2304},slots={2,4,6,8},caps={1,2,2,3};double[] speed={3.3,3.6,3.9,4.5};
            for(int i=0;i<4;i++){
                var s=new ItemStack(MiningContent.BOWS[i]);GearData.refresh(s);check(s.getItem() instanceof OrdinalBowItem,"four registered vanilla-bow subclasses");
                check(s.getMaxDamage()==durability[i]&&GearData.capacity(s)==slots[i]&&EquipmentRules.grade(i+1)==caps[i],"bow durability / sockets / regional cap tier"+(i+1));
                check(!s.isEnchantable()&&GearData.installationError(s,item("ordinal_crystal"),3)!=null,"bow enchant/Ordinal prohibition");
                var ammo=new ItemStack(Items.ARROW,2);var arrows=fire(p,s,ammo,20,entities);check(arrows.size()==1&&ammo.getCount()==1&&s.getDamageValue()==1,"plain bow consumes one arrow and one durability");
                near(arrows.getFirst().getDeltaMovement().length(),speed[i],.1,"vanilla full-draw launch speed multiplier (native inaccuracy retained)");check(arrows.getFirst().isCritArrow(),"native full-draw critical flag retained");clear(entities);
            }
            var ammo=new ItemStack(Items.ARROW,2);var half=fire(p,new ItemStack(MiningContent.BOWS[3]),ammo,10,entities);near(half.getFirst().getDeltaMovement().length(),4.5*BowItem.getPowerForTime(10),.04,"native half-draw speed curve");check(!half.getFirst().isCritArrow(),"half draw has no native random critical");clear(entities);
            for(int grade=1;grade<=3;grade++){
                var s=bow("hydra_bud"+(grade==1?"":"_lv"+grade));ammo=new ItemStack(Items.ARROW);var arrows=fire(p,s,ammo,20,entities);
                check(arrows.size()==(grade==1?1:3)&&ammo.getCount()==1,"branch grade enables Infinity / three-arrow volley");
                for(var a:arrows)check(a.pickup==AbstractArrow.Pickup.DISALLOWED&&a.getPierceLevel()==(grade==3?1:0),"free arrows not recoverable; Lv3 hits two creatures");clear(entities);
            }
            for(var special:List.of(Items.TIPPED_ARROW,Items.SPECTRAL_ARROW)){
                ammo=new ItemStack(special,2);var arrows=fire(p,bow("hydra_bud_lv3"),ammo,20,entities);check(arrows.size()==3&&ammo.getCount()==1,"special ammunition consumes one per volley");
                check(arrows.stream().filter(a->a.getPickupItemStackOrigin().is(special)).count()==1,"only central arrow retains special ammunition");
                check(arrows.stream().filter(a->a.pickup==AbstractArrow.Pickup.DISALLOWED).count()==2,"only paid central special arrow can be recovered");clear(entities);
            }
            p.getInventory().clearContent();var empty=bow("hydra_bud_lv3");p.setItemSlot(EquipmentSlot.MAINHAND,empty);check(!BowEffects.release(empty,world,p,1,4),"Infinity requires an arrow in inventory");
            p.setGameMode(GameType.CREATIVE);var creative=fire(p,new ItemStack(MiningContent.BOWS[0]),ItemStack.EMPTY,20,entities);check(creative.size()==1&&creative.getFirst().pickup==AbstractArrow.Pickup.DISALLOWED,"creative bow uses free fallback arrow from empty inventory");clear(entities);p.setGameMode(GameType.SURVIVAL);
            impacts(p,world,entities);
            System.out.println("BOW047_CHECKS_OK checks="+checks+" real release, charge, ammunition, snapshots, volley budgets and arrow debuffs");
        }finally{
            clear(entities);
            p.stopUsingItem();p.getInventory().clearContent();for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));
            p.setGameMode(oldMode);p.teleport(new TeleportTransition(oldWorld,oldPosition,oldVelocity,oldYaw,oldPitch,TeleportTransition.DO_NOTHING));p.setOnGround(oldGround);ManuscriptEffects.tick(p);
            p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        }
    }
    private static Cow cow(ServerLevel world,double x,double z,List<Entity> entities){var cow=EntityTypes.COW.create(world,EntitySpawnReason.COMMAND);if(cow==null)throw new AssertionError("bow QA cow");cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.setPos(x,290,z);check(world.addFreshEntity(cow),"bow QA temporary target");entities.add(cow);return cow;}
    private static void invokeHit(AbstractArrow arrow,Entity target){try{var m=AbstractArrow.class.getDeclaredMethod("onHitEntity",EntityHitResult.class);m.setAccessible(true);m.invoke(arrow,new EntityHitResult(target));}catch(InvocationTargetException e){throw new AssertionError("bow QA actual arrow hit",e.getCause());}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static AbstractArrow reload(AbstractArrow arrow,ServerLevel world,List<Entity> entities){
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,world.registryAccess());arrow.saveWithoutId(out);var nbt=out.buildResult();
        arrow.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        var restored=arrow.getType().create(world,EntitySpawnReason.COMMAND);check(restored instanceof AbstractArrow,"actual vanilla arrow reconstructed for reload");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,world.registryAccess(),nbt));entities.add(restored);
        check(restored.getUUID().equals(arrow.getUUID()),"entity reload keeps arrow UUID and volley membership");
        return (AbstractArrow)restored;
    }
    private static void impacts(ServerPlayer p,ServerLevel world,List<Entity> entities){
        var s=bow("hydra_bud_lv3","astra_critical_core_lv3","guogao_heart_lv3","power_tower_core_lv3");
        var book=item("true_omega_manuscript");GearData.setCores(book,List.of(item("power_tower_core_lv3"),item("ordinal_crystal_lv4")));GearData.refresh(book);
        var arrows=fire(p,s,new ItemStack(Items.ARROW),book,20,entities);
        var snapshot=((ArrowShotAccess)arrows.getFirst()).googology$shot();check(snapshot!=null&&snapshot.guogao==3&&snapshot.burst>0,"launch carries complete bow core snapshot");
        Cow first=cow(world,8.5,27.5,entities),second=cow(world,8.5,31.5,entities),neighbor=cow(world,10.5,27.5,entities),ally=cow(world,8.5,26.5,entities),third=cow(world,8.5,35.5,entities);
        var scoreboard=world.getScoreboard();PlayerTeam oldTeam=p.getTeam(),team=scoreboard.addPlayerTeam("bow47_"+UUID.randomUUID().toString().substring(0,8));
        scoreboard.addPlayerToTeam(p.getScoreboardName(),team);scoreboard.addPlayerToTeam(ally.getScoreboardName(),team);float playerHealth=p.getHealth();
        try{
        // Swapping weapons after firing cannot grant/remove in-flight effects.
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(var a:arrows){a.setCritArrow(false);a.setDeltaMovement(3,0,0);}
        double direct=snapshot.damage(6),burst=direct*snapshot.burst;
        invokeHit(arrows.getFirst(),first);near(100-first.getHealth(),direct+burst,.003,"first direct target gets one direct and one burst");near(100-neighbor.getHealth(),burst,.003,"neighbor receives one explosion");
        near(ally.getHealth(),100,.001,"volley explosion excludes ally");near(p.getHealth(),playerHealth,.001,"volley explosion excludes shooter");
        check(first.hasEffect(MobEffects.WITHER)&&first.hasEffect(MobEffects.SLOWNESS),"arrow applies Guogao directly");check(!neighbor.hasEffect(MobEffects.WITHER),"explosion never spreads Guogao");check(((ArrowShotAccess)arrows.getFirst()).googology$shot().burstUsed,"per-arrow first burst consumed");
        check(first.getEffect(MobEffects.WITHER).getDuration()==Math.round(20*snapshot.controlSeconds),"Guogao duration retains launch manuscript baseline and Ordinal");
        var reloaded=reload(arrows.getFirst(),world,entities);var reloadShot=((ArrowShotAccess)reloaded).googology$shot();
        check(reloadShot!=null&&reloadShot.livingHits==1&&reloadShot.burstUsed&&reloaded.getPierceLevel()==1,"real entity unload/reload retains one hit, first burst and Pierce1");
        check(BowVolleyLedger.get(world.getServer()).directSeen(snapshot.volley,first.getUUID())&&BowVolleyLedger.get(world.getServer()).burstSeen(snapshot.volley,neighbor.getUUID()),"chunk unload preserves shared direct and splash budgets");
        reloaded.setDeltaMovement(3,0,0);invokeHit(reloaded,second);near(100-second.getHealth(),direct,.003,"pierced second target after reload has no repeated burst");check(second.hasEffect(MobEffects.WITHER),"pierced second target gets direct Guogao");
        reloaded=reload(reloaded,world,entities);reloadShot=((ArrowShotAccess)reloaded).googology$shot();check(reloadShot!=null&&reloadShot.livingHits==2,"second reload retains exhausted two-target budget");
        reloaded.setDeltaMovement(3,0,0);invokeHit(reloaded,third);near(third.getHealth(),100,.001,"third creature stays unharmed after vanilla piercing counters reset by reload");check(!reloaded.isRemoved()&&!third.hasEffect(MobEffects.WITHER),"exhausted arrow cannot hit or debuff again and retains its volley membership");check(!BowEffects.canHit(reloaded,third,reloadShot),"normal collision scan excludes living targets after persisted budget is exhausted");
        float before=first.getHealth();invokeHit(arrows.get(1),first);near(first.getHealth(),before,.001,"overlapping side arrow cannot repeat direct or explosion");check(((ArrowShotAccess)arrows.get(1)).googology$shot().livingHits==0,"blocked duplicate direct hit does not spend a side arrow's valid-hit budget");
        var immune=cow(world,12.5,40.5,entities);immune.setInvulnerable(true);arrows.get(1).setDeltaMovement(3,0,0);invokeHit(arrows.get(1),immune);near(immune.getHealth(),100,.001,"immune creature takes no arrow damage");check(((ArrowShotAccess)arrows.get(1)).googology$shot().livingHits==0&&!BowVolleyLedger.get(world.getServer()).directSeen(snapshot.volley,immune.getUUID()),"immune collision does not count as a valid living hit");
        arrows.get(2).setDeltaMovement(3,0,0);invokeHit(arrows.get(2),neighbor);near(100-neighbor.getHealth(),direct+burst,.003,"target previously splashed may later receive exactly one direct");near(first.getHealth(),before,.001,"second impact site cannot repeat an overlapping explosion");
        var restored=BowShotData.load(reloadShot.save());check(restored!=null&&restored.burstUsed&&restored.livingHits==2&&restored.guogao==snapshot.guogao,"arrow snapshot round-trip preserves used burst, hit count and debuff source");
        }finally{scoreboard.removePlayerFromTeam(ally.getScoreboardName());scoreboard.removePlayerFromTeam(p.getScoreboardName());scoreboard.removePlayerTeam(team);if(oldTeam!=null)scoreboard.addPlayerToTeam(p.getScoreboardName(),oldTeam);}
    }
}
