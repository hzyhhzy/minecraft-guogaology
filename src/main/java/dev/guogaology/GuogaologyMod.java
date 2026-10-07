package dev.guogaology;

import dev.guogaology.portal.PortalTravel;
import dev.guogaology.world.GuogaologyBiomeSource;
import dev.guogaology.world.GuogaologyFeatures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuogaologyMod implements ModInitializer {
    public static final String ID = "guogaology";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final RegistryKey<World> DIMENSION = RegistryKey.of(RegistryKeys.WORLD, id("guogaology"));
    public static final RegistryKey<World> OUTER = RegistryKey.of(RegistryKeys.WORLD, id("outer"));
    public static final RegistryKey<World> GUOGAO = RegistryKey.of(RegistryKeys.WORLD, id("guogao"));

    public static Identifier id(String path) { return Identifier.of(ID, path); }

    @Override
    public void onInitialize() {
        GuogaologyBlocks.initialize();
        GuogaologySounds.initialize();
        WorldMaterials.initialize();
        dev.guogaology.mining.MiningContent.initialize();
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypoints("guogaology:outer",Runnable.class).forEach(Runnable::run);
        CreativeCatalog.initialize();
        dev.guogaology.portal.PortalRitual.initialize();
        dev.guogaology.world.OrdinalDensity.initialize();
        GuogaologyFeatures.initialize();
        GuogaologyBiomeSource.initialize();
        dev.guogaology.world.UnderworldBiomeSource.initialize();
        PortalTravel.initialize();
        dev.guogaology.ambience.LaverMusic.initialize();
        GuogaologyCommands.initialize();
        LOGGER.info("Guogaology has unfolded. Twelve cakes, one apple, and a very anxious ordinal.");
    }
}
