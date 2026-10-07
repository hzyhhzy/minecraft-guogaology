package dev.guogaology.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LivingEntity.class)
public interface ExtraDamageAccessor {
    @Accessor("lastHurt") float guogaology$lastDamage();
    @Accessor("lastHurt") void guogaology$lastDamage(float value);
}
