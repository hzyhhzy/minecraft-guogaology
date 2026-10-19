package dev.guogaology.client.recipebook;

import java.util.List;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.*;
import net.minecraft.world.level.Level;

public final class RecipeBookCollections {
    private RecipeBookCollections(){}
    public static List<RecipeCollection> forSection(List<RecipeCollection> crafting,RecipeBookSections.Section section,Level level){
        var context=SlotDisplayContext.fromLevel(level);
        // Reuse vanilla objects so its search tree, selected entries and craftable sets remain authoritative.
        return crafting.stream().filter(collection->collection.getRecipes().stream().anyMatch(entry->matches(entry,section,context))).toList();
    }
    public static boolean matches(RecipeDisplayEntry entry,RecipeBookSections.Section section,ContextMap context){
        var result=entry.display().result().resolveForFirstStack(context);
        if(result.isEmpty())return false;
        String resultId=BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        boolean modIngredient=false;
        if(section==RecipeBookSections.Section.GGG&&!RecipeBookSections.isModItem(resultId)){
            List<SlotDisplay> inputs=entry.display() instanceof ShapedCraftingRecipeDisplay shaped?shaped.ingredients():
                entry.display() instanceof ShapelessCraftingRecipeDisplay shapeless?shapeless.ingredients():List.of();
            // A vanilla #planks slot can also contain our woods. It does not make a vanilla recipe GGG.
            modIngredient=inputs.stream().map(slot->slot.resolveForStacks(context)).anyMatch(options->!options.isEmpty()&&options.stream().allMatch(stack->{
                String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                return RecipeBookSections.isModItem(id)&&!RecipeBookSections.isCommonWood(id);
            }));
        }
        return RecipeBookSections.includes(section,null,resultId,modIngredient);
    }
}
