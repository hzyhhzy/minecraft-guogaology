package dev.guogaology.client;

import dev.guogaology.client.mixin.InventoryOriginAccessor;
import dev.guogaology.mining.*;
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
            var button=Button.builder(Component.translatable("mining.guogaology.manuscript.button"),b->{
                if(b.active)ClientPlayNetworking.send(OpenManuscriptPayload.INSTANCE);
            }).bounds(0,0,42,16).tooltip(Tooltip.create(Component.translatable("mining.guogaology.manuscript.button_hint"))).build();
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
        // Survival's recipe-book toggle occupies x=104..124. Keep a gap and
        // stay inside the right edge; the origin moves when that book opens.
        button.setX(origin.guogaology$left()+(creative?132:130));button.setY(origin.guogaology$top()+(creative?6:62));
    }
}
