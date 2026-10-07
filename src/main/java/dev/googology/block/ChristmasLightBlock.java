package dev.googology.block;

import net.minecraft.block.Block;

/** A colored light with no number property or number-changing interaction. */
public final class ChristmasLightBlock extends Block {
    private final int color;
    public ChristmasLightBlock(Settings settings,int color){super(settings);this.color=color;}
    public int color(){return color;}
}
