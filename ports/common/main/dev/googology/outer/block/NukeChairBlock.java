package dev.googology.outer.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class NukeChairBlock extends ChairBlock {
   public static final int NUKE_NAUSEA_TICKS = 200;
   public static final int NUKE_NAUSEA_AMPLIFIER = 0;

   public NukeChairBlock(Properties var1) {
      super(var1);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState var1, Level var2, BlockPos var3, Player var4, BlockHitResult var5) {
      InteractionResult var6 = super.useWithoutItem(var1, var2, var3, var4, var5);
      if (!var2.isClientSide() && var6 != InteractionResult.PASS) {
         var4.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
      }

      return var6;
   }
}
