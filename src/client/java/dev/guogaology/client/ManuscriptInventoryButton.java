package dev.guogaology.client;

import dev.guogaology.client.mixin.InventoryOriginAccessor;
import dev.guogaology.mining.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.text.Text;

/** No key binding or offhand use override. Follow the recipe-book layout on every frame. */
public final class ManuscriptInventoryButton {
    public static void initialize(){
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof InventoryScreen)&&!(screen instanceof CreativeInventoryScreen))return;
            var button=ButtonWidget.builder(Text.translatable("mining.guogaology.manuscript.button"),b->{
                if(b.active)ClientPlayNetworking.send(OpenManuscriptPayload.INSTANCE);
            }).dimensions(0,0,42,16).tooltip(Tooltip.of(Text.translatable("mining.guogaology.manuscript.button_hint"))).build();
            Screens.getButtons(screen).add(button);update(client,screen,button);
            ScreenEvents.beforeRender(screen).register((s,graphics,mx,my,delta)->update(client,s,button));
        });
    }
    private static void update(MinecraftClient client,Screen screen,ButtonWidget button){
        boolean creative=screen instanceof CreativeInventoryScreen;
        button.visible=client.player!=null&&client.player.getOffHandStack().getItem() instanceof DenxiManuscript
                &&(!creative||((CreativeInventoryScreen)screen).isInventoryTabSelected());
        button.active=button.visible&&((HandledScreen<?>)screen).getScreenHandler().getCursorStack().isEmpty()&&ClientPlayNetworking.canSend(OpenManuscriptPayload.ID);
        var origin=(InventoryOriginAccessor)screen;
        // Survival's recipe-book toggle occupies x=104..124. Keep a gap and
        // stay inside the right edge; the origin moves when that book opens.
        button.setX(origin.guogaology$left()+(creative?132:130));button.setY(origin.guogaology$top()+(creative?6:62));
    }
}
