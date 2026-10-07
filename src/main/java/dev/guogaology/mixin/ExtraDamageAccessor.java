package dev.guogaology.mixin;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Preserve the direct-hit invulnerability state while applying its separate core explosion. */
@Mixin(LivingEntity.class)
public interface ExtraDamageAccessor {
    @Accessor("lastDamageTaken") float guogaology$lastDamage();
    @Accessor("lastDamageTaken") void guogaology$lastDamage(float value);
}
