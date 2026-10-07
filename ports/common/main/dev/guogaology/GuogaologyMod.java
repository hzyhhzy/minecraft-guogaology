package dev.guogaology;

import dev.guogaology.portal.PortalTravel;
import dev.guogaology.world.GuogaologyBiomeSource;
import dev.guogaology.world.GuogaologyFeatures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GuogaologyMod implements ModInitializer {
    public static final String ID = "guogaology";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, id("guogaology"));
    public static final ResourceKey<Level> OUTER = ResourceKey.create(Registries.DIMENSION, id("outer"));
    public static final ResourceKey<Level> GUOGAO = ResourceKey.create(Registries.DIMENSION, id("guogao"));

    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ID, path); }

    @Override
    public void onInitialize() {
        GuogaologyBlocks.initialize();
        GuogaologySounds.initialize();
        WorldMaterials.initialize();
        dev.guogaology.mining.MiningContent.initialize();
        dev.guogaology.outer.GuogaologyMod.initialize();
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
