package dev.guogaology.outer.client;

import dev.guogaology.outer.GuogaologyMod;
import dev.guogaology.outer.client.model.BeaverModel;
import dev.guogaology.outer.client.model.FlyYModel;
import dev.guogaology.outer.client.model.FruitCakeSlimeModel;
import dev.guogaology.outer.client.model.FruitSlimeModel;
import dev.guogaology.outer.client.model.SnakeModel;
import dev.guogaology.outer.client.model.WhaleModel;
import dev.guogaology.outer.client.render.BeaverRenderer;
import dev.guogaology.outer.client.render.EvilPigRenderer;
import dev.guogaology.outer.client.render.FlyYRenderer;
import dev.guogaology.outer.client.render.FruitCakeSlimeRenderer;
import dev.guogaology.outer.client.render.FruitSlimeRenderer;
import dev.guogaology.outer.client.render.GuogaologyBoatRenderer;
import dev.guogaology.outer.client.render.SeatRenderer;
import dev.guogaology.outer.client.render.SnakeRenderer;
import dev.guogaology.outer.client.render.WhaleRenderer;
import dev.guogaology.outer.registry.ModBlocks;
import dev.guogaology.outer.registry.ModBoats;
import dev.guogaology.outer.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class GuogaologyClient implements ClientModInitializer {
   public static final ModelLayerLocation SNAKE_LAYER = createMainLayer("snake");
   public static final ModelLayerLocation DEEPSEEK_WHALE_LAYER = createMainLayer("deepseek_whale");
   public static final ModelLayerLocation BUSY_BEAVER_LAYER = createMainLayer("busy_beaver");
   public static final ModelLayerLocation FLY_Y_LAYER = createMainLayer("fly_y");
   public static final ModelLayerLocation FRUIT_CAKE_SLIME_LAYER = createMainLayer("fruit_cake_slime");
   public static final ModelLayerLocation FRUIT_SLIME_LAYER = createMainLayer("fruit_slime");

   private static ModelLayerLocation createMainLayer(String var0) {
      return new ModelLayerLocation(GuogaologyMod.id(var0), "main");
   }

   public void onInitializeClient() {
      registerModelLayers();
      registerRenderers();
      // Earlier renderers require explicit alpha layers; 26.x uses sprite alpha.
      for(String wood:ModBlocks.WOOD_IDS){net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,ModBlocks.wood(wood,"leaves"),ModBlocks.wood(wood,"sapling"));}
      for(String wood:ModBlocks.FOLIAGE_IDS){net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,ModBlocks.wood(wood,"leaves"),ModBlocks.wood(wood,"sapling"));}
      net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,ModBlocks.CHRISTMAS_LEAVES);
      net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlocks(net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT,ModBlocks.GRAHAM,ModBlocks.GRAHAM_FLOWER,ModBlocks.GRAHAM_VINE);
      net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(ModBlocks.LHO_GLASS,net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT);
   }

   private static void registerModelLayers() {
      ModelLayerRegistry.registerModelLayer(SNAKE_LAYER, SnakeModel::getTexturedModelData);
      ModelLayerRegistry.registerModelLayer(DEEPSEEK_WHALE_LAYER, WhaleModel::getTexturedModelData);
      ModelLayerRegistry.registerModelLayer(BUSY_BEAVER_LAYER, BeaverModel::getTexturedModelData);
      ModelLayerRegistry.registerModelLayer(FLY_Y_LAYER, FlyYModel::getTexturedModelData);
      ModelLayerRegistry.registerModelLayer(FRUIT_CAKE_SLIME_LAYER, FruitCakeSlimeModel::getTexturedModelData);
      ModelLayerRegistry.registerModelLayer(FRUIT_SLIME_LAYER, FruitSlimeModel::getTexturedModelData);

      for (String var1 : ModBlocks.WOOD_IDS) {
         ModelLayerRegistry.registerModelLayer(boatLayer(var1), BoatModel::createBoatModel);
      }
   }

   private static void registerRenderers() {
      EntityRendererRegistry.register(ModEntities.SNAKE, SnakeRenderer::new);
      EntityRendererRegistry.register(ModEntities.DEEPSEEK_WHALE, WhaleRenderer::new);
      EntityRendererRegistry.register(ModEntities.BUSY_BEAVER, BeaverRenderer::new);
      EntityRendererRegistry.register(ModEntities.FLY_Y, FlyYRenderer::new);
      EntityRendererRegistry.register(ModEntities.FRUIT_CAKE_SLIME, FruitCakeSlimeRenderer::new);
      EntityRendererRegistry.register(ModEntities.EVIL_PIG, EvilPigRenderer::new);
      EntityRendererRegistry.register(ModEntities.FRUIT_SLIME, FruitSlimeRenderer::new);
      EntityRendererRegistry.register(ModEntities.SEAT, SeatRenderer::new);

      for (String var1 : ModBlocks.WOOD_IDS) {
         ModelLayerLocation var2 = boatLayer(var1);
         EntityRendererRegistry.register(
            ModBoats.boat(var1), var2x -> new GuogaologyBoatRenderer(var2x, var2, GuogaologyMod.id("textures/entity/boat/" + var1 + "_boat.png"))
         );
      }
   }

   private static ModelLayerLocation boatLayer(String var0) {
      return new ModelLayerLocation(GuogaologyMod.id(var0 + "_boat"), "main");
   }
}
