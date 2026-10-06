package dev.googology.portal;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;
import java.util.*;

/** PortalForcer's nearest-existing / nearest-clear-site / safe-platform policy, in small tick steps. */
final class PortalSiteSearch {
    static final int RADIUS=24;
    private final PortalTravel.Target target;
    private final List<PortalState.Gate> existing;
    private final List<BlockPos> columns=new ArrayList<>(),candidates=new ArrayList<>();
    private final List<BlockPos> supported=new ArrayList<>(),platforms=new ArrayList<>();
    private final Set<BlockPos> skySites=new HashSet<>();
    private int gateIndex,columnIndex,candidateIndex,phase;
    private boolean finished;
    private BlockPos exit;
    PortalSiteSearch(PortalTravel.Target target){
        this.target=target;
        existing=PortalTravel.nearbyGates(target.world(),target.desired(),target.kind());
        var desired=target.desired();int cx=desired.getX()>>4,cz=desired.getZ()>>4;
        columns.add(PortalTravel.boundedCenter(desired));
        for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++)for(int dx:new int[]{5,8,10})for(int dz:new int[]{5,8,10}){
            var pos=new BlockPos((x<<4)+dx,desired.getY(),(z<<4)+dz);
            if(PortalTravel.inSearchWindow(desired,pos)&&!columns.contains(pos))columns.add(pos);
        }
        columns.sort(Comparator.comparingDouble(p->distance(p,desired)));
    }
    boolean finished(){return finished;}
    BlockPos exit(){return exit;}
    private static double distance(BlockPos a,BlockPos b){
        double x=(double)a.getX()-b.getX(),y=(double)a.getY()-b.getY(),z=(double)a.getZ()-b.getZ();return x*x+y*y+z*z;
    }
    void step(){
        var world=target.world();var desired=target.desired();
        if(phase==0){
            if(gateIndex<existing.size()){
                var gate=existing.get(gateIndex++);
                if(PortalRitual.complete(world,gate)&&PortalTravel.isSafe(world,gate.center().add(0,0,3))){exit=gate.center();finished=true;}
                return;
            }
            phase=1;
        }
        if(phase==1){
            if(columnIndex<columns.size()){
                var column=columns.get(columnIndex++);int top=world.getBottomY()+6;
                var chunk=world.getChunkManager().getWorldChunk(column.getX()>>4,column.getZ()>>4);
                if(chunk==null)return;
                for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)top=Math.max(top,chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(column.getX()+x)&15,(column.getZ()+z)&15)+1);
                int ceiling=world.getTopY()-12;
                // Far above ordinary terrain when needed, with twelve blocks of roof clearance.
                int sky=Math.min(ceiling,Math.max(160,Math.max(top+2,desired.getY()+16)));
                skySites.add(new BlockPos(column.getX(),sky,column.getZ()));
                for(int y:new int[]{desired.getY(),desired.getY()-32,desired.getY()+32,top,sky}){
                    var pos=new BlockPos(column.getX(),y,column.getZ());if(!candidates.contains(pos))candidates.add(pos);
                }
                return;
            }
            candidates.sort(Comparator.comparingDouble(p->distance(p,desired)));phase=2;
        }
        if(phase==2){
            if(candidateIndex<candidates.size()){
                var pos=candidates.get(candidateIndex++);
                int quality=PortalTravel.siteQuality(world,pos);
                if(quality==2)supported.add(pos);else if(quality==1)platforms.add(pos);
                return;
            }
            // Complete all near-site comparisons before choosing the nearest valid site.
            platforms.sort(Comparator.comparingInt((BlockPos p)->skySites.contains(p)?0:1).thenComparingDouble(p->distance(p,desired)));
            supported.addAll(platforms);candidateIndex=0;phase=3;
        }
        if(candidateIndex<supported.size()){
            var pos=supported.get(candidateIndex++);
            if(PortalTravel.siteQuality(world,pos)>0){exit=PortalTravel.buildGate(world,pos,target.kind());finished=true;}
        }else finished=true;
    }
}
