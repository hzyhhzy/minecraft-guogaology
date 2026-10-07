package dev.guogaology.survival;

import java.util.*;
import static dev.guogaology.survival.SanctuaryLayout.*;

/** Rewards only occupy explicitly authored specimen positions, never arbitrary surface voxels. */
final class SanctuaryResources {
    private static final int TARGET=50;
    private SanctuaryResources(){}
    static void apply(SanctuaryLayout p,List<List<Point>> groups){
        var all=new HashSet<Point>();groups.forEach(all::addAll);
        for(int y=0;y<p.height;y++)for(int x=-p.width/2;x<=p.width/2;x++)for(int z=-p.depth/2;z<=p.depth/2;z++){
            byte m=p.at(x,y,z);
            var q=new Point(x,y,z);
            if(m!=CRYSTAL&&!isRegional(p.theme,m)||all.contains(q)||p.finaleRelics().contains(q))continue;
            if(p.theme==SurvivalTheme.ABSENCE)continue;
            p.set(x,y,z,commonReplacement(p.theme,m));
        }
        select(p,groups,false);select(p,groups,true);p.pruneAnchoredSamples();
    }
    private static void select(SanctuaryLayout p,List<List<Point>> groups,boolean regional){
        var queues=new ArrayList<ArrayDeque<Point>>();
        for(var group:groups){var queue=new ArrayDeque<Point>();
            for(var q:group){byte m=p.at(q.x(),q.y(),q.z());if(regional?isRegional(p.theme,m):m==CRYSTAL){queue.add(q);p.authoredResourceMount(q);}}
            queues.add(queue);
        }
        int count=0,kept=0;boolean found=true;
        var perMaterial=new HashMap<Byte,Integer>();
        // Original model order, evenly across furnishings; no coordinate hashes or generic replacements.
        while(found){found=false;for(var queue:queues){
            if(queue.isEmpty())continue;found=true;var q=queue.removeFirst();
            byte m=p.at(q.x(),q.y(),q.z());
            boolean accept=regional&&p.theme==SurvivalTheme.POWER?perMaterial.getOrDefault(m,0)<(m==POWER_CORE?50:30):count<TARGET;
            count++;
            if(accept){p.interiorResource(q,regional);kept++;perMaterial.merge(m,1,Integer::sum);}
            else p.set(q.x(),q.y(),q.z(),commonReplacement(p.theme,p.at(q.x(),q.y(),q.z())));
        }}
        int target=regional&&p.theme==SurvivalTheme.POWER?140:TARGET;
        if(kept!=target)throw new IllegalStateException("Missing fixed display positions: "+p.theme+" "+regional+" "+kept+"/"+target);
    }
    private static byte commonReplacement(SurvivalTheme theme,byte m){
        if(theme==SurvivalTheme.POWER)return RELIC_INLAY;
        return m>=RED&&m<=BLUE?(byte)(TREE_SHELL_RED+m-RED):m==CRYSTAL?SCG_SHELL:GLASS;
    }
    static boolean isRegional(SurvivalTheme theme,byte m){return switch(theme){
        case MATRIX->m==SEQUENCE_CORE;case POWER->m>=RED&&m<=BLUE||m==POWER_CORE;case HYDRA->m==HYDRA_BUD;
        case ABSENCE->m>=PHANTOM_PSI&&m<=PHANTOM_FOS;case WEAVER->m==LAVER_CORE;case ASTRA->m==ASTRA_CORE;case GUOGAO->m==GUOGAO_HEART;case FRONTIER->m==BOUNDARY_CORE;};}
    static byte material(SurvivalTheme theme,int index){return switch(theme){
        case MATRIX->SEQUENCE_CORE;case POWER->index%4==3?POWER_CORE:(byte)(RED+index%4);case HYDRA->HYDRA_BUD;case ABSENCE->(byte)(PHANTOM_PSI+index%3);
        case WEAVER->LAVER_CORE;case ASTRA->ASTRA_CORE;case GUOGAO->GUOGAO_HEART;case FRONTIER->BOUNDARY_CORE;};}
}
