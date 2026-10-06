package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.player.PlayerInventory;

@Mixin(PlayerEntity.class)
public abstract class ManuscriptMiningMixin {
    /** Normalize only the core part before vanilla adds Efficiency and applies Haste/penalties. */
    @WrapOperation(method="getBlockBreakingSpeed",at=@At(value="INVOKE",target="Lnet/minecraft/entity/player/PlayerInventory;getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F"))
    private float googology$baseMiningSpeed(PlayerInventory inventory,BlockState state,Operation<Float> original){
        var tool=((PlayerEntity)(Object)this).getMainHandStack();
        return (float)(original.call(inventory,state)/(GearData.minesWithPick(tool,state)?EquipmentRules.miningMultiplier(GearData.profile(tool),java.util.List.of(),tool.getOrDefault(MiningContent.DEEP,false)):1));
    }
    @Inject(method="getBlockBreakingSpeed",at=@At("RETURN"),cancellable=true)
    private void googology$manuscriptSpeed(BlockState state,CallbackInfoReturnable<Float> result){
        var player=(PlayerEntity)(Object)this;
        result.setReturnValue((float)(result.getReturnValueF()*ManuscriptEffects.miningMultiplier(player,player.getMainHandStack(),state)));
    }
}
