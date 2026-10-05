package dev.googology.client;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import dev.googology.block.*;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import java.util.function.ToIntFunction;

/** Vanilla item overrides use the very same saved block-state component as placement. */
public final class DecorItemModels {
    private DecorItemModels() {}
    private static void register(Block block,ToIntFunction<BlockState> index) {
        FabricModelPredicateProviderRegistry.register(block.asItem(),GoogologyMod.id("appearance"),(stack,world,entity,seed)->
                index.applyAsInt(stack.getOrDefault(DataComponentTypes.BLOCK_STATE,BlockStateComponent.DEFAULT).applyToState(block.getDefaultState()))/256.0f);
    }
    public static void initialize() {
        for(var lamp:GoogologyBlocks.LANTERNS) register(lamp,s->s.get(EmojiLanternBlock.EMOTION)*28+s.get(EmojiLanternBlock.PART));
        for(var lamp:GoogologyBlocks.SEQUENCE_LIGHTS)register(lamp,s->s.get(ChristmasDigitBlock.DIGIT));
        register(GoogologyBlocks.ORDINAL_BRICKS,s->s.get(OrdinalBrickBlock.NUMBER));
        register(GoogologyBlocks.MOSAIC_LIGHT,s->s.get(MosaicLightBlock.COLOR));
        register(GoogologyBlocks.ASTRA_WEAVE,s->s.get(AstraWeaveBlock.COLOR));
        register(GoogologyBlocks.PORTAL_FRAME,s->s.get(GoogologyPortalFrameBlock.STYLE));
        register(GoogologyBlocks.TURING_TAPE,TuringTapeBlock::appearanceIndex);
    }
}
