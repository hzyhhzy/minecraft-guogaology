package dev.guogaology.mixin;
import dev.guogaology.mining.ManuscriptMenu;
import net.minecraft.server.network.*;
import net.minecraft.network.packet.c2s.play.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These invoke sites run only after the native packet handler has entered the server thread. */
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ManuscriptLockMixin {
    @Shadow public ServerPlayerEntity player;
    @Inject(method="onPlayerAction",at=@At(value="INVOKE",target="Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;getAction()Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;"),cancellable=true)
    private void guogaology$keepBook(PlayerActionC2SPacket packet,CallbackInfo ci){
        if(!(player.currentScreenHandler instanceof ManuscriptMenu menu))return;
        switch(packet.getAction()){case SWAP_ITEM_WITH_OFFHAND->ci.cancel();case DROP_ITEM,DROP_ALL_ITEMS->{if(menu.locksHotbar())ci.cancel();}default->{}}
    }
    @Inject(method="onUpdateSelectedSlot",at=@At(value="INVOKE",target="Lnet/minecraft/network/packet/c2s/play/UpdateSelectedSlotC2SPacket;getSelectedSlot()I",ordinal=0),cancellable=true)
    private void guogaology$keepSlot(UpdateSelectedSlotC2SPacket packet,CallbackInfo ci){if(player.currentScreenHandler instanceof ManuscriptMenu menu&&menu.blocksSelection(packet.getSelectedSlot()))ci.cancel();}
}
