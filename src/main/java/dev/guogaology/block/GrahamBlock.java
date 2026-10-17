package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.world.GrahamGrowth;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;

/** The original Graham plant grows into its existing scenery form only in the deep realms. */
public final class GrahamBlock extends PlantBlock implements Fertilizable {
    public static final MapCodec<GrahamBlock> CODEC=createCodec(GrahamBlock::new);
    public GrahamBlock(Settings settings){super(settings);}
    @Override public MapCodec<GrahamBlock> getCodec(){return CODEC;}
    public static boolean supportsDeepGrowth(BlockState state){
        return state.isIn(BlockTags.DIRT)||state.isOf(Blocks.FARMLAND)
                ||state.isOf(GuogaologyBlocks.EPSILON_TURF)||state.isOf(GuogaologyBlocks.GUOGAO_LOAM)
                ||state.isOf(GuogaologyBlocks.POWER_SAND)||state.isOf(GuogaologyBlocks.ROOTBOUND_STONE)
                ||state.getBlock() instanceof NumberStoneBlock;
    }
    @Override protected boolean canPlantOnTop(BlockState state,BlockView world,BlockPos pos){
        return world instanceof World level&&GrahamGrowth.isDeep(level)&&supportsDeepGrowth(state)
                ||super.canPlantOnTop(state,world,pos);
    }
    @Override protected void randomTick(BlockState state,ServerWorld world,BlockPos pos,Random random){
        if(GrahamGrowth.isDeep(world)&&random.nextInt(7)==0)GrahamGrowth.tryGrow(world,pos);
    }
    @Override public boolean isFertilizable(WorldView world,BlockPos pos,BlockState state){
        return world instanceof World level&&GrahamGrowth.isDeep(level)
                &&(!(world instanceof ServerWorld server)||GrahamGrowth.canGrow(server,pos));
    }
    @Override public boolean canGrow(World world,Random random,BlockPos pos,BlockState state){
        return world instanceof ServerWorld server&&GrahamGrowth.canGrow(server,pos);
    }
    @Override public void grow(ServerWorld world,Random random,BlockPos pos,BlockState state){GrahamGrowth.tryGrow(world,pos);}
}
