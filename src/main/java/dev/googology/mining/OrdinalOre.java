package dev.googology.mining;

import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.BlockView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

public final class OrdinalOre extends net.minecraft.block.ExperienceDroppingBlock {
    public final int tier;
    public OrdinalOre(Settings props,int tier){super(net.minecraft.util.math.intprovider.UniformIntProvider.create(1,tier+2),props);this.tier=tier;}
    @Override protected float calcBlockBreakingDelta(BlockState state,PlayerEntity player,BlockView world,BlockPos pos){
        var stack=player.getMainHandStack();
        double power=GearData.denxi(stack);
        if(!MiningContent.GEAR.containsKey(stack.getItem()))power=stack.getMiningSpeedMultiplier(state);
        // Keep haste, fatigue, underwater and airborne penalties from vanilla.
        double normal=Math.max(.0001,stack.getMiningSpeedMultiplier(state));
        double conditions=player.getBlockBreakingSpeed(state)/normal;
        return (float)Math.min(1,power*conditions/(20*EquipmentRules.oreWork(tier)));
    }
}
