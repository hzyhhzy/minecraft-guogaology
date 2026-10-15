package dev.guogaology.mixin;
import dev.guogaology.mining.*;
import net.minecraft.item.ItemStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GrindstoneScreenHandler.class)
public abstract class GrindstoneNoEnchantMixin {
    @Inject(method="updateResult",at=@At("RETURN"))
    private void guogaology$noCurseMerge(CallbackInfo result){
        var menu=(ScreenHandler)(Object)this;var first=menu.getSlot(0).getStack();var second=menu.getSlot(1).getStack();var output=menu.getSlot(2).getStack();
        // A two-item repair would consume a second component inventory. Single-item
        // disenchanting follows vanilla (including curses) and never deletes cores.
        if(MiningContent.GEAR.containsKey(first.getItem())&&MiningContent.GEAR.containsKey(second.getItem())&&(!GearData.cores(first).isEmpty()||!GearData.cores(second).isEmpty()))menu.getSlot(2).setStack(ItemStack.EMPTY);
        else GearData.refresh(output);
    }
}
