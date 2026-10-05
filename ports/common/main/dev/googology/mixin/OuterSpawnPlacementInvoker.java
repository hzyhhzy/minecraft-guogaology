package dev.googology.mixin;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A remapped invoker also works in the obfuscated 1.21.x releases. */
@Mixin(SpawnPlacements.class)
public interface OuterSpawnPlacementInvoker {
    @Invoker("register")
    static <T extends Mob> void registerOuter(EntityType<T> type,SpawnPlacementType placement,Heightmap.Types height,SpawnPlacements.SpawnPredicate<T> predicate){throw new AssertionError();}
}
