package dev.googology.qa.mixin;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** QA-only: the server-generated fixture is disposable, so it needs no backup confirmation. */
@Mixin(WorldOpenFlows.class)
public abstract class FixtureLoaderMixin {
    @Shadow private void openWorldLoadBundledResourcePack(LevelStorageSource.LevelStorageAccess storage,WorldStem stem,PackRepository packs,Runnable cancel){throw new AssertionError();}
    @Inject(method="openWorldCheckWorldStemCompatibility",at=@At("HEAD"),cancellable=true)
    private void openFixture(LevelStorageSource.LevelStorageAccess storage,WorldStem stem,PackRepository packs,Runnable cancel,CallbackInfo ci){
        openWorldLoadBundledResourcePack(storage,stem,packs,cancel);ci.cancel();
    }
}
