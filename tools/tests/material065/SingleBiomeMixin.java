package dev.guogaology.material065.mixin;
import dev.guogaology.world.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BiomeRegions.class,remap=false)
public abstract class SingleBiomeMixin {
    @Inject(method="weights",at=@At("HEAD"),cancellable=true)
    private static void onlyFrontier(long seed,int x,int z,CallbackInfoReturnable<double[]> cir){var weights=new double[BiomeRegions.COUNT];weights[7]=1;cir.setReturnValue(weights);}
}
