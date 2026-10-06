package dev.googology.mixin;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Preserve the direct-hit invulnerability state while applying its separate core explosion. */
@Mixin(LivingEntity.class)
public interface ExtraDamageAccessor {
    @Accessor("lastDamageTaken") float googology$lastDamage();
    @Accessor("lastDamageTaken") void googology$lastDamage(float value);
}
