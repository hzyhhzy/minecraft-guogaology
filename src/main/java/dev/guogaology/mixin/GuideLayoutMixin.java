package dev.guogaology.mixin;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.advancement.GuideLayout;
import net.minecraft.advancement.AdvancementPositioner;
import net.minecraft.advancement.PlacedAdvancement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla packets carry these coordinates to native and paginated clients alike. */
@Mixin(AdvancementPositioner.class)
public class GuideLayoutMixin {
    @Inject(method="arrangeForTree",at=@At("TAIL"))
    private static void guogaology$layout(PlacedAdvancement root,CallbackInfo ci){
        if(root.getAdvancementEntry().id().equals(GuogaologyMod.id("arrival")))guogaology$place(root);
    }
    private static void guogaology$place(PlacedAdvancement node){
        var pos=GuideLayout.position(node.getAdvancementEntry().id().toString());
        if(pos!=null)node.getAdvancement().display().ifPresent(display->display.setPos(pos.x(),pos.y()));
        for(var child:node.getChildren())guogaology$place(child);
    }
}
