package dev.guogaology.client;

import dev.guogaology.GuogaologyBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public final class GuogaologyClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        new dev.guogaology.outer.client.GuogaologyClient().onInitializeClient();
        net.minecraft.client.gui.screens.MenuScreens.register(dev.guogaology.mining.MiningContent.ENHANCEMENT_MENU,EnhancementScreen::new);
        net.minecraft.client.gui.screens.MenuScreens.register(dev.guogaology.mining.MiningContent.MANUSCRIPT_MENU,EnhancementScreen::new);
        PortalAppearance.initialize();
        PortalLoading.initialize();
        ManuscriptInventoryButton.initialize();
        CoreMeshModels.initialize();
        CoreAnimationRenderer.initialize();
        GuogaologyAtmosphere.initialize();
        GuogaologyBlocks.TRANSLUCENT.forEach(block -> BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.TRANSLUCENT));
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, GuogaologyBlocks.DREAD_LEAVES, GuogaologyBlocks.Y_LEAVES, GuogaologyBlocks.GIANT_LAVER);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, GuogaologyBlocks.EPSILON_BLOOM);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, GuogaologyBlocks.ORDINAL_PLANTS);
        BlockRenderLayerMap.putBlock(GuogaologyBlocks.EMOJI_FLOWER, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.TRANSLUCENT, GuogaologyBlocks.PORTAL, GuogaologyBlocks.INNER_PORTAL, GuogaologyBlocks.FRUIT_PORTAL);
    }
}
