package dev.googology.client;
import dev.googology.mining.MiningContent;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;

/** The legacy item-model predicates used by vanilla bow overrides. */
public final class OrdinalBowModels {
    public static void initialize(){for(var bow:MiningContent.BOWS){
        ModelPredicateProviderRegistry.register(bow,Identifier.ofVanilla("pull"),(s,w,e,seed)->e!=null&&e.getActiveItem()==s?(s.getMaxUseTime(e)-e.getItemUseTimeLeft())/20f:0);
        ModelPredicateProviderRegistry.register(bow,Identifier.ofVanilla("pulling"),(s,w,e,seed)->e!=null&&e.isUsingItem()&&e.getActiveItem()==s?1:0);
    }}
}
