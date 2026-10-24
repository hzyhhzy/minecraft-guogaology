package dev.guogaology.releaseqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.guogaology.CoreGrades;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.client.CoreAnimationRenderer;
import dev.guogaology.client.CoreMeshModels;
import dev.guogaology.portal.PortalKind;
import dev.guogaology.portal.PortalTravel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.sdl.SDLVideo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Small production-JAR smoke test, compiled only with -PreleaseQa263. */
public final class Release263Checks implements ClientModInitializer {
    private static final String WORLD="release-263-new";
    private static final String[] SCENES={"all-28-cores", "core-detail", "core-item-gui", "outer", "inner", "underworld", "overworld-return"};
    private static final List<ResourceKey<Level>> REALMS=List.of(GuogaologyMod.OUTER, GuogaologyMod.DIMENSION, GuogaologyMod.GUOGAO);
    private static final int GALLERY_Y=289;
    private final Set<BlockPos> submitted=ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> expectedMoving=new HashSet<>();
    private final JsonArray generation=new JsonArray();
    private final JsonArray pictures=new JsonArray();
    private final List<Block> cores=new ArrayList<>();
    private final long deadline=System.nanoTime()+1_050_000_000_000L;
    private boolean opening, queued, capturing, audited, done;
    private volatile boolean ready, captured;
    private volatile Throwable failure;
    private volatile ResourceKey<Level> expectedDimension=Level.OVERWORLD;
    private volatile Vec3 camera=Vec3.ZERO;
    private volatile float yaw, pitch;
    private int scene, ticks, checks;
    private int motionBlocksAudited;
    private boolean auditWarmQueued;
    private volatile boolean auditWarmReady;
    private int auditWarmTicks;

    @Override public void onInitializeClient() {
        var directory=FabricLoader.getInstance().getGameDir();
        if (!Boolean.getBoolean("guogaology.qa263") || !Files.isRegularFile(directory.resolve("qa263-isolated.json")))
            throw new IllegalStateException("26.3 QA requires its isolated runner marker and explicit flag");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }

    private void tick(Minecraft client) {
        if (done) return;
        try {
            if (failure!=null) throw new RuntimeException(failure);
            if(scene==SCENES.length&&MiningPattern0612Checks.enabled()){if(MiningPattern0612Visual.tick(client))finish(client);return;}
            if (System.nanoTime()>deadline) throw new AssertionError("26.3 smoke timed out at "+SCENES[scene]);
            client.options.pauseOnLostFocus=false;
            client.options.renderDistance().set(4);
            client.options.framerateLimit().set(60);
            client.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            client.gui.toastManager().clear();
            if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
            if (client.level==null) {
                if (!opening && client.isGameLoadFinished() && client.gui.overlay()==null) {
                    opening=true;
                    createWorld(client);
                }
                return;
            }
            if (client.player==null || client.getSingleplayerServer()==null || client.gui.overlay()!=null) return;
            if(General263Audit.enabled()&&!auditWarmReady){
                if(!auditWarmQueued){auditWarmQueued=true;client.getSingleplayerServer().execute(()->{try{General263Audit.prepare(client.getSingleplayerServer().overworld());auditWarmReady=true;}catch(Throwable error){failure=error;}});}return;
            }
            if(General263Audit.enabled()&&auditWarmTicks++<100)return;
            if (!audited) { auditModels(client); if(General263Audit.enabled())General263Audit.client(client); if(Repairs0610Checks.enabled())Repairs0610Checks.client(client); audited=true; }
            if (!queued) {
                queued=true;
                int current=scene;
                var playerId=client.player.getUUID();
                client.getSingleplayerServer().execute(()->{
                    try {
                        var player=client.getSingleplayerServer().getPlayerList().getPlayer(playerId);
                        setup(player, current);
                        ready=true;
                    } catch (Throwable error) { failure=error; }
                });
                return;
            }
            if (!ready || !client.level.dimension().equals(expectedDimension)) return;
            if (scene==2 ? !(client.gui.screen() instanceof AbstractContainerScreen<?>) : client.gui.screen()!=null) return;
            if (client.player.position().distanceToSqr(camera)>4) return;
            client.player.setYRot(yaw); client.player.yRotO=yaw; client.player.setYHeadRot(yaw);
            client.player.setXRot(pitch); client.player.xRotO=pitch;
            if (++ticks<180) return;
            if (!capturing) {
                check(!client.mouseHandler.isMouseGrabbed(), "SDL QA never captures the mouse");
                long flags=SDLVideo.SDL_GetWindowFlags(client.getWindow().handle());
                check((flags & SDLVideo.SDL_WINDOW_HIDDEN)!=0, "SDL window remains hidden");
                check((flags & SDLVideo.SDL_WINDOW_NOT_FOCUSABLE)!=0, "SDL window cannot take focus");
                if (scene==0) check(submitted.containsAll(expectedMoving), "all animated core bodies actually submitted: "+submitted.size()+"/"+expectedMoving.size());
                var folder=client.gameDirectory.toPath().resolve("screenshots");
                Files.createDirectories(folder);
                String name=String.format("release263-%02d-%s.png", scene, SCENES[scene]);
                capturing=true;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image->{
                    try (image) { image.writeToFile(folder.resolve(name)); captured=true; }
                    catch (Throwable error) { failure=error; }
                });
                pictures.add(name);
            }
            if (!captured) return;
            System.out.println("RELEASE263_SCENE_OK "+SCENES[scene]+" dimension="+expectedDimension.identifier());
            if (++scene==SCENES.length) { if(!MiningPattern0612Checks.enabled())finish(client); return; }
            queued=false; ready=false; capturing=false; captured=false; ticks=0;
        } catch (Throwable error) {
            done=true;
            error.printStackTrace();
            try { Files.writeString(client.gameDirectory.toPath().resolve("release-263-failed.txt"), error.toString()); }
            catch (Exception ignored) {}
            client.stop();
        }
    }

    private void createWorld(Minecraft client) throws Exception {
        var path=client.gameDirectory.toPath();
        check(!Files.exists(path.resolve("saves").resolve(WORLD)), "QA never reopens a normal or previous world");
        long seed=Long.getLong("guogaology.qa263.seed", 651600L);
        Files.writeString(path.resolve("fresh-world-seed.txt"), Long.toString(seed), StandardOpenOption.CREATE_NEW);
        var settings=new LevelSettings("Guogaology "+System.getProperty("guogaology.qa263.release")+" isolated 26.3 smoke", GameType.CREATIVE,
                LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
        client.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(seed, true, false),
                WorldPresets::createNormalWorldDimensions, new TitleScreen());
    }

    private void auditModels(Minecraft client) {
        String actual=FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString();
        check(actual.equals(System.getProperty("guogaology.qa263.release")), "exact requested production release loaded: "+actual);
        int family=0;
        for (String root:CoreGrades.ROOTS) {
            int grade=0;
            for (Block block:CoreGrades.levels(root)) {
                cores.add(block);
                String name=BuiltInRegistries.BLOCK.getKey(block).toString();
                var info=CoreMeshModels.distanceInfo(client.getModelManager().getBlockStateModelSet().get(block.defaultBlockState()));
                check(info!=null && info.fallbackFaces()>0, "real baked crystal mesh and fallback "+name);
                if (CoreGrades.upper(block) && info.movingFaces()>0) expectedMoving.add(corePosition(family, grade));
                for (var context:List.of(ItemDisplayContext.GUI, ItemDisplayContext.GROUND, ItemDisplayContext.FIXED)) {
                    var state=new ItemStackRenderState();
                    client.getItemModelResolver().updateForLiving(state, new ItemStack(block), context, client.player);
                    check(!state.isEmpty(), "native item model resolves "+name+" "+context);
                    var bounds=state.getModelBoundingBox();
                    check(bounds.getXsize()>0 && bounds.getYsize()>0, "nonempty native item geometry "+name+" "+context);
                    if (context==ItemDisplayContext.GUI && !root.equals("ordinal_crystal"))
                        check(bounds.getZsize()<.07, "flat regional GUI icon "+name);
                }
                grade++;
            }
            family++;
        }
        check(cores.size()==28, "all 28 core appearances and GUI items");
        CoreAnimationRenderer.distanceObserver=sample->{
            var position=new BlockPos(sample.x(), sample.y(), sample.z());
            if (expectedMoving.contains(position) && sample.faces()>0 && sample.mode()==CoreAnimationRenderer.DistanceMode.ANIMATED)
                submitted.add(position);
        };
        System.out.println("RELEASE263_MODELS_OK cores=28 moving="+expectedMoving.size()+" checks="+checks);
    }

    private static BlockPos corePosition(int family, int grade) {
        return new BlockPos((family-4)*4, GALLERY_Y+grade*4, 0);
    }

    private void setup(ServerPlayer player, int current) throws Exception {
        check(player!=null, "integrated server player exists");
        var server=player.level().getServer();
        player.closeContainer();
        player.setGameMode(GameType.CREATIVE);
        if (current==0) {
            if(General263Audit.enabled())General263Audit.server(player);
            if(Repairs0610Checks.enabled())Repairs0610Checks.server(player);
            auditMotionTags(server.overworld());
            for (var key:REALMS) check(server.getLevel(key)!=null, "registered dimension "+key.identifier());
            check(PortalTravel.destination(Level.OVERWORLD, PortalKind.OUTER).equals(GuogaologyMod.OUTER), "Outer travel mapping");
            check(PortalTravel.destination(GuogaologyMod.OUTER, PortalKind.INNER).equals(GuogaologyMod.DIMENSION), "Inner travel mapping");
            check(PortalTravel.destination(GuogaologyMod.DIMENSION, PortalKind.GUOGAO).equals(GuogaologyMod.GUOGAO), "Underworld travel mapping");
            var world=server.overworld();
            for (var position:BlockPos.betweenClosed(-20,GALLERY_Y-2,-4,20,GALLERY_Y+16,4))
                world.setBlock(position, (position.getY()==GALLERY_Y-2 ? Blocks.SMOOTH_QUARTZ :
                        position.getZ()==-3 ? Blocks.DEEPSLATE_TILES : Blocks.AIR).defaultBlockState(), 2);
            int family=0;
            for (String root:CoreGrades.ROOTS) {
                int grade=0;
                for (var block:CoreGrades.levels(root)) {
                    var position=corePosition(family, grade++);
                    world.setBlock(position.below(), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                    world.setBlock(position, block.defaultBlockState(), 2);
                    check(world.getBlockState(position).is(block), "registered core placed intact");
                    if (CoreGrades.upper(block)) check(world.getBlockEntity(position)!=null, "animated core entity placed");
                }
                family++;
            }
            move(player, world, new Vec3(.5,GALLERY_Y+5.5,34.5), 180, 0);
        } else if (current==1) {
            move(player, server.overworld(), new Vec3(9.5,GALLERY_Y+8,12.5), 180, 3);
        } else if (current==2) {
            move(player, server.overworld(), new Vec3(.5,GALLERY_Y+5.5,34.5), 180, 0);
            var inventory=new SimpleContainer(54);
            for (int i=0;i<cores.size();i++) inventory.setItem(i,new ItemStack(cores.get(i)));
            player.openMenu(new SimpleMenuProvider((id,slots,who)->ChestMenu.sixRows(id,slots,inventory),
                    Component.literal("Guogaology "+System.getProperty("guogaology.qa263.release")+" / Cores")));
            player.containerMenu.broadcastChanges();
        } else if (current>=3 && current<=5) {
            var world=server.getLevel(REALMS.get(current-3));
            naturalScene(player, world);
        } else {
            move(player, server.overworld(), new Vec3(.5,GALLERY_Y+5.5,34.5), 180, 0);
        }
        check(player.level().dimension().equals(expectedDimension), "server completed native teleport");
        System.out.println("RELEASE263_SETUP "+SCENES[current]+" camera="+camera+" dimension="+expectedDimension.identifier());
    }

    private record SurfaceProbe(int x,int z,int top,int occupied,int solid,int motion,int mismatches,JsonObject report) {}

    private void auditMotionTags(ServerLevel world) throws Exception {
        JsonObject catalog;
        try (var input=Release263Checks.class.getResourceAsStream("/guogaology-qa263/block-motion263.json")) {
            check(input!=null,"QA motion contract exists");
            catalog=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        var solids=new HashSet<String>();var leaves=new HashSet<String>();var known=new HashSet<String>();
        for (String key:List.of("motion_no_leaves","leaf_blocks","legacy_non_motion"))
            for (var entry:catalog.getAsJsonArray(key)) {
                String id=entry.getAsString();known.add(id);
                if (key.equals("motion_no_leaves")) solids.add(id);
                if (key.equals("leaf_blocks")) leaves.add(id);
            }
        var actualIds=new HashSet<String>();var reports=new JsonArray();var issues=new JsonArray();
        var at=new BlockPos(0,GALLERY_Y,0);
        for (Block block:BuiltInRegistries.BLOCK) {
            var identity=BuiltInRegistries.BLOCK.getKey(block);
            if (!identity.getNamespace().equals("guogaology")) continue;
            String id=identity.getPath();actualIds.add(id);
            var state=block.defaultBlockState();var shape=state.getCollisionShape(world,at);
            boolean physical=false;
            var row=new JsonObject();row.addProperty("id",identity.toString());
            row.addProperty("class",block.getClass().getName());
            if (!shape.isEmpty()) {
                var box=shape.bounds();
                double mean=(box.getXsize()+box.getYsize()+box.getZsize())/3;
                physical=mean>=.7291666666666666||box.getYsize()>=1;
                row.addProperty("collisionBounds",box.toString());row.addProperty("collisionMean",mean);
            }
            boolean leaf=block instanceof net.minecraft.world.level.block.LeavesBlock;
            row.addProperty("legacyPhysicalSolid",physical);row.addProperty("actualLeavesBlock",leaf);
            if ((physical&&!leaf)!=solids.contains(id)) issues.add("Default collision/catalog disagreement: "+id);
            if (leaf!=leaves.contains(id)) issues.add("Leaves class/catalog disagreement: "+id);
            boolean inherited=catalog.getAsJsonObject("native_inherited_exceptions").has(id);
            boolean noLeaves=solids.contains(id)||inherited,withLeaves=noLeaves||leaves.contains(id);
            var tags=List.of(net.minecraft.tags.BlockTags.BLOCKS_MOTION,net.minecraft.tags.BlockTags.BLOCKS_MOTION_NO_LEAVES,
                    net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP,net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES);
            var flags=new JsonArray();
            for (int i=0;i<tags.size();i++) {
                boolean present=state.is(tags.get(i));flags.add(present);
                if (present!=((i&1)==0?withLeaves:noLeaves)) issues.add("Native motion tag disagreement: "+id+" tag="+tags.get(i));
            }
            row.add("motionTags",flags);row.addProperty("nativeInheritedException",inherited);reports.add(row);
        }
        Files.writeString(FabricLoader.getInstance().getGameDir().resolve("block-motion263-audit.json"),reports.toString());
        check(actualIds.equals(known),"motion catalog covers every registered custom block");
        check(issues.isEmpty(),"actual collision/leaves and all four native motion tags agree: "+issues);
        var feature=world.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE)
                .getValue(GuogaologyMod.id("ore_ordinal_crystal"));
        check(feature instanceof net.minecraft.world.level.levelgen.feature.OreFeature,"native ordinal ore feature loaded");
        motionBlocksAudited=actualIds.size();
        System.out.println("RELEASE263_MOTION_TAGS_OK blocks="+motionBlocksAudited+" solids="+solids.size()+" leaves="+leaves.size()+" inheritedPressurePlate=1");
    }

    private SurfaceProbe probeTerrain(ServerLevel world,int centerX,int centerZ) {
        var chunks=new java.util.HashMap<Long,net.minecraft.world.level.chunk.LevelChunk>();
        int cx=centerX>>4,cz=centerZ>>4;
        for (int x=cx-1;x<=cx+1;x++) for (int z=cz-1;z<=cz+1;z++) {
            var chunk=world.getChunk(x,z);
            check(chunk!=null,"FULL terrain chunk generated");
            chunks.put(((long)x&0xffffffffL)|((long)z<<32),chunk);
        }
        int top=world.getMinY(),landX=centerX,landZ=centerZ,occupied=0,solid=0,motion=0,surface=0,mismatches=0;
        String topBlock="minecraft:air";
        var biomes=new HashSet<String>();
        var position=new BlockPos.MutableBlockPos();
        for (int dx=-12;dx<=12;dx+=4) for (int dz=-12;dz<=12;dz+=4) {
            int x=centerX+dx,z=centerZ+dz;
            var chunk=chunks.get(((long)(x>>4)&0xffffffffL)|((long)(z>>4)<<32));
            int height=world.getMinY(),solidHeight=world.getMinY();
            String firstBlock="minecraft:air";
            // Read actual loaded voxels: heightmap predicates changed to tags in
            // 26.3, so neither transparency nor missing tags may hide real land.
            for (int y=world.getMaxY();y>=world.getMinY();y--) {
                position.set(x,y,z);
                var state=chunk.getBlockState(position);
                if (state.isAir()) continue;
                if (height==world.getMinY()) {
                    height=y+1;
                    firstBlock=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                }
                if (state.getFluidState().isEmpty()&&!state.getCollisionShape(world,position).isEmpty()) {
                    solidHeight=y+1;
                    break;
                }
            }
            int mapped=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x&15,z&15)+1;
            int blocking=chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x&15,z&15)+1;
            if (mapped>world.getMinY()) surface++;
            if (blocking>world.getMinY()) motion++;
            if (mapped!=height) mismatches++;
            if (height>world.getMinY()) occupied++;
            if (solidHeight>world.getMinY()) solid++;
            if (height>top) {top=height;landX=x;landZ=z;topBlock=firstBlock;}
            biomes.add(chunk.getNoiseBiome((x&15)>>2,Math.max(world.getMinY(),height-1)>>2,(z&15)>>2)
                    .unwrapKey().orElseThrow().identifier().toString());
        }
        var report=new JsonObject();
        report.addProperty("centerX",centerX);report.addProperty("centerZ",centerZ);
        report.addProperty("fullChunks",chunks.size());report.addProperty("sampledColumns",49);
        report.addProperty("minY",world.getMinY());report.addProperty("maxY",world.getMaxY());
        report.addProperty("occupiedHeightColumns",occupied);report.addProperty("solidColumns",solid);
        report.addProperty("worldSurfaceColumns",surface);report.addProperty("motionBlockingColumns",motion);
        report.addProperty("worldSurfaceMismatches",mismatches);report.addProperty("highestSample",top);
        report.addProperty("highestBlock",topBlock);
        var ids=new JsonArray();biomes.stream().sorted().forEach(ids::add);report.add("biomes",ids);
        return new SurfaceProbe(landX,landZ,top,occupied,solid,motion,mismatches,report);
    }

    private void naturalScene(ServerPlayer player, ServerLevel world) throws Exception {
        check(world!=null, "destination level exists");
        var probes=new JsonArray();
        SurfaceProbe sample=probeTerrain(world,0,0);probes.add(sample.report);
        // A genuine floating-terrain gap gets at most one coarse ring, never an
        // unbounded search. Existing land with broken maps fails below instead.
        if (sample.occupied==0) {
            search:for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
                if (x==0&&z==0) continue;
                sample=probeTerrain(world,x*256,z*256);probes.add(sample.report);
                if (sample.occupied>0) break search;
            }
        }
        String name=world.dimension().identifier().getPath();
        Files.writeString(FabricLoader.getInstance().getGameDir().resolve("terrain263-"+name+"-probes.json"),probes.toString());
        check(sample.occupied>0&&sample.solid>0,"generated destination has real nonair terrain and collision support; "+probes);
        check(sample.mismatches==0,"WORLD_SURFACE must match actual loaded voxels; "+probes);
        check(sample.motion>0,"26.3 motion-heightmap tags must include real solid terrain (portal landing safety); "+probes);
        int eye=Math.min(world.getMaxY()-8,sample.top+18);
        move(player, world, new Vec3(sample.x+12.5,eye,sample.z+24.5),153,30);
        var report=sample.report.deepCopy();
        report.addProperty("dimension",world.dimension().identifier().toString());
        report.addProperty("camera",camera.toString());report.add("probes",probes);
        generation.add(report);
        System.out.println("RELEASE263_GENERATION "+report);
    }

    private void move(ServerPlayer player, ServerLevel world, Vec3 target, float viewYaw, float viewPitch) {
        expectedDimension=world.dimension(); camera=target; yaw=viewYaw; pitch=viewPitch;
        player.teleport(new TeleportTransition(world,target,Vec3.ZERO,yaw,pitch,TeleportTransition.DO_NOTHING));
        player.getAbilities().flying=true;
        player.onUpdateAbilities();
    }

    private void finish(Minecraft client) throws Exception {
        check(generation.size()==3, "all three custom realms generated and photographed");
        var result=new JsonObject(); result.addProperty("success",true);
        result.addProperty("version",System.getProperty("guogaology.qa263.release"));
        result.addProperty("minecraft","26.3"); result.addProperty("checks",checks);
        result.addProperty("cores",cores.size()); result.addProperty("movingBodiesSubmitted",submitted.size());
        result.addProperty("motionBlocksAudited",motionBlocksAudited);
        result.addProperty("sodium",FabricLoader.getInstance().isModLoaded("sodium"));
        result.addProperty("voxy",FabricLoader.getInstance().isModLoaded("voxy"));
        result.addProperty("seed",Long.getLong("guogaology.qa263.seed",651600L));
        result.add("generation",generation); result.add("screenshots",pictures);
        Files.writeString(client.gameDirectory.toPath().resolve("release-263-ok.json"),result.toString(),StandardOpenOption.CREATE_NEW);
        CoreAnimationRenderer.distanceObserver=null;
        done=true;
        System.out.println("RELEASE263_RUNTIME_OK "+result);
        client.stop();
    }
}
