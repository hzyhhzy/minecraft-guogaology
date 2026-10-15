package dev.guogaology.survival;

import java.util.*;
import static dev.guogaology.survival.SanctuaryLayout.*;

/** A few complete core exhibits; ordinary specimen furnishings hold raw materials. */
final class SanctuaryResources {
    static final int TARGET=8;
    private SanctuaryResources(){}
    static void apply(SanctuaryLayout p,List<List<Point>> groups){
        var all=new HashSet<Point>();groups.forEach(all::addAll);int kept=0;
        for(int y=0;y<p.height;y++)for(int x=-p.width/2;x<=p.width/2;x++)for(int z=-p.depth/2;z<=p.depth/2;z++){
            byte m=p.at(x,y,z);
            var q=new Point(x,y,z);
            if(m==CRYSTAL)throw new IllegalStateException("Finished crystal in scenery: "+p.theme+" "+q);
            if(!isRegional(p.theme,m)||p.finaleRelics().contains(q))continue;
            if(!all.contains(q))throw new IllegalStateException("Unauthored regional core: "+p.theme+" "+q);
            p.authoredResourceMount(q);p.interiorResource(q,true);kept++;
        }
        if(kept!=TARGET)throw new IllegalStateException("Missing fixed core exhibits: "+p.theme+" "+kept+"/"+TARGET);
        p.pruneAnchoredSamples();
    }
    static boolean isRegional(SurvivalTheme theme,byte m){return m==material(theme,0);}
    static byte material(SurvivalTheme theme,int index){return switch(theme){
        case MATRIX->SEQUENCE_CORE;case POWER->POWER_CORE;case HYDRA->HYDRA_BUD;case ABSENCE->PHANTOM_EMPTY;
        case WEAVER->LAVER_CORE;case ASTRA->ASTRA_CORE;case GUOGAO->GUOGAO_HEART;case FRONTIER->BOUNDARY_CORE;};}
}
