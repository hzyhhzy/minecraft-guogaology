package dev.googology.qa.mixin;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** SDL replaced GLFW in 26.3. Create the QA window hidden before a native handle exists. */
@Mixin(Window.class)
public abstract class HiddenWindowMixin {
    @ModifyArg(method="createWindow",at=@At(value="INVOKE",target="Lcom/mojang/renderpearl/api/device/GpuBackend;createWindow(Ljava/lang/String;IIJ)J",remap=false),index=3)
    private long hidden(long flags){return flags|SDLVideo.SDL_WINDOW_HIDDEN|SDLVideo.SDL_WINDOW_NOT_FOCUSABLE;}
}
