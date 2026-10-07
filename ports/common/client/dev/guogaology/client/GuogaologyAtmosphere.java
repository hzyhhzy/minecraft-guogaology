package dev.guogaology.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.GuogaologySounds;
import dev.guogaology.ambience.*;
import dev.guogaology.block.ChristmasDigitBlock;
import dev.guogaology.block.ChristmasLightBlock;
import dev.guogaology.world.WorldNoise;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.*;

/** Client-only atmosphere: loaded-chunk observations and transient meshes, never world edits. */
public final class GuogaologyAtmosphere {
    private GuogaologyAtmosphere() {}
    private record Section(int x,int z,int index) {}
    private record Light(BlockPos pos,int color,long seen) {}
    private record Pulse(BlockPos note,int pitch,int row,long time) {}
    public record GhostView(BlockPos center,long salt,double opacity,boolean dissolving,int faces) {}
    private static final class Ghost {
        final BlockPos center;final long salt,cycle;final List<AmbientPatterns.Face> mesh;long dissolved=-1;
        Ghost(BlockPos center,long salt,long cycle) { this.center=center;this.salt=salt;this.cycle=cycle;mesh=AmbientPatterns.mirage(salt); }
    }
    private static ClientLevel world;
    private static final Map<BlockPos,Light> LIGHTS=new HashMap<>();
    private static final Map<BlockPos,Long> FIBERS=new HashMap<>();
    private static final Map<BlockPos,Pulse> MUSIC=new HashMap<>();
    private static final Map<Long,Ghost> GHOSTS=new LinkedHashMap<>();
    private static final ArrayDeque<Section> SCAN=new ArrayDeque<>();
    private static final List<SoundInstance> SOUNDS=new ArrayList<>();
    private static List<BandRhythm.Beat> beats=List.of();
    private static List<Ghost> visibleGhosts=List.of();
    private static long ticks,notesHeard,silentRests,ambientSounds,ghostBursts;
    private static boolean dirty;
    private static final DustParticleOptions CYAN=new DustParticleOptions(0x7AE8F7,.65f);
    private static final DustParticleOptions MIST=new DustParticleOptions(0xBAC7E8,.8f);

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(TableNotePayload.ID,(payload,context)->receive(context.client(),payload));
        ClientTickEvents.END_CLIENT_TICK.register(GuogaologyAtmosphere::tick);
        WorldRenderEvents.END_EXTRACTION.register(GuogaologyAtmosphere::extract);
        WorldRenderEvents.AFTER_ENTITIES.register(GuogaologyAtmosphere::render);
    }
    private static void synchronize(Minecraft client) {
        if(world==client.level) return;
        for(var sound:SOUNDS) client.getSoundManager().stop(sound);
        SOUNDS.clear();LIGHTS.clear();FIBERS.clear();MUSIC.clear();GHOSTS.clear();SCAN.clear();beats=List.of();visibleGhosts=List.of();dirty=false;
        world=client.level;ticks=0;
    }
    private static void receive(Minecraft client,TableNotePayload payload) {
        synchronize(client);if(world==null||client.player==null) return;
        if(payload.row()<0) { MUSIC.remove(payload.table());return; }
        MUSIC.put(payload.table(),new Pulse(payload.note(),payload.semitone(),payload.row(),world.getGameTime()));
        if(payload.semitone()<0) { silentRests++;return; }
        double distance=client.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.table()));
        float volume=(float)(.24*Math.clamp(1-Math.sqrt(distance)/128,0,1));
        var sound=payload.marked()?GuogaologySounds.LAVER_ACCENT:GuogaologySounds.LAVER_STRING;
        play(client,sound,SoundSource.RECORDS,net.minecraft.world.phys.Vec3.atCenterOf(payload.note()),volume,(float)Math.pow(2,(payload.semitone()-12)/12.0));
        world.addParticle(ParticleTypes.NOTE,payload.note().getX()+.5,payload.note().getY()+1.15,payload.note().getZ()+.5,payload.semitone()/24.0,0,0);
        world.addParticle(CYAN,payload.note().getX()+.5,payload.note().getY()+1.03,payload.note().getZ()+.5,0,.01,0);
        notesHeard++;
    }
    private static void play(Minecraft client,SoundEvent event,SoundSource category,Vec3 at,float volume,float pitch) {
        if(volume<.001) return;
        // Distance is attenuated above, leaving notes audible across a wide triangular table.
        var sound=new SimpleSoundInstance(event.location(),category,volume,pitch,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,at.x,at.y,at.z,false);
        client.getSoundManager().play(sound);SOUNDS.add(sound);
    }
    private static boolean underworld() { return world.dimension().equals(GuogaologyMod.GUOGAO); }
    private static boolean lho(BlockPos pos) { return world.dimension().equals(GuogaologyMod.DIMENSION)&&world.getBiome(pos).is(GuogaologyMod.id("lho_absence")); }
    private static boolean lamp(BlockState state){return state.getBlock() instanceof ChristmasDigitBlock||state.getBlock() instanceof ChristmasLightBlock;}
    private static int lampColor(BlockState state){return state.getBlock() instanceof ChristmasLightBlock plain?plain.color():((ChristmasDigitBlock)state.getBlock()).color();}
    private static boolean interesting(BlockState state) { return lamp(state)||fiber(state); }
    private static boolean fiber(BlockState state) { return state.is(GuogaologyBlocks.TIANYI_FIBER)||state.is(GuogaologyBlocks.WHITE_FIBER)||state.is(GuogaologyBlocks.LTY_YARN); }
    private static void tick(Minecraft client) {
        synchronize(client);if(world==null||client.player==null||client.isPaused()) return;
        ticks++;long time=world.getGameTime();var pos=client.player.blockPosition();
        MUSIC.entrySet().removeIf(e->time-e.getValue().time>100||client.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(e.getKey()))>128*128);
        if(underworld()||!MUSIC.isEmpty()) scan(pos);
        if(ticks%20==0) {
            dirty|=LIGHTS.entrySet().removeIf(e->ticks-e.getValue().seen>400||e.getKey().distSqr(pos)>88*88);
            FIBERS.entrySet().removeIf(e->ticks-e.getValue()>400||e.getKey().distSqr(pos)>88*88);
            if(dirty) { beats=BandRhythm.order(LIGHTS.values().stream().map(l->new BandRhythm.Lamp(new BandRhythm.Position(l.pos.getX(),l.pos.getY(),l.pos.getZ()),l.color)).toList());dirty=false; }
            SOUNDS.removeIf(sound->!client.getSoundManager().isActive(sound));
        }
        if(underworld()&&!beats.isEmpty()&&time%180==0) {
            long salt=WorldNoise.mix(time/180);var beat=beats.get((int)Math.floorMod(salt,beats.size()));var p=beat.pos();
            var at=new Vec3(p.x()+.5,p.y()+.5,p.z()+.5);double distance=at.distanceTo(client.player.position());
            var sound=(salt&1)==0?GuogaologySounds.DISTANT_BELL:GuogaologySounds.VINE_CREAK;
            float volume=(float)((salt&1)==0?.10:.075)/(float)(1+distance/48);
            play(client,sound,SoundSource.AMBIENT,at,volume,(salt&1)==0?.56f:.52f);ambientSounds++;
        }
        if(ticks%10==0) ghosts(client,pos,time);
    }
    private static void scan(BlockPos center) {
        if(SCAN.isEmpty()) {
            int cy=world.getSectionIndex(center.getY());
            for(int r=0;r<=4;r++) for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=r||!world.hasChunk((center.getX()>>4)+x,(center.getZ()>>4)+z)) continue;
                for(int dy=0;dy<=5;dy++) for(int sign:new int[]{-1,1}) {
                    if(dy==0&&sign==1) continue;int section=cy+dy*sign;
                    if(section>=0&&section<world.getSectionsCount()) SCAN.add(new Section((center.getX()>>4)+x,(center.getZ()>>4)+z,section));
                }
            }
        }
        int scanned=0,visited=0;
        while(!SCAN.isEmpty()&&scanned<2&&visited++<80) {
            var ref=SCAN.removeFirst();var chunk=world.getChunk(ref.x,ref.z,ChunkStatus.FULL,false);if(chunk==null) continue;
            var section=chunk.getSections()[ref.index];if(section.hasOnlyAir()||!section.maybeHas(GuogaologyAtmosphere::interesting)) continue;scanned++;
            int bottom=world.getSectionYFromSectionIndex(ref.index)*16;
            for(int x=0;x<16;x++) for(int y=0;y<16;y++) for(int z=0;z<16;z++) {
                var state=section.getBlockState(x,y,z);if(!interesting(state)) continue;
                var p=new BlockPos(ref.x*16+x,bottom+y,ref.z*16+z);if(p.distSqr(center)>88*88) continue;
                if(fiber(state)) { if(FIBERS.size()<512||FIBERS.containsKey(p)) FIBERS.put(p,ticks);continue; }
                if(!underworld()||(LIGHTS.size()>=2048&&!LIGHTS.containsKey(p))) continue;
                int color=lampColor(state);
                var old=LIGHTS.put(p,new Light(p,color,ticks));if(old==null||old.color!=color) dirty=true;
            }
        }
    }
    private static void ghosts(Minecraft client,BlockPos observer,long time) {
        if(!lho(observer)) { visibleGhosts=List.of();return; }
        long cycle=time/1600;GHOSTS.entrySet().removeIf(e->e.getValue().cycle!=cycle||e.getValue().center.distSqr(observer)>240*240);
        int cx=Math.floorDiv(observer.getX(),96),cz=Math.floorDiv(observer.getZ(),96);
        for(int x=cx-2;x<=cx+2;x++) for(int z=cz-2;z<=cz+2;z++) {
            long key=((long)x<<32)^(z&0xffffffffL),salt=WorldNoise.hash(33143,x,0,z);
            if(Math.floorMod(salt,3L)==0||GHOSTS.containsKey(key)||GHOSTS.size()>=32) continue;
            int px=x*96+20+(int)(WorldNoise.unit(salt)*56),pz=z*96+20+(int)(WorldNoise.unit(WorldNoise.mix(salt+17))*56),py=105+(int)(WorldNoise.unit(WorldNoise.mix(salt+41))*62);
            var p=new BlockPos(px,py,pz);
            if(p.distSqr(observer)>172*172||!world.hasChunk(px>>4,pz>>4)||!lho(p)||!world.getBlockState(p).isAir()||!world.getBlockState(p.below(8)).isAir()) continue;
            GHOSTS.put(key,new Ghost(p,WorldNoise.mix(salt+cycle*917),cycle));
        }
        for(var ghost:GHOSTS.values()) if(ghost.dissolved<0&&ghost.center.distSqr(observer)<48*48) {
            ghost.dissolved=time;ghostBursts++;
            for(int i=0;i<36;i++) {
                long salt=WorldNoise.mix(ghost.salt+i*433L);double a=WorldNoise.unit(salt)*Math.PI*2,r=4+WorldNoise.unit(WorldNoise.mix(salt))*14;
                world.addParticle(MIST,ghost.center.getX()+Math.cos(a)*r,ghost.center.getY()+(WorldNoise.unit(WorldNoise.mix(salt+71))-.5)*7,ghost.center.getZ()+Math.sin(a)*r,Math.cos(a)*.035,.018,Math.sin(a)*.035);
                if(i%3==0) world.addParticle(ParticleTypes.END_ROD,ghost.center.getX()+Math.cos(a)*r,ghost.center.getY()+WorldNoise.unit(salt)*3,ghost.center.getZ()+Math.sin(a)*r,Math.cos(a)*.03,.013,Math.sin(a)*.03);
            }
        }
        visibleGhosts=GHOSTS.values().stream().filter(g->AmbientPatterns.mirageOpacity(time,g.salt,Math.sqrt(g.center.distSqr(observer)),g.dissolved<0?-1:time-g.dissolved)>.001).sorted(Comparator.comparingDouble(g->g.center.distSqr(observer))).limit(2).toList();
    }
    private record Quad(Direction direction,double x,double y,double z,double sx,double sy,double sz,float r,float g,float b,float alpha) {}
    private static final RenderStateDataKey<List<Quad>> QUADS = RenderStateDataKey.create(() -> "guogaology:atmosphere");
    private static void face(List<Quad> quads,Direction direction,double x,double y,double z,double sx,double sy,double sz,float r,float g,float b,float alpha) {
        quads.add(new Quad(direction,x,y,z,sx,sy,sz,r,g,b,alpha));
    }
    private static void blockFilm(List<Quad> quads,Vec3 camera,BlockPos pos,double drift,float r,float g,float b,float alpha) {
        double x=pos.getX()-camera.x+drift-.003,y=pos.getY()-camera.y-.003,z=pos.getZ()-camera.z-.003;
        for(var direction:Direction.values()) {
            double dot=(camera.x-pos.getX()-.5)*direction.getStepX()+(camera.y-pos.getY()-.5)*direction.getStepY()+(camera.z-pos.getZ()-.5)*direction.getStepZ();
            if(dot>.5) face(quads,direction,x,y,z,1.006,1.006,1.006,r,g,b,alpha);
        }
    }
    private static void extract(WorldExtractionContext context) {
        var quads = new ArrayList<Quad>();
        context.worldState().setData(QUADS, List.of());
        if(world==null||world!=context.world()) return;
        Vec3 camera=context.camera().position();double time=world.getGameTime()+context.tickCounter().getGameTimeDeltaPartialTick(false);

        if(underworld()) for(var beat:beats) {
            var p=beat.pos();var pos=new BlockPos(p.x(),p.y(),p.z());
            var state=world.getBlockState(pos);
            if(pos.distToCenterSqr(camera)>84*84||!lamp(state)) continue;
            double brightness=BandRhythm.brightness(beat,time);
            blockFilm(quads,camera,pos,0,.012f,.022f,.018f,(float)(.80*(1-brightness)));
            if(brightness>.76) blockFilm(quads,camera,pos,.001,.80f,.97f,.84f,(float)((brightness-.76)*.8));
        }
        for(var pulse:MUSIC.values()) {
            double age=time-pulse.time;if(age>12) continue;
            blockFilm(quads,camera,pulse.note,0,.55f,.96f,1f,(float)(.52*(1-age/12)));
        }
        for(var ghost:visibleGhosts) {
            double distance=net.minecraft.world.phys.Vec3.atCenterOf(ghost.center).distanceTo(camera),age=ghost.dissolved<0?-1:time-ghost.dissolved;
            float alpha=(float)AmbientPatterns.mirageOpacity(time,ghost.salt,distance,age);if(alpha<=.001) continue;
            double x=ghost.center.getX()-camera.x,y=ghost.center.getY()-camera.y+Math.sin(time/57+WorldNoise.unit(ghost.salt)*6)*.6,z=ghost.center.getZ()-camera.z;
            for(var f:ghost.mesh) {
                float shade=f.direction()==1?1:f.direction()==0?.55f:.78f;
                face(quads,Direction.values()[f.direction()],x+f.x(),y+f.y(),z+f.z(),3,2,3,.65f*shade,.72f*shade,.91f*shade,alpha);
            }
        }
            context.worldState().setData(QUADS, List.copyOf(quads));
    }
    private static void render(WorldRenderContext context) {
        var quads = context.worldState().getDataOrDefault(QUADS, List.of());
        if(quads.isEmpty()) return;
        var out = context.consumers().getBuffer(RenderTypes.debugQuads());
        var pose = context.matrices().last();
        for(var q:quads) {
            // Clockwise winding seen from inside, so the outside remains visible with culling.
            double x=q.x,y=q.y,z=q.z,X=x+q.sx,Y=y+q.sy,Z=z+q.sz;
            double[][] v=switch(q.direction) {
                case DOWN -> new double[][]{{x,y,Z},{x,y,z},{X,y,z},{X,y,Z}};
                case UP -> new double[][]{{x,Y,z},{x,Y,Z},{X,Y,Z},{X,Y,z}};
                case NORTH -> new double[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}};
                case SOUTH -> new double[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}};
                case WEST -> new double[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}};
                case EAST -> new double[][]{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}};
            };
            for(var p:v) out.addVertex(pose,(float)p[0],(float)p[1],(float)p[2]).setColor(q.r,q.g,q.b,q.alpha);
        }
    }
    public static long notesHeard() { return notesHeard; }
    public static long silentRests() { return silentRests; }
    public static long ambientSounds() { return ambientSounds; }
    public static long ghostBursts() { return ghostBursts; }
    public static int lightCount() { return beats.size(); }
    public static int fiberCount() { return FIBERS.size(); }
    public static double meanBrightness() { return world==null?0:beats.stream().mapToDouble(b->BandRhythm.brightness(b,world.getGameTime())).average().orElse(0); }
    public static int activeSoundCount() { return (int)SOUNDS.stream().filter(s->Minecraft.getInstance().getSoundManager().isActive(s)).count(); }
    public static List<GhostView> ghostViews(Vec3 camera) {
        if(world==null) return List.of();double time=world.getGameTime();
        return visibleGhosts.stream().map(g->new GhostView(g.center,g.salt,AmbientPatterns.mirageOpacity(time,g.salt,net.minecraft.world.phys.Vec3.atCenterOf(g.center).distanceTo(camera),g.dissolved<0?-1:time-g.dissolved),g.dissolved>=0,g.mesh.size())).toList();
    }
}
