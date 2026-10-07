package dev.guogaology.client.mixin;

import dev.guogaology.client.CoreMeshModels;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 1.21.1 counterpart of modern Minecraft's native display_context item selector. */
@Mixin(ItemRenderer.class)
public abstract class CoreItemViewMixin {
    @ModifyVariable(method="renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private BakedModel guogaology$coreView(BakedModel original,ItemStack stack,ModelTransformationMode mode,boolean leftHand,
            MatrixStack matrices,VertexConsumerProvider consumers,int light,int overlay,BakedModel unused){
        return CoreMeshModels.itemView(stack,mode,original);
    }
}
