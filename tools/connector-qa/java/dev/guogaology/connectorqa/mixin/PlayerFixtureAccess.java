package dev.guogaology.connectorqa.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Remove only the login grace period in this disposable damage-test fixture. */
@Mixin(ServerPlayerEntity.class)
public interface PlayerFixtureAccess {
    @Accessor("joinInvulnerabilityTicks") void qa$joinInvulnerability(int ticks);
}
