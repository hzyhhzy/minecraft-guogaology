package dev.googology.world;

import dev.googology.GoogologyBlocks;
import dev.googology.block.ChristmasDigitBlock;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

/** Community notation examples supplied by the user. Their order is intentional. */
public final class ChristmasSequences {
    private ChristmasSequences() {}
    public static List<Integer> pick(long salt,int site) { return OrdinalLightBand.pick(salt,site).digits(); }
    public static BlockState light(int digit, int color) {
        return GoogologyBlocks.SEQUENCE_LIGHTS[Math.floorMod(color,6)].defaultBlockState().setValue(ChristmasDigitBlock.DIGIT,digit);
    }
    public static BlockState blank(int color){return GoogologyBlocks.PLAIN_LIGHTS[Math.floorMod(color,6)].defaultBlockState();}
    public static BlockState digit(int n){return GoogologyBlocks.ordinalBrick(n);}
}
