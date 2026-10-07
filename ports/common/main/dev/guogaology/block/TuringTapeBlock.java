package dev.guogaology.block;

import com.mojang.serialization.MapCodec;
import dev.guogaology.GuogaologyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Live six-neighbor tape. Only the moving head carries logical left/right around corners. */
public final class TuringTapeBlock extends StatefulDecorBlock {
    public static final MapCodec<TuringTapeBlock> CODEC=simpleCodec(TuringTapeBlock::new);
    public static final BooleanProperty INK=BooleanProperty.create("ink"), POWERED=BooleanProperty.create("powered");
    // Route 0 has no direction; 1..30 are ordered pairs of distinct face neighbors.
    public static final IntegerProperty ROUTE=IntegerProperty.create("route",0,30);
    public static final int BLOCKED=7,STEP_TICKS=4;
    public static final IntegerProperty HEAD=IntegerProperty.create("head",0,BLOCKED);
    public record Neighbors(Direction first,Direction second) {
        public boolean contains(Direction d){return d==first||d==second;}
        public Direction other(Direction d){return d==first?second:d==second?first:null;}
        Direction positive(){
            for(var d:new Direction[]{Direction.EAST,Direction.UP,Direction.SOUTH})if(contains(d))return d;
            return second;
        }
    }
    public static BlockState startButton(){
        return net.minecraft.world.level.block.Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);
    }
    public TuringTapeBlock(Properties settings){
        super(settings.lightLevel(s->TapeProgram.running(s.getValue(HEAD))?10:0));
        registerDefaultState(getStateDefinition().any().setValue(INK,false).setValue(POWERED,false).setValue(ROUTE,0).setValue(HEAD,0));
    }
    @Override public MapCodec<TuringTapeBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(INK,POWERED,ROUTE,HEAD);}
    @Override protected BlockState portableState(BlockState s,BlockPos p){return s.setValue(POWERED,false).setValue(HEAD,0).setValue(ROUTE,0);}
    public static BlockState oriented(BlockState s,Direction positive,Direction negative){
        if(positive==negative)throw new IllegalArgumentException("Tape exits must differ");
        int p=positive.get3DDataValue(),n=negative.get3DDataValue();
        return s.setValue(ROUTE,1+p*5+n-(n>p?1:0));
    }
    private static Direction positive(BlockState s){return Direction.from3DDataValue((s.getValue(ROUTE)-1)/5);}
    private static Direction negative(BlockState s){
        int value=s.getValue(ROUTE)-1,p=value/5,n=value%5;
        return Direction.from3DDataValue(n>=p?n+1:n);
    }
    /** Cores terminate a finite tape. No query requests an unloaded chunk. */
    public Neighbors neighbors(Level world,BlockPos pos){
        Direction first=null,second=null;
        for(var d:Direction.values()){
            var neighbor=pos.relative(d);if(!world.hasChunkAt(neighbor))return null;
            var state=world.getBlockState(neighbor);
            if(!state.is(this)&&!state.is(GuogaologyBlocks.BOUNDARY_CORE))continue;
            if(first==null)first=d;else if(second==null)second=d;else return null;
        }
        return second==null?null:new Neighbors(first,second);
    }
    private static boolean valid(BlockState s,Neighbors n){return n!=null&&s.getValue(ROUTE)>0&&n.contains(positive(s))&&n.contains(negative(s));}
    public static Direction nextDirection(BlockState s){
        if(!TapeProgram.running(s.getValue(HEAD))||s.getValue(ROUTE)==0)return null;
        int move=TapeProgram.transition(s.getValue(HEAD),s.getValue(INK)).move();
        return move==0?null:move>0?positive(s):negative(s);
    }
    public static int appearanceIndex(BlockState s){
        var d=nextDirection(s);int ink=s.getValue(INK)?1:0;
        return d==null?(TapeProgram.running(s.getValue(HEAD))?14+ink:ink):2+d.get3DDataValue()*2+ink;
    }
    private static BlockState stopped(BlockState s){return s.setValue(HEAD,BLOCKED).setValue(ROUTE,0);}
    @Override protected void onPlace(BlockState state,Level world,BlockPos pos,BlockState oldState,boolean notify){if(!oldState.is(this))refresh(world,pos);}
    @Override protected void neighborChanged(BlockState state,Level world,BlockPos pos,Block source,net.minecraft.world.level.redstone.Orientation orientation,boolean notify){refresh(world,pos);}
    private void refresh(Level world,BlockPos pos){
        if(!(world instanceof ServerLevel server))return;
        var state=world.getBlockState(pos);if(!state.is(this))return;
        boolean powered=world.hasNeighborSignal(pos);var pair=neighbors(world,pos);
        var next=state.setValue(POWERED,powered);
        if(TapeProgram.running(state.getValue(HEAD))&&!valid(state,pair))next=stopped(next);
        if(powered&&!state.getValue(POWERED)&&pair!=null){
            var positive=pair.positive();next=oriented(next.setValue(HEAD,1),positive,pair.other(positive));
            server.scheduleTick(pos,this,STEP_TICKS);
        }
        if(next!=state)world.setBlock(pos,next,Block.UPDATE_CLIENTS);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level world,BlockPos pos,Player player,BlockHitResult hit){
        if(!world.isClientSide())world.setBlock(pos,state.cycle(INK),Block.UPDATE_CLIENTS);
        return InteractionResult.SUCCESS;
    }
    @Override protected void tick(BlockState s,ServerLevel world,BlockPos pos,RandomSource random){
        int head=s.getValue(HEAD);if(!TapeProgram.running(head))return;
        if(!valid(s,neighbors(world,pos))){world.setBlock(pos,stopped(s),Block.UPDATE_CLIENTS);return;}
        var step=TapeProgram.transition(head,s.getValue(INK));
        if(step.move()==0){world.setBlock(pos,s.setValue(HEAD,TapeProgram.HALT).setValue(ROUTE,0),Block.UPDATE_CLIENTS);return;}
        var direction=nextDirection(s);var target=pos.relative(direction);
        var destination=world.getBlockState(target);
        var pair=neighbors(world,target);var incoming=direction.getOpposite();
        if(!destination.is(this)||pair==null||!pair.contains(incoming)){
            world.setBlock(pos,stopped(s),Block.UPDATE_CLIENTS);return;
        }
        if(TapeProgram.running(destination.getValue(HEAD))){
            world.setBlock(pos,stopped(s),Block.UPDATE_CLIENTS);
            world.setBlock(target,stopped(destination),Block.UPDATE_CLIENTS);return;
        }
        world.setBlock(pos,s.setValue(INK,step.write()).setValue(HEAD,0).setValue(ROUTE,0),Block.UPDATE_CLIENTS);
        var next=destination.setValue(HEAD,step.next());
        // After a positive step the incoming face is logical left; after a negative step it is right.
        next=step.next()==TapeProgram.HALT?next.setValue(ROUTE,0):step.move()>0
                ?oriented(next,pair.other(incoming),incoming):oriented(next,incoming,pair.other(incoming));
        world.setBlock(target,next,Block.UPDATE_CLIENTS);
        if(step.next()!=TapeProgram.HALT)world.scheduleTick(target,this,STEP_TICKS);
    }
}
