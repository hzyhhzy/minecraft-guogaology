package dev.googology.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import dev.googology.mining.ManuscriptEffects;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ExperienceOrbEntity.class)
public abstract class ManuscriptExperienceMixin {
    @ModifyArg(method="onPlayerCollision",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerEntity;addExperience(I)V"),index=0)
    private int manuscriptExperience(int amount,@Local(argsOnly=true)PlayerEntity player){return (int)Math.round(amount*(1+Math.min(3,.25*(ManuscriptEffects.points(player,4)+dev.googology.mining.GearData.points(player.getMainHandStack(),4)))));}
}
