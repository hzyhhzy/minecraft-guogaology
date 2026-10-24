package dev.guogaology.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.guogaology.mining.MiningEffects;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.IceBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep native ice/hive side effects consistent with the native Silk Touch loot view. */
@Mixin({IceBlock.class,BeehiveBlock.class})
public abstract class NativeSilkMiningMixin {
    @WrapOperation(method="playerDestroy",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;hasTag(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/tags/TagKey;)Z"))
    private boolean guogaology$nativeSilk(ItemStack tool,TagKey<Enchantment> tag,Operation<Boolean> original,@Local(argsOnly=true) Player player){
        if(original.call(tool,tag))return true;
        var view=MiningEffects.silkTool(tool,player.registryAccess());
        return view!=tool&&original.call(view,tag);
    }
}
