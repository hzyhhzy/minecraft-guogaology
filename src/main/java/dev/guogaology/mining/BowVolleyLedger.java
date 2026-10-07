package dev.guogaology.mining;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import java.util.UUID;

/** Shared server save keeps hit deduplication intact across arrow chunk unloading/reloading. */
public final class BowVolleyLedger extends PersistentState {
    private BowVolleyData data=new BowVolleyData();
    public static final Type<BowVolleyLedger> TYPE=new Type<>(BowVolleyLedger::new,BowVolleyLedger::read,null);
    public static BowVolleyLedger get(MinecraftServer server){return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE,"guogaology_bow_volleys");}
    private static BowVolleyLedger read(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){var state=new BowVolleyLedger();state.data=BowVolleyData.load(nbt.getString("volleys"));return state;}
    @Override public NbtCompound writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){nbt.putString("volleys",data.save());return nbt;}
    public void register(String id,UUID arrow){data.register(id,arrow);markDirty();}
    public boolean directSeen(String id,UUID target){return data.directSeen(id,target);}
    public boolean burstSeen(String id,UUID target){return data.burstSeen(id,target);}
    public void direct(String id,UUID target){data.claimDirect(id,target);markDirty();}
    public boolean burst(String id,UUID target){boolean fresh=data.claimBurst(id,target);if(fresh)markDirty();return fresh;}
    public void destroy(String id,UUID arrow){data.destroy(id,arrow);markDirty();}
}
