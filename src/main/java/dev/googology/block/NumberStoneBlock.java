package dev.googology.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import dev.googology.GoogologyBlocks;

/** Each digit is a separate registered block and item, with no appearance component. */
public final class NumberStoneBlock extends Block {
    public final int digit;
    public NumberStoneBlock(Settings settings,int digit){super(settings);this.digit=digit;}
    /** Deterministic generation only: placed and harvested items keep their own ID. */
    public static BlockState natural(int x,int y,int z){
        long v=x*73856093L^y*83492791L^z*19349663L^0x4e756d626572L;
        v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;v=(v^(v>>>27))*0x94d049bb133111ebL;v^=v>>>31;
        int pick=(int)Math.floorMod(v,1023L),n=0;
        while(n<9&&(pick-=(512>>n))>=0)n++;
        return GoogologyBlocks.NUMBER_STONES[n].getDefaultState();
    }
}
