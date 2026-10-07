package dev.guogaology.mining;

import dev.guogaology.mixin.ExtraDamageAccessor;
import net.minecraft.item.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import java.util.*;
import java.util.function.BiFunction;

/** Server-authoritative snapshots, ammunition and damage on ordinary vanilla arrows. */
public final class BowEffects {
    private BowEffects(){}
    public static boolean release(ItemStack bow,World level,LivingEntity owner,float draw,int tier){
        if(!(owner instanceof PlayerEntity player)||draw<.1f)return false;
        ItemStack ammo=player.getProjectileType(bow);if(ammo.isEmpty()){if(!player.getAbilities().creativeMode)return false;ammo=new ItemStack(Items.ARROW);}
        if(!(level instanceof ServerWorld world))return true;
        var own=GearData.profile(bow);var bookStack=ManuscriptEffects.held(owner);var book=GearData.profile(bookStack);
        int bookTier=GearData.bookTier(bookStack),branch=EquipmentRules.highest(own,2);
        boolean deep=ManuscriptEffects.deep(world),free=player.getAbilities().creativeMode||branch>0&&ammo.isOf(Items.ARROW);
        var shot=new BowShotData(UUID.randomUUID().toString(),player.getUuid().toString(),player.getScoreboardTeam()==null?"":player.getScoreboardTeam().getName(),
            deep?0:EquipmentRules.attack(0,own,book,false,EquipmentRules.bookBase(bookTier),EquipmentRules.bookAttackHp(bookTier)),
            deep?EquipmentRules.attack(1,own,book,true,EquipmentRules.bookBase(bookTier),EquipmentRules.bookAttackHp(bookTier)):1,
            draw,EquipmentRules.criticalCoefficient(own,List.of(),deep),EquipmentRules.highest(own,7),
            EquipmentRules.controlDuration(own,book,EquipmentRules.bookDurationBase(bookTier)));
        int count=branch>=2?3:1;var arrows=new ArrayList<PersistentProjectileEntity>();
        for(int i=0;i<count;i++){
            ItemStack round=i==0?ammo.copyWithCount(1):new ItemStack(Items.ARROW);
            ArrowItem type=round.getItem() instanceof ArrowItem a?a:(ArrowItem)Items.ARROW;
            PersistentProjectileEntity arrow=type.createArrow(world,round,player,bow.copy());
            var access=(ArrowShotAccess)arrow;access.guogaology$shot(shot.copy());if(branch>=3)access.guogaology$setPierceLevel((byte)1);
            arrow.setCritical(draw>=1);if(i>0||free)arrow.pickupType=PersistentProjectileEntity.PickupPermission.DISALLOWED;
            float spread=i==0?0:i==1?-10:10;
            arrow.setVelocity(player,player.getPitch(),player.getYaw()+spread,0,(float)(3*draw*EquipmentRules.arrowSpeedMultiplier(tier)),1);
            arrows.add(arrow);BowVolleyLedger.get(world.getServer()).register(shot.volley,arrow.getUuid());
        }
        for(var arrow:arrows)if(!world.spawnEntity(arrow))BowVolleyLedger.get(world.getServer()).destroy(shot.volley,arrow.getUuid());
        if(!free)ammo.decrement(1);
        Hand hand=owner.getActiveHand();bow.damage(1,owner,hand==Hand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ENTITY_ARROW_SHOOT,SoundCategory.PLAYERS,1,1/(world.getRandom().nextFloat()*.4f+1.2f)+draw*.5f);
        player.incrementStat(Stats.USED.getOrCreateStat(bow.getItem()));return true;
    }
    public static boolean canHit(PersistentProjectileEntity arrow,Entity target,BowShotData shot){if(target instanceof LivingEntity&&!shot.canHitLiving())return false;return !(arrow.getWorld() instanceof ServerWorld world)||!BowVolleyLedger.get(world.getServer()).directSeen(shot.volley,target.getUuid());}
    public static boolean hit(PersistentProjectileEntity arrow,Entity target,DamageSource source,float vanilla,BowShotData shot,BiFunction<DamageSource,Float,Boolean> original){
        if(!(arrow.getWorld() instanceof ServerWorld world))return original.apply(source,vanilla);
        var ledger=BowVolleyLedger.get(world.getServer());if(ledger.directSeen(shot.volley,target.getUuid()))return false;
        float damage=shot.damage(vanilla);
        boolean hit=target instanceof LivingEntity living&&ledger.burstSeen(shot.volley,target.getUuid())?extraDamage(living,()->original.apply(source,damage)):original.apply(source,damage);
        if(!hit)return false;ledger.direct(shot.volley,target.getUuid());
        if(target instanceof LivingEntity living){
            shot.recordLivingHit();
            control(world,living,shot,arrow.getOwner());
            if(!shot.burstUsed){shot.burstUsed=true;if(shot.burst>0)burst(world,arrow,living,damage,shot,ledger);}
        }
        return true;
    }
    private static void burst(ServerWorld world,PersistentProjectileEntity arrow,LivingEntity target,float direct,BowShotData shot,BowVolleyLedger ledger){
        world.spawnParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);
        world.playSound(null,target.getBlockPos(),SoundEvents.ENTITY_GENERIC_EXPLODE.value(),SoundCategory.PLAYERS,.55f,1.25f);
        Entity owner=arrow.getOwner();var source=world.getDamageSources().explosion(arrow,owner);
        for(var other:world.getEntitiesByClass(LivingEntity.class,target.getBoundingBox().expand(3),e->!e.getUuid().toString().equals(shot.owner)&&!friendly(e,owner,shot.team))){
            if(other.squaredDistanceTo(target)>9||other!=target&&!target.canSee(other)||!ledger.burst(shot.volley,other.getUuid()))continue;
            float amount=(float)(direct*shot.burst);
            if(ledger.directSeen(shot.volley,other.getUuid()))extraDamage(other,()->other.damage(source,amount));else other.damage(source,amount);
        }
    }
    private static boolean friendly(LivingEntity other,Entity owner,String team){return owner!=null&&other.isTeammate(owner)||owner==null&&!team.isEmpty()&&other.getScoreboardTeam()!=null&&team.equals(other.getScoreboardTeam().getName());}
    private static boolean extraDamage(LivingEntity other,java.util.function.BooleanSupplier operation){
        int timer=other.timeUntilRegen;var access=(ExtraDamageAccessor)other;float previous=access.guogaology$lastDamage();
        other.timeUntilRegen=0;try{return operation.getAsBoolean();}finally{other.timeUntilRegen=timer;access.guogaology$lastDamage(previous);}
    }
    private static void control(ServerWorld world,LivingEntity target,BowShotData shot,Entity owner){
        if(shot.guogao==0||!target.isAlive()||target.getUuid().toString().equals(shot.owner)||friendly(target,owner,shot.team))return;int ticks=Math.max(1,(int)Math.round(20*shot.controlSeconds));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,ticks,shot.guogao-1));
        if(shot.guogao==1)target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,ticks,1));
        else{target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,ticks,shot.guogao-1));target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,ticks,shot.guogao-2));if(shot.guogao>=3){target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,ticks,0));if(world.getRandom().nextFloat()<.2f)target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,ticks,0));}}
    }
    public static void removed(Entity arrow,Entity.RemovalReason reason,BowShotData shot){if(reason.shouldDestroy()&&arrow.getWorld() instanceof ServerWorld world)BowVolleyLedger.get(world.getServer()).destroy(shot.volley,arrow.getUuid());}
    public static void loaded(Entity arrow,BowShotData shot){if(arrow.getWorld() instanceof ServerWorld world)BowVolleyLedger.get(world.getServer()).register(shot.volley,arrow.getUuid());}
}
