package dev.guogaology.client.recipebook;

import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;

/** An unregistered client tab key, as supported by vanilla's extended category interface. */
public enum SectionRecipeBookCategory implements ExtendedRecipeBookCategory {
    CORES(RecipeBookSections.Section.CORES),GGG(RecipeBookSections.Section.GGG);
    private final RecipeBookSections.Section section;
    SectionRecipeBookCategory(RecipeBookSections.Section section){this.section=section;}
    public RecipeBookSections.Section section(){return section;}
}
