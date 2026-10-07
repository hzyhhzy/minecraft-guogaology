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
        int[] furniture={30,56,21,20,44,35,50,35};
        int[] solids={231307,0,134925,89370,156231,317704,474031,135744};
        for(var theme:SurvivalTheme.values()){
            var p=SanctuaryLayout.of(theme);var reached=reachable(p);
            check(p.furnishings().size()==furniture[theme.ordinal()],"restored pre-reduction furnishing count "+theme+" "+p.furnishings().size());
            if(theme!=SurvivalTheme.POWER)check(p.solidCount()==solids[theme.ordinal()],"exact pre-reduction occupied volume "+theme);
            check(p.interiorResources().stream().filter(q->!q.regional()).count()==50,"restored50 authored ordinal crystals "+theme);
            check(p.interiorResources().stream().filter(InteriorResource::regional).count()==(theme==SurvivalTheme.POWER?140:50),"restored authored regional total "+theme);
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
                    check(count(p,POWER_CORE)==50,"restored fifty Power cores");for(byte m:new byte[]{RED,GREEN,BLUE})check(count(p,m)==30,"restored thirty TREE "+m);
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
                    byte[] materials={PHANTOM_PSI,PHANTOM_Z,PHANTOM_FOS,PHANTOM_FFFZ,PHANTOM_EMPTY};int[] totals={41,43,78,65,48};
                    for(int i=0;i<materials.length;i++)check(count(p,materials[i])==totals[i],"restored LHO interior, ornament and railing materials "+materials[i]);
                    check(count(p,CRYSTAL)==227,"restored LHO authored and architectural crystals");
                }
                case GUOGAO->{
                    check(p.numberLamps().size()==87&&p.emojiLampCenters().size()==24,"existing exterior lamps retained");
                    for(var lamp:p.numberLamps()){var q=lamp.center();for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++)check(p.at(q.x()+dx,q.y()+dy,q.z()+dz)==CRYSTAL,"intact 27-crystal lamp deposit");}
                    for(var q:p.emojiLampCenters())check(p.at(q.x(),q.y(),q.z())==GUOGAO_HEART,"intact emoji heart deposit");
                    check(p.finaleCores().size()==8&&p.finaleRelics().size()==17,"eight finale cores and nine existing companion relics");
                    check(count(p,CRYSTAL)==50+87*27+8,"all restored Underworld crystals, with protected exceptions");
                    check(count(p,GUOGAO_HEART)==50+24+1,"all restored Underworld hearts, with protected exceptions");
                }
                default->check(count(p,SanctuaryResources.material(theme,0))==(theme==SurvivalTheme.FRONTIER?52:50),"restored authored regional cores and original functional endpoints "+theme);
            }
            if(theme!=SurvivalTheme.GUOGAO&&theme!=SurvivalTheme.ABSENCE)check(count(p,CRYSTAL)==50,"restored ordinary crystal material count "+theme);
            if(theme==SurvivalTheme.FRONTIER)check(p.at(-45,72,0)==BOUNDARY_CORE&&p.at(45,72,0)==BOUNDARY_CORE,"original functional tape endpoints retained separately");
            if(theme==SurvivalTheme.WEAVER){
                int functional=0;for(var f:p.furnishings())if(f.kind().startsWith("playable_iblp_")){
                    var q=f.floor();int cores=0;for(int dx=-f.radiusX();dx<=f.radiusX();dx++)for(int dz=-f.radiusZ();dz<=f.radiusZ();dz++)if(p.at(q.x()+dx,q.y()+3,q.z()+dz)==LAVER_CORE)cores++;
                    check(cores==(f.kind().equals("playable_iblp_initial")?2:1),"exact playable table roots/marks retained");functional+=cores;
                }check(functional==5,"five functional table cores retained within the restored fifty");
            }
            check(p.furnishings().stream().noneMatch(f->f.kind().startsWith("treasure_")||f.kind().startsWith("ordinary_")),"cancelled replacement models are absent "+theme);
            check(p.furnishings().stream().anyMatch(f->f.kind().equals("fixed_"+theme.id+"_specimen_display")),"original multi-specimen displays restored "+theme);
            for(var reward:p.interiorResources()){
                var q=reward.pos();check(p.authoredResourceMounts().contains(q),"retained rewards occupy original authored mounts "+theme);
                check(reward.regional()?SanctuaryResources.isRegional(theme,p.at(q.x(),q.y(),q.z())):p.at(q.x(),q.y(),q.z())==CRYSTAL,"recorded restored reward agrees with final material "+q);
            }
            if(args.length>0)compareBaseline(p,Path.of(args[0]).resolve(theme.id+".vox"));
            System.out.println(theme+": authoredRegional="+p.interiorResources().stream().filter(InteriorResource::regional).count()+", authoredCrystals=50, furniture="+p.furnishings().size()+", reachable="+reached.cardinality());
        }
        System.out.println("LANDMARK_RESTORED_0405_OK: "+assertions+" assertions");
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
