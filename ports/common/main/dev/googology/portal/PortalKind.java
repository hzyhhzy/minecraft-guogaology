package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum PortalKind {
    GGG, GUOGAO;
    public Block block() { return this==GGG?GoogologyBlocks.PORTAL:GoogologyBlocks.FRUIT_PORTAL; }
    public static boolean isPortal(BlockState state) { return state.is(GoogologyBlocks.PORTAL)||state.is(GoogologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.is(GoogologyBlocks.FRUIT_PORTAL)?GUOGAO:GGG; }
}
