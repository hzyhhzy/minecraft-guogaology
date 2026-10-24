package dev.guogaology.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.guogaology.mining.OrdinalBowItem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep vanilla's hand-selection rules, recognizing our actively drawn bows. */
@Mixin(HeldItemRenderer.class)
public abstract class OrdinalBowHandSelectionMixin {
    @WrapOperation(method={"getHandRenderType","getUsingItemHandRenderType"},
        at=@At(value="INVOKE",target="Lnet/minecraft/item/ItemStack;isOf(Lnet/minecraft/item/Item;)Z"))
    private static boolean guogaology$activeBow(ItemStack stack,Item item,Operation<Boolean> original,
                                               @Local(argsOnly=true) ClientPlayerEntity player) {
        return original.call(stack,item)||item==Items.BOW&&player.isUsingItem()
            &&player.getActiveItem()==stack&&stack.getItem() instanceof OrdinalBowItem;
    }
}
