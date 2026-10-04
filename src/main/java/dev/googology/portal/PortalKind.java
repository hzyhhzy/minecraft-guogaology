package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public enum PortalKind {
    GGG, GUOGAO;
    public Block block() { return this==GGG?GoogologyBlocks.PORTAL:GoogologyBlocks.FRUIT_PORTAL; }
    public static boolean isPortal(BlockState state) { return state.isOf(GoogologyBlocks.PORTAL)||state.isOf(GoogologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.isOf(GoogologyBlocks.FRUIT_PORTAL)?GUOGAO:GGG; }
}
