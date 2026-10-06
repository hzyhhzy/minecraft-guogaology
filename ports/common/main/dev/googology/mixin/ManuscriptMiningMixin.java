package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.ItemStack;

@Mixin(Player.class)
public abstract class ManuscriptMiningMixin {
    /** Normalize only the core part before vanilla adds Efficiency and applies Haste/penalties. */
    @WrapOperation(method="getDestroySpeed",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;getDestroySpeed(Lnet/minecraft/world/level/block/state/BlockState;)F"))
    private float googology$baseMiningSpeed(ItemStack tool,BlockState state,Operation<Float> original){
        return (float)(original.call(tool,state)/(GearData.minesWithPick(tool,state)?EquipmentRules.miningMultiplier(GearData.profile(tool),java.util.List.of(),tool.getOrDefault(MiningContent.DEEP,false)):1));
    }
    @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true)
    private void googology$manuscriptSpeed(BlockState state,CallbackInfoReturnable<Float> result){
        var player=(Player)(Object)this;
        result.setReturnValue((float)(result.getReturnValueF()*ManuscriptEffects.miningMultiplier(player,player.getMainHandItem(),state)));
    }
}
