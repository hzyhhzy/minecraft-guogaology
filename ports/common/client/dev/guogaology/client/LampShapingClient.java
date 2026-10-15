package dev.guogaology.client;

import dev.guogaology.lamp.LampShapingContent;
import net.minecraft.client.gui.screens.MenuScreens;

public final class LampShapingClient {
    private LampShapingClient(){}
    public static void initializeClient(){MenuScreens.register(LampShapingContent.MENU,LampShapingScreen::new);}
}
