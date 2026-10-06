package dev.googology.mining;

import dev.googology.mixin.ExtraDamageAccessor;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import java.util.*;
import java.util.function.BiFunction;

/** Server-authoritative snapshots, ammunition and damage on ordinary vanilla arrows. */
public final class BowEffects {
    private BowEffects(){}
    public static boolean release(ItemStack bow,Level level,LivingEntity owner,float draw,int tier){
        if(!(owner instanceof Player player)||draw<.1f)return false;
        ItemStack ammo=player.getProjectile(bow);if(ammo.isEmpty()){if(!player.hasInfiniteMaterials())return false;ammo=new ItemStack(Items.ARROW);}
        if(!(level instanceof ServerLevel world))return true;
        var own=GearData.profile(bow);var bookStack=ManuscriptEffects.held(owner);var book=GearData.profile(bookStack);
        int bookTier=GearData.bookTier(bookStack),branch=EquipmentRules.highest(own,2);
        boolean deep=ManuscriptEffects.deep(world),free=player.hasInfiniteMaterials()||branch>0&&ammo.is(Items.ARROW);
        var shot=new BowShotData(UUID.randomUUID().toString(),player.getUUID().toString(),player.getTeam()==null?"":player.getTeam().getName(),
            deep?0:EquipmentRules.attack(0,own,book,false,EquipmentRules.bookBase(bookTier),EquipmentRules.bookAttackHp(bookTier)),
            deep?EquipmentRules.attack(1,own,book,true,EquipmentRules.bookBase(bookTier),EquipmentRules.bookAttackHp(bookTier)):1,
            draw,EquipmentRules.criticalCoefficient(own,List.of(),deep),EquipmentRules.highest(own,7),
            EquipmentRules.controlDuration(own,book,EquipmentRules.bookDurationBase(bookTier)));
        int count=branch>=2?3:1;var arrows=new ArrayList<AbstractArrow>();
        for(int i=0;i<count;i++){
            ItemStack round=i==0?ammo.copyWithCount(1):new ItemStack(Items.ARROW);
            ArrowItem type=round.getItem() instanceof ArrowItem a?a:(ArrowItem)Items.ARROW;
            AbstractArrow arrow=type.createArrow(world,round,player,bow.copy());
            var access=(ArrowShotAccess)arrow;access.googology$shot(shot.copy());if(branch>=3)access.googology$setPierceLevel((byte)1);
            arrow.setCritArrow(draw>=1);if(i>0||free)arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
            float spread=i==0?0:i==1?-10:10;
            arrow.shootFromRotation(player,player.getXRot(),player.getYRot()+spread,0,(float)(3*draw*EquipmentRules.arrowSpeedMultiplier(tier)),1);
            arrows.add(arrow);BowVolleyLedger.get(world.getServer()).register(shot.volley,arrow.getUUID());
        }
        for(var arrow:arrows)if(!world.addFreshEntity(arrow))BowVolleyLedger.get(world.getServer()).destroy(shot.volley,arrow.getUUID());
        if(!free)ammo.shrink(1);
        InteractionHand hand=owner.getUsedItemHand();bow.hurtAndBreak(1,owner,hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1/(world.getRandom().nextFloat()*.4f+1.2f)+draw*.5f);
        player.awardStat(Stats.ITEM_USED.get(bow.getItem()));return true;
    }
    public static boolean canHit(AbstractArrow arrow,Entity target,BowShotData shot){if(target instanceof LivingEntity&&!shot.canHitLiving())return false;return !(arrow.level() instanceof ServerLevel world)||!BowVolleyLedger.get(world.getServer()).directSeen(shot.volley,target.getUUID());}
    public static boolean hit(AbstractArrow arrow,Entity target,DamageSource source,float vanilla,BowShotData shot,BiFunction<DamageSource,Float,Boolean> original){
        if(!(arrow.level() instanceof ServerLevel world))return original.apply(source,vanilla);
        var ledger=BowVolleyLedger.get(world.getServer());if(ledger.directSeen(shot.volley,target.getUUID()))return false;
        float damage=shot.damage(vanilla);
        boolean hit=target instanceof LivingEntity living&&ledger.burstSeen(shot.volley,target.getUUID())?extraDamage(living,()->original.apply(source,damage)):original.apply(source,damage);
        if(!hit)return false;ledger.direct(shot.volley,target.getUUID());
        if(target instanceof LivingEntity living){
            shot.recordLivingHit();
            control(world,living,shot,arrow.getOwner());
            if(!shot.burstUsed){shot.burstUsed=true;if(shot.burst>0)burst(world,arrow,living,damage,shot,ledger);}
        }
        return true;
    }
    private static void burst(ServerLevel world,AbstractArrow arrow,LivingEntity target,float direct,BowShotData shot,BowVolleyLedger ledger){
        world.sendParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);
        world.playSound(null,target.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.55f,1.25f);
        Entity owner=arrow.getOwner();var source=world.damageSources().explosion(arrow,owner);
        for(var other:world.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(3),e->!e.getUUID().toString().equals(shot.owner)&&!friendly(e,owner,shot.team))){
            if(other.distanceToSqr(target)>9||other!=target&&!target.hasLineOfSight(other)||!ledger.burst(shot.volley,other.getUUID()))continue;
            float amount=(float)(direct*shot.burst);
            if(ledger.directSeen(shot.volley,other.getUUID()))extraDamage(other,()->other.hurtServer(world,source,amount));else other.hurtServer(world,source,amount);
        }
    }
    private static boolean friendly(LivingEntity other,Entity owner,String team){return owner!=null&&other.isAlliedTo(owner)||owner==null&&!team.isEmpty()&&other.getTeam()!=null&&team.equals(other.getTeam().getName());}
    private static boolean extraDamage(LivingEntity other,java.util.function.BooleanSupplier operation){
        int timer=other.invulnerableTime;var access=(ExtraDamageAccessor)other;float previous=access.googology$lastDamage();
        other.invulnerableTime=0;try{return operation.getAsBoolean();}finally{other.invulnerableTime=timer;access.googology$lastDamage(previous);}
    }
    private static void control(ServerLevel world,LivingEntity target,BowShotData shot,Entity owner){
        if(shot.guogao==0||!target.isAlive()||target.getUUID().toString().equals(shot.owner)||friendly(target,owner,shot.team))return;int ticks=Math.max(1,(int)Math.round(20*shot.controlSeconds));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,ticks,shot.guogao-1));
        if(shot.guogao==1)target.addEffect(new MobEffectInstance(MobEffects.POISON,ticks,1));
        else{target.addEffect(new MobEffectInstance(MobEffects.WITHER,ticks,shot.guogao-1));target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,ticks,shot.guogao-2));if(shot.guogao>=3){target.addEffect(new MobEffectInstance(MobEffects.NAUSEA,ticks,0));if(world.getRandom().nextFloat()<.2f)target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,ticks,0));}}
    }
    public static void removed(Entity arrow,Entity.RemovalReason reason,BowShotData shot){if(reason.shouldDestroy()&&arrow.level() instanceof ServerLevel world)BowVolleyLedger.get(world.getServer()).destroy(shot.volley,arrow.getUUID());}
    public static void loaded(Entity arrow,BowShotData shot){if(arrow.level() instanceof ServerLevel world)BowVolleyLedger.get(world.getServer()).register(shot.volley,arrow.getUUID());}
}
