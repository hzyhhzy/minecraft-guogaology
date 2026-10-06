package dev.googology.client;

import com.google.gson.*;
import dev.googology.CoreGrades;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperUnbakedModel;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.*;
import net.fabricmc.fabric.api.renderer.v1.model.MeshBakedGeometry;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/** Reload-time world meshes; Ordinal Lv1 inherits its original block geometry in inventory. */
public final class CoreMeshModels {
    private static final Identifier ATLAS=Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    private CoreMeshModels(){}
    public static void initialize(){
        PreparableModelLoadingPlugin.<Map<String,JsonObject>>register((shared,executor)->CompletableFuture.supplyAsync(()->{
            Map<String,JsonObject> data=new HashMap<>();
            shared.resourceManager().listResources("core_meshes",id->id.getNamespace().equals("googology")&&id.getPath().endsWith(".json")).forEach((id,resource)->{
                try(var reader=resource.openAsReader()){
                    data.put(id.getPath().substring("core_meshes/".length(),id.getPath().length()-5),JsonParser.parseReader(reader).getAsJsonObject());
                }catch(Exception e){throw new IllegalStateException("Cannot load crystal surface "+id,e);}
            });
            for(String root:CoreGrades.ROOTS)for(int level=2;level<=(root.equals("ordinal_crystal")?4:3);level++)
                if(!data.containsKey(root+"_lv"+level))throw new IllegalStateException("Missing crystal mesh: "+root+"_lv"+level);
            return Map.copyOf(data);
        },executor),(data,context)->{
            Map<String,Geometry> baked=new ConcurrentHashMap<>();
            context.modifyModelOnLoad().register((model,event)->{
                Identifier id=event.id();
                // Flat icons do not inherit a block model; original Ordinal Lv1 still does.
                if(!id.getNamespace().equals("googology")||!id.getPath().startsWith("block/"))return model;
                String name=id.getPath().substring(6);JsonObject mesh=data.get(name);
                if(mesh==null)return model;
                return new WrapperUnbakedModel(model){
                    @Override public UnbakedGeometry geometry(){
                        return (slots,baker,state,debug)->new MeshBakedGeometry(baked.computeIfAbsent(name,
                                key->new Geometry(mesh,baker.sprites(),debug)).all);
                    }
                };
            });
            context.modifyBlockModelAfterBake().register((model,event)->{
                Identifier id=BuiltInRegistries.BLOCK.getKey(event.state().getBlock());
                JsonObject mesh=id.getNamespace().equals("googology")?data.get(id.getPath()):null;
                if(mesh==null)return model;
                Geometry geometry=baked.computeIfAbsent(id.getPath(),key->new Geometry(mesh,event.baker().sprites(),()->id.toString()));
                return new GemModel(model,geometry);
            });
        });
    }
    public static boolean polished(BlockStateModel model){return model instanceof GemModel;}
    public record DistanceGeometryInfo(int shellFaces,int staticFaces,int fallbackFaces,int movingFaces){}
    public static DistanceGeometryInfo distanceInfo(BlockStateModel model){
        return model instanceof GemModel gem?new DistanceGeometryInfo(gem.geometry.fixedFaces,gem.geometry.staticInterior.size(),gem.geometry.fallback.size(),gem.geometry.interior.values().stream().mapToInt(List::size).sum()):null;
    }
    static GemModel animated(BlockStateModel model){return model instanceof GemModel gem?gem:null;}
    static final class GemModel implements BlockStateModel {
        final Geometry geometry;
        private final TextureAtlasSprite particle;
        private final CoreFallbackPart fallback;
        GemModel(BlockStateModel base,Geometry geometry){this.geometry=geometry;particle=base.particleIcon();fallback=new CoreFallbackPart(geometry.fallback,particle);}
        @Override public void collectParts(RandomSource random,List<BlockModelPart> parts){parts.add(fallback);}
        @Override public TextureAtlasSprite particleIcon(){return particle;}
        @Override public void emitQuads(QuadEmitter emitter,BlockAndTintGetter view,BlockPos pos,BlockState state,RandomSource random,Predicate<Direction> cullTest){geometry.fixed.outputTo(emitter);}
        @Override public Object createGeometryKey(BlockAndTintGetter view,BlockPos pos,BlockState state,RandomSource random){return this;}
    }
    static final class Geometry {
        final Mesh all,fixed;
        final List<BakedQuad> fallback;
        final Map<Integer,List<Surface>> interior;
        final String motion;
        final float motionScale;
        final boolean extended;
        final List<Surface> staticInterior;
        final float animationRadius,staticRadius;
        final int fixedFaces;
        Geometry(JsonObject mesh,SpriteGetter sprites,ModelDebugName debug){
            motion=mesh.has("motion")?mesh.get("motion").getAsString():"rotate";
            motionScale=mesh.has("motion_scale")?mesh.get("motion_scale").getAsFloat():1;
            extended=mesh.has("extended")&&mesh.get("extended").getAsBoolean();
            Map<String,TextureAtlasSprite> textures=new HashMap<>();
            mesh.getAsJsonObject("textures").entrySet().forEach(e->textures.put(e.getKey(),sprites.get(new Material(ATLAS,Identifier.parse(e.getValue().getAsString())),debug)));
            MutableMesh every=Renderer.get().mutableMesh(),shell=Renderer.get().mutableMesh();
            QuadEmitter everyOut=every.emitter(),shellOut=shell.emitter();
            Map<Integer,List<Surface>> moving=new TreeMap<>();
            List<JsonObject> vanilla=new ArrayList<>();
            int fixedCount=0;float movingRadius=.866026f;
            for(var element:mesh.getAsJsonArray("quads")){
                JsonObject q=element.getAsJsonObject();Surface face=new Surface(q,textures.get(q.get("t").getAsString()));
                face.emit(everyOut);
                int part=q.has("part")?q.get("part").getAsInt():1;
                if(part==0)fixedCount++;else movingRadius=Math.max(movingRadius,face.radius()*(part>=10?1:motionScale));
                // LOD cells retain the shell and static interior. Orbiting outer
                // ornaments cannot be represented faithfully within one cell.
                if(part<10)vanilla.add(q);
                if(part==0)face.emit(shellOut);else moving.computeIfAbsent(part,k->new ArrayList<>()).add(face);
            }
            all=every.immutableCopy();fixed=shell.immutableCopy();
            // Voxy's software baker writes depth even for translucent faces.
            // Draw the body before its glass, otherwise the first shell hides it.
            vanilla.sort(Comparator.comparingDouble(CoreFallbackPart::radiusSquared));
            fallback=vanilla.stream().map(q->CoreFallbackPart.bake(q,textures.get(q.get(q.has("lod_t")?"lod_t":"t").getAsString()))).toList();
            moving.replaceAll((key,value)->List.copyOf(value));interior=Collections.unmodifiableMap(moving);
            var still=new ArrayList<Surface>();float stillRadius=.866026f;
            for(var entry:moving.entrySet())if(entry.getKey()<10){
                var pose=CoreStaticPose.matrix(motion,entry.getKey(),motionScale);
                for(var surface:entry.getValue()){var posed=new Surface(surface,pose);still.add(posed);stillRadius=Math.max(stillRadius,posed.radius());}
            }
            staticInterior=List.copyOf(still);animationRadius=movingRadius;staticRadius=stillRadius;fixedFaces=fixedCount;
        }
    }
    static final class Surface {
        final float[] xyz=new float[12],normal=new float[12],uv=new float[8];
        final int[] argb=new int[4];
        final Direction cull,face;
        Surface(JsonObject q,TextureAtlasSprite sprite){
            float nx=0,ny=0,nz=0;
            for(int i=0;i<4;i++){
                var vertex=q.getAsJsonArray("v").get(i).getAsJsonArray();
                var normals=q.getAsJsonArray("n").get(i).getAsJsonArray();
                var texture=q.getAsJsonArray("uv").get(i).getAsJsonArray();
                for(int axis=0;axis<3;axis++){xyz[i*3+axis]=vertex.get(axis).getAsFloat()/16f;normal[i*3+axis]=normals.get(axis).getAsFloat();}
                int abgr=q.getAsJsonArray("c").get(i).getAsInt();
                argb[i]=(abgr&0xff00ff00)|((abgr&0xff)<<16)|((abgr>>>16)&0xff);
                uv[i*2]=sprite.getU(texture.get(0).getAsFloat());uv[i*2+1]=sprite.getV(texture.get(1).getAsFloat());
                nx+=normal[i*3];ny+=normal[i*3+1];nz+=normal[i*3+2];
            }
            face=Math.abs(nx)>=Math.abs(ny)&&Math.abs(nx)>=Math.abs(nz)?(nx<0?Direction.WEST:Direction.EAST):
                    Math.abs(ny)>=Math.abs(nz)?(ny<0?Direction.DOWN:Direction.UP):(nz<0?Direction.NORTH:Direction.SOUTH);
            cull=q.has("cull")?Direction.byName(q.get("cull").getAsString()):null;
        }
        Surface(Surface source,org.joml.Matrix4f pose){
            cull=source.cull;face=source.face;System.arraycopy(source.uv,0,uv,0,8);System.arraycopy(source.argb,0,argb,0,4);
            var normals=pose.normal(new org.joml.Matrix3f());
            for(int i=0;i<4;i++){
                var point=new org.joml.Vector3f(source.xyz[i*3],source.xyz[i*3+1],source.xyz[i*3+2]);pose.transformPosition(point);
                xyz[i*3]=point.x;xyz[i*3+1]=point.y;xyz[i*3+2]=point.z;
                var n=new org.joml.Vector3f(source.normal[i*3],source.normal[i*3+1],source.normal[i*3+2]);normals.transform(n).normalize();normal[i*3]=n.x;normal[i*3+1]=n.y;normal[i*3+2]=n.z;
            }
        }
        float radius(){float r=0;for(int i=0;i<4;i++){float x=xyz[i*3]-.5f,y=xyz[i*3+1]-.5f,z=xyz[i*3+2]-.5f;r=Math.max(r,(float)Math.sqrt(x*x+y*y+z*z));}return r;}
        void emit(QuadEmitter emitter){
            emitter.cullFace(cull).nominalFace(face).renderLayer(ChunkSectionLayer.TRANSLUCENT).diffuseShade(false).ambientOcclusion(TriState.FALSE);
            for(int i=0;i<4;i++)emitter.pos(i,xyz[i*3],xyz[i*3+1],xyz[i*3+2]).normal(i,normal[i*3],normal[i*3+1],normal[i*3+2]).color(i,argb[i]).uv(i,uv[i*2],uv[i*2+1]);
            emitter.emit();
        }
    }
}
