package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneNoEnchantMixin {
    @Inject(method="createResult",at=@At("RETURN"))
    private void guogaology$noCurseMerge(CallbackInfo result){var menu=(AbstractContainerMenu)(Object)this;var first=menu.getSlot(0).getItem();var second=menu.getSlot(1).getItem();var output=menu.getSlot(2).getItem();if(MiningContent.GEAR.containsKey(first.getItem())&&MiningContent.GEAR.containsKey(second.getItem())&&(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty()))menu.getSlot(2).set(ItemStack.EMPTY);else if(GearData.forbidsEnchantments(output))output.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);}
}
