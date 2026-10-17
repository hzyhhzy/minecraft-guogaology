package dev.guogaology.block;

import java.util.Map;
import java.util.LinkedHashMap;

/** Fixed survival progress for the 27 finished cores; no player/tool modifiers. */
public final class CoreMiningRules {
    private static final Map<String,Integer> TICKS;
    static {
        var ticks=new LinkedHashMap<String,Integer>();
        for(String root:new String[]{"sequence_core","power_tower_core","hydra_bud","lho_trace",
                "laver_core","astra_critical_core","boundary_core","guogao_heart"})
            for(int grade=1;grade<=3;grade++)ticks.put(root+(grade==1?"":"_lv"+grade),100<<(grade-1));
        for(int stage=2;stage<=4;stage++)ticks.put("ordinal_crystal_lv"+stage,100<<(stage-2));
        TICKS=Map.copyOf(ticks);
    }
    private CoreMiningRules(){}
    public static int ticks(String id){return TICKS.getOrDefault(id,0);}
    /** Tiny round-up avoids an extra client tick from accumulated float error. */
    public static float progress(String id){int ticks=ticks(id);return ticks==0?0:(1.00001f/ticks);}
}
