package dev.guogaology.survival;

import dev.guogaology.world.UnderworldLakes;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Only whole-plant exclusions depend on buildings; natural lake terrain does not. */
public final class SanctuaryClearing {
    private record Key(long seed,int gx,int gz) {}
    private static final ConcurrentHashMap<Key,Optional<SanctuaryPlacement.Position>> SITES=new ConcurrentHashMap<>();
    private SanctuaryClearing() {}
    private static SanctuaryPlacement.Position site(long seed,int gx,int gz){
        if(SITES.size()>16384)SITES.clear();
        return SITES.computeIfAbsent(new Key(seed,gx,gz),k->Optional.ofNullable(SanctuaryPlacement.find(seed,gx,gz,true))).orElse(null);
    }
    /** Remove whole large plants, rather than clipping their boughs against a rectangular site. */
    public static boolean excludesPlant(long seed,int x,int z,double reach){
        int span=(int)Math.ceil(UnderworldLakes.DEEP_RADIUS+reach);
        for(int gx=Math.floorDiv(x-span,SanctuaryPlacement.SPACING);gx<=Math.floorDiv(x+span,SanctuaryPlacement.SPACING);gx++)
            for(int gz=Math.floorDiv(z-span,SanctuaryPlacement.SPACING);gz<=Math.floorDiv(z+span,SanctuaryPlacement.SPACING);gz++){
                var p=site(seed,gx,gz);if(p!=null&&Math.hypot(x-p.x(),z-p.z())<UnderworldLakes.DEEP_RADIUS+reach)return true;
            }
        return false;
    }
    /** Keep small fruit/emoji trees out of the front approach as whole plants. */
    public static boolean excludesApproach(long seed,int x,int z,double reach){
        return excludesPlant(seed,x,z,reach);
    }
}
