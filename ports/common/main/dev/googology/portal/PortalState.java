package dev.googology.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Stored with the world, so each player's return address survives reconnects and restarts. */
public final class PortalState extends SavedData {
    public record ReturnPoint(String dimension,Vec3 pos,float yaw,float pitch) {
        static final com.mojang.serialization.Codec<ReturnPoint> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
            com.mojang.serialization.Codec.STRING.fieldOf("dimension").forGetter(ReturnPoint::dimension),
            Vec3.CODEC.fieldOf("pos").forGetter(ReturnPoint::pos),
            com.mojang.serialization.Codec.FLOAT.fieldOf("yaw").forGetter(ReturnPoint::yaw),
            com.mojang.serialization.Codec.FLOAT.fieldOf("pitch").forGetter(ReturnPoint::pitch)).apply(i,ReturnPoint::new));
    }
    public record Gate(String dimension,BlockPos center,PortalKind kind) {
        static final com.mojang.serialization.Codec<Gate> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
            com.mojang.serialization.Codec.STRING.fieldOf("dimension").forGetter(Gate::dimension),
            BlockPos.CODEC.fieldOf("center").forGetter(Gate::center),
            com.mojang.serialization.Codec.STRING.xmap(PortalKind::valueOf,PortalKind::name).fieldOf("kind").forGetter(Gate::kind)).apply(i,Gate::new));
    }
    public static final com.mojang.serialization.Codec<PortalState> CODEC=com.mojang.serialization.codecs.RecordCodecBuilder.create(i->i.group(
        Gate.CODEC.listOf().optionalFieldOf("gates",java.util.List.of()).forGetter(s->java.util.List.copyOf(s.gates.values())),
        com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING,ReturnPoint.CODEC).optionalFieldOf("returns",Map.of()).forGetter(s->s.returns),
        BlockPos.CODEC.optionalFieldOf("hub").forGetter(s->java.util.Optional.ofNullable(s.hub))).apply(i,PortalState::new));
    public static final net.minecraft.world.level.saveddata.SavedDataType<PortalState> TYPE=new net.minecraft.world.level.saveddata.SavedDataType<>("googology_portals",PortalState::new,CODEC,null);
    private final Map<String,ReturnPoint> returns=new HashMap<>();
    private final Map<String,Gate> gates=new HashMap<>();
    private BlockPos hub;
    public PortalState() { }
    private PortalState(java.util.List<Gate> entries,Map<String,ReturnPoint> returns,java.util.Optional<BlockPos> hub) {
        entries.forEach(g->gates.put(gateKey(g.dimension,g.center),g));
        this.returns.putAll(returns);this.hub=hub.orElse(null);
    }
    private static String gateKey(String dimension,BlockPos pos) { return dimension+"/"+pos.asLong(); }
    public void addGate(Gate gate) { gates.put(gateKey(gate.dimension,gate.center),gate);setDirty(); }
    public void removeGate(Gate gate) { gates.remove(gateKey(gate.dimension,gate.center));setDirty(); }
    public java.util.Collection<Gate> gates() { return java.util.List.copyOf(gates.values()); }
    public Gate gateAt(String dimension,BlockPos pos) {
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            Gate gate=gates.get(gateKey(dimension,pos.offset(x,0,z)));
            if(gate!=null && (Math.abs(pos.getX()-gate.center.getX())<=1 && Math.abs(pos.getZ()-gate.center.getZ())<=1
                    || PortalRitual.isFrameOffset(pos.getX()-gate.center.getX(),pos.getZ()-gate.center.getZ()))) return gate;
        }
        return null;
    }

    public static PortalState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public ReturnPoint getReturn(UUID id) { return returns.get(id.toString()); }
    public void setReturn(UUID id, ReturnPoint point) { returns.put(id.toString(), point); setDirty(); }
    public BlockPos getHub() { return hub; }
    public void setHub(BlockPos pos) { hub = pos.immutable(); setDirty(); }

}
