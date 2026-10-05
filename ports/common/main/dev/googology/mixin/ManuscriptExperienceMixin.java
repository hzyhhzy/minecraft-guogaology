package dev.googology.mixin;
import com.llamalad7.mixinextras.sugar.Local;
import dev.googology.mining.ManuscriptEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Apply only to experience actually awarded after pickup/mending, once per orb. */
@Mixin(ExperienceOrb.class)
public abstract class ManuscriptExperienceMixin {
    @ModifyArg(method="playerTouch",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"),index=0)
    private int manuscriptExperience(int amount,@Local(argsOnly=true)Player player){return (int)Math.round(amount*(1+Math.min(3,.25*(ManuscriptEffects.points(player,4)+dev.googology.mining.GearData.points(player.getMainHandItem(),4)))));}
}
