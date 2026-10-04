package dev.googology.mixin;

import dev.googology.mining.*;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class EquipmentRepairRecipeMixin {
    @Inject(method="canCombineStacks",at=@At("HEAD"),cancellable=true)
    private static void googology$keepCores(ItemStack first,ItemStack second,CallbackInfoReturnable<Boolean> result){
        if(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty())result.setReturnValue(false);
    }
    @Inject(method="craft",at=@At("RETURN"))
    private void googology$numericRepair(CraftingRecipeInput input,RegistryWrapper.WrapperLookup lookup,CallbackInfoReturnable<ItemStack> result){
        var output=result.getReturnValue();if(output.isEmpty()||!output.contains(MiningContent.DIGIT))return;
        double sum=0;int count=0;for(int i=0;i<input.getSize();i++)if(!input.getStackInSlot(i).isEmpty()){sum+=input.getStackInSlot(i).getOrDefault(MiningContent.DIGIT,0d);count++;}
        if(count>0){output.set(MiningContent.DIGIT,(double)EquipmentRules.digit(sum/count));GearData.refresh(output);}
    }
}
