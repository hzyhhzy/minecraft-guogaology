package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.survival.*;
import dev.googology.world.WorldNoise;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import java.lang.reflect.*;
import java.util.*;

/** Runs the production chunk renderer against native states and native chest block entities.
 * Only its write sink is recorded: no real world or production geometry is modified.
 */
final class LandmarkChest043Checks {
    private static int checks,renderedChunks,writes,boxes;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void run(ServerPlayer player){
        checks=renderedChunks=writes=boxes=0;
        int[] secondary={2,4,2,2,2,3,2,3};
        for(var theme:SurvivalTheme.values())for(int rotation=0;rotation<4;rotation++){
            var site=new SurvivalStructures.Site(-512+rotation*5,16,512+(rotation*7)%16,theme,-4L+rotation);
            var recorder=new Recorder(player.level(),site);var layout=site.layout();
            // Owners first, surrounding chunks afterwards: later chunk work must never
            // erase a chest that an earlier chunk has already placed.
            var owners=new HashSet<ChunkPos>();var chunks=new HashSet<ChunkPos>();
            for(var chest:layout.chests()){
                var pos=site.position(chest.floor(),1);var chunk=ChunkPos.containing(pos);owners.add(chunk);
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)chunks.add(new ChunkPos(chunk.x()+dx,chunk.z()+dz));
            }
            var ordered=new ArrayList<>(chunks);ordered.sort(Comparator.<ChunkPos>comparingInt(c->owners.contains(c)?0:1).thenComparingInt(ChunkPos::x).thenComparingInt(ChunkPos::z));
            for(var chunk:ordered){recorder.active=chunk;SurvivalStructures.render(recorder.world,site,chunk);renderedChunks++;}
            check(recorder.chests.size()==1+secondary[theme.ordinal()],"actual final chest entity count "+theme+" / "+rotation);
            Direction facing=Direction.SOUTH;for(int i=0;i<rotation;i++)facing=facing.getClockWise();
            int main=0,side=0,index=0;
            for(var cache:layout.chests()){
                var pos=site.position(cache.floor(),1);var state=recorder.state(pos);var chest=recorder.chests.get(pos.asLong());
                check(state.is(Blocks.CHEST)&&chest!=null,"real chunk renderer creates chest and block entity "+theme+" "+cache);
                check(state.getValue(ChestBlock.FACING)==facing,"chest facing follows rotated accessible front");
                check(recorder.chestWrites.getOrDefault(pos.asLong(),0)==1,"exactly one owning chunk places each chest");
                check(recorder.state(pos.above()).isAir(),"completed native materials leave lid unobstructed");
                check(recorder.state(pos.below()).isFaceSturdy(recorder.world,pos.below(),Direction.UP),"native floor actually supports chest");
                var front=pos.relative(facing);check(recorder.state(front).getCollisionShape(recorder.world,front).isEmpty()&&recorder.state(front.above()).getCollisionShape(recorder.world,front.above()).isEmpty(),"actual facing side fits player collision");
                check(recorder.state(front.below()).isFaceSturdy(recorder.world,front.below(),Direction.UP),"actual front has supporting walkable floor");
                var loot=ResourceKey.create(Registries.LOOT_TABLE,GoogologyMod.id("chests/"+theme.id+(cache.relic()?"_sanctum":"_ruin")));
                check(loot.equals(chest.getLootTable()),"actual chest receives correct primary/secondary loot key");
                check(chest.getLootTableSeed()==WorldNoise.mix(site.hash()+index++*71),"loot seed uses stable global chest index, independent of chunk order");
                check(player.level().getServer().reloadableRegistries().getLootTable(loot)!=LootTable.EMPTY,"assigned actual loot table is loaded");
                if(cache.relic())main++;else side++;boxes++;
            }
            check(main==1&&side==secondary[theme.ordinal()],"native main/secondary counts "+theme);
            System.out.println("LANDMARK_CHEST_NATIVE "+theme+" rotation="+rotation+" main="+main+" secondary="+side+" ownerChunks="+owners.size()+" renderedChunks="+ordered.size());
        }
        System.out.println("LANDMARK_CHESTS_NATIVE_043_OK checks="+checks+" themes=8 rotations=4 chests="+boxes+" chunks="+renderedChunks+" clippedWrites="+writes+" nativeBlockEntities=true lootAssigned=true actualFacingClear=true");
    }
    private static final class Recorder implements InvocationHandler {
        final ServerLevel level;
        final WorldGenLevel world;
        final Set<Long> watched=new HashSet<>();
        final Map<Long,BlockState> states=new HashMap<>();
        final Map<Long,ChestBlockEntity> chests=new HashMap<>();
        final Map<Long,Integer> chestWrites=new HashMap<>();
        ChunkPos active;
        Recorder(ServerLevel level,SurvivalStructures.Site site){
            this.level=level;
            for(var cache:site.layout().chests()){
                var p=site.position(cache.floor(),1);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)for(int dy=-2;dy<=4;dy++)watched.add(p.offset(dx,dy,dz).asLong());
            }
            // Solid initial terrain proves that AIR writes actually carve these cells.
            for(long pos:watched)states.put(pos,Blocks.STONE.defaultBlockState());
            world=(WorldGenLevel)Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),new Class<?>[]{WorldGenLevel.class},this);
        }
        BlockState state(BlockPos pos){return states.getOrDefault(pos.asLong(),Blocks.AIR.defaultBlockState());}
        @Override public Object invoke(Object proxy,Method method,Object[] args){
            return switch(method.getName()){
                case "getLevel"->level;
                case "getSeed"->level.getSeed();
                case "getMinY"->-64;
                case "getMaxY"->319;
                case "getHeight"->384;
                case "isOutsideBuildHeight"->{int y=args[0] instanceof BlockPos p?p.getY():(Integer)args[0];yield y< -64||y>319;}
                case "getBlockState"->state((BlockPos)args[0]);
                case "getBlockEntity"->chests.get(((BlockPos)args[0]).asLong());
                case "setBlock"->{
                    var p=(BlockPos)args[0];var state=(BlockState)args[1];writes++;
                    if(Math.floorDiv(p.getX(),16)!=active.x()||Math.floorDiv(p.getZ(),16)!=active.z())throw new AssertionError("renderer wrote outside its active chunk: "+active+" -> "+p);
                    long key=p.asLong();
                    if(watched.contains(key)){
                        states.put(key,state);chests.remove(key);
                        if(state.is(Blocks.CHEST)){chests.put(key,new ChestBlockEntity(p.immutable(),state));chestWrites.merge(key,1,Integer::sum);}
                    }else if(state.is(Blocks.CHEST))throw new AssertionError("undeclared chest placement "+p);
                    yield true;
                }
                case "toString"->"Landmark043RecordedWorld";
                case "hashCode"->System.identityHashCode(proxy);
                case "equals"->proxy==args[0];
                default->throw new AssertionError("Unrecorded WorldGenLevel call "+method);
            };
        }
    }
}
