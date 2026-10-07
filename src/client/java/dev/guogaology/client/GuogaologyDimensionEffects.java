package dev.guogaology.client;

import dev.guogaology.GuogaologyMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.math.Vec3d;

/** Darkness belongs to the separate underworld, never to Astra's occasional firs. */
public final class GuogaologyDimensionEffects extends DimensionEffects.Overworld {
    public static float forestAmount() {
        var client=MinecraftClient.getInstance();
        return client.world!=null && client.world.getRegistryKey().equals(GuogaologyMod.GUOGAO) ? 1 : 0;
    }
    @Override public SkyType getSkyType() { return forestAmount()>.5f ? SkyType.NONE : SkyType.NORMAL; }
    @Override public Vec3d adjustFogColor(Vec3d color,float sunHeight) {
        return super.adjustFogColor(color,sunHeight).multiply(1-forestAmount()*.77);
    }
    @Override public boolean shouldBrightenLighting() {
        var client=MinecraftClient.getInstance();
        return client.world!=null && client.world.getBiome(client.gameRenderer.getCamera().getBlockPos()).matchesId(GuogaologyMod.id("laver_tablelands"));
    }
}
