package dev.guogaology.mixin;
import dev.guogaology.mining.MiningEffects;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(Block.class)
public abstract class BlockYieldMixin {
    @WrapMethod(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> guogaology$yield(net.minecraft.world.level.block.state.BlockState state,ServerLevel world,BlockPos pos,BlockEntity blockEntity,Entity owner,ItemStack tool,Operation<List<ItemStack>> original){return original.call(state,world,pos,blockEntity,owner,(Object)tool instanceof ItemStack stack?MiningEffects.lootTool(state,world,owner,stack):tool);}
    // Keep loot and XP on the same eligible Silk Touch copy. Wrap the entry point:
    // NeoForge moves the XP callback into its block-drop event pipeline.
    @WrapMethod(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V")
    private static void guogaology$experience(net.minecraft.world.level.block.state.BlockState state,Level world,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemStack tool,Operation<Void> original){
        original.call(state,world,pos,blockEntity,breaker,world instanceof ServerLevel server?MiningEffects.experienceTool(state,server,breaker,tool):tool);
    }
}
