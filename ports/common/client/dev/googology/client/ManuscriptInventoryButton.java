package dev.googology.client;

import dev.googology.client.mixin.InventoryOriginAccessor;
import dev.googology.mining.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;

/** No key binding or offhand use override. Follow the recipe-book layout on every frame. */
public final class ManuscriptInventoryButton {
    public static void initialize(){
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof InventoryScreen)&&!(screen instanceof CreativeModeInventoryScreen))return;
            var button=Button.builder(Component.translatable("mining.googology.manuscript.button"),b->{
                if(b.active)ClientPlayNetworking.send(OpenManuscriptPayload.INSTANCE);
            }).bounds(0,0,46,16).tooltip(Tooltip.create(Component.translatable("mining.googology.manuscript.button_hint"))).build();
            Screens.getButtons(screen).add(button);update(client,screen,button);
            ScreenEvents.beforeRender(screen).register((s,graphics,mx,my,delta)->update(client,s,button));
        });
    }
    private static void update(Minecraft client,Screen screen,Button button){
        boolean creative=screen instanceof CreativeModeInventoryScreen;
        button.visible=client.player!=null&&client.player.getOffhandItem().getItem() instanceof DenxiManuscript
                &&(!creative||((CreativeModeInventoryScreen)screen).isInventoryOpen());
        button.active=button.visible&&((AbstractContainerScreen<?>)screen).getMenu().getCarried().isEmpty()&&ClientPlayNetworking.canSend(OpenManuscriptPayload.ID);
        var origin=(InventoryOriginAccessor)screen;
        button.setX(origin.googology$left()+(creative?132:97));button.setY(origin.googology$top()+(creative?6:62));
    }
}
