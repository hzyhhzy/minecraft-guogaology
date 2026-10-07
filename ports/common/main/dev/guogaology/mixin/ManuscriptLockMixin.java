package dev.guogaology.mixin;
import dev.guogaology.mining.ManuscriptMenu;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These invoke sites run only after the native packet handler has entered the server thread. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ManuscriptLockMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handlePlayerAction",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;getAction()Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket$Action;"),cancellable=true)
    private void guogaology$keepBook(ServerboundPlayerActionPacket packet,CallbackInfo ci){
        if(!(player.containerMenu instanceof ManuscriptMenu menu))return;
        switch(packet.getAction()){case SWAP_ITEM_WITH_OFFHAND->ci.cancel();case DROP_ITEM,DROP_ALL_ITEMS->{if(menu.locksHotbar())ci.cancel();}default->{}}
    }
    @Inject(method="handleSetCarriedItem",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ServerboundSetCarriedItemPacket;getSlot()I",ordinal=0),cancellable=true)
    private void guogaology$keepSlot(ServerboundSetCarriedItemPacket packet,CallbackInfo ci){if(player.containerMenu instanceof ManuscriptMenu menu&&menu.blocksSelection(packet.getSlot()))ci.cancel();}
}
