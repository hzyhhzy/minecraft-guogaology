package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilMenu.class)
public abstract class AnvilNoEnchantMixin {
    @Inject(method="createResult",at=@At("RETURN"))
    private void guogaology$noEnchantedOutput(CallbackInfo result){var menu=(AbstractContainerMenu)(Object)this;var first=menu.getSlot(0).getItem();var second=menu.getSlot(1).getItem();var output=menu.getSlot(2).getItem();boolean coreMerge=MiningContent.GEAR.containsKey(first.getItem())&&MiningContent.GEAR.containsKey(second.getItem())&&(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty());if(coreMerge||GearData.forbidsEnchantments(output)&&output.isEnchanted())menu.getSlot(2).set(ItemStack.EMPTY);}
}
