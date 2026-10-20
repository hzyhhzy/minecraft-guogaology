package dev.guogaology.client.compat.jei;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

/** Optional structural patch; inspecting class bytes never loads JEI classes. */
public final class JeiTagCompatPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("Guogaology/JEI");
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var jei = FabricLoader.getInstance().getModContainer("jei");
        if (jei.isEmpty() || !targetClassName.equals(JeiTagCompatShape.TARGET)) return false;
        String version = jei.get().getMetadata().getVersion().getFriendlyString();
        try {
            ClassNode target = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName, false);
            String reason = JeiTagCompatShape.incompatibility(target);
            if (reason == null) {
                LOGGER.info("Protecting server tags in JEI {}: compatible fallback bytecode", version);
                return true;
            }
            LOGGER.warn("Skipping optional JEI {} tag protection: {}", version, reason);
        } catch (Exception failure) {
            LOGGER.warn("Skipping optional JEI {} tag protection: cannot inspect class bytes", version, failure);
        }
        return false;
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        String reason = JeiTagCompatShape.incompatibility(targetClass);
        if (reason != null) throw new IllegalStateException("JEI fallback changed after inspection; refusing unsafe tag redirection: " + reason);
    }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
