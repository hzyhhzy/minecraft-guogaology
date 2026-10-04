package dev.googology.world;

import dev.googology.GoogologyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import dev.googology.block.ChristmasDigitBlock;
import java.util.List;

/** Community notation examples supplied by the user. Their order is intentional. */
public final class ChristmasSequences {
    private ChristmasSequences() {}
    public static List<Integer> pick(long salt,int site) { return OrdinalLightBand.pick(salt,site).digits(); }
    public static BlockState light(int digit, int color) {
        return GoogologyBlocks.SEQUENCE_LIGHTS[Math.floorMod(color,6)].getDefaultState().with(ChristmasDigitBlock.DIGIT,digit);
    }
    public static BlockState blank(int color){return light(ChristmasDigitBlock.BLANK,color);}
    public static BlockState digit(int n){return GoogologyBlocks.ordinalBrick(n);}
}
