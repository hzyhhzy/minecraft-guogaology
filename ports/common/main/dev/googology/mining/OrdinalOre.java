package dev.googology.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class OrdinalOre extends net.minecraft.world.level.block.DropExperienceBlock {
    public final int tier;
    public OrdinalOre(Properties props,int tier){super(net.minecraft.util.valueproviders.UniformInt.of(1,tier+2),props);this.tier=tier;}
    @Override protected float getDestroyProgress(BlockState state,Player player,BlockGetter world,BlockPos pos){
        var stack=player.getMainHandItem();
        double power=GearData.denxi(stack);
        if(!MiningContent.GEAR.containsKey(stack.getItem()))power=stack.getDestroySpeed(state);
        // Keep haste, fatigue, underwater and airborne penalties from vanilla.
        double normal=Math.max(.0001,stack.getDestroySpeed(state));
        double conditions=player.getDestroySpeed(state)/normal;
        return (float)Math.min(1,power*conditions/(20*EquipmentRules.oreWork(tier)));
    }
}
