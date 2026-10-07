package dev.guogaology.portal;

import dev.guogaology.GuogaologyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public enum PortalKind {
    OUTER, GUOGAO, INNER;
    /** Appearance is the destination, not the pair of realms joined by this gate. */
    public int appearance(String source) {
        return switch(this){
            case OUTER -> 0;
            case INNER -> source.equals("guogaology:guogaology")?0:1;
            case GUOGAO -> source.equals("guogaology:guogao")?1:2;
        };
    }
    public Block block() { return this==OUTER?GuogaologyBlocks.PORTAL:this==INNER?GuogaologyBlocks.INNER_PORTAL:GuogaologyBlocks.FRUIT_PORTAL; }
    public static boolean isPortal(BlockState state) { return state.isOf(GuogaologyBlocks.INNER_PORTAL)||state.isOf(GuogaologyBlocks.PORTAL)||state.isOf(GuogaologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.isOf(GuogaologyBlocks.FRUIT_PORTAL)?GUOGAO:state.isOf(GuogaologyBlocks.INNER_PORTAL)?INNER:OUTER; }
}
