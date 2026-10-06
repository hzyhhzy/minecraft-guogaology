package dev.googology.mining;
import net.minecraft.world.entity.LivingEntity;
/** One pre-hit protection snapshot for the damage and all native armor wear points. */
public final class ArmorProtectionContext {
    private record Frame(LivingEntity owner,double factor){}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private ArmorProtectionContext(){}
    public static Object enter(LivingEntity owner,double factor){var prior=CURRENT.get();CURRENT.set(new Frame(owner,factor));return prior;}
    public static void restore(Object previous){if(previous==null)CURRENT.remove();else CURRENT.set((Frame)previous);}
    public static double factor(LivingEntity owner){var frame=CURRENT.get();return frame!=null&&frame.owner()==owner?frame.factor():GearData.protectionFactor(owner);}
}
