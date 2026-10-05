package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum PortalKind {
    GGG, GUOGAO, INNER;
    public Block block() { return this==GGG?GoogologyBlocks.PORTAL:this==INNER?GoogologyBlocks.INNER_PORTAL:GoogologyBlocks.FRUIT_PORTAL; }
    public static boolean isPortal(BlockState state) { return state.is(GoogologyBlocks.INNER_PORTAL)||state.is(GoogologyBlocks.PORTAL)||state.is(GoogologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.is(GoogologyBlocks.FRUIT_PORTAL)?GUOGAO:state.is(GoogologyBlocks.INNER_PORTAL)?INNER:GGG; }
}
