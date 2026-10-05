package dev.googology;

import dev.googology.portal.PortalTravel;
import dev.googology.world.GoogologyBiomeSource;
import dev.googology.world.GoogologyFeatures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoogologyMod implements ModInitializer {
    public static final String ID = "googology";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, id("googology"));
    public static final ResourceKey<Level> OUTER = ResourceKey.create(Registries.DIMENSION, id("outer"));
    public static final ResourceKey<Level> GUOGAO = ResourceKey.create(Registries.DIMENSION, id("guogao"));

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }

    @Override
    public void onInitialize() {
        GoogologyBlocks.initialize();
        GoogologySounds.initialize();
        WorldMaterials.initialize();
        dev.googology.mining.MiningContent.initialize();
        dev.googology.outer.GoogologyMod.initialize();
        CreativeCatalog.initialize();
        dev.googology.portal.PortalRitual.initialize();
        dev.googology.world.OrdinalDensity.initialize();
        GoogologyFeatures.initialize();
        GoogologyBiomeSource.initialize();
        PortalTravel.initialize();
        dev.googology.ambience.LaverMusic.initialize();
        GoogologyCommands.initialize();
        LOGGER.info("Googology has unfolded. Twelve cakes, one apple, and a very anxious ordinal.");
    }
}
