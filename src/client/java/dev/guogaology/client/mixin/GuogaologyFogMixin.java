package dev.guogaology.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.guogaology.GuogaologyMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FogShape;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class GuogaologyFogMixin {
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void guogaology$underworldMist(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null || fogType != BackgroundRenderer.FogType.FOG_TERRAIN
                || !client.world.getRegistryKey().equals(GuogaologyMod.GUOGAO)
                || camera.getSubmersionType() != CameraSubmersionType.NONE
                || client.player.hasStatusEffect(StatusEffects.BLINDNESS) || client.player.hasStatusEffect(StatusEffects.DARKNESS)) return;
        if (client.world.getBiome(BlockPos.ofFloored(camera.getPos())).matchesId(GuogaologyMod.id("guogao_forest"))) {
            RenderSystem.setShaderFogShape(FogShape.CYLINDER);
            RenderSystem.setShaderFogStart(Math.min(viewDistance * 0.5f, 112.0f));
            RenderSystem.setShaderFogEnd(viewDistance);
        }
    }
}
