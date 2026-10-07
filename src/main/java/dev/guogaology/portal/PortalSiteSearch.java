package dev.guogaology.portal;

import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.Heightmap;
import java.util.*;

/** Reuse gates, find real ground, then use a shore/water/void platform, in bounded tick steps. */
final class PortalSiteSearch {
    static final int RADIUS=24,SCAN_HEIGHT_PER_STEP=32;
    private record Candidate(BlockPos pos,PortalTravel.Site site) {}
    private final PortalTravel.Target target;
    private final List<PortalState.Gate> existing;
    private final List<BlockPos> columns=new ArrayList<>(),candidates=new ArrayList<>();
    private final List<Candidate> platforms=new ArrayList<>();
    private final Set<BlockPos> seen=new HashSet<>();
    private int gateIndex,columnIndex,candidateIndex,phase,scanY=Integer.MIN_VALUE;
    private boolean finished;
    private BlockPos exit;
    PortalSiteSearch(PortalTravel.Target target){
        this.target=target;
        existing=PortalTravel.nearbyGates(target.world(),target.desired(),target.kind());
        var desired=target.desired();int cx=desired.getX()>>4,cz=desired.getZ()>>4;
        columns.add(PortalTravel.boundedCenter(desired));
        // The entire build footprint and its block-update rim stay inside loaded chunks.
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
    private void candidate(BlockPos column,int y){
        var world=target.world();
        if(y<world.getBottomY()+5||y+5>=world.getTopY())return;
        var pos=new BlockPos(column.getX(),y,column.getZ());
        if(seen.add(pos))candidates.add(pos);
    }
    private void scanColumn(ServerWorld world,BlockPos column){
        if(scanY==Integer.MIN_VALUE){
            var chunk=world.getChunkManager().getWorldChunk(column.getX()>>4,column.getZ()>>4);
            if(chunk==null){columnIndex++;return;}
            scanY=Math.min(world.getTopY()-6,chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,column.getX()&15,column.getZ()&15));
            int top=world.getBottomY()+5;
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)top=Math.max(top,chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(column.getX()+x)&15,(column.getZ()+z)&15)+1);
            // Emergency candidates only; any real ground/water contact outranks these.
            candidate(column,target.desired().getY());
            candidate(column,Math.min(world.getTopY()-6,top+1));
        }
        for(int n=0;n<SCAN_HEIGHT_PER_STEP&&scanY>=world.getBottomY()+4;n++,scanY--){
            var floor=new BlockPos(column.getX(),scanY,column.getZ());
            if(!PortalTravel.clearable(world,floor.up()))continue;
            if(PortalTravel.ground(world,floor)){
                for(int up=0;up<=PortalTravel.FOUNDATION_DEPTH;up++)candidate(column,scanY+1+up);
            }else if(world.getBlockState(floor).isOf(net.minecraft.block.Blocks.WATER))candidate(column,scanY+2);
        }
        if(scanY<world.getBottomY()+4){columnIndex++;scanY=Integer.MIN_VALUE;}
    }
    void step(){
        if(finished)return;
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
            if(columnIndex<columns.size()){scanColumn(world,columns.get(columnIndex));return;}
            candidates.sort(Comparator.comparingDouble(p->distance(p,desired)));phase=2;
        }
        if(phase==2){
            if(candidateIndex<candidates.size()){
                var pos=candidates.get(candidateIndex++);var site=PortalTravel.assessSite(world,pos);
                // Nearest completely flat supported site is already optimal.
                if(site.quality()==3){exit=PortalTravel.buildGate(world,pos,target.kind());finished=exit!=null;}
                else if(site.quality()>0)platforms.add(new Candidate(pos,site));
                return;
            }
            // Slopes before ledges, ledges before water, water before empty sky.
            // Less earthwork wins ties, avoiding a needless pedestal above flat ground.
            platforms.sort(Comparator.<Candidate>comparingInt(c->-c.site().quality())
                    .thenComparingInt(c->-c.site().ground()).thenComparingInt(c->-c.site().water())
                    .thenComparingInt(c->c.site().fill()).thenComparingDouble(c->distance(c.pos(),desired)));
            candidateIndex=0;phase=3;
        }
        if(candidateIndex<platforms.size()){
            exit=PortalTravel.buildGate(world,platforms.get(candidateIndex++).pos(),target.kind());finished=exit!=null;
        }else finished=true;
    }
}
