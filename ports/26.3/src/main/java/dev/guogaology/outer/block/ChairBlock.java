package dev.guogaology.outer.block;

import dev.guogaology.outer.entity.SeatEntity;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChairBlock extends Block {
   private static final VoxelShape SEAT = Shapes.box(0.125, 0.375, 0.125, 0.875, 0.5, 0.875);
   private static final Map<Direction, VoxelShape> SHAPES = Map.of(
      Direction.NORTH,
      Shapes.or(SEAT, Shapes.box(0.125, 0.5, 0.75, 0.875, 0.875, 0.875)),
      Direction.SOUTH,
      Shapes.or(SEAT, Shapes.box(0.125, 0.5, 0.125, 0.875, 0.875, 0.25)),
      Direction.WEST,
      Shapes.or(SEAT, Shapes.box(0.75, 0.5, 0.125, 0.875, 0.875, 0.875)),
      Direction.EAST,
      Shapes.or(SEAT, Shapes.box(0.125, 0.5, 0.125, 0.25, 0.875, 0.875))
   );

   public ChairBlock(Properties var1) {
      super(var1);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> var1) {
      var1.add(new Property[]{BlockStateProperties.HORIZONTAL_FACING});
   }

   protected VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return SHAPES.getOrDefault(var1.getValue(BlockStateProperties.HORIZONTAL_FACING), SHAPES.get(Direction.NORTH));
   }

   protected InteractionResult useWithoutItem(BlockState var1, Level var2, BlockPos var3, Player var4, BlockHitResult var5) {
      if (var2.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         return (InteractionResult)(SeatEntity.sit(var2, var3, var4) ? InteractionResult.SUCCESS : InteractionResult.PASS);
      }
   }
}
