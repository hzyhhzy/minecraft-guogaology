package dev.googology.client;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public final class GoogologyClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("guogaology:outer_client",Runnable.class).forEach(Runnable::run);
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(dev.googology.mining.MiningContent.ENHANCEMENT_MENU,EnhancementScreen::new);
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(dev.googology.mining.MiningContent.MANUSCRIPT_MENU,EnhancementScreen::new);
        PortalAppearance.initialize();
        CoreMeshModels.initialize();
        CoreAnimationRenderer.initialize();
        DecorItemModels.initialize();
        OrdinalBowModels.initialize();
        GoogologyAtmosphere.initialize();
        DimensionRenderingRegistry.registerDimensionEffects(GoogologyMod.id("googology"),new GoogologyDimensionEffects());
        DimensionRenderingRegistry.registerDimensionEffects(GoogologyMod.id("guogao"),new GoogologyDimensionEffects());
        // The world-height landmarks provide their own cloud sculptures.
        DimensionRenderingRegistry.registerCloudRenderer(GoogologyMod.DIMENSION, context -> {});
        DimensionRenderingRegistry.registerCloudRenderer(GoogologyMod.GUOGAO, context -> {});
        GoogologyBlocks.TRANSLUCENT.forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getTranslucent()));
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutoutMipped(), GoogologyBlocks.DREAD_LEAVES, GoogologyBlocks.Y_LEAVES, GoogologyBlocks.GIANT_LAVER);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), GoogologyBlocks.EPSILON_BLOOM);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), GoogologyBlocks.ORDINAL_PLANTS);
        BlockRenderLayerMap.INSTANCE.putBlock(GoogologyBlocks.EMOJI_FLOWER,RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(GoogologyBlocks.PORTAL, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(GoogologyBlocks.FRUIT_PORTAL, RenderLayer.getTranslucent());
    }
}
