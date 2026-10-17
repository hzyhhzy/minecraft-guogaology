package dev.guogaology;

import dev.guogaology.block.NaturalResourceBlock;
import dev.guogaology.block.HydraSprigBlock;
import dev.guogaology.survival.BlockBalance;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.List;

/** Visible collectible scenery components; none requires a pre-existing core. */
public final class CoreResources {
    public static final Block MATRIX_SHEET = node("matrix_sheet", false);
    public static final Block ARROW_KNOT = node("arrow_knot", false);
    public static final Block SCG_NODE = node("scg_node", false);
    public static final Block HYDRA_SPRIG = node("hydra_sprig", false);
    public static final Block LAVER_GEL = node("laver_gel", true);
    public static final Block CLOUD_PEARL = node("cloud_pearl", false);
    public static final Block SET_MEMBRANE = node("set_membrane", true);
    public static final Block DREAD_RESIN = node("dread_resin", true);
    public static final Block GUOGAO_DROP = node("guogao_drop", false);
    public static final Block ORDINAL_CRYSTAL_ORE = ore("ordinal_crystal_ore", false);
    public static final Block EMPTY_CRYSTAL_ORE = ore("empty_crystal_ore", true);
    public static final Block DREAD_CRYSTAL_ORE = ore("dread_crystal_ore", false);
    public static final Block TURING_ENDCAP = ore("turing_endcap", false);
    public static final List<Block> MATERIALS = List.of(MATRIX_SHEET, ARROW_KNOT, SCG_NODE,
            HYDRA_SPRIG, LAVER_GEL, CLOUD_PEARL, SET_MEMBRANE, DREAD_RESIN, GUOGAO_DROP);
    public static final List<Block> ORES = List.of(ORDINAL_CRYSTAL_ORE, EMPTY_CRYSTAL_ORE, DREAD_CRYSTAL_ORE);
    public static final List<Block> ALL = java.util.stream.Stream.concat(MATERIALS.stream(),
            java.util.stream.Stream.concat(ORES.stream(), java.util.stream.Stream.of(TURING_ENDCAP))).toList();
    private static Block node(String name, boolean clear) {
        var settings=BlockBalance.apply(name,BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).sound(SoundType.AMETHYST).noOcclusion());
        return register(name,name.equals("hydra_sprig")?new HydraSprigBlock(settings):new NaturalResourceBlock(settings),clear);
    }
    private static Block ore(String name, boolean clear) {
        var settings=BlockBalance.apply(name, BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE).sound(SoundType.STONE));
        if(clear)settings.noOcclusion();
        return register(name, new Block(settings), clear);
    }
    private static Block register(String name, Block block, boolean clear) {
        Registry.register(BuiltInRegistries.BLOCK, GuogaologyMod.id(name), block);
        Registry.register(BuiltInRegistries.ITEM, GuogaologyMod.id(name), new BlockItem(block,
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, GuogaologyMod.id(name)))
                        .useBlockDescriptionPrefix()));
        GuogaologyBlocks.ALL.add(block);
        if(clear) GuogaologyBlocks.TRANSLUCENT.add(block);
        return block;
    }
    public static void initialize() { /* Class initialization performs registration. */ }
    private CoreResources() {}
}
