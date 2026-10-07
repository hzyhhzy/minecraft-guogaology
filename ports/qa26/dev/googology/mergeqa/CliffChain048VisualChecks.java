package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.world.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Read-only native generation checks and four photographs in disposable, hidden fo262. */
public final class CliffChain048VisualChecks {
    private static final int SEARCH_MIN=12000,SEARCH_MAX=18000;
    // Offline discoveries for the existing fixture; every candidate is replanned from the LIVE seed.
    private static final int[][] PREFERRED_CELLS={{68,67},{64,73},{56,59},{76,72},{70,70},{66,71},{72,72},{72,62}};
    private static final String[] SCENES={"overview","upper-attachment","lower-attachment","wet-valley-foot"};
    private record Camera(Vec3 position,Vec3 target,float yaw,float pitch) {}
    private record Subject(CliffDescentChains.Plan plan,Map<BlockPos,Integer> geometry,
                           Map<BlockPos,Integer> samples,Map<BlockPos,Boolean> holes,
                           List<BlockPos> contacts,Camera[] cameras) {}
    private int scene,ticks,checks,settling,searchIndex,plansFound;
    private boolean opening,queued,capturing,done;
    private volatile boolean taskFinished,ready,photographed,verificationQueued,verified;
    private volatile Throwable failure;
    private volatile float yaw,pitch;
    private Subject subject,primary;
    private long seed;
    private final long deadline=System.nanoTime()+950_000_000_000L;

    public static void initialize(){var q=new CliffChain048VisualChecks();ClientTickEvents.END_CLIENT_TICK.register(q::tick);}
    private void require(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("cliff-chain QA timeout scene="+scene+" searched="+searchIndex+" plans="+plansFound);
            c.options.pauseOnLostFocus=false;c.options.renderDistance().set(10);c.options.framerateLimit().set(60);
            if(!c.gui.hud.isHidden())c.gui.hud.toggle();
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){
                queued=true;taskFinished=false;ready=false;var id=c.player.getUUID();
                c.getSingleplayerServer().execute(()->{
                    try{ready=setup(c.getSingleplayerServer().getPlayerList().getPlayer(id));}
                    catch(Throwable e){failure=e;}
                    finally{taskFinished=true;}
                });return;
            }
            if(!taskFinished)return;
            if(!ready){queued=false;return;}
            c.player.setYRot(yaw);c.player.setXRot(pitch);c.player.yRotO=yaw;c.player.xRotO=pitch;c.player.setYHeadRot(yaw);
            if(capturing){
                if(!photographed)return;
                if(++scene==SCENES.length){
                    done=true;Files.writeString(Path.of("port-client-ok.txt"),"CLIFF_CHAIN048_OK checks="+checks+" scenes=4 seed="+seed+" dryLinks="+primary.plan.links()+" wetLinks="+subject.plan.links()+" sampledBlocks="+(primary.samples.size()+subject.samples.size())+" waterHoles="+subject.holes.values().stream().filter(Boolean::booleanValue).count()+" native FULL generation / dry bridge and wet valley-foot chain / alternating open links / natural attachments / original air and water retained\n");c.stop();return;
                }
                if(scene==3){primary=subject;subject=null;searchIndex=0;plansFound=0;}
                ticks=0;settling=0;queued=false;capturing=false;photographed=false;verified=false;verificationQueued=false;return;
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
            // Server FULL is insufficient: wait for actual client delivery and native mesh rebuilding.
            for(var pos:subject.samples.keySet())if(!c.level.hasChunkAt(pos))return;
            for(var pos:subject.holes.keySet())if(!c.level.hasChunkAt(pos))return;
            if(++settling<80)return;
            var path=c.gameDirectory.toPath().resolve("screenshots/cliff-chain-048-"+scene+"-"+SCENES[scene]+".png");Files.createDirectories(path.getParent());
            capturing=true;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(path);photographed=true;}catch(Throwable e){failure=e;}});
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }

    private boolean setup(ServerPlayer player){
        require(player!=null,"QA player remains present");
        var world=player.level().getServer().getLevel(GoogologyMod.GUOGAO);require(world!=null,"underworld exists");
        if(subject==null){
            seed=ProceduralTerrain.seed(world.getChunkSource().randomState());
            // Bound each server task; this search never requests, loads or writes a world chunk.
            int first=Math.floorDiv(SEARCH_MIN,CliffDescentChains.CELL),last=Math.floorDiv(SEARCH_MAX,CliffDescentChains.CELL),side=last-first+1;
            int total=side*side+PREFERRED_CELLS.length;
            for(int work=0;work<6&&searchIndex<total;work++){
                int index=searchIndex++,gridIndex=index-PREFERRED_CELLS.length;
                int cx=index<PREFERRED_CELLS.length?PREFERRED_CELLS[index][0]:first+gridIndex%side;
                int cz=index<PREFERRED_CELLS.length?PREFERRED_CELLS[index][1]:first+gridIndex/side;
                var plan=CliffDescentChains.plan(seed,cx,cz);if(plan==null)continue;
                plansFound++;
                double drop=plan.top().y()-plan.bottom().y(),length=distance(plan.top(),plan.bottom());
                if(plan.links()<9||length<100||drop<45||!inSearch(plan.top())||!inSearch(plan.bottom()))continue;
                var bottom=BlockPos.containing(plan.bottom().x(),plan.bottom().y()+2,plan.bottom().z());
                if(scene<3&&(plan.valleyFoot()||plan.bottom().y()<4))continue;
                if(scene==3&&(!plan.valleyFoot()||!waterAt(seed,bottom)))continue;
                var candidate=prepareSubject(plan,scene==3);if(candidate==null)continue;
                subject=candidate;
                long wetSamples=subject.samples.keySet().stream().filter(pos->waterAt(seed,pos)).count();
                System.out.println("CLIFF_CHAIN048_SITE seed="+seed+" owner="+cx+","+cz+" top="+plan.top()+" bottom="+plan.bottom()+" links="+plan.links()+" length="+length+" nativeSamples="+subject.samples.size()+" underwaterSamples="+wetSamples+" openLinks="+subject.holes.size()+" waterHoles="+subject.holes.values().stream().filter(Boolean::booleanValue).count());
                for(int i=0;i<subject.cameras.length;i++)if(subject.cameras[i]!=null)System.out.println("CLIFF_CHAIN048_CAMERA "+(scene==3?SCENES[3]:SCENES[i])+" "+subject.cameras[i]);
                break;
            }
            if(subject==null){require(searchIndex<total,"long "+(scene==3?"wet valley-foot":"dry bridge")+" chain with clear camera found in new 12000..18000 terrain; plans="+plansFound);return false;}
        }
        var camera=subject.cameras[scene==3?2:scene];player.closeContainer();player.setGameMode(GameType.SPECTATOR);player.setNoGravity(true);player.removeAllEffects();
        // Preserve native terrain and scenery. Only a submerged QA camera gets temporary night vision.
        if(waterAt(seed,BlockPos.containing(camera.position.add(0,1.62,0))))player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,2400,0,false,false));
        // Teleport alone drives normal chunk delivery; no feature placement or chunk-forcing calls.
        player.teleport(new TeleportTransition(world,camera.position,Vec3.ZERO,camera.yaw,camera.pitch,TeleportTransition.DO_NOTHING));
        player.hasChangedDimension();if(!player.connection.hasClientLoaded())player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        yaw=player.getYRot();pitch=player.getXRot();return true;
    }

    private Subject prepareSubject(CliffDescentChains.Plan plan,boolean lowerOnly){
        int minX=(int)Math.floor(Math.min(plan.top().x(),plan.bottom().x())-9),maxX=(int)Math.ceil(Math.max(plan.top().x(),plan.bottom().x())+9);
        int minZ=(int)Math.floor(Math.min(plan.top().z(),plan.bottom().z())-9),maxZ=(int)Math.ceil(Math.max(plan.top().z(),plan.bottom().z())+9);
        var geometry=new HashMap<BlockPos,Integer>();
        // The production rasterizer supplies expected positions only; its sink is an in-memory map.
        CliffDescentChains.render(new VoxelBrush(minX,maxX,-62,317,minZ,maxZ,(x,y,z,m)->geometry.put(new BlockPos(x,y,z),m)),plan);
        if(geometry.size()<500)return null;
        var field=new TerrainSamples(plan.seed(),true);
        var holes=new LinkedHashMap<BlockPos,Boolean>();
        double length=distance(plan.top(),plan.bottom()),spacing=(length-2*plan.halfLength())/(plan.links()-1);
        // One independent center per loop catches accidental filling, including both alternating planes.
        for(int i=0;i<plan.links();i++){
            var center=along(plan,(plan.halfLength()+i*spacing)/length);
            var hole=rounded(center);
            if(geometry.containsKey(hole)||!open(field,hole))return null;
            holes.put(hole,waterAt(plan.seed(),hole));
        }
        if(lowerOnly&&!holes.containsValue(true))return null;
        var contacts=new ArrayList<BlockPos>();
        for(var end:List.of(plan.top(),plan.bottom())){
            int x=(int)Math.round(end.x()),y=(int)Math.round(end.y()),z=(int)Math.round(end.z());
            var below=new BlockPos(x,y-5,z);
            if(geometry.containsKey(below)||field.density(x,y-5,z)<=0)return null;
            contacts.add(below);
            int sides=0;
            for(int[] d:new int[][]{{-2,0},{2,0},{0,-2},{0,2}}){
                var side=new BlockPos(x+d[0],y-4,z+d[1]);
                if(!geometry.containsKey(side)&&field.density(side.getX(),side.getY(),side.getZ())>0){contacts.add(side);sides++;}
            }
            if(sides<1)return null;
        }
        var samples=new LinkedHashMap<BlockPos,Integer>();
        var byHeight=new TreeMap<Integer,List<BlockPos>>();
        geometry.forEach((pos,material)->byHeight.computeIfAbsent(pos.getY(),ignored->new ArrayList<>()).add(pos));
        for(var row:byHeight.values()){
            row.sort(Comparator.comparingInt((BlockPos pos)->pos.getX()).thenComparingInt(pos->pos.getZ()));
            // Multiple real voxels on EVERY generated height, not merely the two endpoints.
            for(int i=0;i<Math.min(4,row.size());i++){var pos=row.get(i*(row.size()-1)/Math.max(1,Math.min(4,row.size())-1));samples.put(pos,geometry.get(pos));}
        }
        // Keep each material represented at every link, and inspect both complete anchor bases.
        for(int i=0;i<plan.links();i++){
            Vec3 center=along(plan,(plan.halfLength()+i*spacing)/length);
            for(int material:new int[]{NaturalForms.SCG_EDGE,UnderworldScenery.BLACKSTONE}){
                var nearest=geometry.entrySet().stream().filter(e->e.getValue()==material).min(Comparator.comparingDouble(e->distanceSquared(center,e.getKey()))).orElseThrow();
                samples.put(nearest.getKey(),nearest.getValue());
            }
        }
        for(var end:List.of(plan.top(),plan.bottom()))for(var e:geometry.entrySet()){
            var p=e.getKey();if(Math.abs(p.getX()-end.x())<=1&&Math.abs(p.getZ()-end.z())<=1&&p.getY()>=end.y()-4&&p.getY()<=end.y()+3)samples.put(p,e.getValue());
        }
        var cameras=new Camera[3];
        for(int i=lowerOnly?2:0;i<cameras.length;i++){cameras[i]=camera(plan,field,geometry,i);if(cameras[i]==null)return null;}
        require(plan.top().y()>plan.bottom().y(),"chain strictly descends from upper ground to lower ground");
        require(holes.size()==plan.links(),"one unfilled central aperture in every alternating link");
        require(samples.values().containsAll(List.of(NaturalForms.SCG_EDGE,UnderworldScenery.BLACKSTONE)),"two native chain materials represented");
        if(lowerOnly)require(holes.containsValue(true),"wet valley-foot fixture includes a genuine water-filled link aperture");
        return new Subject(plan,geometry,samples,Map.copyOf(holes),List.copyOf(contacts),cameras);
    }

    private static Camera camera(CliffDescentChains.Plan plan,TerrainSamples field,Map<BlockPos,Integer> geometry,int scene){
        boolean submergedEnd=scene==2&&waterAt(plan.seed(),new BlockPos((int)Math.round(plan.bottom().x()),(int)Math.round(plan.bottom().y())+2,(int)Math.round(plan.bottom().z())));
        double t=scene==0?.5:scene==1?.08:.92;
        Vec3 target=submergedEnd?new Vec3(plan.bottom().x(),plan.bottom().y()+1,plan.bottom().z()):along(plan,t);
        double heading=Math.atan2(plan.bottom().z()-plan.top().z(),plan.bottom().x()-plan.top().x());
        double[] radii=scene==0?new double[]{140,128,112,96}:submergedEnd?new double[]{16,10,24,34}:new double[]{45,58,70};
        // View from the open side of the cliff, oblique enough to expose both loop planes.
        for(double radius:radii)for(double angle:new double[]{-Math.PI/3,Math.PI/3,-Math.PI/2,Math.PI/2,-Math.PI/6,Math.PI/6,0,Math.PI}){
            double cameraY=submergedEnd?Math.min(target.y+2,-3.5):target.y+(scene==0?12:18);
            Vec3 position=new Vec3(target.x+Math.cos(heading+angle)*radius,cameraY,target.z+Math.sin(heading+angle)*radius);
            if(position.y>310||position.y< -52)continue;
            var body=BlockPos.containing(position);
            boolean clear=true;
            for(int dx=-1;dx<=1&&clear;dx++)for(int dz=-1;dz<=1&&clear;dz++)for(int dy=0;dy<=2;dy++){
                var pos=body.offset(dx,dy,dz);if(!open(field,pos)||geometry.containsKey(pos)){clear=false;break;}
            }
            if(!clear)continue;
            // Keep the whole chain inside the normally delivered ten-chunk camera neighborhood.
            for(var pos:geometry.keySet())if(Math.abs(pos.getX()-position.x)>140||Math.abs(pos.getZ()-position.z)>140){clear=false;break;}
            if(!clear)continue;
            var eye=position.add(0,1.62,0);
            if(submergedEnd&&!waterAt(plan.seed(),BlockPos.containing(eye)))continue;
            double[] fractions=scene==0?new double[]{.08,.3,.5,.7,.92}:scene==1?new double[]{.02,.12,.25}:submergedEnd?new double[]{.92,.97,1}:new double[]{.75,.88,.98};
            for(double f:fractions)if(!clearRay(field,eye,along(plan,f))){clear=false;break;}
            if(!clear)continue;
            float yaw=(float)Math.toDegrees(Math.atan2(position.x-target.x,target.z-position.z));
            float pitch=(float)Math.toDegrees(Math.atan2(eye.y-target.y,Math.hypot(position.x-target.x,position.z-target.z)));
            return new Camera(position,target,yaw,pitch);
        }
        return null;
    }
    private static boolean clearRay(TerrainSamples field,Vec3 from,Vec3 target){
        double length=from.distanceTo(target);int steps=(int)Math.ceil(length/2);
        for(int i=1;i<steps;i++){double t=i/(double)steps;if(t>.97)break;var pos=BlockPos.containing(from.lerp(target,t));if(!open(field,pos))return false;}
        return true;
    }

    /** Never requests or synchronously generates chunks: only already loaded FULL chunks qualify. */
    private boolean verifyGenerated(ServerPlayer player){
        var world=player.level();require(world.dimension().equals(GoogologyMod.GUOGAO),"native underworld dimension");
        var positions=new ArrayList<>(subject.samples.keySet());positions.addAll(subject.holes.keySet());positions.addAll(subject.contacts);
        var camera=BlockPos.containing(subject.cameras[scene==3?2:scene].position);positions.add(camera);positions.add(camera.above());
        for(var pos:positions)if(world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null)return false;
        for(var e:subject.samples.entrySet())require(world.getBlockState(e.getKey()).is(material(e.getValue())),"native generated chain material "+e.getValue()+" at "+e.getKey()+" found "+world.getBlockState(e.getKey()));
        for(var e:subject.holes.entrySet()){
            var state=world.getBlockState(e.getKey());require(e.getValue()?state.is(Blocks.WATER):state.isAir(),"alternating chain loop retains native "+(e.getValue()?"water":"air")+" at "+e.getKey()+" found "+state);
        }
        for(var pos:subject.contacts){var state=world.getBlockState(pos);require(!state.isAir()&&state.getFluidState().isEmpty()&&!state.is(Blocks.POLISHED_ANDESITE)&&!state.is(Blocks.POLISHED_BLACKSTONE),"anchor directly touches retained natural ground "+pos+" found "+state);}
        for(var pos:List.of(camera,camera.above())){var state=world.getBlockState(pos);require(waterAt(seed,pos)?state.is(Blocks.WATER):state.isAir(),"actual camera has native air/water clearance "+pos+" found "+state);}
        System.out.println("CLIFF_CHAIN048_GENERATED scene="+SCENES[scene]+" sampled="+subject.samples.size()+" holes="+subject.holes.size()+" contacts="+subject.contacts.size()+" camera="+camera);
        return true;
    }
    private static Block material(int id){return switch(id){case NaturalForms.SCG_EDGE->Blocks.POLISHED_ANDESITE;case UnderworldScenery.BLACKSTONE->Blocks.POLISHED_BLACKSTONE;default->throw new AssertionError("Unmapped cliff-chain material "+id);};}
    private static Vec3 along(CliffDescentChains.Plan plan,double t){return new Vec3(plan.top().x()+(plan.bottom().x()-plan.top().x())*t,plan.top().y()+2.5+(plan.bottom().y()-plan.top().y())*t,plan.top().z()+(plan.bottom().z()-plan.top().z())*t);}
    private static BlockPos rounded(Vec3 v){return new BlockPos((int)Math.round(v.x),(int)Math.round(v.y),(int)Math.round(v.z));}
    private static boolean open(TerrainSamples field,BlockPos p){return p.getY()> -60&&p.getY()<318&&field.density(p.getX(),p.getY(),p.getZ())<=0&&field.uncarved(p.getX(),p.getY(),p.getZ())<=0;}
    private static boolean waterAt(long seed,BlockPos p){return p.getY()>=WaterField.UNDERWORLD_LEVEL-32&&TerrainField.column(seed,p.getX(),p.getZ(),true).water.submerged(p.getY());}
    private static boolean inSearch(CliffDescentChains.Point p){return p.x()>=SEARCH_MIN&&p.x()<SEARCH_MAX&&p.z()>=SEARCH_MIN&&p.z()<SEARCH_MAX;}
    private static double distance(CliffDescentChains.Point a,CliffDescentChains.Point b){return Math.sqrt(Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2));}
    private static double distanceSquared(Vec3 a,BlockPos b){return Math.pow(a.x-b.getX(),2)+Math.pow(a.y-b.getY(),2)+Math.pow(a.z-b.getZ(),2);}
}
