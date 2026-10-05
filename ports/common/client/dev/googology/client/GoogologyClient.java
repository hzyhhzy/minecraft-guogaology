package dev.googology.client;

import dev.googology.GoogologyBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class GoogologyClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        new dev.googology.outer.client.GoogologyClient().onInitializeClient();
        net.minecraft.client.gui.screens.MenuScreens.register(dev.googology.mining.MiningContent.ENHANCEMENT_MENU,EnhancementScreen::new);
        PortalAppearance.initialize();
        CoreMeshModels.initialize();
        CoreAnimationRenderer.initialize();
        GoogologyAtmosphere.initialize();
        GoogologyBlocks.TRANSLUCENT.forEach(block -> BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.TRANSLUCENT));
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, GoogologyBlocks.DREAD_LEAVES, GoogologyBlocks.Y_LEAVES);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, GoogologyBlocks.EPSILON_BLOOM);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, GoogologyBlocks.ORDINAL_PLANTS);
        BlockRenderLayerMap.putBlock(GoogologyBlocks.EMOJI_FLOWER, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.TRANSLUCENT, GoogologyBlocks.PORTAL, GoogologyBlocks.INNER_PORTAL, GoogologyBlocks.FRUIT_PORTAL);
    }
}
