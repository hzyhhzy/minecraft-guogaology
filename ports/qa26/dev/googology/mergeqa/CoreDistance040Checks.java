package dev.googology.mergeqa;

import dev.googology.CoreGrades;
import dev.googology.client.CoreAnimationRenderer;
import dev.googology.client.CoreMeshModels;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.concurrent.ConcurrentHashMap;

/** Development-only real baked-body / submitted-distance checks and photo site. */
public final class CoreDistance040Checks {
    private static final ConcurrentHashMap<BlockPos,CoreAnimationRenderer.DistanceSample> SEEN=new ConcurrentHashMap<>();
    private static int expectedSubmitted;
    private CoreDistance040Checks(){}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}

    /** Run on the client thread after resource baking, before the photo scenes. */
    public static void audit(Minecraft client){
        client.options.renderDistance().set(8);
        require(CoreAnimationRenderer.ANIMATION_DISTANCE==64,"animation radius remains exactly 64");
        require(CoreAnimationRenderer.distanceMode(64*64,128)==CoreAnimationRenderer.DistanceMode.ANIMATED,"64 block boundary is animated");
        require(CoreAnimationRenderer.distanceMode(65*65,128)==CoreAnimationRenderer.DistanceMode.STATIC,"65 blocks uses static");
        require(CoreAnimationRenderer.distanceMode(100*100,128)==CoreAnimationRenderer.DistanceMode.STATIC,"100 blocks within ordinary view uses static");
        require(CoreAnimationRenderer.distanceMode(129*129,128)==CoreAnimationRenderer.DistanceMode.NONE,"no body beyond ordinary view");
        int checked=0;expectedSubmitted=0;
        for(String root:CoreGrades.ROOTS)for(var block:CoreGrades.levels(root)){
            var model=client.getModelManager().getBlockStateModelSet().get(block.defaultBlockState());
            var info=CoreMeshModels.distanceInfo(model);
            require(info!=null&&info.fallbackFaces()>0,"Voxy body fallback survives: "+root);
            if(CoreGrades.upper(block)){
                if(info.movingFaces()>0){
                    require(info.staticFaces()>0,"cached static body is nonempty: "+root);
                    require(info.fallbackFaces()>info.shellFaces(),"fallback includes body rather than only glass: "+root);
                    expectedSubmitted++;
                }else{
                    // Ordinal Lv2 is intentionally still: its entire crystal
                    // belongs to part0 and remains in the ordinary chunk mesh.
                    require(info.shellFaces()>0&&info.fallbackFaces()==info.shellFaces(),"complete fixed body retained: "+root);
                }
            }
            checked++;
        }
        require(checked==28,"all 28 core appearances inspected");
        CoreAnimationRenderer.distanceObserver=sample->{
            if(sample.x()>=400&&sample.x()<=418&&sample.z()==400)SEEN.put(new BlockPos(sample.x(),sample.y(),sample.z()),sample);
        };
        System.out.println("CORE_DISTANCE_BAKED_OK 28 cores; 64 animated, 65/100 static; fallback retained");
    }

    /** Server-thread scene setup. Distances 12 / 65 / 100 make useful photos. */
    public static void display(ServerPlayer player,int distance){
        var level=player.level().getServer().overworld();
        for(var pos:BlockPos.betweenClosed(398,229,397,420,240,405))level.setBlock(pos,pos.getY()==229?Blocks.SMOOTH_QUARTZ.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        int index=0;
        for(String root:CoreGrades.ROOTS)for(var block:CoreGrades.levels(root))if(CoreGrades.upper(block)){
            level.setBlock(new BlockPos(400+index%7*3,231+index/7*3,400),block.defaultBlockState(),2);index++;
        }
        SEEN.clear();player.setGameMode(GameType.CREATIVE);
        player.teleport(new TeleportTransition(level,new Vec3(409.5,233,400.5+distance),Vec3.ZERO,180,0,TeleportTransition.DO_NOTHING));
        player.getAbilities().flying=true;player.onUpdateAbilities();
    }

    /** Run client-side after the scene has rendered: observes real BER submits. */
    public static void assertRendered(int distance){
        require(!SEEN.isEmpty(),"no crystal body submitted at distance "+distance);
        var expected=distance>64?CoreAnimationRenderer.DistanceMode.STATIC:CoreAnimationRenderer.DistanceMode.ANIMATED;
        if(expected==CoreAnimationRenderer.DistanceMode.STATIC)require(SEEN.size()==expectedSubmitted,"all moving cores must submit their static bodies: "+SEEN.size()+" / "+expectedSubmitted);
        for(var sample:SEEN.values()){
            require(sample.mode()==expected,"wrong actual submission mode at "+sample);
            require(sample.faces()>0,"empty submitted body at "+sample);
            if(expected==CoreAnimationRenderer.DistanceMode.STATIC)require(sample.batches()==1,"distant body must be one cached batch");
        }
        System.out.println("CORE_DISTANCE_SUBMIT_OK distance="+distance+" bodies="+SEEN.size()+" mode="+expected);
    }
}
