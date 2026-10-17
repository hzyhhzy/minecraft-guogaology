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
    private static final boolean BIOMES=Boolean.getBoolean("guogaology.qa.advancements510");
    private static final String[] TARGETS={"minecraft:story/root","minecraft:adventure/root","minecraft:husbandry/root","minecraft:nether/root","minecraft:end/root","guogaology:arrival",
        "guogaology:guide/enter_inner","guogaology:guide/landmark_matrix","guogaology:guide/landmark_power","guogaology:guide/landmark_hydra","guogaology:guide/landmark_absence","guogaology:guide/landmark_weaver","guogaology:guide/landmark_astra","guogaology:guide/landmark_frontier","guogaology:guide/landmark_guogao","guogaology:guide/core_frontier_2","guogaology:guide/core_frontier_3","guogaology:guide/material_absence","guogaology:guide/omega_mastery",
        "guogaology:guide/enter_outer","guogaology:guide/biome_matrix","guogaology:guide/biome_power","guogaology:guide/biome_hydra","guogaology:guide/biome_absence","guogaology:guide/biome_weaver","guogaology:guide/biome_astra","guogaology:guide/biome_frontier","guogaology:guide/enter_underworld"};
    private static String prefix(){return BIOMES?"advancements510":"advancements057";}
    private boolean opening,queued,done,shown,initialShown;private volatile boolean ready,captured,initialCaptured;private volatile Throwable failure;
    private int frame,ticks,initialTicks;private final long deadline=System.nanoTime()+900_000_000_000L;
    public static void initialize(){var qa=new Advancement057Runtime();ClientTickEvents.END_CLIENT_TICK.register(qa::tick);}
    /** User-requested visible review: open the saved test world once, then yield all control. */
    public static void initializeReview(){
        boolean[] state={false,false};
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(state[1])return;
            try{
                if(c.level==null){if(!state[0]&&c.isGameLoadFinished()&&c.gui.overlay()==null){state[0]=true;c.createWorldOpenFlows().openWorld("port-qa",()->{});}return;}
                if(c.player==null||c.getConnection()==null)return;
                var handler=c.getConnection().getAdvancements();var root=handler.get(Identifier.parse("guogaology:arrival"));
                if(root==null||handler.getTree().get(Identifier.parse("guogaology:guide/enter_inner"))==null)return;
                boolean paginated=FabricLoader.getInstance().isModLoaded("paginatedadvancements");c.gui.setScreen(screen(handler,paginated));handler.setSelectedTab(root,true);
                hover(c,handler,"guogaology:guide/enter_inner",paginated);state[1]=true;
                Files.writeString(c.gameDirectory.toPath().resolve("review-ready.txt"),"Visible interactive advancement review; no automated actions or exit.\n");
                System.out.println("ADVANCEMENT510_REVIEW_READY user controls this window; no automatic shutdown");
            }catch(Throwable e){state[1]=true;e.printStackTrace();}
        });
    }
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
                    if(!BASELINE){
                        initialHandler.setSelectedTab(initialHandler.get(Identifier.parse("guogaology:arrival")),true);
                        if(c.getResourceManager().getResource(Identifier.parse("guogaology:textures/item/guide_emblem.png")).isEmpty())throw new AssertionError("Missing cake-and-apple guide icon");
                        if(BIOMES)for(String theme:List.of("matrix","power","hydra","absence","weaver","astra","frontier","guogao")){
                            var resource=c.getResourceManager().getResource(Identifier.parse("guogaology:models/item/landmark_emblem_"+theme+".json")).orElseThrow();
                            try(var reader=resource.openAsReader()){
                                var model=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                                for(var value:model.getAsJsonObject("textures").entrySet()){
                                    var texture=Identifier.parse(value.getValue().getAsString());
                                    if(c.getResourceManager().getResource(Identifier.fromNamespaceAndPath(texture.getNamespace(),"textures/"+texture.getPath()+".png")).isEmpty())throw new AssertionError("Missing landmark texture "+texture);
                                }
                            }
                        }
                    }
                    System.out.println("ADVANCEMENT057_INITIAL tabs="+count+" baseline="+BASELINE+" before vanilla root conditions; "+screen.getClass().getSimpleName());initialShown=true;
                }
                if(initialTicks==80&&!BASELINE)hover(c,c.getConnection().getAdvancements(),"guogaology:guide/enter_outer",FabricLoader.getInstance().isModLoaded("paginatedadvancements"));
                if(initialTicks==110){var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(prefix()+"-initial.png"));initialCaptured=true;}catch(Throwable e){failure=e;}});}
                if(!initialCaptured)return;c.gui.setScreen(null);
            }
            if(!queued){queued=true;var uuid=c.player.getUUID();c.getSingleplayerServer().execute(()->{try{Advancement057Checks.run(c.getSingleplayerServer().getPlayerList().getPlayer(uuid),BASELINE);ready=true;}catch(Throwable e){failure=e;}});return;}
            if(!ready||c.gui.overlay()!=null)return;
            var handler=c.getConnection().getAdvancements();
            for(String root:Advancement057Checks.ROOTS)if(handler.get(Identifier.parse(root))==null)return;
            if(BIOMES)for(var node:handler.getTree().nodes())if(node.holder().id().getNamespace().equals("guogaology")&&node.advancement().display().isPresent()){
                var expected=dev.guogaology.advancement.GuideLayout.position(node.holder().id().toString());var actual=node.advancement().display().orElseThrow();
                if(expected==null||Math.abs(expected.x()-actual.getX())>.001||Math.abs(expected.y()-actual.getY())>.001)throw new AssertionError("Client received shuffled layout "+node.holder().id());
            }
            boolean paginated=FabricLoader.getInstance().isModLoaded("paginatedadvancements");
            String root=Advancement057Checks.ROOTS[Math.min(frame,5)];
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
            if(++ticks==20)hover(c,handler,BIOMES?TARGETS[frame]:frame==6?"guogaology:guide/enter_inner":frame==7?"guogaology:guide/enter_underworld":root,paginated);
            if(ticks==70){var folder=c.gameDirectory.toPath().resolve("screenshots");Files.createDirectories(folder);int picture=frame;Screenshot.takeScreenshot(c.gameRenderer.mainRenderTarget(),image->{try(image){image.writeToFile(folder.resolve(prefix()+"-"+picture+".png"));captured=true;}catch(Throwable e){failure=e;}});}
            if(!captured)return;
            if(++frame==(BIOMES?TARGETS.length:8)){
                String report=(BIOMES?"ADVANCEMENT510_RUNTIME_OK":"ADVANCEMENT057_RUNTIME_OK")+" baseline="+BASELINE+" vanilla five roots/native criteria and resource paths, guide decoding/actions, client synchronization and "+frame+" inspected-page candidates.\n";Files.writeString(Path.of("port-client-ok.txt"),report);done=true;c.stop();
            }else{c.gui.setScreen(null);shown=false;captured=false;ticks=0;}
        }catch(Throwable e){done=true;e.printStackTrace();try{Files.writeString(Path.of("port-client-failed.txt"),e.toString());}catch(Exception ignored){}c.stop();}
    }
    private static AdvancementsScreen screen(ClientAdvancements handler,boolean paginated)throws Exception{
        return paginated?(AdvancementsScreen)Class.forName("de.dafuqs.paginatedadvancements.client.PaginatedAdvancementScreen").getConstructor(ClientAdvancements.class).newInstance(handler):new AdvancementsScreen(handler);
    }
    private static void hover(Minecraft c,ClientAdvancements handler,String root,boolean paginated)throws Exception{
        var screen=(AdvancementsScreen)c.gui.screen();var field=screen.getClass().getDeclaredField("selectedTab");field.setAccessible(true);var tab=field.get(screen);
        String target=root;
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
