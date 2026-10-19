package dev.guogaology.client.mixin;

import dev.guogaology.client.recipebook.*;
import java.util.*;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.*;
import net.minecraft.world.item.crafting.ExtendedRecipeBookCategory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ClientRecipeBook.class)
public abstract class RecipeBookCollectionsMixin {
    @Unique private final Map<SectionRecipeBookCategory,List<RecipeCollection>> guogaology$sections=new EnumMap<>(SectionRecipeBookCategory.class);
    @Shadow public abstract List<RecipeCollection> getCollection(ExtendedRecipeBookCategory category);
    @Inject(method={"rebuildCollections","clear"},at=@At("TAIL"))
    private void guogaology$invalidateSections(CallbackInfo ci){guogaology$sections.clear();}
    @Inject(method="getCollection",at=@At("HEAD"),cancellable=true)
    private void guogaology$sectionCollections(ExtendedRecipeBookCategory category,CallbackInfoReturnable<List<RecipeCollection>> cir){
        if(!(category instanceof SectionRecipeBookCategory section))return;
        var level=Minecraft.getInstance().level;
        if(level==null){cir.setReturnValue(List.of());return;}
        cir.setReturnValue(guogaology$sections.computeIfAbsent(section,key->RecipeBookCollections.forSection(getCollection(SearchRecipeBookCategory.CRAFTING),key.section(),level)));
    }
}
