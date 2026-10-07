package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.world.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Actual dimension biome dispatch and generated scenery, in a disposable hidden fo262 only. */
public final class Underworld046Checks {
    private int scene,ticks,checks,verifiedTicks;
    private boolean opening,queued,capturing,done;
    private volatile boolean ready,photographed;
    private volatile boolean verificationQueued,verified;
    private volatile Throwable failure;
    private volatile float yaw,pitch;
    private BlockPos[] sites;
    private final BlockPos[] anchors=new BlockPos[4];
    private final UnderworldScenery.SurfacePlan[] scenes=new UnderworldScenery.SurfacePlan[3];
    private DescendingChain.Plan caveChain;
    private DescendingChain.Chamber caveRoom;
    private int caveFloor;
    private float caveYaw,cavePitch;
    private final Map<BlockPos,Integer> expected=new LinkedHashMap<>();
    private final List<BlockPos> caveClearance=new ArrayList<>();
    private final long deadline=System.nanoTime()+950_000_000_000L;
    public static void initialize(){var q=new Underworld046Checks();ClientTickEvents.END_CLIENT_TICK.register(q::tick);}
    private void require(boolean ok,String what){checks++;if(!ok)throw new AssertionError(what);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("underworld QA timeout, scene="+scene);
            c.options.pauseOnLostFocus=false;c.options.renderDistance().set(5);c.options.framerateLimit().set(60);
            if(!c.gui.hud.isHidden())c.gui.hud.toggle();
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){queued=true;ready=false;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{
                try{setup(c.getSingleplayerServer().getPlayerList().getPlayer(id));ready=true;}catch(Throwable e){failure=e;}
            });return;}
            if(!ready)return;
            c.player.setYRot(yaw);c.player.setXRot(pitch);c.player.yRotO=yaw;c.player.xRotO=pitch;c.player.setYHeadRot(yaw);
            if(capturing){
                if(!photographed)return;
                if(++scene==4){done=true;Files.writeString(Path.of("port-client-ok.txt"),"UNDERWORLD046_OK checks="+checks+" scenes=4 native biome dispatch and newly generated terrain/scenery\n");c.stop();return;}
                ticks=0;verifiedTicks=0;queued=false;capturing=false;photographed=false;verified=false;verificationQueued=false;return;
            }
            if(++ticks<180||c.gui.screen()!=null||c.gui.overlay()!=null)return;
            if(!verified){
                if(!verificationQueued&&ticks%20==0){
                    verificationQueued=true;var id=c.player.getUUID();
                    c.getSingleplayerServer().execute(()->{
                        try{verified=verifyGenerated(c.getSingleplayerServer().getPlayerList().getPlayer(id));}
                        catch(Throwable e){failure=e;}
                        finally{verificationQueued=false;}
                    });
                }
                return;
            }
            // Await real client chunk delivery too; the remaining settling ticks let native meshes rebuild.
            for(var pos:expected.keySet())if(!c.level.hasChunkAt(pos))return;
            if(++verifiedTicks<60)return;
            var path=c.gameDirectory.toPath().resolve("screenshots/underworld-046-"+scene+"-"+UnderworldRegions.NAMES[scene]+".png");Files.createDirectories(path.getParent());
            capturing=true;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),im->{try(im){im.writeToFile(path);photographed=true;}catch(Throwable e){failure=e;}});
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private void setup(ServerPlayer p){
        var world=p.level().getServer().getLevel(GuogaologyMod.GUOGAO);
        require(world!=null,"underworld exists");
        long seed=ProceduralTerrain.seed(world.getChunkSource().randomState());
        if(sites==null){
            sites=new BlockPos[4];var source=world.getChunkSource().getGenerator().getBiomeSource();
            require(source.getClass().getSimpleName().equals("UnderworldBiomeSource"),"live new biome source: "+source.getClass());
            var sampler=world.getChunkSource().randomState().sampler();
            int[] counts=new int[3];
            for(int z=12000;z<16096;z+=64)for(int x=12000;x<16096;x+=64){
                int k=UnderworldRegions.surfaceKind(seed,x,z);counts[k]++;
                if(k!=UnderworldRegions.FOREST||sites[k]!=null)continue;
                var col=TerrainField.column(seed,x,z,true);int ground=col.surface(false,317);
                if(ground==Integer.MIN_VALUE||ground>265)continue;
                if(ground<60||col.water.fluid())continue;
                if(dev.guogaology.survival.SanctuaryClearing.excludesPlant(seed,x,z,70))continue;
                var biome=source.getNoiseBiome(x>>2,(ground+5)>>2,z>>2,sampler);
                require(biome.unwrapKey().orElseThrow().identifier().getPath().equals(UnderworldRegions.NAMES[k]),"surface biome mapping "+k);
                sites[k]=new BlockPos(x,Math.max(ground,0)+38,z+35);
                anchors[k]=new BlockPos(x,ground+5,z);
            }
            // Sparse scenery must be the actual authored subject, not an arbitrary nearby empty patch.
            for(int kind:new int[]{UnderworldRegions.STRATA,UnderworldRegions.MARSH}){
                int cell=kind==UnderworldRegions.STRATA?UnderworldScenery.STRATA_CELL:UnderworldScenery.MARSH_CELL;
                outer:for(int cz=12000/cell;cz<16096/cell;cz++)for(int cx=12000/cell;cx<16096/cell;cx++){
                    var plan=UnderworldScenery.surfacePlan(seed,cx,cz,kind);if(plan==null)continue;
                    int floor=plan.roots().stream().mapToInt(UnderworldScenery.Root::y).max().orElseThrow();
                    if(kind==UnderworldRegions.STRATA&&(floor<25||floor>260))continue;
                    var anchor=new BlockPos(plan.x(),Math.max(floor,0)+5,plan.z());
                    if(!source.getNoiseBiome(anchor.getX()>>2,anchor.getY()>>2,anchor.getZ()>>2,sampler).unwrapKey().orElseThrow().identifier().getPath().equals(UnderworldRegions.NAMES[kind]))continue;
                    scenes[kind]=plan;anchors[kind]=anchor;
                    int[] camera=switch(plan.turn()){case 1->new int[]{-40,0};case 2->new int[]{0,-40};case 3->new int[]{40,0};default->new int[]{0,40};};
                    sites[kind]=new BlockPos(plan.x()+camera[0],kind==UnderworldRegions.STRATA?floor+45:25,plan.z()+camera[1]);
                    break outer;
                }
            }
            for(int k=0;k<3;k++)require(sites[k]!=null&&counts[k]>50,"surface region present "+k+" count="+counts[k]);
            outer:for(int z=30;z<44;z++)for(int x=30;x<44;x++){
                var plan=DescendingChain.plan(seed,x,z);if(plan==null)continue;
                for(var room:plan.chambers()){
                    if(room.index()!=3)continue; // A real central Guogao Heart makes missing feature dispatch detectable.
                    int xx=(int)Math.round(room.x()),yy=(int)Math.round(room.y()),zz=(int)Math.round(room.z());
                    var sample=new TerrainSamples(seed,true);
                    if(sample.density(xx,yy,zz)>=0||sample.uncarved(xx,yy,zz)<6)continue;
                    if(UnderworldRegions.biomeKind(seed,xx,yy,zz)!=UnderworldRegions.DESCENT)continue;
                    if(dev.guogaology.survival.SanctuaryClearing.excludesPlant(seed,xx,zz,10))continue;
                    int floor=DescendingChain.chamberFloorY(seed,room);if(floor==Integer.MIN_VALUE)continue;
                    require(source.getNoiseBiome(xx>>2,yy>>2,zz>>2,sampler).unwrapKey().orElseThrow().identifier().getPath().equals(UnderworldRegions.NAMES[3]),"native underground biome");
                    caveChain=plan;caveRoom=room;caveFloor=floor;anchors[3]=new BlockPos(xx,yy,zz);
                    var next=plan.chambers().get(room.index()+1);
                    double nx=next.x()-xx,nz=next.z()-zz,length=Math.hypot(nx,nz);nx/=length;nz/=length;
                    int cameraX=xx+(int)Math.round(-nx*5-nz*2),cameraZ=zz+(int)Math.round(-nz*5+nx*2);
                    sites[3]=new BlockPos(cameraX,yy+2,cameraZ);
                    require(sample.density(cameraX,yy+2,cameraZ)<=0&&sample.density(cameraX,yy+3,cameraZ)<=0,"cave overview camera in air");
                    caveYaw=(float)Math.toDegrees(Math.atan2(cameraX-xx,zz-cameraZ));
                    cavePitch=(float)Math.toDegrees(Math.atan2(yy+3.6-(floor+4),Math.hypot(cameraX-xx,cameraZ-zz)));
                    break outer;
                }
            }
            require(sites[3]!=null,"descending chamber found");
            System.out.println("UNDERWORLD046_SITES seed="+seed+" "+java.util.Arrays.toString(sites)+" surfaceSamples="+java.util.Arrays.toString(counts));
        }
        expected.clear();caveClearance.clear();
        if(scene==1||scene==2){
            var plan=scenes[scene];var all=new HashMap<BlockPos,Integer>();
            UnderworldScenery.drawSurface(new VoxelBrush(plan.x()-27,plan.x()+27,-62,317,plan.z()-27,plan.z()+27,(x,y,z,m)->all.put(new BlockPos(x,y,z),m)),plan);
            sampleExpected(all);
        }else if(scene==3){
            var all=new HashMap<BlockPos,Integer>();
            UnderworldScenery.drawChamber(new VoxelBrush(caveRoom.x()-8,caveRoom.x()+8,-62,317,caveRoom.z()-8,caveRoom.z()+8,(x,y,z,m)->all.put(new BlockPos(x,y,z),m)),caveChain.salt(),caveRoom,caveFloor);
            sampleExpected(all);
            expected.put(new BlockPos(caveRoom.x(),caveFloor+5,caveRoom.z()),NaturalForms.GUOGAO_HEART);
            var field=new TerrainSamples(seed,true);
            for(int[] d:new int[][]{{7,0},{-7,0},{0,7},{0,-7}}){
                var pos=new BlockPos(caveRoom.x()+d[0],caveRoom.y()-1,caveRoom.z()+d[1]);
                if(field.density(pos.getX(),pos.getY(),pos.getZ())<=0&&field.density(pos.getX(),pos.getY()+1,pos.getZ())<=0&&!all.containsKey(pos)&&!all.containsKey(pos.above()))caveClearance.add(pos);
            }
            require(caveClearance.size()>=2,"room sculpture preserves at least two human-height passages");
        }
        var pos=sites[scene];p.closeContainer();p.setGameMode(GameType.SPECTATOR);p.setNoGravity(true);p.removeAllEffects();
        // Keep native ambient lighting. A temporary QA night-vision effect is used only inside the cavern.
        if(scene==3)p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,1200,0,false,false));
        float cameraYaw=scene==3?caveYaw:scene==1||scene==2?180+scenes[scene].turn()*90:180;
        p.teleport(new TeleportTransition(world,new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5),Vec3.ZERO,cameraYaw,scene==3?cavePitch:28,TeleportTransition.DO_NOTHING));
        p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        yaw=p.getYRot();pitch=p.getXRot();
    }
    private void sampleExpected(Map<BlockPos,Integer> all){
        var entries=new ArrayList<>(all.entrySet());entries.sort(Comparator.comparingInt((Map.Entry<BlockPos,Integer> e)->e.getKey().getY()).thenComparingInt(e->e.getKey().getX()).thenComparingInt(e->e.getKey().getZ()));
        require(entries.size()>100,"authored subject has complete geometry");
        for(int i=0;i<32;i++){var e=entries.get(i*(entries.size()-1)/31);expected.put(e.getKey(),e.getValue());}
    }
    /** Never synchronously generates terrain for the check: only inspect already loaded FULL chunks. */
    private boolean verifyGenerated(ServerPlayer p){
        var world=p.level();
        var positions=new ArrayList<>(expected.keySet());positions.add(anchors[scene]);positions.addAll(caveClearance);
        for(var pos:positions)if(world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)return false;
        require(world.getBiome(anchors[scene]).unwrapKey().orElseThrow().identifier().getPath().equals(UnderworldRegions.NAMES[scene]),"generated native biome "+scene);
        for(var e:expected.entrySet())require(world.getBlockState(e.getKey()).is(material(e.getValue())),"actual generated scenery material "+e.getValue()+" at "+e.getKey()+" found "+world.getBlockState(e.getKey()));
        for(var pos:caveClearance){require(world.getBlockState(pos).isAir(),"room side passage feet "+pos);require(world.getBlockState(pos.above()).isAir(),"room side passage head "+pos);}
        System.out.println("UNDERWORLD046_GENERATED scene="+scene+" blocks="+expected.size()+" clearPassages="+caveClearance.size()+" anchor="+anchors[scene]);
        return true;
    }
    private static Block material(int id){return switch(id){
        case NaturalForms.FIR_WOOD->GuogaologyBlocks.DREAD_LOG;
        case NaturalForms.FIR_LEAF->GuogaologyBlocks.DREAD_LEAVES;
        case NaturalForms.CRYSTAL->GuogaologyBlocks.ORDINAL_CRYSTAL;
        case NaturalForms.GUOGAO_HEART->GuogaologyBlocks.GUOGAO_HEART;
        case UnderworldScenery.BLACKSTONE->Blocks.POLISHED_BLACKSTONE;
        case UnderworldScenery.SMOKED_GLASS->Blocks.STAINED_GLASS.gray();
        default->throw new AssertionError("Unmapped scenery QA material "+id);
    };}
}
