package dev.googology.portal;

import dev.googology.GoogologyBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public enum PortalKind {
    GGG, GUOGAO, INNER;
    /** Appearance is the destination, not the pair of realms joined by this gate. */
    public int appearance(String source) {
        return switch(this){
            case GGG -> 0;
            case INNER -> source.equals("googology:googology")?0:1;
            case GUOGAO -> source.equals("googology:guogao")?1:2;
        };
    }
    public Block block() { return this==GGG?GoogologyBlocks.PORTAL:this==INNER?GoogologyBlocks.INNER_PORTAL:GoogologyBlocks.FRUIT_PORTAL; }
    public static boolean isPortal(BlockState state) { return state.is(GoogologyBlocks.INNER_PORTAL)||state.is(GoogologyBlocks.PORTAL)||state.is(GoogologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.is(GoogologyBlocks.FRUIT_PORTAL)?GUOGAO:state.is(GoogologyBlocks.INNER_PORTAL)?INNER:GGG; }
}
