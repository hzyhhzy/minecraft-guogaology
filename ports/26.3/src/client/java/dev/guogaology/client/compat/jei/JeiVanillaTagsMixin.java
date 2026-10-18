package dev.guogaology.client.compat.jei;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** Preserve server-authoritative tags while JEI builds its client-only recipe fallback. */
@Pseudo
@Mixin(targets = "mezz.jei.common.recipes.VanillaClientRecipeLoader", remap = false)
public abstract class JeiVanillaTagsMixin {
    @Redirect(
        method = "loadVanillaRecipeRegistry",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagLoader;loadTagsForExistingRegistries(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;)Ljava/util/List;", ordinal = 0),
        require = 1, expect = 1
    )
    private static List<?> guogaology$keepSynchronizedBaseTags(ResourceManager resources, RegistryAccess activeRegistries) {
        // These registries belong to the live connection. JEI's vanilla-only pack
        // omits mod/datapack tags and must not stage or apply replacement tags here.
        // Empty pending tags reuse the existing lookups in buildUpdatedLookups.
        // The second TagLoader call still loads JEI's newly-created WORLD registries.
        return List.of();
    }
}
