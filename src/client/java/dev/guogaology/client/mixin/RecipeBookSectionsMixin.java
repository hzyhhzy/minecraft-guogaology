package dev.guogaology.client.mixin;

import dev.guogaology.client.recipebook.*;
import java.util.List;
import net.minecraft.client.gui.screen.recipebook.*;
import net.minecraft.client.recipebook.*;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookWidget.class)
public abstract class RecipeBookSectionsMixin {
    @Shadow @Final private List<RecipeGroupButtonWidget> tabButtons;
    @Shadow private RecipeGroupButtonWidget currentTab;
    @Shadow protected AbstractRecipeScreenHandler<?,?> craftingScreenHandler;
    @Shadow private int parentHeight;
    @Unique private RecipeBookSections.Section guogaology$rememberedSection;
    @Inject(method="reset",at=@At("HEAD"))
    private void guogaology$rememberSection(CallbackInfo ci){
        guogaology$rememberedSection=currentTab instanceof SectionRecipeBookButton button?button.section():null;
    }
    @Inject(method="reset",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screen/recipebook/RecipeBookWidget;refreshResults(Z)V"))
    private void guogaology$appendSections(CallbackInfo ci){
        if(craftingScreenHandler.getCategory()!=RecipeBookCategory.CRAFTING)return;
        for(var section:RecipeBookSections.Section.values()){
            var button=new SectionRecipeBookButton(section);tabButtons.add(button);
            if(section==guogaology$rememberedSection){currentTab.setToggled(false);currentTab=button;button.setToggled(true);}
        }
    }
    @Redirect(method="refreshResults",at=@At(value="INVOKE",target="Lnet/minecraft/client/recipebook/ClientRecipeBook;getResultsForGroup(Lnet/minecraft/client/recipebook/RecipeBookGroup;)Ljava/util/List;"))
    private List<RecipeResultCollection> guogaology$sectionCollections(ClientRecipeBook book,RecipeBookGroup category){
        return currentTab instanceof SectionRecipeBookButton button?RecipeBookCollections.forSection(book,button.section()):book.getResultsForGroup(category);
    }
    @Inject(method="refreshTabButtons",at=@At("TAIL"))
    private void guogaology$fitTabStrip(CallbackInfo ci){
        if(tabButtons.stream().noneMatch(b->b instanceof SectionRecipeBookButton))return;
        var visible=tabButtons.stream().filter(b->b.visible).toList();int top=RecipeBookSections.tabStripTop(parentHeight,visible.size());
        for(int i=0;i<visible.size();i++)visible.get(i).setY(top+27*i);
    }
}
