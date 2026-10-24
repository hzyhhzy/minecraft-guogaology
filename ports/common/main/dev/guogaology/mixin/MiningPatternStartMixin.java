package dev.guogaology.mixin;

import dev.guogaology.mining.MiningEffects;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep template orientation at native mining START, rather than re-reading yaw after the block breaks. */
@Mixin(ServerPlayerGameMode.class)
public abstract class MiningPatternStartMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method="handleBlockBreakAction",at=@At("HEAD"))
    private void guogaology$miningStart(BlockPos pos,ServerboundPlayerActionPacket.Action action,Direction face,int maxHeight,int sequence,CallbackInfo ci){
        if(action==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)MiningEffects.captureStart(player,pos);
        else if(action==ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK)MiningEffects.cancelStart(player);
    }
}
