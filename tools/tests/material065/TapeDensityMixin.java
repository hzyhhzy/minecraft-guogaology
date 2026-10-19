package dev.guogaology.material065.mixin;
import dev.guogaology.world.*;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=SceneryDistribution.class,remap=false)
abstract class TapePassMixin {
    @Inject(method="passes",at=@At("RETURN"),cancellable=true)
    private static void density(boolean under,CallbackInfoReturnable<List<SceneryDistribution.Pass>> cir){if(under||!FrontierTape065Census.override)return;cir.setReturnValue(cir.getReturnValue().stream().map(p->p.form()==SceneryDistribution.Form.FOLDED_TAPE?new SceneryDistribution.Pass(p.form(),FrontierTape065Census.cell,p.reach(),p.biome(),p.divisor(),p.skipZero()):p).toList());}
}
@Mixin(value=SceneryDistribution.Pass.class,remap=false)
abstract class TapeRetentionMixin {
    @Inject(method="accepts",at=@At("HEAD"),cancellable=true)
    private void density(int kind,long salt,CallbackInfoReturnable<Boolean> cir){var p=(SceneryDistribution.Pass)(Object)this;if(FrontierTape065Census.override&&p.form()==SceneryDistribution.Form.FOLDED_TAPE)cir.setReturnValue(kind==p.biome()&&WorldNoise.unit(WorldNoise.mix(salt+21429))<FrontierTape065Census.chance);}
}
