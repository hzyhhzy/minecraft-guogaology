package dev.googology.mixin;
import dev.googology.mining.MiningEffects;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Block.class)
public abstract class BlockYieldMixin {
    @WrapMethod(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> googology$yield(net.minecraft.world.level.block.state.BlockState state,ServerLevel world,BlockPos pos,BlockEntity blockEntity,Entity owner,ItemStack tool,Operation<List<ItemStack>> original){return original.call(state,world,pos,blockEntity,owner,(Object)tool instanceof ItemStack stack?MiningEffects.lootTool(state,world,owner,stack):tool);}
}
