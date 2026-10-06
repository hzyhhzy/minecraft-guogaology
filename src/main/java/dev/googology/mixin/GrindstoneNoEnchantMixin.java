package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneNoEnchantMixin {
    @Inject(method="updateResult",at=@At("RETURN"))
    private void googology$noCurseMerge(CallbackInfo result){var menu=(ScreenHandler)(Object)this;var first=menu.getSlot(0).getStack();var second=menu.getSlot(1).getStack();var output=menu.getSlot(2).getStack();if(MiningContent.GEAR.containsKey(first.getItem())&&MiningContent.GEAR.containsKey(second.getItem())&&(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty()))menu.getSlot(2).setStack(ItemStack.EMPTY);else if(GearData.forbidsEnchantments(output))output.set(net.minecraft.component.DataComponentTypes.ENCHANTMENTS,net.minecraft.component.type.ItemEnchantmentsComponent.DEFAULT);}
}
