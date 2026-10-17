package dev.guogaology.releaseqa263;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.sdl.SDLVideo;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/** Title-screen control with no Guogaology classes, assets, worlds or initializer. */
public final class Startup263Control implements ClientModInitializer {
    private final long deadline=System.nanoTime()+300_000_000_000L;
    private boolean title, requested, done;
    private volatile boolean captured;
    private volatile Throwable failure;
    private int ticks;

    @Override public void onInitializeClient() {
        var loader=FabricLoader.getInstance();
        require(Boolean.getBoolean("guogaology.qa263") && Files.isRegularFile(loader.getGameDir().resolve("qa263-isolated.json")),
                "Startup control requires the isolated runner");
        require(!loader.isModLoaded("guogaology"), "Startup control must not load the production Mod");
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        System.out.println("STARTUP263_CONTROL_INITIALIZED no production Mod");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void tick(Minecraft client) {
        if (done) return;
        try {
            if (failure!=null) throw new RuntimeException(failure);
            require(System.nanoTime()<deadline, "Startup title control timed out");
            client.options.pauseOnLostFocus=false;
            client.options.framerateLimit().set(60);
            if (!client.isGameLoadFinished() || client.gui.overlay()!=null) return;
            require(client.level==null && client.getSingleplayerServer()==null, "Title control never opens a world");
            if (!title) {
                title=true;
                client.gui.setScreen(new TitleScreen());
                System.out.println("STARTUP263_CONTROL_TITLE_READY");
                return;
            }
            if (!(client.gui.screen() instanceof TitleScreen)) return;
            if (++ticks<180) return;
            if (!requested) {
                long flags=SDLVideo.SDL_GetWindowFlags(client.getWindow().handle());
                require((flags & SDLVideo.SDL_WINDOW_HIDDEN)!=0, "Control SDL window must stay hidden");
                require((flags & SDLVideo.SDL_WINDOW_NOT_FOCUSABLE)!=0, "Control SDL window cannot take focus");
                require(!client.mouseHandler.isMouseGrabbed(), "Control never captures the pointer");
                var folder=client.gameDirectory.toPath().resolve("screenshots");
                Files.createDirectories(folder);
                requested=true;
                Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image->{
                    try (image) { image.writeToFile(folder.resolve("startup263-title.png")); captured=true; }
                    catch (Throwable error) { failure=error; }
                });
            }
            if (!captured) return;
            var result=new JsonObject();
            result.addProperty("success",true); result.addProperty("control",true);
            result.addProperty("minecraft","26.3");
            result.addProperty("version",System.getProperty("guogaology.qa263.release"));
            result.addProperty("productionModLoaded",false);
            result.addProperty("sodium",FabricLoader.getInstance().isModLoaded("sodium"));
            result.addProperty("voxy",FabricLoader.getInstance().isModLoaded("voxy"));
            result.addProperty("hidden",true); result.addProperty("mouseUnlocked",true);
            result.addProperty("titleTicks",ticks); result.addProperty("screenshot","startup263-title.png");
            Files.writeString(client.gameDirectory.toPath().resolve("startup-263-control-ok.json"),result.toString(),StandardOpenOption.CREATE_NEW);
            done=true;
            System.out.println("STARTUP263_CONTROL_OK "+result);
            client.stop();
        } catch (Throwable error) {
            done=true; error.printStackTrace();
            try { Files.writeString(client.gameDirectory.toPath().resolve("startup-263-control-failed.txt"),error.toString()); }
            catch (Exception ignored) {}
            client.stop();
        }
    }
}
