package dev.googology.mixin;

import dev.googology.mining.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class EquipmentRepairRecipeMixin {
    @Inject(method="canCombine",at=@At("HEAD"),cancellable=true)
    private static void googology$keepCores(ItemStack first,ItemStack second,CallbackInfoReturnable<Boolean> result){
        if(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty())result.setReturnValue(false);
    }
    @Inject(method="assemble",at=@At("RETURN"))
    private void googology$numericRepair(CraftingInput input,HolderLookup.Provider lookup,CallbackInfoReturnable<ItemStack> result){
        var output=result.getReturnValue();if(output.isEmpty()||!output.has(MiningContent.DIGIT))return;
        double sum=0;int count=0;for(int i=0;i<input.size();i++)if(!input.getItem(i).isEmpty()){sum+=input.getItem(i).getOrDefault(MiningContent.DIGIT,0d);count++;}
        if(count>0){output.set(MiningContent.DIGIT,(double)EquipmentRules.digit(sum/count));GearData.refresh(output);}
    }
}
