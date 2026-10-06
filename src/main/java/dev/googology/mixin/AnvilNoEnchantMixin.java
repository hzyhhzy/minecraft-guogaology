package dev.googology.mixin;
import dev.googology.mining.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilScreenHandler.class)
public abstract class AnvilNoEnchantMixin {
    @Inject(method="updateResult",at=@At("RETURN"))
    private void googology$noEnchantedOutput(CallbackInfo result){var menu=(ScreenHandler)(Object)this;var first=menu.getSlot(0).getStack();var second=menu.getSlot(1).getStack();var output=menu.getSlot(2).getStack();boolean coreMerge=MiningContent.GEAR.containsKey(first.getItem())&&MiningContent.GEAR.containsKey(second.getItem())&&(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty());if(coreMerge||GearData.forbidsEnchantments(output)&&output.hasEnchantments())menu.getSlot(2).setStack(ItemStack.EMPTY);}
}
