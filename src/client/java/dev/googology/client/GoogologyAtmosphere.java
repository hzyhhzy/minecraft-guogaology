package dev.googology.client;

import dev.googology.GoogologyBlocks;
import dev.googology.GoogologyMod;
import dev.googology.GoogologySounds;
import dev.googology.ambience.*;
import dev.googology.block.ChristmasDigitBlock;
import dev.googology.world.WorldNoise;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.ChunkStatus;
import org.joml.Vector3f;
import java.util.*;

/** Client-only atmosphere: loaded-chunk observations and transient meshes, never world edits. */
public final class GoogologyAtmosphere {
    private GoogologyAtmosphere() {}
    private record Section(int x,int z,int index) {}
    private record Light(BlockPos pos,int color,long seen) {}
    private record Pulse(BlockPos note,int pitch,int row,long time) {}
    public record GhostView(BlockPos center,long salt,double opacity,boolean dissolving,int faces) {}
    private static final class Ghost {
        final BlockPos center;final long salt,cycle;final List<AmbientPatterns.Face> mesh;long dissolved=-1;
        Ghost(BlockPos center,long salt,long cycle) { this.center=center;this.salt=salt;this.cycle=cycle;mesh=AmbientPatterns.mirage(salt); }
    }
    private static ClientWorld world;
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
    private static final DustParticleEffect CYAN=new DustParticleEffect(new Vector3f(.48f,.91f,.97f),.65f);
    private static final DustParticleEffect MIST=new DustParticleEffect(new Vector3f(.73f,.78f,.91f),.8f);

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(TableNotePayload.ID,(payload,context)->receive(context.client(),payload));
        ClientTickEvents.END_CLIENT_TICK.register(GoogologyAtmosphere::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(GoogologyAtmosphere::render);
    }
    private static void synchronize(MinecraftClient client) {
        if(world==client.world) return;
        for(var sound:SOUNDS) client.getSoundManager().stop(sound);
        SOUNDS.clear();LIGHTS.clear();FIBERS.clear();MUSIC.clear();GHOSTS.clear();SCAN.clear();beats=List.of();visibleGhosts=List.of();dirty=false;
        world=client.world;ticks=0;
    }
    private static void receive(MinecraftClient client,TableNotePayload payload) {
        synchronize(client);if(world==null||client.player==null) return;
        if(payload.row()<0) { MUSIC.remove(payload.table());return; }
        MUSIC.put(payload.table(),new Pulse(payload.note(),payload.semitone(),payload.row(),world.getTime()));
        if(payload.semitone()<0) { silentRests++;return; }
        double distance=client.player.squaredDistanceTo(payload.table().toCenterPos());
        float volume=(float)(.24*Math.clamp(1-Math.sqrt(distance)/128,0,1));
        var sound=payload.marked()?GoogologySounds.LAVER_ACCENT:GoogologySounds.LAVER_STRING;
        play(client,sound,SoundCategory.RECORDS,payload.note().toCenterPos(),volume,(float)Math.pow(2,(payload.semitone()-12)/12.0));
        world.addParticle(ParticleTypes.NOTE,payload.note().getX()+.5,payload.note().getY()+1.15,payload.note().getZ()+.5,payload.semitone()/24.0,0,0);
        world.addParticle(CYAN,payload.note().getX()+.5,payload.note().getY()+1.03,payload.note().getZ()+.5,0,.01,0);
        notesHeard++;
    }
    private static void play(MinecraftClient client,SoundEvent event,SoundCategory category,Vec3d at,float volume,float pitch) {
        if(volume<.001) return;
        // Distance is attenuated above, leaving notes audible across a wide triangular table.
        var sound=new PositionedSoundInstance(event.getId(),category,volume,pitch,Random.create(),false,0,SoundInstance.AttenuationType.NONE,at.x,at.y,at.z,false);
        client.getSoundManager().play(sound);SOUNDS.add(sound);
    }
    private static boolean underworld() { return world.getRegistryKey().equals(GoogologyMod.GUOGAO); }
    private static boolean lho(BlockPos pos) { return world.getRegistryKey().equals(GoogologyMod.DIMENSION)&&world.getBiome(pos).matchesId(GoogologyMod.id("lho_absence")); }
    private static boolean interesting(BlockState state) { return state.getBlock() instanceof ChristmasDigitBlock||fiber(state); }
    private static boolean fiber(BlockState state) { return state.isOf(GoogologyBlocks.TIANYI_FIBER)||state.isOf(GoogologyBlocks.WHITE_FIBER)||state.isOf(GoogologyBlocks.LTY_YARN); }
    private static void tick(MinecraftClient client) {
        synchronize(client);if(world==null||client.player==null||client.isPaused()) return;
        ticks++;long time=world.getTime();var pos=client.player.getBlockPos();
        MUSIC.entrySet().removeIf(e->time-e.getValue().time>100||client.player.squaredDistanceTo(e.getKey().toCenterPos())>128*128);
        if(underworld()||!MUSIC.isEmpty()) scan(pos);
        if(ticks%20==0) {
            dirty|=LIGHTS.entrySet().removeIf(e->ticks-e.getValue().seen>400||e.getKey().getSquaredDistance(pos)>88*88);
            FIBERS.entrySet().removeIf(e->ticks-e.getValue()>400||e.getKey().getSquaredDistance(pos)>88*88);
            if(dirty) { beats=BandRhythm.order(LIGHTS.values().stream().map(l->new BandRhythm.Lamp(new BandRhythm.Position(l.pos.getX(),l.pos.getY(),l.pos.getZ()),l.color)).toList());dirty=false; }
            SOUNDS.removeIf(sound->!client.getSoundManager().isPlaying(sound));
        }
        if(underworld()&&!beats.isEmpty()&&time%180==0) {
            long salt=WorldNoise.mix(time/180);var beat=beats.get((int)Math.floorMod(salt,beats.size()));var p=beat.pos();
            var at=new Vec3d(p.x()+.5,p.y()+.5,p.z()+.5);double distance=at.distanceTo(client.player.getPos());
            var sound=(salt&1)==0?GoogologySounds.DISTANT_BELL:GoogologySounds.VINE_CREAK;
            float volume=(float)((salt&1)==0?.10:.075)/(float)(1+distance/48);
            play(client,sound,SoundCategory.AMBIENT,at,volume,(salt&1)==0?.56f:.52f);ambientSounds++;
        }
        if(ticks%10==0) ghosts(client,pos,time);
    }
    private static void scan(BlockPos center) {
        if(SCAN.isEmpty()) {
            int cy=world.getSectionIndex(center.getY());
            for(int r=0;r<=4;r++) for(int x=-r;x<=r;x++) for(int z=-r;z<=r;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=r||!world.isChunkLoaded((center.getX()>>4)+x,(center.getZ()>>4)+z)) continue;
                for(int dy=0;dy<=5;dy++) for(int sign:new int[]{-1,1}) {
                    if(dy==0&&sign==1) continue;int section=cy+dy*sign;
                    if(section>=0&&section<world.countVerticalSections()) SCAN.add(new Section((center.getX()>>4)+x,(center.getZ()>>4)+z,section));
                }
            }
        }
        int scanned=0,visited=0;
        while(!SCAN.isEmpty()&&scanned<2&&visited++<80) {
            var ref=SCAN.removeFirst();var chunk=world.getChunk(ref.x,ref.z,ChunkStatus.FULL,false);if(chunk==null) continue;
            var section=chunk.getSectionArray()[ref.index];if(section.isEmpty()||!section.hasAny(GoogologyAtmosphere::interesting)) continue;scanned++;
            int bottom=world.sectionIndexToCoord(ref.index)*16;
            for(int x=0;x<16;x++) for(int y=0;y<16;y++) for(int z=0;z<16;z++) {
                var state=section.getBlockState(x,y,z);if(!interesting(state)) continue;
                var p=new BlockPos(ref.x*16+x,bottom+y,ref.z*16+z);if(p.getSquaredDistance(center)>88*88) continue;
                if(fiber(state)) { if(FIBERS.size()<512||FIBERS.containsKey(p)) FIBERS.put(p,ticks);continue; }
                if(!underworld()||(LIGHTS.size()>=2048&&!LIGHTS.containsKey(p))) continue;
                int color=((ChristmasDigitBlock)state.getBlock()).color();
                var old=LIGHTS.put(p,new Light(p,color,ticks));if(old==null||old.color!=color) dirty=true;
            }
        }
    }
    private static void ghosts(MinecraftClient client,BlockPos observer,long time) {
        if(!lho(observer)) { visibleGhosts=List.of();return; }
        long cycle=time/1600;GHOSTS.entrySet().removeIf(e->e.getValue().cycle!=cycle||e.getValue().center.getSquaredDistance(observer)>240*240);
        int cx=Math.floorDiv(observer.getX(),96),cz=Math.floorDiv(observer.getZ(),96);
        for(int x=cx-2;x<=cx+2;x++) for(int z=cz-2;z<=cz+2;z++) {
            long key=((long)x<<32)^(z&0xffffffffL),salt=WorldNoise.hash(33143,x,0,z);
            if(Math.floorMod(salt,3L)==0||GHOSTS.containsKey(key)||GHOSTS.size()>=32) continue;
            int px=x*96+20+(int)(WorldNoise.unit(salt)*56),pz=z*96+20+(int)(WorldNoise.unit(WorldNoise.mix(salt+17))*56),py=105+(int)(WorldNoise.unit(WorldNoise.mix(salt+41))*62);
            var p=new BlockPos(px,py,pz);
            if(p.getSquaredDistance(observer)>172*172||!world.isChunkLoaded(px>>4,pz>>4)||!lho(p)||!world.getBlockState(p).isAir()||!world.getBlockState(p.down(8)).isAir()) continue;
            GHOSTS.put(key,new Ghost(p,WorldNoise.mix(salt+cycle*917),cycle));
        }
        for(var ghost:GHOSTS.values()) if(ghost.dissolved<0&&ghost.center.getSquaredDistance(observer)<48*48) {
            ghost.dissolved=time;ghostBursts++;
            for(int i=0;i<36;i++) {
                long salt=WorldNoise.mix(ghost.salt+i*433L);double a=WorldNoise.unit(salt)*Math.PI*2,r=4+WorldNoise.unit(WorldNoise.mix(salt))*14;
                world.addParticle(MIST,ghost.center.getX()+Math.cos(a)*r,ghost.center.getY()+(WorldNoise.unit(WorldNoise.mix(salt+71))-.5)*7,ghost.center.getZ()+Math.sin(a)*r,Math.cos(a)*.035,.018,Math.sin(a)*.035);
                if(i%3==0) world.addParticle(ParticleTypes.END_ROD,ghost.center.getX()+Math.cos(a)*r,ghost.center.getY()+WorldNoise.unit(salt)*3,ghost.center.getZ()+Math.sin(a)*r,Math.cos(a)*.03,.013,Math.sin(a)*.03);
            }
        }
        visibleGhosts=GHOSTS.values().stream().filter(g->AmbientPatterns.mirageOpacity(time,g.salt,Math.sqrt(g.center.getSquaredDistance(observer)),g.dissolved<0?-1:time-g.dissolved)>.001).sorted(Comparator.comparingDouble(g->g.center.getSquaredDistance(observer))).limit(2).toList();
    }
    private static void face(WorldRenderContext context,VertexConsumer out,Direction direction,double x,double y,double z,double sx,double sy,double sz,float r,float g,float b,float alpha) {
        WorldRenderer.renderFilledBoxFace(context.matrixStack(),out,direction,(float)x,(float)y,(float)z,(float)(x+sx),(float)(y+sy),(float)(z+sz),r,g,b,alpha);
    }
    private static void blockFilm(WorldRenderContext context,VertexConsumer out,Vec3d camera,BlockPos pos,double drift,float r,float g,float b,float alpha) {
        double x=pos.getX()-camera.x+drift-.003,y=pos.getY()-camera.y-.003,z=pos.getZ()-camera.z-.003;
        for(var direction:Direction.values()) {
            double dot=(camera.x-pos.getX()-.5)*direction.getOffsetX()+(camera.y-pos.getY()-.5)*direction.getOffsetY()+(camera.z-pos.getZ()-.5)*direction.getOffsetZ();
            if(dot>.5) face(context,out,direction,x,y,z,1.006,1.006,1.006,r,g,b,alpha);
        }
    }
    private static void render(WorldRenderContext context) {
        if(world==null||world!=context.world()||context.consumers()==null||context.matrixStack()==null) return;
        Vec3d camera=context.camera().getPos();double time=world.getTime()+context.tickCounter().getTickDelta(false);
        var out=context.consumers().getBuffer(RenderLayer.getDebugQuads());
        if(underworld()) for(var beat:beats) {
            var p=beat.pos();var pos=new BlockPos(p.x(),p.y(),p.z());
            var state=world.getBlockState(pos);
            if(pos.getSquaredDistance(camera)>84*84||(!(state.getBlock() instanceof ChristmasDigitBlock))) continue;
            double brightness=BandRhythm.brightness(beat,time);
            blockFilm(context,out,camera,pos,0,.012f,.022f,.018f,(float)(.80*(1-brightness)));
            if(brightness>.76) blockFilm(context,out,camera,pos,.001,.80f,.97f,.84f,(float)((brightness-.76)*.8));
        }
        for(var pulse:MUSIC.values()) {
            double age=time-pulse.time;if(age>12) continue;
            blockFilm(context,out,camera,pulse.note,0,.55f,.96f,1f,(float)(.52*(1-age/12)));
        }
        for(var ghost:visibleGhosts) {
            double distance=ghost.center.toCenterPos().distanceTo(camera),age=ghost.dissolved<0?-1:time-ghost.dissolved;
            float alpha=(float)AmbientPatterns.mirageOpacity(time,ghost.salt,distance,age);if(alpha<=.001) continue;
            double x=ghost.center.getX()-camera.x,y=ghost.center.getY()-camera.y+Math.sin(time/57+WorldNoise.unit(ghost.salt)*6)*.6,z=ghost.center.getZ()-camera.z;
            for(var f:ghost.mesh) {
                float shade=f.direction()==1?1:f.direction()==0?.55f:.78f;
                face(context,out,Direction.values()[f.direction()],x+f.x(),y+f.y(),z+f.z(),3,2,3,.65f*shade,.72f*shade,.91f*shade,alpha);
            }
        }
    }
    public static long notesHeard() { return notesHeard; }
    public static long silentRests() { return silentRests; }
    public static long ambientSounds() { return ambientSounds; }
    public static long ghostBursts() { return ghostBursts; }
    public static int lightCount() { return beats.size(); }
    public static int fiberCount() { return FIBERS.size(); }
    public static double meanBrightness() { return world==null?0:beats.stream().mapToDouble(b->BandRhythm.brightness(b,world.getTime())).average().orElse(0); }
    public static int activeSoundCount() { return (int)SOUNDS.stream().filter(s->MinecraftClient.getInstance().getSoundManager().isPlaying(s)).count(); }
    public static List<GhostView> ghostViews(Vec3d camera) {
        if(world==null) return List.of();double time=world.getTime();
        return visibleGhosts.stream().map(g->new GhostView(g.center,g.salt,AmbientPatterns.mirageOpacity(time,g.salt,g.center.toCenterPos().distanceTo(camera),g.dissolved<0?-1:time-g.dissolved),g.dissolved>=0,g.mesh.size())).toList();
    }
}
