package dev.guogaology.client.mixin;

import dev.guogaology.client.recipebook.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookBrandIconMixin {
    @Shadow private boolean selected;
    @Inject(method="renderIcon",at=@At("HEAD"),cancellable=true)
    private void guogaology$brandIcon(GuiGraphics graphics,CallbackInfo ci){
        var button=(RecipeBookTabButton)(Object)this;if(button.getCategory()!=SectionRecipeBookCategory.GGG)return;
        graphics.pose().pushMatrix();graphics.pose().translate(button.getX()+9+(selected?-2:0),button.getY()+5);graphics.pose().scale(.125f,.125f);
        graphics.blit(RenderPipelines.GUI_TEXTURED,Identifier.parse(RecipeBookSections.BRAND_ICON_TEXTURE),0,0,0f,0f,128,128,128,128);graphics.pose().popMatrix();ci.cancel();
    }
}
