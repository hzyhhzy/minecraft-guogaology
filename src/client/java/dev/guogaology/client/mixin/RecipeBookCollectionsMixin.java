package dev.guogaology.client.mixin;

import dev.guogaology.client.recipebook.*;
import java.util.*;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.recipe.RecipeEntry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Move dedicated client sections out of concrete vanilla tabs, while keeping the native search source intact. */
@Mixin(ClientRecipeBook.class)
public abstract class RecipeBookCollectionsMixin {
    @Unique private Set<RecipeResultCollection> guogaology$dedicated;
    @Shadow private static Map<RecipeBookGroup,List<List<RecipeEntry<?>>>> toGroupedMap(Iterable<RecipeEntry<?>> entries){throw new AssertionError();}
    @Redirect(method="reload",at=@At(value="INVOKE",target="Lnet/minecraft/client/recipebook/ClientRecipeBook;toGroupedMap(Ljava/lang/Iterable;)Ljava/util/Map;"))
    private Map<RecipeBookGroup,List<List<RecipeEntry<?>>>> guogaology$partitionEntries(Iterable<RecipeEntry<?>> input,Iterable<RecipeEntry<?>> all,DynamicRegistryManager lookup){
        return RecipeBookCollections.partition(toGroupedMap(input),lookup);
    }
    @Inject(method="reload",at=@At("TAIL"))
    private void guogaology$invalidate(CallbackInfo ci){guogaology$dedicated=null;}
    @Inject(method="getResultsForGroup",at=@At("RETURN"),cancellable=true)
    private void guogaology$vanillaCategories(RecipeBookGroup category,CallbackInfoReturnable<List<RecipeResultCollection>> cir){
        if(category!=RecipeBookGroup.CRAFTING_BUILDING_BLOCKS&&category!=RecipeBookGroup.CRAFTING_REDSTONE&&category!=RecipeBookGroup.CRAFTING_EQUIPMENT&&category!=RecipeBookGroup.CRAFTING_MISC)return;
        if(guogaology$dedicated==null){
            guogaology$dedicated=Collections.newSetFromMap(new IdentityHashMap<>());
            var book=(ClientRecipeBook)(Object)this;
            // These only query CRAFTING_SEARCH, which is never filtered by this injection.
            for(var section:RecipeBookSections.Section.values())guogaology$dedicated.addAll(RecipeBookCollections.forSection(book,section));
        }
        cir.setReturnValue(cir.getReturnValue().stream().filter(collection->!guogaology$dedicated.contains(collection)).toList());
    }
}
