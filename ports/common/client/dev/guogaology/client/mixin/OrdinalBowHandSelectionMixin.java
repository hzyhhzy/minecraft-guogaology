package dev.guogaology.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.guogaology.mining.OrdinalBowItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep vanilla's hand-selection rules, recognizing our actively drawn bows. */
@Mixin(ItemInHandRenderer.class)
public abstract class OrdinalBowHandSelectionMixin {
    @WrapOperation(method={"evaluateWhichHandsToRender","selectionUsingItemWhileHoldingBowLike"},
        at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    private static boolean guogaology$activeBow(ItemStack stack,Item item,Operation<Boolean> original,
                                               @Local(argsOnly=true) LocalPlayer player) {
        return original.call(stack,item)||item==Items.BOW&&player.isUsingItem()
            &&player.getUseItem()==stack&&stack.getItem() instanceof OrdinalBowItem;
    }
}
