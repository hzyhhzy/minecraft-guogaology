package dev.guogaology.client;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.GuogaologyMod;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public final class GuogaologyClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("guogaology:outer_client",Runnable.class).forEach(Runnable::run);
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(dev.guogaology.mining.MiningContent.ENHANCEMENT_MENU,EnhancementScreen::new);
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(dev.guogaology.mining.MiningContent.MANUSCRIPT_MENU,EnhancementScreen::new);
        PortalAppearance.initialize();
        PortalLoading.initialize();
        ManuscriptInventoryButton.initialize();
        CoreMeshModels.initialize();
        CoreAnimationRenderer.initialize();
        DecorItemModels.initialize();
        OrdinalBowModels.initialize();
        GuogaologyAtmosphere.initialize();
        DimensionRenderingRegistry.registerDimensionEffects(GuogaologyMod.id("guogaology"),new GuogaologyDimensionEffects());
        DimensionRenderingRegistry.registerDimensionEffects(GuogaologyMod.id("guogao"),new GuogaologyDimensionEffects());
        // The world-height landmarks provide their own cloud sculptures.
        DimensionRenderingRegistry.registerCloudRenderer(GuogaologyMod.DIMENSION, context -> {});
        DimensionRenderingRegistry.registerCloudRenderer(GuogaologyMod.GUOGAO, context -> {});
        GuogaologyBlocks.TRANSLUCENT.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent()));
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutoutMipped(), GuogaologyBlocks.DREAD_LEAVES, GuogaologyBlocks.Y_LEAVES, GuogaologyBlocks.GIANT_LAVER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), GuogaologyBlocks.EPSILON_BLOOM);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), GuogaologyBlocks.ORDINAL_PLANTS);
        BlockRenderLayerMap.INSTANCE.putBlock(GuogaologyBlocks.EMOJI_FLOWER,RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(GuogaologyBlocks.PORTAL, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(GuogaologyBlocks.FRUIT_PORTAL, RenderLayer.getTranslucent());
    }
}
