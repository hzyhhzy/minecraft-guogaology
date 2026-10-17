package dev.guogaology.mergeqa;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.Identifier;

/** Hidden native UI validation; optional installed pagination mod is QA-only. */
public final class Advancement057Runtime {
    private static final boolean BASELINE=Boolean.getBoolean("guogaology.qa.advancementsBaseline");
    private boolean opening,queued,done,shown,initialShown;private volatile boolean ready,captured,initialCaptured;private volatile Throwable failure;
    private int frame,ticks,initialTicks;private final long deadline=System.nanoTime()+900_000_000_000L;
    public static void initialize(){var qa=new Advancement057Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    private void tick(Minecraft c){
        if(done)return;
        try{
            if(failure!=null)throw new RuntimeException(failure);
            if(System.nanoTime()>deadline)throw new AssertionError("Advancement QA timed out "+frame);
            c.options.pauseOnLostFocus=false;c.options.renderDistance().set(3);c.options.framerateLimit().set(60);c.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);c.gui.toastManager().clear();
            if(c.level==null){if(!opening&&c.isGameLoadFinished()&&c.gui.overlay()==null){opening=true;c.createWorldOpenFlows().openWorld("port-qa",c::stop);}return;}
            if(c.player==null||c.getSingleplayerServer()==null||c.getConnection()==null)return;
            if(!queued){
                if(++initialTicks<60)return;
                if(!initialShown){
                    var initialHandler=c.getConnection().getAdvancements();var screen=screen(initialHandler,FabricLoader.getInstance().isModLoaded("paginatedadvancements"));c.gui.setScreen(screen);
                    var tabs=screen.getClass().getDeclaredField("tabs");tabs.setAccessible(true);int count=((Map<?,?>)tabs.get(screen)).size();
                    if(count!=(BASELINE?0:1))throw new AssertionError("Unexpected initial visible tab count "+count);
                    System.out.println("ADVANCEMENT057_INITIAL tabs="+count+" baseline="+BASELINE+" before vanilla root conditions; "+screen.getClass().getSimpleName());initialShown=true;
                }
                if(initialTicks==110){var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("advancements057-initial.png"));initialCaptured=true;}catch(Throwable e){failure=e;}});}
                if(!initialCaptured)return;c.gui.setScreen(null);
            }
            if(!queued){queued=true;var uuid=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{Advancement057Checks.run(c.getSingleplayerServer().getPlayerList().getPlayer(uuid),BASELINE);ready=true;}catch(Throwable e){failure=e;}});return;}
            if(!ready||c.gui.overlay()!=null)return;
            var handler=c.getConnection().getAdvancements();
            for(String root:Advancement057Checks.ROOTS)if(handler.get(Identifier.parse(root))==null)return;
            boolean paginated=FabricLoader.getInstance().isModLoaded("paginatedadvancements");
            String root=Advancement057Checks.ROOTS[frame%6];
            if(!shown){
                for(var node:handler.getTree().nodes())if(node.advancement().isRoot()&&node.advancement().display().isPresent()){
                    var texture=node.advancement().display().orElseThrow().getBackground().orElseThrow().texturePath();
                    boolean exists=c.getResourceManager().getResource(texture).isPresent();
                    if(!exists&&!(BASELINE&&node.holder().id().equals(Identifier.parse("guogaology:arrival"))))throw new AssertionError("Missing advancement texture "+node.holder().id()+" "+texture);
                    System.out.println("ADVANCEMENT057_BACKGROUND "+node.holder().id()+" "+texture+" exists="+exists);
                }
                AdvancementsScreen screen=screen(handler,paginated);
                c.gui.setScreen(screen);screen=(AdvancementsScreen)c.gui.screen();handler.setSelectedTab(handler.get(Identifier.parse(root)),true);
                var tabs=screen.getClass().getDeclaredField("tabs");tabs.setAccessible(true);var values=(Map<?,?>)tabs.get(screen);
                if(values.size()!=6||!values.containsKey(handler.get(Identifier.parse(root))))throw new AssertionError("All five vanilla tabs plus Mod must remain selectable: "+values.keySet());
                var selected=screen.getClass().getDeclaredField("selectedTab");selected.setAccessible(true);if(selected.get(screen)==null)throw new AssertionError("Selected advancement page was lost");
                System.out.println("ADVANCEMENT057_PAGE "+screen.getClass().getSimpleName()+" "+root+" tabs="+values.size()+" clientNodes="+handler.getTree().nodes().size());shown=true;ticks=0;
            }
            if(++ticks==20)hover(c,handler,root,paginated);
            if(ticks==70){var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=frame;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve("advancements057-"+picture+".png"));captured=true;}catch(Throwable e){failure=e;}});}
            if(!captured)return;
            if(++frame==6){
                String report="ADVANCEMENT057_RUNTIME_OK baseline="+BASELINE+" vanilla five roots/native criteria and resource paths, guide decoding/actions, client synchronization and "+frame+" inspected-page candidates.\n";Files.writeString(Path.of("port-client-ok.txt"),report);done=true;c.stop();
            }else{c.gui.setScreen(null);shown=false;captured=false;ticks=0;}
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private static AdvancementsScreen screen(ClientAdvancements handler,boolean paginated)throws Exception{
        return paginated?(AdvancementsScreen)Class.forName("de.dafuqs.paginatedadvancements.client.PaginatedAdvancementScreen").getConstructor(ClientAdvancements.class).newInstance(handler):new AdvancementsScreen(handler);
    }
    private static void hover(Minecraft c,ClientAdvancements handler,String root,boolean paginated)throws Exception{
        var screen=(AdvancementsScreen)c.gui.screen();var field=screen.getClass().getDeclaredField("selectedTab");field.setAccessible(true);var tab=field.get(screen);
        String target=root.equals("guogaology:arrival")&&!BASELINE?"guogaology:guide/portal_inner":root;
        var node=handler.getTree().get(Identifier.parse(target));var widget=screen.getAdvancementWidget(node);
        if(widget==null)throw new AssertionError("Missing native advancement widget "+target);
        int x=screen.width/2,y=screen.height/2;
        int left=paginated?41:(screen.width-252)/2+9,top=paginated?70:(screen.height-140)/2+18;
        var sx=tab.getClass().getDeclaredField(paginated?"originX":"scrollX");var sy=tab.getClass().getDeclaredField(paginated?"originY":"scrollY");sx.setAccessible(true);sy.setAccessible(true);
        sx.setDouble(tab,x-left-widget.getX()-13);sy.setDouble(tab,y-top-widget.getY()-13);
        // Set only this hidden client's logical GUI hover, never the OS cursor.
        var mx=c.mouseHandler.getClass().getDeclaredField("xpos");var my=c.mouseHandler.getClass().getDeclaredField("ypos");mx.setAccessible(true);my.setAccessible(true);
        mx.setDouble(c.mouseHandler,x*(double)c.getWindow().getWidth()/screen.width);my.setDouble(c.mouseHandler,y*(double)c.getWindow().getHeight()/screen.height);
        var display=node.advancement().display().orElseThrow();String title=display.getTitle().getString(),description=display.getDescription().getString();
        if(title.contains("advancement.")||description.contains("advancement."))throw new AssertionError("Untranslated advancement copy");
        System.out.println("ADVANCEMENT057_COPY "+target+" "+title+" / "+description);
    }
}
