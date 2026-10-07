package dev.guogaology.mining;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Scoped to the normal nearest-target goal; special anger/brain rules remain vanilla. */
public final class OrdinaryTargeting {
    private static final ThreadLocal<Mob> SCAN=new ThreadLocal<>();
    private OrdinaryTargeting(){}
    public static Mob enter(Mob mob){var old=SCAN.get();SCAN.set(mob);return old;}
    public static void leave(Mob old){if(old==null)SCAN.remove();else SCAN.set(old);}
    private static boolean ordinary(Mob mob){return mob instanceof Enemy&&!(mob instanceof NeutralMob);}
    public static boolean blocked(LivingEntity source,LivingEntity candidate,double range){
        var scanning=SCAN.get();if(scanning==null||scanning!=source||!ordinary(scanning)||!(candidate instanceof Player))return false;
        int level=ManuscriptEffects.level(ManuscriptEffects.held(candidate),3);
        if(level>=2)return true;
        return level==1&&range>0&&source.distanceToSqr(candidate)>range*range*.09;
    }
    public static boolean revengeBlocked(Mob mob){
        var attacker=mob.getLastHurtByMob();return ordinary(mob)&&attacker instanceof Player&&ManuscriptEffects.level(ManuscriptEffects.held(attacker),3)>=3;
    }
}
