package dev.googology.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class OrdinalOre extends net.minecraft.world.level.block.DropExperienceBlock {
    public final int tier;
    public OrdinalOre(Properties props,int tier){super(net.minecraft.util.valueproviders.UniformInt.of(1,tier+2),props);this.tier=tier;}

}
