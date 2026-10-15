package dev.guogaology.lamp;

import dev.guogaology.GuogaologyMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;

/** Self-contained registration, shared by the survival and creative catalogs. */
public final class LampShapingContent {
    public static final String ID="guogao_lamp_shaper";
    public static final ScreenHandlerType<LampShapingMenu> MENU=Registry.register(Registries.SCREEN_HANDLER,
            GuogaologyMod.id("lamp_shaping"),new ScreenHandlerType<>(LampShapingMenu::new,FeatureFlags.VANILLA_FEATURES));
    public static final Block BLOCK=Registry.register(Registries.BLOCK,GuogaologyMod.id(ID),
            new LampShapingBlock(AbstractBlock.Settings.create().strength(3.5f,6).nonOpaque()));
    public static final Item ITEM=Registry.register(Registries.ITEM,GuogaologyMod.id(ID),new BlockItem(BLOCK,new Item.Settings()));
    private LampShapingContent(){}
    public static void initialize(){}
}
