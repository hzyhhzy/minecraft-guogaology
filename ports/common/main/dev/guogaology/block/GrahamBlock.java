package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.outer.block.ModBushBlock;
import dev.guogaology.world.GrahamGrowth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Deep growth is added without changing the imported bush's behavior in other realms. */
public final class GrahamBlock extends ModBushBlock {
    public static final MapCodec<BushBlock> CODEC=simpleCodec(GrahamBlock::new);
    public GrahamBlock(Properties settings){super(settings);}
    @Override public MapCodec<BushBlock> codec(){return CODEC;}
    public static boolean supportsDeepGrowth(BlockState state){
        return state.is(BlockTags.DIRT)||state.is(Blocks.FARMLAND)
                ||state.is(GuogaologyBlocks.EPSILON_TURF)||state.is(GuogaologyBlocks.GUOGAO_LOAM)
                ||state.is(GuogaologyBlocks.POWER_SAND)||state.is(GuogaologyBlocks.ROOTBOUND_STONE)
                ||state.getBlock() instanceof NumberStoneBlock;
    }
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter world,BlockPos pos){
        return world instanceof Level level&&GrahamGrowth.isDeep(level)&&supportsDeepGrowth(state)
                ||super.mayPlaceOn(state,world,pos);
    }
    @Override protected void randomTick(BlockState state,ServerLevel world,BlockPos pos,RandomSource random){
        if(GrahamGrowth.isDeep(world)&&random.nextInt(7)==0)GrahamGrowth.tryGrow(world,pos);
    }
    @Override public boolean isValidBonemealTarget(LevelReader world,BlockPos pos,BlockState state){
        if(world instanceof Level level&&GrahamGrowth.isDeep(level))
            return !(world instanceof ServerLevel server)||GrahamGrowth.canGrow(server,pos);
        return super.isValidBonemealTarget(world,pos,state);
    }
    @Override public boolean isBonemealSuccess(Level world,RandomSource random,BlockPos pos,BlockState state){
        if(GrahamGrowth.isDeep(world))return world instanceof ServerLevel server&&GrahamGrowth.canGrow(server,pos);
        return super.isBonemealSuccess(world,random,pos,state);
    }
    @Override public void performBonemeal(ServerLevel world,RandomSource random,BlockPos pos,BlockState state){
        if(GrahamGrowth.isDeep(world))GrahamGrowth.tryGrow(world,pos);
        else super.performBonemeal(world,random,pos,state);
    }
}
