package dev.guogaology.mixin;

import dev.guogaology.mining.MiningEffects;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep template orientation at native mining START, rather than re-reading yaw after the block breaks. */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class MiningPatternStartMixin {
    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method="processBlockBreakingAction",at=@At("HEAD"))
    private void guogaology$miningStart(BlockPos pos,PlayerActionC2SPacket.Action action,Direction face,int maxHeight,int sequence,CallbackInfo ci){
        if(action==PlayerActionC2SPacket.Action.START_DESTROY_BLOCK)MiningEffects.captureStart(player,pos);
        else if(action==PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)MiningEffects.cancelStart(player);
    }
}
