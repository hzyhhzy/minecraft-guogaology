package dev.guogaology.client;

import com.google.gson.*;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.CoreGrades;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/** Geometry is baked once; the block entity renderer only transforms the cached interior. */
public final class CoreMeshModels {
    private static final Identifier ATLAS=Identifier.ofVanilla("textures/atlas/blocks.png");
    private static volatile Set<String> portableNames=Set.of();
    private CoreMeshModels(){}
    public static void initialize(){
        PreparableModelLoadingPlugin.<Map<String,JsonObject>>register((resources,executor)->CompletableFuture.supplyAsync(()->{
            Map<String,JsonObject> data=new HashMap<>();
            resources.findResources("core_meshes",id->id.getNamespace().equals("guogaology")&&id.getPath().endsWith(".json")).forEach((id,resource)->{
                try(var reader=resource.getReader()){
                    data.put(id.getPath().substring("core_meshes/".length(),id.getPath().length()-5),JsonParser.parseReader(reader).getAsJsonObject());
                }catch(Exception e){throw new IllegalStateException("Cannot load crystal surface "+id,e);}
            });
            // Resource packs may also provide a static mesh for the nine original materials.
            // Required crafted forms still fail loudly if any model is missing.
            for(String root:CoreGrades.ROOTS)for(int level=2;level<=(root.equals("ordinal_crystal")?4:3);level++)
                if(!data.containsKey(root+"_lv"+level))throw new IllegalStateException("Missing crystal mesh: "+root+"_lv"+level);
            return Map.copyOf(data);
        },executor),(data,context)->{
            portableNames=data.keySet().stream().filter(name->!name.equals("ordinal_crystal")).collect(java.util.stream.Collectors.toUnmodifiableSet());
            context.addModels(portableNames.stream().map(name->GuogaologyMod.id("item/core_views/"+name)).toList());
            context.modifyModelAfterBake().register((model,event)->{
            if(model==null||model instanceof GemModel||model instanceof InventoryIcon)return model;
            // Ordinal Lv1 keeps its original three-dimensional inventory mesh.
            // Every other core has an independent flat item/generated sprite.
            if(event.topLevelId()!=null&&event.topLevelId().variant().equals("inventory")&&!event.topLevelId().id().equals(GuogaologyMod.id("ordinal_crystal"))){
                var item=event.topLevelId().id();
                return item.getNamespace().equals("guogaology")&&portableNames.contains(item.getPath())?new InventoryIcon(model):model;
            }
            Identifier id=event.resourceId();if(id==null&&event.topLevelId()!=null)id=event.topLevelId().id();
            if(id==null||!id.getNamespace().equals("guogaology"))return model;
            String path=id.getPath();boolean portable=path.startsWith("item/core_views/");
            if(portable)path=path.substring("item/core_views/".length());
            else if(path.startsWith("item/")&&!path.equals("item/ordinal_crystal"))return model;
            if(path.startsWith("block/")||path.startsWith("item/"))path=path.substring(path.indexOf('/')+1);
            var geometry=data.get(path);return geometry==null?model:new GemModel(model,geometry,event.textureGetter(),portable);
            });
        });
    }
    /** Legacy item pipeline has no native display-context selector; only the model argument changes. */
    public static BakedModel itemView(ItemStack stack,ModelTransformationMode mode,BakedModel original){
        if(mode==ModelTransformationMode.GUI)return original instanceof InventoryIcon icon?icon.getWrappedModel():original;
        if(stack.isEmpty())return original;
        Identifier id=net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        if(!id.getNamespace().equals("guogaology")||!portableNames.contains(id.getPath()))return original;
        var models=(FabricBakedModelManager)net.minecraft.client.MinecraftClient.getInstance().getBakedModelManager();
        var portable=models.getModel(GuogaologyMod.id("item/core_views/"+id.getPath()));
        return portable instanceof GemModel?portable:original;
    }
    /** Vanilla's dropped-stack layout checks depth before calling ItemRenderer. */
    private static final class InventoryIcon extends ForwardingBakedModel {
        InventoryIcon(BakedModel icon){super(icon);}
        @Override public boolean hasDepth(){return true;}
    }
    public static boolean polished(BakedModel model){return model instanceof GemModel;}
    /** Read-only geometry inspection for the separate QA mod. No per-frame baking. */
    public record DistanceGeometryInfo(int shellFaces,int staticFaces,int fallbackFaces,int movingFaces){}
    public static DistanceGeometryInfo distanceInfo(BakedModel model){
        return model instanceof GemModel gem?new DistanceGeometryInfo(gem.shell.size(),gem.staticInterior.size(),gem.fallback.size(),gem.interior.values().stream().mapToInt(List::size).sum()):null;
    }
    static GemModel animated(BakedModel model){return model instanceof GemModel gem?gem:null;}
    static final class GemModel implements BakedModel,FabricBakedModel {
        private final BakedModel base;
        private final List<BakedQuad> general;
        final Map<Integer,List<BakedQuad>> interior=new TreeMap<>();
        final String motion;
        final float motionScale;
        final boolean extended;
        final List<BakedQuad> staticInterior;
        final float animationRadius,staticRadius;
        private final List<BakedQuad> shell;
        private final List<BakedQuad> fallback;
        private final RenderMaterial material;
        private final Map<Direction,List<BakedQuad>> culled=new EnumMap<>(Direction.class);
        GemModel(BakedModel base,JsonObject mesh,Function<SpriteIdentifier,Sprite> sprites,boolean portable){
            this.base=base;var all=new ArrayList<BakedQuad>();var fixed=new ArrayList<BakedQuad>();var distant=new ArrayList<BakedQuad>();
            material=RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
            motion=mesh.has("motion")?mesh.get("motion").getAsString():"rotate";
            motionScale=mesh.has("motion_scale")?mesh.get("motion_scale").getAsFloat():1;
            extended=mesh.has("extended")&&mesh.get("extended").getAsBoolean();
            Map<String,Sprite> textures=new HashMap<>();mesh.getAsJsonObject("textures").entrySet().forEach(e->
                    textures.put(e.getKey(),sprites.apply(new SpriteIdentifier(ATLAS,Identifier.of(e.getValue().getAsString())))));
            for(var element:mesh.getAsJsonArray("quads")){
                var q=element.getAsJsonObject();var vertices=q.getAsJsonArray("v");var colors=q.getAsJsonArray("c");
                var normals=q.getAsJsonArray("n");var uv=q.getAsJsonArray("uv");var sprite=textures.get(q.get("t").getAsString());
                int[] packed=new int[32];float nx=0,ny=0,nz=0;
                for(int i=0;i<4;i++){
                    var v=vertices.get(i).getAsJsonArray();var normal=normals.get(i).getAsJsonArray();
                    for(int j=0;j<3;j++)packed[i*8+j]=Float.floatToRawIntBits(v.get(j).getAsFloat()/16f);
                    packed[i*8+3]=colors.get(i).getAsInt();
                    packed[i*8+4]=Float.floatToRawIntBits(sprite.getFrameU(uv.get(i).getAsJsonArray().get(0).getAsFloat()));
                    packed[i*8+5]=Float.floatToRawIntBits(sprite.getFrameV(uv.get(i).getAsJsonArray().get(1).getAsFloat()));
                    float x=normal.get(0).getAsFloat(),y=normal.get(1).getAsFloat(),z=normal.get(2).getAsFloat();
                    packed[i*8+7]=(Math.round(x*127)&255)|((Math.round(y*127)&255)<<8)|((Math.round(z*127)&255)<<16);
                    nx+=x;ny+=y;nz+=z;
                }
                var quad=new BakedQuad(packed,-1,Direction.getFacing(nx,ny,nz),sprite,false);
                int part=q.has("part")?q.get("part").getAsInt():1;
                if(part<10){
                    var fallbackSprite=textures.get(q.get(q.has("lod_t")?"lod_t":"t").getAsString());
                    var fallbackUv=q.getAsJsonArray(q.has("lod_uv")?"lod_uv":"uv");int[] lod=packed.clone();
                    for(int i=0;i<4;i++){
                        for(int axis=0;axis<3;axis++)lod[i*8+axis]=Float.floatToRawIntBits(Math.clamp(Float.intBitsToFloat(lod[i*8+axis]),0f,1f));
                        lod[i*8+3]=0xffffffff;
                        lod[i*8+4]=Float.floatToRawIntBits(fallbackSprite.getFrameU(fallbackUv.get(i).getAsJsonArray().get(0).getAsFloat()));
                        lod[i*8+5]=Float.floatToRawIntBits(fallbackSprite.getFrameV(fallbackUv.get(i).getAsJsonArray().get(1).getAsFloat()));
                    }
                    distant.add(new BakedQuad(lod,-1,Direction.getFacing(nx,ny,nz),fallbackSprite,false));
                }
                if(part==0)fixed.add(quad);else interior.computeIfAbsent(part,k->new ArrayList<>()).add(quad);
                if(q.has("cull"))culled.computeIfAbsent(Direction.byName(q.get("cull").getAsString()),ignored->new ArrayList<>()).add(quad);
                else all.add(quad);
            }
            distant.sort(Comparator.comparingDouble(quad->{
                double radius=0;var vertices=quad.getVertexData();
                for(int axis=0;axis<3;axis++){double center=0;for(int i=0;i<4;i++)center+=Float.intBitsToFloat(vertices[i*8+axis])/4;radius+=(center-.5)*(center-.5);}
                return radius;
            }));
            shell=List.copyOf(fixed);fallback=List.copyOf(distant);interior.replaceAll((part,list)->List.copyOf(list));
            var still=new ArrayList<BakedQuad>();float movingRadius=.866026f,stillRadius=.866026f;
            for(var entry:interior.entrySet()){
                float scale=entry.getKey()>=10?1:motionScale;
                var pose=entry.getKey()<10?CoreStaticPose.matrix(motion,entry.getKey(),motionScale):null;
                for(var quad:entry.getValue()){
                    movingRadius=Math.max(movingRadius,radius(quad)*scale);
                    // Ordinary-distance static bodies match the zero-phase
                    // interior, not Voxy's clamped cell/material approximation.
                    if(pose!=null){var posed=staticQuad(quad,pose);still.add(posed);stillRadius=Math.max(stillRadius,radius(posed));}
                }
            }
            staticInterior=List.copyOf(still);animationRadius=movingRadius;staticRadius=stillRadius;
            if(portable){
                // Match the actual zero-phase body, including outer decorations;
                // Voxy's clipped single-cell approximation is deliberately not used.
                var held=new ArrayList<BakedQuad>(fixed);
                for(var entry:interior.entrySet()){
                    var pose=CoreStaticPose.matrix(motion,entry.getKey(),motionScale);
                    for(var quad:entry.getValue())held.add(staticQuad(quad,pose));
                }
                general=List.copyOf(held);culled.clear();
            }else general=List.copyOf(all);
            culled.replaceAll((side,list)->List.copyOf(list));
        }
        private static float radius(BakedQuad quad){
            float radius=0;int[] v=quad.getVertexData();
            for(int i=0;i<4;i++){float x=Float.intBitsToFloat(v[i*8])-.5f,y=Float.intBitsToFloat(v[i*8+1])-.5f,z=Float.intBitsToFloat(v[i*8+2])-.5f;radius=Math.max(radius,(float)Math.sqrt(x*x+y*y+z*z));}
            return radius;
        }
        private static BakedQuad staticQuad(BakedQuad quad,org.joml.Matrix4f pose){
            int[] vertices=quad.getVertexData().clone();var normalMatrix=pose.normal(new org.joml.Matrix3f());float nx=0,ny=0,nz=0;
            for(int i=0;i<4;i++){
                int offset=i*8;var point=new org.joml.Vector3f(Float.intBitsToFloat(vertices[offset]),Float.intBitsToFloat(vertices[offset+1]),Float.intBitsToFloat(vertices[offset+2]));pose.transformPosition(point);
                vertices[offset]=Float.floatToRawIntBits(point.x);vertices[offset+1]=Float.floatToRawIntBits(point.y);vertices[offset+2]=Float.floatToRawIntBits(point.z);
                int packed=vertices[offset+7];var normal=new org.joml.Vector3f((byte)packed/127f,(byte)(packed>>>8)/127f,(byte)(packed>>>16)/127f);normalMatrix.transform(normal).normalize();
                vertices[offset+7]=(Math.round(normal.x*127)&255)|((Math.round(normal.y*127)&255)<<8)|((Math.round(normal.z*127)&255)<<16);nx+=normal.x;ny+=normal.y;nz+=normal.z;
            }
            return new BakedQuad(vertices,-1,Direction.getFacing(nx,ny,nz),quad.getSprite(),false);
        }
        @Override public List<BakedQuad> getQuads(BlockState state,Direction face,Random random){
            // Ordinary quad consumers (including LOD renderers) get the complete
            // static body. FRAPI below keeps the nearby animated body separate.
            if(state!=null)return face==null?fallback:List.of();
            return face==null?general:culled.getOrDefault(face,List.of());
        }
        @Override public boolean isVanillaAdapter(){return false;}
        @Override public void emitBlockQuads(BlockRenderView view,BlockState state,BlockPos pos,Supplier<Random> random,RenderContext context){
            var emitter=context.getEmitter();for(var quad:shell)emitter.fromVanilla(quad,material,null).emit();
        }
        @Override public void emitItemQuads(ItemStack stack,Supplier<Random> random,RenderContext context){
            var emitter=context.getEmitter();for(var quad:general)emitter.fromVanilla(quad,material,null).emit();
            for(var list:culled.values())for(var quad:list)emitter.fromVanilla(quad,material,null).emit();
        }
        @Override public boolean useAmbientOcclusion(){return false;}
        @Override public boolean hasDepth(){return true;}
        @Override public boolean isSideLit(){return false;}
        @Override public boolean isBuiltin(){return false;}
        @Override public Sprite getParticleSprite(){return base.getParticleSprite();}
        @Override public ModelTransformation getTransformation(){return base.getTransformation();}
        @Override public ModelOverrideList getOverrides(){return base.getOverrides();}
    }
}
