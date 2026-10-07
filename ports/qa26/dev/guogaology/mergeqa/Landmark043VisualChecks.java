package dev.guogaology.mergeqa;

import dev.guogaology.survival.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Nine focused in-game screenshots. Copies only authored exhibits and the final pagoda cutaway. */
public final class Landmark043VisualChecks {
    private int stage,ticks,checks,totalBlocks;
    private boolean opening,queued,capturing,done;
    private volatile boolean ready,photographed;
    private volatile Throwable failure;
    private volatile float yaw,pitch;
    private final Set<BlockPos> placed=new LinkedHashSet<>();
    private final long deadline=System.nanoTime()+285_000_000_000L;
    public static void initialize(){var check=new Landmark043VisualChecks();ClientTickEvents.END_CLIENT_TICK.register(check::tick);}
    private void require(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private String name(){return stage<8?String.format("landmark-043-%02d-%s-restored-interior-left-specimens-right",stage+1,SurvivalTheme.values()[stage].id):"landmark-043-09-power-highest-arena-cutaway";}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("landmark visual QA timeout "+stage);
            c.options.pauseOnLostFocus=false;c.options.renderDistance().set(5);c.options.framerateLimit().set(60);
            if(!c.gui.hud.isHidden())c.gui.hud.toggle();
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!queued){
                queued=true;ready=false;var id=c.player.getUUID();
                c.getSingleplayerServer().execute(()->{try{setup(c.getSingleplayerServer().getPlayerList().getPlayer(id));ready=true;}catch(Throwable e){failure=e;}});return;
            }
            if(!ready)return;
            c.player.setYRot(yaw);c.player.setXRot(pitch);c.player.yRotO=yaw;c.player.xRotO=pitch;c.player.setYHeadRot(yaw);
            if(capturing){
                if(!photographed)return;
                if(++stage==9){done=true;Files.writeString(Path.of("port-client-ok.txt"),"LANDMARK043_VISUAL_OK checks="+checks+" scenes=9 authoredExhibits=16 placedBlocks="+totalBlocks+" restored themed interiors vs specimen displays / original floor and palette / native chests / top-floor roof+posts+arena+stairs+main-chest cutaway\n");c.stop();return;}
                ticks=0;queued=false;capturing=false;photographed=false;return;
            }
            if(++ticks<85||c.gui.screen()!=null||c.gui.overlay()!=null)return;
            var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);var path=folder.resolve(name()+".png");
            capturing=true;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(path);photographed=true;}catch(Throwable e){failure=e;}});
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private void put(ServerLevel level,BlockPos pos,BlockState state){
        level.setBlock(pos,state,2);placed.add(pos.immutable());totalBlocks++;
    }
    private void setup(ServerPlayer p)throws Exception{
        if(stage==0)LandmarkChest043Checks.run(p);
        var world=p.level().getServer().overworld();
        for(var pos:placed)world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);placed.clear();
        p.closeContainer();p.setGameMode(GameType.SPECTATOR);p.setNoGravity(true);
        for(int n=0;n<p.getInventory().getContainerSize();n++)p.getInventory().setItem(n,ItemStack.EMPTY);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,12000,0,false,false));
        if(stage<8)exhibits(world,p,SurvivalTheme.values()[stage]);else pagoda(world,p);
        p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        yaw=p.getYRot();pitch=p.getXRot();
    }
    private void exhibits(ServerLevel world,ServerPlayer p,SurvivalTheme theme)throws Exception{
        var layout=SanctuaryLayout.of(theme);var site=new SurvivalStructures.Site(0,0,0,theme,0);
        var normal=model(theme,false);var treasure=model(theme,true);
        var ordinaryKind=kind(normal);var treasureKind=kind(treasure);
        var a=layout.furnishings().stream().filter(f->f.kind().equals(ordinaryKind)).findFirst().orElseThrow(()->new AssertionError("ordinary exhibit absent: "+theme+" / "+ordinaryKind));
        var b=layout.furnishings().stream().filter(f->f.kind().equals(treasureKind)&&f.blocks()==cells(treasure).size()).findFirst().orElseThrow(()->new AssertionError("complete restored specimen display absent: "+theme+" / "+treasureKind));
        int ordinary=copyExhibit(world,site,a,normal,-7),rare=copyExhibit(world,site,b,treasure,7);
        require(ordinary>0&&rare>0&&!ordinaryKind.equals(treasureKind),"both genuine restored interior and dedicated specimen display "+theme);
        System.out.println("LANDMARK043_EXHIBIT "+theme+" ordinary="+a+" treasure="+b+" blocks="+ordinary+"/"+rare);
        p.teleport(new TeleportTransition(world,new Vec3(.5,286,23),Vec3.ZERO,180,10,TeleportTransition.DO_NOTHING));
    }
    private static Object model(SurvivalTheme theme,boolean treasure)throws Exception{
        var owner=Class.forName("dev.guogaology.survival.SanctuaryInteriors");
        if(treasure){var method=owner.getDeclaredMethod("resourceCase",SurvivalTheme.class,int.class,int.class,int.class);method.setAccessible(true);return method.invoke(null,theme,3,3,0);}
        String name=switch(theme){case MATRIX->"matrixConsole";case POWER->"graph";case HYDRA->"flowerBed";case ABSENCE->"absentDisplay";case WEAVER->"gummyTray";case ASTRA->"meeting";case GUOGAO->"confectionTable";case FRONTIER->"setStudy";};
        boolean parameter=theme==SurvivalTheme.POWER||theme==SurvivalTheme.ABSENCE;
        var method=parameter?owner.getDeclaredMethod(name,boolean.class):owner.getDeclaredMethod(name);method.setAccessible(true);
        return parameter?method.invoke(null,false):method.invoke(null);
    }
    private static String kind(Object model)throws Exception{var field=model.getClass().getDeclaredField("name");field.setAccessible(true);return (String)field.get(model);}
    @SuppressWarnings("unchecked") private static Map<SanctuaryLayout.Point,Byte> cells(Object model){try{var field=model.getClass().getDeclaredField("blocks");field.setAccessible(true);return (Map<SanctuaryLayout.Point,Byte>)field.get(model);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    @SuppressWarnings("unchecked") private int copyExhibit(ServerLevel world,SurvivalStructures.Site site,SanctuaryLayout.Furnishing furnishing,Object model,int centerX)throws Exception{
        var cells=cells(model);var floor=furnishing.floor();
        int minX=cells.keySet().stream().mapToInt(SanctuaryLayout.Point::x).min().orElseThrow()-1,maxX=cells.keySet().stream().mapToInt(SanctuaryLayout.Point::x).max().orElseThrow()+1;
        int minZ=cells.keySet().stream().mapToInt(SanctuaryLayout.Point::z).min().orElseThrow()-1,maxZ=cells.keySet().stream().mapToInt(SanctuaryLayout.Point::z).max().orElseThrow()+1;
        var stateRotation=switch(Math.floorMod(2-furnishing.rotation(),4)){case 1->Rotation.CLOCKWISE_90;case 2->Rotation.CLOCKWISE_180;case 3->Rotation.COUNTERCLOCKWISE_90;default->Rotation.NONE;};
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)for(int y=-1;y<=0;y++){
            var offset=rotate(new SanctuaryLayout.Point(x,y,z),furnishing.rotation());
            int sx=floor.x()+offset.x(),sy=floor.y()+y,sz=floor.z()+offset.z();byte code=site.layout().at(sx,sy,sz);
            if(code>SanctuaryLayout.AIR)put(world,new BlockPos(centerX-x,279+y,-z),SurvivalStructures.stateFor(site,sx,sy,sz,code).rotate(stateRotation));
        }
        int n=0;
        for(var local:cells.keySet()){
            var offset=rotate(local,furnishing.rotation());
            int x=floor.x()+offset.x(),y=floor.y()+offset.y(),z=floor.z()+offset.z();byte code=site.layout().at(x,y,z);
            require(code>SanctuaryLayout.AIR,"whole authored cell survives final layout "+furnishing.kind()+" / "+local);
            // Final-layout coordinates preserve the real material policy and saved variants.
            var state=SurvivalStructures.stateFor(site,x,y,z,code).rotate(stateRotation);
            put(world,new BlockPos(centerX-local.x(),279+local.y(),-local.z()),state);n++;
        }
        require(n==furnishing.blocks(),"copy exact authored model cell count "+furnishing.kind());return n;
    }
    private static SanctuaryLayout.Point rotate(SanctuaryLayout.Point q,int r){return switch(r%4){
        case 1->new SanctuaryLayout.Point(-q.z(),q.y(),q.x());case 2->new SanctuaryLayout.Point(-q.x(),q.y(),-q.z());case 3->new SanctuaryLayout.Point(q.z(),q.y(),-q.x());default->q;};}
    private void pagoda(ServerLevel world,ServerPlayer p){
        var layout=SanctuaryLayout.of(SurvivalTheme.POWER);var site=new SurvivalStructures.Site(0,60,80,SurvivalTheme.POWER,0);
        require(layout.arena.y()==209,"arena is the tenth and highest floor");int posts=0,roof=0,body=0;
        for(int y=206;y<layout.height;y++)for(int z=-32;z<=32;z++)for(int x=-32;x<=32;x++){
            byte code=layout.at(x,y,z);if(code<=SanctuaryLayout.AIR)continue;
            // Keep the complete arena and stair landing; remove only the front upper shell for the view.
            if(y>layout.arena.y()+4&&z>0)continue;
            put(world,new BlockPos(x,y+60,z+80),SurvivalStructures.stateFor(site,x,y,z,code));body++;
            if(code==SanctuaryLayout.LOG&&y>layout.arena.y()+4)posts++;
            if(code==SanctuaryLayout.DARK&&y>=232)roof++;
        }
        int main=0;for(var chest:layout.chests())if(chest.relic()){
            var q=chest.floor();require(q.y()==layout.arena.y(),"main chest floor follows highest arena");
            put(world,new BlockPos(q.x(),q.y()+61,q.z()+80),Blocks.CHEST.defaultBlockState());main++;
        }
        require(posts>0&&roof>0&&main==1&&body<30000,"bounded cutaway contains roof, posts, arena and one main chest");
        var last=layout.route().stream().filter(q->q.y()==209&&q.x()>=14).findFirst().orElseThrow(()->new AssertionError("highest stair landing absent"));
        require(layout.at(last.x(),last.y(),last.z())>SanctuaryLayout.AIR,"highest stair landing has support");
        System.out.println("LANDMARK043_PAGODA arena="+layout.arena+" stair="+last+" blocks="+body+" posts="+posts+" roof="+roof+" mainChests="+main);
        p.teleport(new TeleportTransition(world,new Vec3(49,298,143),Vec3.ZERO,142,20,TeleportTransition.DO_NOTHING));
    }
}
