package dev.guogaology.mining;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public final class OrdinalOre extends net.minecraft.block.ExperienceDroppingBlock {
    public final int tier;
    public OrdinalOre(Settings props,int tier){super(net.minecraft.util.math.intprovider.UniformIntProvider.create(1,tier+2),props);this.tier=tier;}

}
