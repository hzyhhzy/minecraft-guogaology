package dev.guogaology.mining;
import net.minecraft.entity.LivingEntity;
/** One pre-hit protection snapshot for the damage and all native armor wear points. */
public final class ArmorProtectionContext {
    private record Frame(LivingEntity owner,double factor){}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private ArmorProtectionContext(){}
    public static Object enter(LivingEntity owner,double factor){var prior=CURRENT.get();CURRENT.set(new Frame(owner,factor));return prior;}
    public static void restore(Object previous){if(previous==null)CURRENT.remove();else CURRENT.set((Frame)previous);}
    /** The active pre-hit snapshot also identifies the owner during native wear. */
    public static LivingEntity owner(){var frame=CURRENT.get();return frame==null?null:frame.owner();}
    public static double factor(LivingEntity owner){var frame=CURRENT.get();return frame!=null&&frame.owner()==owner?frame.factor():GearData.protectionFactor(owner);}
}
