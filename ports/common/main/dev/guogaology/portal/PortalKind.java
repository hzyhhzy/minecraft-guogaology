package dev.guogaology.portal;

import dev.guogaology.GuogaologyBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
    public static boolean isPortal(BlockState state) { return state.is(GuogaologyBlocks.INNER_PORTAL)||state.is(GuogaologyBlocks.PORTAL)||state.is(GuogaologyBlocks.FRUIT_PORTAL); }
    public static PortalKind of(BlockState state) { return state.is(GuogaologyBlocks.FRUIT_PORTAL)?GUOGAO:state.is(GuogaologyBlocks.INNER_PORTAL)?INNER:OUTER; }
}
