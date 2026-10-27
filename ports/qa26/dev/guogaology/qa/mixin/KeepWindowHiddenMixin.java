package dev.guogaology.qa.mixin;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The native constructor shows its initially hidden GLFW window a second time. */
@Mixin(Minecraft.class)
public abstract class KeepWindowHiddenMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lorg/lwjgl/glfw/GLFW;glfwShowWindow(J)V",remap=false),require=1)
    private void keepHidden(long window){
        if(Boolean.getBoolean("guogaology.qa.review"))GLFW.glfwShowWindow(window);
    }
}
