package dev.guogaology.client.recipebook;

import java.util.List;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.registry.Registries;

public final class RecipeBookCollections {
    private RecipeBookCollections(){}
    public static List<RecipeResultCollection> forSection(ClientRecipeBook book,RecipeBookSections.Section section){
        // Keep native objects: the vanilla search tree and craftable sets use their identity.
        return book.getResultsForGroup(RecipeBookGroup.CRAFTING_SEARCH).stream().filter(collection->
            collection.getAllRecipes().stream().anyMatch(entry->RecipeBookSections.includes(section,entry.id().toString(),
                Registries.ITEM.getId(entry.value().getResult(collection.getRegistryManager()).getItem()).toString(),false))).toList();
    }
}
