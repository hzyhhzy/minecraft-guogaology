package dev.guogaology.connectorqa.mixin;
import net.minecraft.client.util.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The launcher also disables NeoForge's early window in this disposable instance. */
@Mixin(Window.class)
public abstract class HiddenWindowMixin {
    @Inject(method="<init>",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V",shift=At.Shift.AFTER,remap=false))
    private void hidden(CallbackInfo ci){
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW,GLFW.GLFW_FALSE);
    }
}
