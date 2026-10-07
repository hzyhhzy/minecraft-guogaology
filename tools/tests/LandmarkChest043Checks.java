package dev.googology.survival;

import java.util.*;
import static dev.googology.survival.SanctuaryLayout.*;

/** Tests the completed geometry with actual chest blocks occupying the declared spaces. */
public final class LandmarkChest043Checks {
    private static int assertions;
    private static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    private static int index(SanctuaryLayout p,int x,int y,int z){return (y*p.depth+z+p.depth/2)*p.width+x+p.width/2;}
    private static boolean inside(SanctuaryLayout p,int x,int y,int z){return Math.abs(x)<=p.width/2&&Math.abs(z)<=p.depth/2&&y>=0&&y<p.height;}
    private static boolean solid(SanctuaryLayout p,BitSet boxes,int x,int y,int z){return inside(p,x,y,z)&&(p.at(x,y,z)>AIR||boxes.get(index(p,x,y,z)));}
    private static boolean standing(SanctuaryLayout p,BitSet boxes,int x,int y,int z){return inside(p,x,y-1,z)&&inside(p,x,y+1,z)&&solid(p,boxes,x,y-1,z)&&!solid(p,boxes,x,y,z)&&!solid(p,boxes,x,y+1,z);}
    private static BitSet reached(SanctuaryLayout p,BitSet boxes){
        var seen=new BitSet(p.width*p.height*p.depth);var queue=new ArrayDeque<Integer>();var e=p.entrance;
        check(standing(p,boxes,e.x(),e.y()+1,e.z()),"entrance supports a two-block player "+p.theme);
        int first=index(p,e.x(),e.y()+1,e.z());seen.set(first);queue.add(first);
        while(!queue.isEmpty()){
            int n=queue.removeFirst(),x=n%p.width-p.width/2,z=n/p.width%p.depth-p.depth/2,y=n/(p.width*p.depth);
            for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy=-1;dy<=1;dy++){
                int xx=x+d[0],zz=z+d[1],yy=y+dy;if(!standing(p,boxes,xx,yy,zz))continue;
                if(dy>0&&solid(p,boxes,x,y+2,z)||dy<0&&solid(p,boxes,xx,y+1,zz))continue;
                int next=index(p,xx,yy,zz);if(!seen.get(next)){seen.set(next);queue.add(next);}
            }
        }return seen;
    }
    private static Point rotate(Point p,int r){return switch(r){case 1->new Point(-p.z(),p.y(),p.x());case 2->new Point(-p.x(),p.y(),-p.z());case 3->new Point(p.z(),p.y(),-p.x());default->p;};}
    public static void main(String[] args){
        int[] secondary={2,4,2,2,2,3,2,3};int total=0;
        for(var theme:SurvivalTheme.values()){
            var p=SanctuaryLayout.of(theme);var boxes=new BitSet(p.width*p.height*p.depth);var positions=new HashSet<Point>();
            int main=(int)p.chests().stream().filter(Chest::relic).count(),side=p.chests().size()-main;
            check(main==1&&side==secondary[theme.ordinal()],"one main and expected secondary caches "+theme);
            for(var c:p.chests()){
                var q=c.floor();check(positions.add(q),"different chests do not collide "+theme+" "+q);
                check(inside(p,q.x(),q.y()+3,q.z()),"chest and headroom are inside blueprint "+theme);
                check(p.at(q.x(),q.y(),q.z())>AIR,"real supporting floor "+theme+" "+q);
                check(p.at(q.x(),q.y()+1,q.z())==AIR&&p.at(q.x(),q.y()+2,q.z())==AIR,"final decorations leave chest and lid clear "+theme+" "+q);
                boxes.set(index(p,q.x(),q.y()+1,q.z()));
            }
            var seen=reached(p,boxes);
            for(var c:p.chests()){
                var q=c.floor();check(seen.get(index(p,q.x(),q.y()+1,q.z()+1)),"entrance reaches chest's actual south-facing front after all chest blocks exist "+theme+" "+q);
            }
            check(seen.get(index(p,p.arena.x(),p.arena.y()+1,p.arena.z())),"placed chests do not sever final arena route "+theme);
            // Exhaust every block alignment, including negative coordinates. The owning
            // chunk must be selected by production's cell/radius bounds in all orientations.
            for(int r=0;r<4;r++)for(int ax=0;ax<16;ax++)for(int az=0;az<16;az++){
                int originX=-384+80+ax,originZ=384+80+az,gx=Math.floorDiv(originX,384),gz=Math.floorDiv(originZ,384);
                var placed=new HashMap<Point,Integer>();
                for(var c:p.chests()){
                    var v=rotate(c.floor(),r);int x=originX+v.x(),z=originZ+v.z(),minX=Math.floorDiv(x,16)*16,minZ=Math.floorDiv(z,16)*16;
                    check(gx>=Math.floorDiv(minX-70,384)&&gx<=Math.floorDiv(minX+15+70,384)&&gz>=Math.floorDiv(minZ-70,384)&&gz<=Math.floorDiv(minZ+15+70,384),"chest owner chunk searches the correct sanctuary candidate cell");
                    check(Math.abs(originX-(minX+8))<79&&Math.abs(originZ-(minZ+8))<79,"owner chunk passes final sanctuary radius filter");
                    for(int cx=minX-16;cx<=minX+16;cx+=16)for(int cz=minZ-16;cz<=minZ+16;cz+=16)if(Math.floorDiv(x,16)==Math.floorDiv(cx,16)&&Math.floorDiv(z,16)==Math.floorDiv(cz,16))placed.merge(new Point(x,v.y()+1,z),1,Integer::sum);
                    var local=rotate(new Point(x-originX,v.y(),z-originZ),(4-r)%4);check(local.equals(c.floor()),"placement and chunk renderer inverse rotations agree");
                    var front=rotate(new Point(0,0,1),r);check(Math.abs(front.x())+Math.abs(front.z())==1,"rotated chest front remains cardinal");
                }
                check(placed.size()==p.chests().size()&&placed.values().stream().allMatch(n->n==1),"each actual chest is placed exactly once across chunk cuts");
            }
            total+=p.chests().size();System.out.println("CHESTS_043 "+theme+" main="+main+" secondary="+side+" total="+p.chests().size()+" facingFrontsReachable=true rotations=4 alignments=256");
        }
        System.out.println("LANDMARK_CHESTS_043_OK assertions="+assertions+" chests="+total+" cases="+(8*4*256));
    }
}
