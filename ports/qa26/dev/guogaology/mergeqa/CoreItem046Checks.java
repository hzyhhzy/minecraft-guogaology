package dev.guogaology.mergeqa;

import com.google.gson.JsonParser;
import dev.guogaology.mining.EquipmentRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Disposable fo262 only: actual display-context resolution, then native screenshots. */
public final class CoreItem046Checks {
    private static final int Z=7100;
    private int checks,scene,ticks;private boolean opening,queued,done,audited,capturing;private volatile boolean ready,captured;private volatile Throwable failure;
    private final long deadline=System.nanoTime()+360_000_000_000L;
    public static void initialize(){var qa=new CoreItem046Checks();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static List<String> names(){var names=new ArrayList<String>();for(int type=0;type<9;type++)for(int level=1;level<=(type==8?4:3);level++)names.add(EquipmentRules.CORES[type]+(level==1?"":"_lv"+level));return names;}
    private ItemStackRenderState state(Minecraft c,String name,ItemDisplayContext context){var state=new ItemStackRenderState();c.getItemModelResolver().updateForLiving(state,item(name),context,c.player);return state;}
    private void audit(Minecraft c)throws Exception{
        for(String name:names()){
            boolean original=name.equals("ordinal_crystal");
            try(var reader=c.getResourceManager().getResourceOrThrow(Identifier.parse("guogaology:items/"+name+".json")).openAsReader()){
                var model=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("model");
                check(model.get("type").getAsString().equals(original?"minecraft:model":"minecraft:select"),"actual item entrypoint "+name);
                if(!original){check(model.get("property").getAsString().equals("minecraft:display_context"),"native display context "+name);check(model.getAsJsonArray("cases").get(0).getAsJsonObject().get("when").getAsString().equals("gui"),"only GUI uses icon "+name);}
            }
            var gui=state(c,name,ItemDisplayContext.GUI);check(!gui.isEmpty(),"GUI loads "+name);
            var guiBounds=gui.getModelBoundingBox();check(original||guiBounds.getZsize()<.07,"GUI remains genuinely flat "+name+" "+guiBounds);
            for(var context:ItemDisplayContext.values())if(context!=ItemDisplayContext.GUI){
                var rendered=state(c,name,context);check(!rendered.isEmpty(),"loaded portable context "+name+" "+context);
                var particle=rendered.pickParticleMaterial(net.minecraft.util.RandomSource.create(46));check(particle!=null&&!particle.sprite().contents().name().getPath().contains("missing"),"real portable material "+name+" "+context);
                var bounds=rendered.getModelBoundingBox();check(bounds.getXsize()>.08&&bounds.getYsize()>.08&&bounds.getZsize()>.08,"actual three dimensional extents "+name+" "+context+" "+bounds);
                check(!rendered.isAnimated(),"no animated item model "+name+" "+context);
                check(bounds.equals(state(c,name,context).getModelBoundingBox()),"stable cached pose "+name+" "+context);
            }
        }
        System.out.println("CORE_ITEM046_CONTEXT_OK checks="+checks+" coreItems=28 flatGui=27 staticHandGroundFixed=27 ordinalOriginal=1");
    }
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("core item QA timeout "+scene);
            c.options.pauseOnLostFocus=false;c.options.framerateLimit().set(60);c.options.renderDistance().set(4);c.options.setCameraType(CameraType.FIRST_PERSON);
            c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null)return;
            if(!audited){audit(c);audited=true;}
            if(!queued){queued=true;ready=false;var id=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{setup(c.getSingleplayerServer().getPlayerList().getPlayer(id));ready=true;}catch(Throwable e){failure=e;}});return;}
            if(!ready||++ticks<100)return;
            if(capturing){
                if(!captured)return;
                if(++scene==7){done=true;Files.writeString(Path.of("port-client-ok.txt"),"CORE_ITEM046_OK checks="+checks+" 28items / 10contexts / GUI icons + static main/offhand/dropped/fixed / 7nativeScreenshots\n");c.stop();return;}
                queued=false;ticks=0;capturing=false;captured=false;return;
            }
            var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int frame=scene;capturing=true;
            Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("core-item046-"+frame+".png"));captured=true;}catch(Exception e){failure=e;}});
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private void setup(ServerPlayer p){
        var world=p.level();p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.closeContainer();
        for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);
        p.getInventory().setSelectedSlot(0);
        if(scene==0){
            var inventory=new SimpleContainer(54);int i=0;for(String name:names())inventory.setItem(i++,item(name));
            p.openMenu(new SimpleMenuProvider((id,slots,who)->ChestMenu.sixRows(id,slots,inventory),Component.literal("Core inventory icons / Lv1–3")));
        }else if(scene==1){
            for(var q:BlockPos.betweenClosed(new BlockPos(-9,219,Z-3),new BlockPos(9,219,Z+12)))world.setBlock(q,Blocks.SMOOTH_QUARTZ.defaultBlockState(),2);
            for(int family=0;family<9;family++){
                String name=EquipmentRules.CORES[family]+"_lv"+(family==8?4:3);int x=(family%3-1)*6,y=220+(2-family/3)*5;
                for(var q:BlockPos.betweenClosed(new BlockPos(x-2,y,Z-1),new BlockPos(x+2,y+3,Z-1)))world.setBlock(q,Blocks.DEEPSLATE_TILES.defaultBlockState(),2);
                world.setBlock(new BlockPos(x-1,y+1,Z),BuiltInRegistries.BLOCK.getValue(Identifier.parse("guogaology:"+name)).defaultBlockState(),2);
                var frame=new ItemFrame(world,new BlockPos(x+1,y+2,Z),Direction.SOUTH);frame.setItem(item(name));frame.setInvulnerable(true);world.addFreshEntity(frame);
                world.setBlock(new BlockPos(x+1,y,Z+1),Blocks.SEA_LANTERN.defaultBlockState(),2);
                var dropped=new ItemEntity(world,x+1.5,y+1.2,Z+1.5,item(name));dropped.setDeltaMovement(Vec3.ZERO);dropped.setNoGravity(true);dropped.setNeverPickUp();dropped.setUnlimitedLifetime();world.addFreshEntity(dropped);
            }
            p.teleportTo(world,.5,224.7,Z+19,Set.of(),180,-4,false);
        }else if(scene<5){
            p.getInventory().setItem(0,item(new String[]{"lho_trace","sequence_core_lv3","ordinal_crystal_lv4"}[scene-2]));
            p.teleportTo(world,.5,224.7,Z+15,Set.of(),180,0,false);
        }else if(scene==5){
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,item("laver_core_lv3"));
            p.teleportTo(world,.5,224.7,Z+15,Set.of(),180,0,false);
        }else{
            p.teleportTo(world,5.5,221.2,Z+4.5,Set.of(),210,20,false);
        }
        p.containerMenu.broadcastChanges();
    }
}
