package dev.guogaology.mining;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;
import java.util.UUID;

/** Unloading and changing dimension preserve the volley; destroying its final arrow removes it. */
public final class BowVolleyLedger extends SavedData {
    private BowVolleyData data=new BowVolleyData();
    public static final Codec<BowVolleyLedger> CODEC=RecordCodecBuilder.create(i->i.group(Codec.STRING.optionalFieldOf("volleys","{}").forGetter(s->s.data.save())).apply(i,BowVolleyLedger::new));
    public static final SavedDataType<BowVolleyLedger> TYPE=new SavedDataType<>("guogaology_bow_volleys",BowVolleyLedger::new,CODEC,null);
    public BowVolleyLedger(){}
    private BowVolleyLedger(String text){data=BowVolleyData.load(text);}
    public static BowVolleyLedger get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public void register(String id,UUID arrow){data.register(id,arrow);setDirty();}
    public boolean directSeen(String id,UUID target){return data.directSeen(id,target);}
    public boolean burstSeen(String id,UUID target){return data.burstSeen(id,target);}
    public void direct(String id,UUID target){data.claimDirect(id,target);setDirty();}
    public boolean burst(String id,UUID target){boolean fresh=data.claimBurst(id,target);if(fresh)setDirty();return fresh;}
    public void destroy(String id,UUID arrow){data.destroy(id,arrow);setDirty();}
}
