package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.advancement.GuideLayout;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.advancements.AdvancementNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla packets carry these coordinates to native and paginated clients alike. */
@Mixin(TreeNodePosition.class)
public class GuideLayoutMixin {
    @Inject(method="run",at=@At("TAIL"))
    private static void guogaology$layout(AdvancementNode root,CallbackInfo ci){
        if(root.holder().id().equals(GuogaologyMod.id("arrival")))guogaology$place(root);
    }
    private static void guogaology$place(AdvancementNode node){
        var pos=GuideLayout.position(node.holder().id().toString());
        if(pos!=null)node.advancement().display().ifPresent(display->display.setLocation(pos.x(),pos.y()));
        for(var child:node.children())guogaology$place(child);
    }
}
