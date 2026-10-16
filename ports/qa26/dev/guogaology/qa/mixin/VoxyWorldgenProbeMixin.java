package dev.guogaology.qa.mixin;

import dev.guogaology.mergeqa.DedicatedVoxyProbe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observation only. Never replaces the installed generator's packet or ingest code. */
@Pseudo
@Mixin(targets="com.ethan.voxyworldgenv2.integration.VoxyIntegration",remap=false)
public abstract class VoxyWorldgenProbeMixin {
    @Inject(method="rawIngest(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/LevelChunkSection;IIILnet/minecraft/world/level/chunk/DataLayer;Lnet/minecraft/world/level/chunk/DataLayer;)V",at=@At("RETURN"),remap=false)
    private static void received(Level world,LevelChunkSection section,int x,int y,int z,DataLayer block,DataLayer sky,CallbackInfo ci){
        if(Boolean.getBoolean("guogaology.qa.voxyworldgen"))DedicatedVoxyProbe.record(world,section,x,y,z);
    }
}
