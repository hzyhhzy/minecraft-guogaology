package dev.guogaology.outer.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Turn the donor's climate edge into a bounded geographic coastline.
 * The original climate source still defines the void and the underground biome.
 * The fallback contains only its ordinary climate entries, so reclaimed inland
 * regions receive the complete neighboring biome, including vegetation. */
public final class LhoBorderBiomeSource extends BiomeSource {
    public static final int WIDTH=48;
    public static final MapCodec<LhoBorderBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            MultiNoiseBiomeSource.DIRECT_CODEC.codec().fieldOf("source").forGetter(s->s.source),
            MultiNoiseBiomeSource.DIRECT_CODEC.codec().fieldOf("inland").forGetter(s->s.inland)
    ).apply(i,LhoBorderBiomeSource::new));
    // Vanilla's nearest-entry cache can break ties in query-order-dependent ways.
    // The donor contains touching/overlapping parameter boxes. Resolve ties by
    // their declared order before caching, keeping chunk workers deterministic.
    private final Climate.ParameterList<Holder<Biome>> source,inland;
    private final ThreadLocal<Cache> caches=new ThreadLocal<>();
    private static final int[][] OFFSETS=offsets();
    public LhoBorderBiomeSource(Climate.ParameterList<Holder<Biome>> source,Climate.ParameterList<Holder<Biome>> inland){this.source=source;this.inland=inland;}
    @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.concat(source.values().stream(),inland.values().stream()).map(com.mojang.datafixers.util.Pair::getSecond).distinct();}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
        Cache cache=caches.get();
        if(cache==null||cache.sampler!=sampler){cache=new Cache(sampler);caches.set(cache);}
        Holder<Biome> original=y==(150>>2)?cache.surface(x,z):source.findValueBruteForce(sampler.sample(x,y,z));
        if(!original.is(LhoChunkGenerator.LHO_EDGE_BIOME))return original;
        return cache.nearVoid(x,z)?original:inland.findValueBruteForce(sampler.sample(x,y,z));
    }
    private final class Cache {
        final Climate.Sampler sampler;
        final Map<Long,Holder<Biome>> surface=lru(16384);
        final Map<Long,Boolean> coast=lru(4096);
        Cache(Climate.Sampler sampler){this.sampler=sampler;}
        Holder<Biome> surface(int x,int z){
            long key=key(x,z);Holder<Biome> old=surface.get(key);
            if(old!=null)return old;
            Holder<Biome> biome=source.findValueBruteForce(sampler.sample(x,150>>2,z));surface.put(key,biome);return biome;
        }
        boolean nearVoid(int x,int z){
            long key=key(x,z);Boolean old=coast.get(key);if(old!=null)return old;
            for(int[] d:OFFSETS)if(surface(x+d[0],z+d[1]).is(LhoChunkGenerator.LHO_VOID_BIOME)){coast.put(key,true);return true;}
            coast.put(key,false);return false;
        }
    }
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static <T> Map<Long,T> lru(int size){return new LinkedHashMap<>(256,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,T> e){return size()>size;}
    };}
    private static int[][] offsets(){
        List<int[]> out=new ArrayList<>();int r=WIDTH/4;
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r)out.add(new int[]{x,z});
        out.sort(Comparator.comparingInt(d->d[0]*d[0]+d[1]*d[1]));return out.toArray(int[][]::new);
    }
}
