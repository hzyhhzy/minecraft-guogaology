package dev.guogaology.lamp;

import dev.guogaology.GuogaologyMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/** Self-contained registration, shared by the survival and creative catalogs. */
public final class LampShapingContent {
    public static final String ID="guogao_lamp_shaper";
    public static final MenuType<LampShapingMenu> MENU=Registry.register(BuiltInRegistries.MENU,
            GuogaologyMod.id("lamp_shaping"),new MenuType<>(LampShapingMenu::new,FeatureFlags.VANILLA_SET));
    public static final Block BLOCK=Registry.register(BuiltInRegistries.BLOCK,GuogaologyMod.id(ID),
            new LampShapingBlock(dev.guogaology.mining.MiningContent.blockSettings(ID).strength(3.5f,6).noOcclusion()));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,GuogaologyMod.id(ID),new BlockItem(BLOCK,dev.guogaology.mining.MiningContent.itemSettings(ID).useBlockDescriptionPrefix()));
    private LampShapingContent(){}
    public static void initialize(){}
}
