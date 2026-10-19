package dev.guogaology.client.mixin;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.client.recipebook.*;
import java.util.*;
import net.minecraft.client.gui.screens.recipebook.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookSectionsMixin {
    @Shadow @Final @Mutable private List<RecipeBookComponent.TabInfo> tabInfos;
    @Shadow @Final private List<RecipeBookTabButton> tabButtons;
    @Shadow private int height;
    @Inject(method="<init>",at=@At("TAIL"))
    private void guogaology$appendSections(CallbackInfo ci){
        if(tabInfos.stream().noneMatch(tab->tab.category()==SearchRecipeBookCategory.CRAFTING))return;
        var tabs=new ArrayList<>(tabInfos);
        tabs.add(new RecipeBookComponent.TabInfo(GuogaologyBlocks.ORDINAL_CRYSTAL.asItem().getDefaultInstance(),Optional.empty(),SectionRecipeBookCategory.CORES));
        tabs.add(new RecipeBookComponent.TabInfo(net.minecraft.world.item.ItemStack.EMPTY,Optional.empty(),SectionRecipeBookCategory.GGG));
        tabInfos=List.copyOf(tabs);
    }
    @Inject(method="updateTabs",at=@At("TAIL"))
    private void guogaology$fitTabStrip(boolean animate,CallbackInfo ci){
        if(tabInfos.stream().noneMatch(tab->tab.category() instanceof SectionRecipeBookCategory))return;
        var visible=tabButtons.stream().filter(button->button.visible).toList();int top=RecipeBookSections.tabStripTop(height,visible.size());
        for(int i=0;i<visible.size();i++)visible.get(i).setY(top+27*i);
    }
}
