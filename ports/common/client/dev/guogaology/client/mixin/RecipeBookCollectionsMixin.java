package dev.guogaology.client.mixin;

import dev.guogaology.client.recipebook.*;
import java.util.*;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ClientRecipeBook.class)
public abstract class RecipeBookCollectionsMixin {
    @Unique private final Map<SectionRecipeBookCategory,List<RecipeCollection>> guogaology$sections=new EnumMap<>(SectionRecipeBookCategory.class);
    @Unique private Set<RecipeCollection> guogaology$dedicated;
    @Shadow public abstract List<RecipeCollection> getCollection(ExtendedRecipeBookCategory category);
    @Inject(method="categorizeAndGroupRecipes",at=@At("RETURN"),cancellable=true)
    private static void guogaology$partitionEntries(CallbackInfoReturnable<Map<RecipeBookCategory,List<List<RecipeDisplayEntry>>>> cir){
        var level=Minecraft.getInstance().level;if(level!=null)cir.setReturnValue(RecipeBookCollections.partition(cir.getReturnValue(),level));
    }
    @Inject(method={"rebuildCollections","clear"},at=@At("TAIL"))
    private void guogaology$invalidateSections(CallbackInfo ci){guogaology$sections.clear();guogaology$dedicated=null;}
    @Inject(method="getCollection",at=@At("HEAD"),cancellable=true)
    private void guogaology$sectionCollections(ExtendedRecipeBookCategory category,CallbackInfoReturnable<List<RecipeCollection>> cir){
        if(!(category instanceof SectionRecipeBookCategory section))return;
        var level=Minecraft.getInstance().level;
        if(level==null){cir.setReturnValue(List.of());return;}
        cir.setReturnValue(guogaology$sections.computeIfAbsent(section,key->RecipeBookCollections.forSection(getCollection(SearchRecipeBookCategory.CRAFTING),key.section(),level)));
    }
    @Inject(method="getCollection",at=@At("RETURN"),cancellable=true)
    private void guogaology$vanillaCategories(ExtendedRecipeBookCategory category,CallbackInfoReturnable<List<RecipeCollection>> cir){
        if(category!=RecipeBookCategories.CRAFTING_BUILDING_BLOCKS&&category!=RecipeBookCategories.CRAFTING_REDSTONE&&category!=RecipeBookCategories.CRAFTING_EQUIPMENT&&category!=RecipeBookCategories.CRAFTING_MISC)return;
        // The client has no active registry context before joining a level. Do not cache an empty partition.
        if(Minecraft.getInstance().level==null)return;
        if(guogaology$dedicated==null){
            guogaology$dedicated=Collections.newSetFromMap(new IdentityHashMap<>());
            // Custom tabs are derived solely from unchanged CRAFTING search collections, never these concrete tabs.
            for(var section:SectionRecipeBookCategory.values())guogaology$dedicated.addAll(getCollection(section));
        }
        cir.setReturnValue(cir.getReturnValue().stream().filter(collection->!guogaology$dedicated.contains(collection)).toList());
    }
}
