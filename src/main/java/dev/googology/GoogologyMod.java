package dev.googology;

import dev.googology.portal.PortalTravel;
import dev.googology.world.GoogologyBiomeSource;
import dev.googology.world.GoogologyFeatures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoogologyMod implements ModInitializer {
    public static final String ID = "googology";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final RegistryKey<World> DIMENSION = RegistryKey.of(RegistryKeys.WORLD, id("googology"));
    public static final RegistryKey<World> OUTER = RegistryKey.of(RegistryKeys.WORLD, id("outer"));
    public static final RegistryKey<World> GUOGAO = RegistryKey.of(RegistryKeys.WORLD, id("guogao"));

    public static Identifier id(String path) { return Identifier.of(ID, path); }

    @Override
    public void onInitialize() {
        GoogologyBlocks.initialize();
        GoogologySounds.initialize();
        WorldMaterials.initialize();
        dev.googology.mining.MiningContent.initialize();
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("guogaology:outer",Runnable.class).forEach(Runnable::run);
        CreativeCatalog.initialize();
        dev.googology.portal.PortalRitual.initialize();
        dev.googology.world.OrdinalDensity.initialize();
        GoogologyFeatures.initialize();
        GoogologyBiomeSource.initialize();
        dev.googology.world.UnderworldBiomeSource.initialize();
        PortalTravel.initialize();
        dev.googology.ambience.LaverMusic.initialize();
        GoogologyCommands.initialize();
        LOGGER.info("Googology has unfolded. Twelve cakes, one apple, and a very anxious ordinal.");
    }
}
