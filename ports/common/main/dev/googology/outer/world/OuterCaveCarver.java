package dev.googology.outer.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.*;
import java.util.function.Function;

/** Retains the donor's data-driven count/thickness on pre-26.3 cave APIs.
 * Tunnel cutting remains delegated to Minecraft. No global vanilla carver is changed. */
public final class OuterCaveCarver extends CaveWorldCarver {
    public static final class Settings extends CaveCarverConfiguration {
        final IntProvider count;final FloatProvider thickness,outerFloor;final boolean weird;
        public static final Codec<Settings> OUTER_CODEC=RecordCodecBuilder.create(i->i.group(
            CaveCarverConfiguration.CODEC.fieldOf("base").forGetter((Settings s)->s),
            IntProvider.codec(0,64).fieldOf("count").forGetter(s->s.count),
            FloatProvider.CODEC.fieldOf("thickness").forGetter(s->s.thickness),
            Codec.BOOL.fieldOf("weird_thickness_bias").forGetter(s->s.weird),
            FloatProvider.CODEC.fieldOf("floor_level").forGetter(s->s.outerFloor)
        ).apply(i,Settings::new));
        Settings(CaveCarverConfiguration base,IntProvider count,FloatProvider thickness,boolean weird,FloatProvider floor){
            super(base,base.horizontalRadiusMultiplier,base.verticalRadiusMultiplier,floor);
            this.count=count;this.thickness=thickness;this.weird=weird;this.outerFloor=floor;
        }
    }
    public OuterCaveCarver(){super(Settings.OUTER_CODEC.xmap(s->s,s->(Settings)s));}
    @Override public boolean carve(CarvingContext context,CaveCarverConfiguration cfg,ChunkAccess chunk,
            Function<BlockPos,Holder<Biome>> biome,RandomSource random,Aquifer aquifer,ChunkPos source,CarvingMask mask){
        var settings=(Settings)cfg;
        int reach=SectionPos.sectionToBlockCoord(getRange()*2-1);
        int starts=settings.count.sample(random);
        for(int i=0;i<starts;i++){
            double x=source.getBlockX(random.nextInt(16)),y=cfg.y.sample(random,context),z=source.getBlockZ(random.nextInt(16));
            double horizontal=cfg.horizontalRadiusMultiplier.sample(random),vertical=cfg.verticalRadiusMultiplier.sample(random),floor=settings.outerFloor.sample(random);
            CarveSkipChecker skip=(c,dx,dy,dz,blockY)->dy<=floor||dx*dx+dy*dy+dz*dz>=1;
            int tunnels=1;
            if(random.nextInt(4)==0){
                double roomHeight=cfg.yScale.sample(random);float roomWidth=1+random.nextFloat()*6;
                createRoom(context,cfg,chunk,biome,aquifer,x,y,z,roomWidth,roomHeight,mask,skip);
                tunnels+=random.nextInt(4);
            }
            for(int j=0;j<tunnels;j++){
                float yaw=random.nextFloat()*(float)(2*Math.PI),pitch=(random.nextFloat()-.5f)/4;
                float thickness=settings.thickness.sample(random);
                if(settings.weird&&random.nextInt(10)==0)thickness*=random.nextFloat()*random.nextFloat()*3+1;
                int length=reach-random.nextInt(reach/4);
                createTunnel(context,cfg,chunk,biome,random.nextLong(),aquifer,x,y,z,horizontal,vertical,thickness,yaw,pitch,0,length,1,mask,skip);
            }
        }
        return true;
    }
}
