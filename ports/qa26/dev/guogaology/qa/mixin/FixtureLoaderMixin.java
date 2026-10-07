package dev.guogaology.qa.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** QA-only: the server-generated fixture is disposable, so it needs no backup confirmation. */
@Mixin(WorldOpenFlows.class)
public abstract class FixtureLoaderMixin {
    @Inject(method="openWorld",at=@At("HEAD"),cancellable=true)
    private void createFreshQaWorld(String name,Runnable cancel,CallbackInfo ci){
        if(!Boolean.getBoolean("guogaology.qa.freshworld")||!name.equals("port-qa"))return;
        var client=Minecraft.getInstance();
        var directory=client.gameDirectory.toPath();
        if(java.nio.file.Files.exists(directory.resolve("saves").resolve(name)))
            throw new IllegalStateException("Fresh QA must never reopen an existing world");
        var defaults=WorldOptions.defaultWithRandomSeed();
        var options=new WorldOptions(Long.getLong("guogaology.qa.seed",defaults.seed()),
                defaults.generateStructures(),defaults.generateBonusChest());
        try{
            java.nio.file.Files.writeString(directory.resolve("fresh-world-seed.txt"),Long.toString(options.seed()),
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        var settings=new LevelSettings("Guogaology fresh namespace QA",GameType.CREATIVE,
                LevelSettings.DifficultySettings.DEFAULT,true,WorldDataConfiguration.DEFAULT);
        ((WorldOpenFlows)(Object)this).createFreshLevel(name,settings,options,
                WorldPresets::createNormalWorldDimensions,new TitleScreen());
        ci.cancel();
    }
    @Shadow private void openWorldLoadBundledResourcePack(LevelStorageSource.LevelStorageAccess storage,WorldStem stem,PackRepository packs,Runnable cancel){throw new AssertionError();}
    @Inject(method="openWorldCheckWorldStemCompatibility",at=@At("HEAD"),cancellable=true)
    private void openFixture(LevelStorageSource.LevelStorageAccess storage,WorldStem stem,PackRepository packs,Runnable cancel,CallbackInfo ci){
        openWorldLoadBundledResourcePack(storage,stem,packs,cancel);ci.cancel();
    }
}
