package dev.guogaology.world;

import dev.guogaology.GuogaologyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import dev.guogaology.block.ChristmasDigitBlock;
import java.util.List;

/** Community notation examples supplied by the user. Their order is intentional. */
public final class ChristmasSequences {
    private ChristmasSequences() {}
    public static List<Integer> pick(long salt,int site) { return OrdinalLightBand.pick(salt,site).digits(); }
    public static BlockState light(int digit, int color) {
        return GuogaologyBlocks.SEQUENCE_LIGHTS[Math.floorMod(color,6)].getDefaultState().with(ChristmasDigitBlock.DIGIT,digit);
    }
    public static BlockState blank(int color){return GuogaologyBlocks.PLAIN_LIGHTS[Math.floorMod(color,6)].getDefaultState();}
    public static BlockState digit(int n){return GuogaologyBlocks.ordinalBrick(n);}
}
