package dev.googology.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(LivingEntity.class)
public interface ExtraDamageAccessor {
    @Accessor("lastHurt") float googology$lastDamage();
    @Accessor("lastHurt") void googology$lastDamage(float value);
}
