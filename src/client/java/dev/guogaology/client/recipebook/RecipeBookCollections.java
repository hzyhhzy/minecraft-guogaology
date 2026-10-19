package dev.guogaology.client.recipebook;

import java.util.*;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.registry.Registries;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.recipe.RecipeEntry;

public final class RecipeBookCollections {
    private RecipeBookCollections(){}
    public static List<RecipeResultCollection> forSection(ClientRecipeBook book,RecipeBookSections.Section section){
        // Keep native objects: the vanilla search tree and craftable sets use their identity.
        return book.getResultsForGroup(RecipeBookGroup.CRAFTING_SEARCH).stream().filter(collection->
            collection.getAllRecipes().stream().anyMatch(entry->section(entry,collection.getRegistryManager())==section)).toList();
    }
    private static RecipeBookSections.Section section(RecipeEntry<?> entry,DynamicRegistryManager lookup){
        var result=entry.value().getResult(lookup);if(result.isEmpty())return null;
        String id=Registries.ITEM.getId(result.getItem()).toString();
        for(var section:RecipeBookSections.Section.values())if(RecipeBookSections.includes(section,entry.id().toString(),id,false))return section;
        return null;
    }
    /** Split entry groups before vanilla creates its indexed collection objects; equal output IDs do not imply ownership. */
    public static Map<RecipeBookGroup,List<List<RecipeEntry<?>>>> partition(Map<RecipeBookGroup,List<List<RecipeEntry<?>>>> source,DynamicRegistryManager lookup){
        var result=new HashMap<RecipeBookGroup,List<List<RecipeEntry<?>>>>();
        source.forEach((category,groups)->{
            if(category!=RecipeBookGroup.CRAFTING_BUILDING_BLOCKS&&category!=RecipeBookGroup.CRAFTING_REDSTONE&&category!=RecipeBookGroup.CRAFTING_EQUIPMENT&&category!=RecipeBookGroup.CRAFTING_MISC){result.put(category,groups);return;}
            var split=new ArrayList<List<RecipeEntry<?>>>();
            for(var entries:groups){
                var partitions=new LinkedHashMap<RecipeBookSections.Section,List<RecipeEntry<?>>>();
                for(var entry:entries)partitions.computeIfAbsent(section(entry,lookup),key->new ArrayList<>()).add(entry);
                split.addAll(partitions.values());
            }
            result.put(category,split);
        });
        return result;
    }
}
