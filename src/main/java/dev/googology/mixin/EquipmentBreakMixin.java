package dev.googology.mixin;

import dev.googology.mining.*;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/** Snapshot before vanilla destroys the item; never returns gems on ordinary wear. */
@Mixin(ItemStack.class)
public abstract class EquipmentBreakMixin {
    @WrapMethod(method="damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V")
    private void googology$returnCores(int amount,LivingEntity owner,EquipmentSlot slot,Operation<Void> original){
        var stack=(ItemStack)(Object)this;
        var saved=MiningContent.GEAR.containsKey(stack.getItem())?GearData.cores(stack).stream().map(ItemStack::copy).toList():java.util.List.<ItemStack>of();
        int before=stack.getCount();
        original.call(amount,owner,slot);
        if(stack.getCount()<before&&!saved.isEmpty()&&owner.getWorld() instanceof ServerWorld world){
            for(var core:saved){var drop=new ItemEntity(world,owner.getX(),owner.getY()+.5,owner.getZ(),core);drop.setToDefaultPickupDelay();world.spawnEntity(drop);}
        }
    }
}
