package dev.googology.block;

import com.mojang.serialization.MapCodec;
import dev.googology.GoogologyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/** Live six-neighbor tape. Only the moving head carries logical left/right around corners. */
public final class TuringTapeBlock extends StatefulDecorBlock {
    public static final MapCodec<TuringTapeBlock> CODEC=createCodec(TuringTapeBlock::new);
    public static final BooleanProperty INK=BooleanProperty.of("ink"), POWERED=BooleanProperty.of("powered");
    // Route 0 has no direction; 1..30 are ordered pairs of distinct face neighbors.
    public static final IntProperty ROUTE=IntProperty.of("route",0,30);
    public static final int BLOCKED=7,STEP_TICKS=4;
    public static final IntProperty HEAD=IntProperty.of("head",0,BLOCKED);
    public record Neighbors(Direction first,Direction second) {
        public boolean contains(Direction d){return d==first||d==second;}
        public Direction other(Direction d){return d==first?second:d==second?first:null;}
        Direction positive(){
            for(var d:new Direction[]{Direction.EAST,Direction.UP,Direction.SOUTH})if(contains(d))return d;
            return second;
        }
    }
    public static BlockState startButton(){
        return net.minecraft.block.Blocks.STONE_BUTTON.getDefaultState()
                .with(net.minecraft.block.WallMountedBlock.FACE,net.minecraft.block.enums.BlockFace.FLOOR);
    }
    public TuringTapeBlock(Settings settings){
        super(settings.luminance(s->TapeProgram.running(s.get(HEAD))?10:0));
        setDefaultState(getStateManager().getDefaultState().with(INK,false).with(POWERED,false).with(ROUTE,0).with(HEAD,0));
    }
    @Override public MapCodec<TuringTapeBlock> getCodec(){return CODEC;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(INK,POWERED,ROUTE,HEAD);}
    @Override protected BlockState portableState(BlockState s,BlockPos p){return s.with(POWERED,false).with(HEAD,0).with(ROUTE,0);}
    public static BlockState oriented(BlockState s,Direction positive,Direction negative){
        if(positive==negative)throw new IllegalArgumentException("Tape exits must differ");
        int p=positive.getId(),n=negative.getId();
        return s.with(ROUTE,1+p*5+n-(n>p?1:0));
    }
    private static Direction positive(BlockState s){return Direction.byId((s.get(ROUTE)-1)/5);}
    private static Direction negative(BlockState s){
        int value=s.get(ROUTE)-1,p=value/5,n=value%5;
        return Direction.byId(n>=p?n+1:n);
    }
    /** Cores terminate a finite tape. No query requests an unloaded chunk. */
    public Neighbors neighbors(World world,BlockPos pos){
        Direction first=null,second=null;
        for(var d:Direction.values()){
            var neighbor=pos.offset(d);if(!world.isChunkLoaded(neighbor))return null;
            var state=world.getBlockState(neighbor);
            if(!state.isOf(this)&&!state.isOf(GoogologyBlocks.BOUNDARY_CORE))continue;
            if(first==null)first=d;else if(second==null)second=d;else return null;
        }
        return second==null?null:new Neighbors(first,second);
    }
    private static boolean valid(BlockState s,Neighbors n){return n!=null&&s.get(ROUTE)>0&&n.contains(positive(s))&&n.contains(negative(s));}
    public static Direction nextDirection(BlockState s){
        if(!TapeProgram.running(s.get(HEAD))||s.get(ROUTE)==0)return null;
        int move=TapeProgram.transition(s.get(HEAD),s.get(INK)).move();
        return move==0?null:move>0?positive(s):negative(s);
    }
    public static int appearanceIndex(BlockState s){
        var d=nextDirection(s);int ink=s.get(INK)?1:0;
        return d==null?(TapeProgram.running(s.get(HEAD))?14+ink:ink):2+d.getId()*2+ink;
    }
    private static BlockState stopped(BlockState s){return s.with(HEAD,BLOCKED).with(ROUTE,0);}
    @Override protected void onBlockAdded(BlockState state,World world,BlockPos pos,BlockState oldState,boolean notify){if(!oldState.isOf(this))refresh(world,pos);}
    @Override protected void neighborUpdate(BlockState state,World world,BlockPos pos,Block source,BlockPos sourcePos,boolean notify){refresh(world,pos);}
    private void refresh(World world,BlockPos pos){
        if(!(world instanceof ServerWorld server))return;
        var state=world.getBlockState(pos);if(!state.isOf(this))return;
        boolean powered=world.isReceivingRedstonePower(pos);var pair=neighbors(world,pos);
        var next=state.with(POWERED,powered);
        if(TapeProgram.running(state.get(HEAD))&&!valid(state,pair))next=stopped(next);
        if(powered&&!state.get(POWERED)&&pair!=null){
            var positive=pair.positive();next=oriented(next.with(HEAD,1),positive,pair.other(positive));
            server.scheduleBlockTick(pos,this,STEP_TICKS);
        }
        if(next!=state)world.setBlockState(pos,next,Block.NOTIFY_LISTENERS);
    }
    @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit){
        if(!world.isClient)world.setBlockState(pos,state.cycle(INK),Block.NOTIFY_LISTENERS);
        return ActionResult.SUCCESS;
    }
    @Override protected void scheduledTick(BlockState s,ServerWorld world,BlockPos pos,Random random){
        int head=s.get(HEAD);if(!TapeProgram.running(head))return;
        if(!valid(s,neighbors(world,pos))){world.setBlockState(pos,stopped(s),Block.NOTIFY_LISTENERS);return;}
        var step=TapeProgram.transition(head,s.get(INK));
        if(step.move()==0){world.setBlockState(pos,s.with(HEAD,TapeProgram.HALT).with(ROUTE,0),Block.NOTIFY_LISTENERS);return;}
        var direction=nextDirection(s);var target=pos.offset(direction);
        var destination=world.getBlockState(target);
        var pair=neighbors(world,target);var incoming=direction.getOpposite();
        if(!destination.isOf(this)||pair==null||!pair.contains(incoming)){
            world.setBlockState(pos,stopped(s),Block.NOTIFY_LISTENERS);return;
        }
        if(TapeProgram.running(destination.get(HEAD))){
            world.setBlockState(pos,stopped(s),Block.NOTIFY_LISTENERS);
            world.setBlockState(target,stopped(destination),Block.NOTIFY_LISTENERS);return;
        }
        world.setBlockState(pos,s.with(INK,step.write()).with(HEAD,0).with(ROUTE,0),Block.NOTIFY_LISTENERS);
        var next=destination.with(HEAD,step.next());
        // After a positive step the incoming face is logical left; after a negative step it is right.
        next=step.next()==TapeProgram.HALT?next.with(ROUTE,0):step.move()>0
                ?oriented(next,pair.other(incoming),incoming):oriented(next,incoming,pair.other(incoming));
        world.setBlockState(target,next,Block.NOTIFY_LISTENERS);
        if(step.next()!=TapeProgram.HALT)world.scheduleBlockTick(target,this,STEP_TICKS);
    }
}
