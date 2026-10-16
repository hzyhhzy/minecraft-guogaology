package dev.guogaology.mergeqa;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;

/** Records actual WorldGen network ingestion; no synthetic sections are supplied. */
public final class DedicatedVoxyProbe {
    public record Section(int x,int y,int z) {}
    public static final class Metrics {
        public long sections;
        public final Set<Long> farChunks=new HashSet<>();
        public final Set<Section> farSections=new LinkedHashSet<>();
    }
    private static final Map<String,Metrics> DATA=new HashMap<>();
    public static long wrongWorld;
    public static Metrics metrics(String dimension){return DATA.computeIfAbsent(dimension,k->new Metrics());}
    public static void reset(String dimension){DATA.put(dimension,new Metrics());}
    public static void record(Level world,LevelChunkSection section,int x,int y,int z){
        var client=Minecraft.getInstance();
        if(client.level!=world){wrongWorld++;return;}
        var metric=metrics(world.dimension().identifier().toString());metric.sections++;
        // Network palette reads do not update LevelChunkSection's native block count.
        if(client.player==null||!section.getStates().maybeHas(state->!state.isAir()))return;
        int cx=client.player.blockPosition().getX()>>4,cz=client.player.blockPosition().getZ()>>4;
        if(Math.max(Math.abs(x-cx),Math.abs(z-cz))<7||client.level.getChunkSource().hasChunk(x,z))return;
        metric.farChunks.add(((long)x<<32)^(z&0xffffffffL));
        if(metric.farSections.size()<1024)metric.farSections.add(new Section(x,y,z));
    }
}
