package dev.guogaology.client;

import dev.guogaology.lamp.LampShapingContent;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class LampShapingClient {
    private LampShapingClient(){}
    public static void initializeClient(){HandledScreens.register(LampShapingContent.MENU,LampShapingScreen::new);}
}
