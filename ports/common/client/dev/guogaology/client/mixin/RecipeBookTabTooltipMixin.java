package dev.guogaology.client.mixin;

import dev.guogaology.client.recipebook.SectionRecipeBookCategory;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookTabButton.class)
public abstract class RecipeBookTabTooltipMixin {
    @Inject(method="<init>",at=@At("TAIL"))
    private void guogaology$sectionTooltip(CallbackInfo ci){
        var button=(RecipeBookTabButton)(Object)this;
        if(button.getCategory() instanceof SectionRecipeBookCategory section){
            var name=Component.translatable(section.section().translationKey());button.setMessage(name);button.setTooltip(Tooltip.create(name));
        }
    }
}
