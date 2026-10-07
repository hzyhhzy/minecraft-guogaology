package dev.guogaology.portal;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stored with the world, so each player's return address survives reconnects and restarts. */
public final class PortalState extends PersistentState {
    public record ReturnPoint(String dimension, Vec3d pos, float yaw, float pitch) {}
    public static final Type<PortalState> TYPE = new Type<>(PortalState::new, PortalState::read, null);
    private final Map<UUID, ReturnPoint> returns = new HashMap<>();
    private BlockPos hub;
    public record Gate(String dimension,BlockPos center,PortalKind kind,String targetDimension) {
        // Commands carry an explicit transient target; physical gates retain their three-field save format.
        public Gate(String dimension,BlockPos center,PortalKind kind){this(dimension,center,kind,"");}}
    private final Map<String,Gate> gates=new HashMap<>();
    private static String gateKey(String dimension,BlockPos pos) { return dimension+"/"+pos.asLong(); }
    public void addGate(Gate gate) { gates.put(gateKey(gate.dimension,gate.center),gate);markDirty(); }
    public void removeGate(Gate gate) { gates.remove(gateKey(gate.dimension,gate.center));markDirty(); }
    public java.util.Collection<Gate> gates() { return java.util.List.copyOf(gates.values()); }
    public Gate gateAt(String dimension,BlockPos pos) {
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            Gate gate=gates.get(gateKey(dimension,pos.add(x,0,z)));
            if(gate!=null && (Math.abs(pos.getX()-gate.center.getX())<=1 && Math.abs(pos.getZ()-gate.center.getZ())<=1
                    || PortalRitual.isFrameOffset(pos.getX()-gate.center.getX(),pos.getZ()-gate.center.getZ()))) return gate;
        }
        return null;
    }

    public static PortalState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, "guogaology_portals");
    }
    public ReturnPoint getReturn(UUID id) { return returns.get(id); }
    public void setReturn(UUID id, ReturnPoint point) { returns.put(id, point); markDirty(); }
    public BlockPos getHub() { return hub; }
    public void setHub(BlockPos pos) { hub = pos.toImmutable(); markDirty(); }

    public static PortalState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PortalState state = new PortalState();
        NbtList gates=nbt.getList("Gates",NbtElement.COMPOUND_TYPE);
        for(int i=0;i<gates.size();i++) {
            var entry=gates.getCompound(i);
            try { state.addGate(new Gate(entry.getString("Dimension"),BlockPos.fromLong(entry.getLong("Center")),PortalKind.valueOf(entry.getString("Kind")))); }
            catch(IllegalArgumentException ignored) { }
        }
        if (nbt.contains("Hub", NbtElement.LONG_TYPE)) state.hub = BlockPos.fromLong(nbt.getLong("Hub"));
        NbtList list = nbt.getList("Returns", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            if (entry.containsUuid("Player")) {
                state.returns.put(entry.getUuid("Player"), new ReturnPoint(entry.getString("Dimension"),
                        new Vec3d(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z")), entry.getFloat("Yaw"), entry.getFloat("Pitch")));
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList gates=new NbtList();
        for(Gate gate:this.gates.values()) {
            var entry=new NbtCompound();entry.putString("Dimension",gate.dimension);entry.putLong("Center",gate.center.asLong());entry.putString("Kind",gate.kind.name());gates.add(entry);
        }
        nbt.put("Gates",gates);
        if (hub != null) nbt.putLong("Hub", hub.asLong());
        NbtList list = new NbtList();
        returns.forEach((id, point) -> {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", id);
            entry.putString("Dimension", point.dimension());
            entry.putDouble("X", point.pos().x);
            entry.putDouble("Y", point.pos().y);
            entry.putDouble("Z", point.pos().z);
            entry.putFloat("Yaw", point.yaw());
            entry.putFloat("Pitch", point.pitch());
            list.add(entry);
        });
        nbt.put("Returns", list);
        return nbt;
    }
}
