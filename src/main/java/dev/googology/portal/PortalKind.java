package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public enum PortalKind {
    GGG, GUOGAO, INNER;
    public Block block() { return this==GGG?GoogologyBlocks.PORTAL:this==INNER?GoogologyBlocks.INNER_PORTAL:GoogologyBlocks.FRUIT_PORTAL; }
    public static boolean isPortal(BlockState state) { return state.isOf(GoogologyBlocks.INNER_PORTAL)||state.isOf(GoogologyBlocks.PORTAL)||state.isOf(GoogologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.isOf(GoogologyBlocks.FRUIT_PORTAL)?GUOGAO:state.isOf(GoogologyBlocks.INNER_PORTAL)?INNER:GGG; }
}
