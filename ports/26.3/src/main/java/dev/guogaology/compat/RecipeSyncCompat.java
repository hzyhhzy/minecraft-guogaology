package dev.guogaology.compat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.BuiltInRegistries;

/** Server-authoritative recipe data for optional client recipe viewers. */
public final class RecipeSyncCompat implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register on both physical sides. A client without JEI can still host a
        // logical LAN server for a player who uses JEI. Fabric handles the normal
        // login/reload synchronization; recipe data always comes from that server.
        BuiltInRegistries.RECIPE_SERIALIZER.forEach(serializer -> {
            String namespace = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer).getNamespace();
            if (namespace.equals("minecraft") || namespace.equals("guogaology")) {
                // Include vanilla recipes too: a nonempty partial recipe set
                // suppresses JEI's vanilla fallback. Registration is idempotent.
                RecipeSynchronization.synchronizeRecipeSerializer(serializer);
            }
        });
    }
}
