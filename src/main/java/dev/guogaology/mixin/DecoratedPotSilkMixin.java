package dev.guogaology.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.guogaology.mining.MiningEffects;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.block.DecoratedPotBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native pot shattering runs before the ordinary loot-context tool copy exists. */
@Mixin(DecoratedPotBlock.class)
public abstract class DecoratedPotSilkMixin {
    @WrapOperation(method="onBreak",at=@At(value="INVOKE",target="Lnet/minecraft/enchantment/EnchantmentHelper;hasAnyEnchantmentsIn(Lnet/minecraft/item/ItemStack;Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean guogaology$nativeSilk(ItemStack tool,TagKey<Enchantment> tag,Operation<Boolean> original,@Local(argsOnly=true) PlayerEntity player){
        if(original.call(tool,tag))return true;
        var view=MiningEffects.silkTool(tool,player.getRegistryManager());
        return view!=tool&&original.call(view,tag);
    }
}
