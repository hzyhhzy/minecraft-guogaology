package dev.guogaology.client.recipebook;

import dev.guogaology.GuogaologyBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.RecipeGroupButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Extra client button without extending RecipeBookGroup's enum or changing saved book settings. */
public final class SectionRecipeBookButton extends RecipeGroupButtonWidget {
    private final RecipeBookSections.Section section;
    public SectionRecipeBookButton(RecipeBookSections.Section section){
        super(RecipeBookGroup.CRAFTING_MISC);this.section=section;
        setMessage(Text.translatable(section.translationKey()));setTooltip(Tooltip.of(getMessage()));
    }
    public RecipeBookSections.Section section(){return section;}
    @Override public boolean hasKnownRecipes(ClientRecipeBook book){
        visible=RecipeBookCollections.forSection(book,section).stream().anyMatch(c->c.isInitialized()&&c.hasFittingRecipes());return visible;
    }
    @Override public void checkForNewRecipes(MinecraftClient client){}
    @Override public void renderWidget(DrawContext context,int mouseX,int mouseY,float delta){
        int selected=isToggled()?-2:0;
        context.drawGuiTexture(Identifier.ofVanilla(isToggled()?"recipe_book/tab_selected":"recipe_book/tab"),getX()+selected,getY(),getWidth(),getHeight());
        context.drawItemWithoutEntity(new ItemStack(section==RecipeBookSections.Section.CORES?GuogaologyBlocks.ORDINAL_CRYSTAL:GuogaologyBlocks.ORDINAL_BRICKS),getX()+9+selected,getY()+5);
    }
}
