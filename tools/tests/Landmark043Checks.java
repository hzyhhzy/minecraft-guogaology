package dev.guogaology.survival;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import static dev.guogaology.survival.SanctuaryLayout.*;

/** Exact blueprint harvest counts, retained furnishings, and real two-block walking routes. */
public final class Landmark043Checks {
    private static int assertions;
    private static void check(boolean condition,String text){assertions++;if(!condition)throw new AssertionError(text);}
    private static int index(SanctuaryLayout p,int x,int y,int z){return (y*p.depth+z+p.depth/2)*p.width+x+p.width/2;}
    private static boolean standing(SanctuaryLayout p,int x,int y,int z){return Math.abs(x)<=p.width/2&&Math.abs(z)<=p.depth/2&&y>0&&y<p.height-1
            &&p.at(x,y-1,z)>AIR&&p.at(x,y,z)<=AIR&&p.at(x,y+1,z)<=AIR;}
    private static BitSet reachable(SanctuaryLayout p){
        var reached=new BitSet();var queue=new ArrayDeque<Integer>();var e=p.entrance;
        check(standing(p,e.x(),e.y()+1,e.z()),"clear entrance "+p.theme);
        int first=index(p,e.x(),e.y()+1,e.z());reached.set(first);queue.add(first);
        while(!queue.isEmpty()){
            int n=queue.removeFirst(),x=n%p.width-p.width/2,z=n/p.width%p.depth-p.depth/2,y=n/(p.width*p.depth);
            for(var d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy=-1;dy<=1;dy++){
                int xx=x+d[0],zz=z+d[1],yy=y+dy;if(!standing(p,xx,yy,zz))continue;
                int next=index(p,xx,yy,zz);if(!reached.get(next)){reached.set(next);queue.add(next);}
            }
        }
        return reached;
    }
    private static int count(SanctuaryLayout p,byte code){int n=0;for(int y=0;y<p.height;y++)for(int z=-p.depth/2;z<=p.depth/2;z++)for(int x=-p.width/2;x<=p.width/2;x++)if(p.at(x,y,z)==code)n++;return n;}
    public static void main(String[] args)throws Exception{
        if(args.length==2&&args[0].equals("--write-baseline")){writeBaseline(Path.of(args[1]));return;}
        for(var theme:SurvivalTheme.values()){
            var p=SanctuaryLayout.of(theme);var reached=reachable(p);
            check(p.furnishings().size()>=15,"retained complete themed furnishings "+theme);
            check(p.interiorResources().size()==8,"exactly eight authored regional cores "+theme);
            check(p.interiorResources().stream().allMatch(InteriorResource::regional),"no decorative finished crystal "+theme);
            check(count(p,CRYSTAL)==0,"no finished crystal marker anywhere "+theme);
            var levels=new TreeSet<Integer>();p.interiorResources().forEach(q->levels.add(q.pos().y()));
            check(levels.size()>=2,"core exhibits are distributed across floors "+theme+" "+levels);
            int midpoint=theme==SurvivalTheme.WEAVER?16:(levels.first()+levels.last())/2;
            check(p.interiorResources().stream().filter(q->q.pos().y()>=midpoint).count()>=4,"core exhibits have upper-floor bias "+theme);
            check(reached.get(index(p,p.arena.x(),p.arena.y()+1,p.arena.z())),"walkable arena "+theme);
            for(var f:p.furnishings())if(!f.suspended()){
                boolean accessible=false;var q=f.floor();
                for(int dx=-f.radiusX()-1;dx<=f.radiusX()+1;dx++)for(int dz=-f.radiusZ()-1;dz<=f.radiusZ()+1;dz++)
                    if(Math.abs(dx)==f.radiusX()+1||Math.abs(dz)==f.radiusZ()+1)accessible|=reached.get(index(p,q.x()+dx,q.y()+1,q.z()+dz));
                check(accessible,"walkable furniture "+theme+" "+f);
            }
            for(var chest:p.chests()){
                boolean accessible=false;var q=chest.floor();
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)accessible|=reached.get(index(p,q.x()+dx,q.y()+1,q.z()+dz));
                check(accessible,"walkable chest "+theme+" "+q);
            }
            for(var q:p.combatPoints()){
                boolean accessible=false;for(int y=Math.max(1,q.y()-4);y<=Math.min(p.height-6,q.y()+5);y++)
                    if(standing(p,q.x(),y,q.z())&&reached.get(index(p,q.x(),y,q.z())))accessible=true;
                check(accessible,"walkable battle position "+theme+" "+q);
            }
            switch(theme){
                case POWER->{
                    check(count(p,POWER_CORE)==8,"eight Power cores");
                    for(byte m:new byte[]{RED,GREEN,BLUE})check(count(p,m)>=3,"ordinary TREE nodes remain "+m);
                    check(p.height==250&&p.arena.y()==209,"highest tenth-storey battle floor, same tower height");
                    check(p.route().getLast().equals(p.arena),"main ascent finishes at top arena");
                    check(p.chests().stream().filter(Chest::relic).allMatch(c->c.floor().y()==209),"main reward moves with arena");
                    check(p.combatPoints().stream().allMatch(q->q.y()>=209&&q.y()<=212),"all battle positions move upstairs");
                    check(p.furnishings().stream().filter(f->f.floor().y()==186&&(f.kind().equals("iteration_arrow_arch")||f.kind().equals("graham_sapling_study")||f.kind().equals("tree_function_model")||f.kind().equals("scg_graph_model"))).count()==4,"ninth-floor four-model gallery");
                    check(p.at(0,185,0)==FLOOR&&p.at(0,186,0)==FLOOR,"ordinary ninth-storey gallery floor restored");
                    for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)if(x*x+z*z<=16*16)for(int y=214;y<=227;y++)
                        check(p.at(x,y,z)<=AIR,"highest arena has fourteen blocks of open headroom, no restored shell/furniture overlap");
                    for(var c:p.chests())if(c.relic()){var q=c.floor();check(p.at(q.x(),q.y(),q.z())>AIR&&p.at(q.x(),q.y()+1,q.z())==AIR&&p.at(q.x(),q.y()+2,q.z())==AIR,"main chest has actual supported placement and lid clearance");}
                }
                case ABSENCE->{
                    check(count(p,PHANTOM_EMPTY)==8,"only eight authored Absence cores");
                    for(byte m:new byte[]{PHANTOM_PSI,PHANTOM_Z,PHANTOM_FOS,PHANTOM_FFFZ})
                        check(count(p,m)>0,"LHO natural symbol materials remain "+m);
                }
                case GUOGAO->{
                    check(p.numberLamps().size()==87&&p.emojiLampCenters().size()==24,"exterior lamp silhouettes retained");
                    for(var lamp:p.numberLamps()){
                        var q=lamp.center();check(p.at(q.x(),q.y(),q.z())==PRIMARY_RAW,"ordinary resin lamp center");
                        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)
                            if(dx!=0||dy!=0||dz!=0)check(p.at(q.x()+dx,q.y()+dy,q.z()+dz)==GLASS,"ordinary glass lamp chamber");
                    }
                    for(var q:p.emojiLampCenters())check(p.at(q.x(),q.y(),q.z())==LIGHT,"emoji lamp has no free heart");
                    check(p.finaleCores().size()==8&&p.finaleRelics().size()==17,"eight final tributes and retained companion ornaments");
                    check(count(p,GUOGAO_HEART)==9,"eight indoor hearts plus one finale heart");
                }
                default->check(count(p,SanctuaryResources.material(theme,0))==8,"no extra architectural cores "+theme);
            }
            if(theme==SurvivalTheme.FRONTIER)
                check(p.at(-45,72,0)==SECONDARY_RAW&&p.at(45,72,0)==SECONDARY_RAW,"inert ordinary tape endcaps");
            if(theme==SurvivalTheme.WEAVER){
                int functional=0;for(var f:p.furnishings())if(f.kind().startsWith("playable_iblp_")){
                    var q=f.floor();int cores=0;for(int dx=-f.radiusX();dx<=f.radiusX();dx++)for(int dz=-f.radiusZ();dz<=f.radiusZ();dz++)if(p.at(q.x()+dx,q.y()+3,q.z()+dz)==LAVER_CORE)cores++;
                    check(cores==(f.kind().equals("playable_iblp_initial")?2:1),"exact playable table roots/marks retained");functional+=cores;
                }check(functional==5,"five functional table cores retained within the restored fifty");
            }
            check(p.furnishings().stream().anyMatch(f->f.kind().equals("core_"+theme.id+"_reliquary")),"purpose-built one-core devices "+theme);
            for(var reward:p.interiorResources()){
                var q=reward.pos();check(p.authoredResourceMounts().contains(q),"retained rewards occupy original authored mounts "+theme);
                check(reward.regional()?SanctuaryResources.isRegional(theme,p.at(q.x(),q.y(),q.z())):p.at(q.x(),q.y(),q.z())==CRYSTAL,"recorded restored reward agrees with final material "+q);
            }

            System.out.println(theme+": authoredRegional="+p.interiorResources().stream().filter(InteriorResource::regional).count()+", authoredCrystals=0, furniture="+p.furnishings().size()+", reachable="+reached.cardinality());
        }
        System.out.println("LANDMARK_RESOURCES_055_OK: "+assertions+" assertions");
    }
    private static void writeBaseline(Path folder)throws Exception{
        Files.createDirectories(folder);
        for(var theme:SurvivalTheme.values()){
            var p=SanctuaryLayout.of(theme);
            try(var out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(folder.resolve(theme.id+".vox"))))){
                out.writeInt(p.width);out.writeInt(p.height);out.writeInt(p.depth);
                for(int y=0;y<p.height;y++)for(int z=-p.depth/2;z<=p.depth/2;z++)for(int x=-p.width/2;x<=p.width/2;x++)out.writeByte(p.at(x,y,z));
            }
            Files.writeString(folder.resolve(theme.id+"-interiors.txt"),p.furnishings().toString());
            Files.writeString(folder.resolve(theme.id+"-resources.txt"),p.interiorResources().toString());
        }
        System.out.println("PRE_REDUCTION_BASELINE_SAVED "+folder);
    }
    private static void compareBaseline(SanctuaryLayout p,Path file)throws Exception{
        byte[] old=Files.readAllBytes(file);
        try(var in=new DataInputStream(new ByteArrayInputStream(old))){check(in.readInt()==p.width&&in.readInt()==p.height&&in.readInt()==p.depth,"same blueprint size");}
        int changes=0;String first="";
        for(int y=0;y<p.height;y++)for(int z=-p.depth/2;z<=p.depth/2;z++)for(int x=-p.width/2;x<=p.width/2;x++){
            byte before=old[12+index(p,x,y,z)],after=p.at(x,y,z);
            if(before!=after){if(changes==0)first=x+","+y+","+z+" "+before+"->"+after;changes++;}
            if(p.theme==SurvivalTheme.POWER)for(int level=0;level<10;level++)if(y>=PowerPagoda.floor(level)+6&&y<=PowerPagoda.floor(level)+12&&PowerPagoda.wall(x,z,PowerPagoda.radius(level)-1))
                check(before==AIR?after>AIR:before==after,"hexadecimal window contour retained/restored at "+x+","+y+","+z+" old="+before+" new="+after);
        }
        if(p.theme!=SurvivalTheme.POWER){
            check(changes==0,"exact pre-reduction blueprint restored "+p.theme+" changes="+changes+" first="+first);
            check(Files.readString(file.resolveSibling(p.theme.id+"-interiors.txt")).strip().equals(p.furnishings().toString()),"exact original furnishing records "+p.theme);
            var resources=file.resolveSibling(p.theme.id+"-resources.txt");
            if(Files.exists(resources))check(Files.readString(resources).strip().equals(p.interiorResources().toString()),"exact original authored reward positions "+p.theme);
        }
        System.out.println("RESTORE_BASELINE "+p.theme+" changedVoxels="+changes+(p.theme==SurvivalTheme.POWER?" retained top-arena and ninth-gallery fixes":""));
    }
}
