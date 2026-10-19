package dev.guogaology.mixin;
import dev.guogaology.mining.MiningEffects;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Block.class)
public abstract class BlockYieldMixin {
    @WrapMethod(method="getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> guogaology$yield(BlockState state,ServerWorld world,BlockPos pos,BlockEntity blockEntity,Entity owner,ItemStack tool,Operation<List<ItemStack>> original){return original.call(state,world,pos,blockEntity,owner,MiningEffects.lootTool(state,world,owner,tool));}
    // Keep loot and XP on the same eligible Silk Touch copy. Wrap the entry point:
    // NeoForge moves the XP callback into its block-drop event pipeline.
    @WrapMethod(method="dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V")
    private static void guogaology$experience(BlockState state,World world,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemStack tool,Operation<Void> original){
        original.call(state,world,pos,blockEntity,breaker,world instanceof ServerWorld server?MiningEffects.experienceTool(state,server,breaker,tool):tool);
    }
}
