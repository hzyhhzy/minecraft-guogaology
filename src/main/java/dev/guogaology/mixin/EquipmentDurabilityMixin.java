package dev.guogaology.mixin;

import dev.guogaology.mining.GearData;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import dev.guogaology.mining.ArmorProtectionContext;
import net.minecraft.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Stable shared native wear hook, before Unbreaking, on both 1.21.1 loaders. */
@Mixin(EnchantmentHelper.class)
public abstract class EquipmentDurabilityMixin {
    // NeoForge splits ItemStack.damage and widens its owner argument. Hook the
    // unchanged helper instead; the native damage snapshot owns armor reduction.
    @WrapMethod(method="getItemDamage")
    private static int guogaology$wear(ServerWorld world,ItemStack stack,int amount,Operation<Integer> original){
        int adjusted=GearData.wearCost(stack,amount,ArmorProtectionContext.owner(),world.random::nextDouble);
        return original.call(world,stack,adjusted);
    }
}
