package dev.googology.mining;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.player.PlayerEntity;

/** Scoped to the normal nearest-target goal; special anger/brain rules remain vanilla. */
public final class OrdinaryTargeting {
    private static final ThreadLocal<MobEntity> SCAN=new ThreadLocal<>();
    private OrdinaryTargeting(){}
    public static MobEntity enter(MobEntity mob){var old=SCAN.get();SCAN.set(mob);return old;}
    public static void leave(MobEntity old){if(old==null)SCAN.remove();else SCAN.set(old);}
    private static boolean ordinary(MobEntity mob){return mob instanceof Monster&&!(mob instanceof Angerable);}
    public static boolean blocked(LivingEntity source,LivingEntity candidate,double range){
        var scanning=SCAN.get();if(scanning==null||scanning!=source||!ordinary(scanning)||!(candidate instanceof PlayerEntity))return false;
        int level=ManuscriptEffects.level(ManuscriptEffects.held(candidate),3);
        if(level>=2)return true;
        return level==1&&range>0&&source.squaredDistanceTo(candidate)>range*range*.09;
    }
    public static boolean revengeBlocked(MobEntity mob){
        var attacker=mob.getAttacker();return ordinary(mob)&&attacker instanceof PlayerEntity&&ManuscriptEffects.level(ManuscriptEffects.held(attacker),3)>=3;
    }
}
