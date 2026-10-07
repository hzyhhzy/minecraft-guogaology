package dev.googology;

import dev.googology.block.FruitJellyBlock;
import dev.googology.block.GoogologyPortalBlock;
import dev.googology.block.AbsenceBlock;
import dev.googology.block.LtyChimeBlock;
import dev.googology.block.EmojiLanternBlock;
import dev.googology.block.OrdinalBrickBlock;
import dev.googology.block.GoogologyPortalFrameBlock;
import dev.googology.block.ChristmasDigitBlock;
import dev.googology.block.ChristmasLightBlock;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import java.util.ArrayList;
import java.util.List;

public final class GoogologyBlocks {
    public static final List<Block> ALL = new ArrayList<>();
    public static final List<Block> TRANSLUCENT = new ArrayList<>();
    public static final Block GUOGAO_LOAM = block("guogao_loam", new Block(dev.googology.survival.BlockBalance.apply("guogao_loam",BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.WARPED_HYPHAE))));
    public static final Block ROOTBOUND_STONE = block("rootbound_stone", new Block(dev.googology.survival.BlockBalance.apply("rootbound_stone",BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.WARPED_STEM))));
    public static final Block[] NUMBER_STONES = numberStones();
    public static final Block ORDINAL_STONE = NUMBER_STONES[0];
    private static Block[] numberStones(){
        var blocks=new Block[10];
        for(int n=0;n<10;n++)blocks[n]=block(n==0?"ordinal_stone":"ordinal_stone_"+n,new dev.googology.block.NumberStoneBlock(dev.googology.survival.BlockBalance.apply(n==0?"ordinal_stone":"ordinal_stone_"+n,BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.STONE)),n));
        return blocks;
    }
    public static final Block POWER_SAND = block("power_sand", new Block(dev.googology.survival.BlockBalance.apply("power_sand",BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_ORANGE))));
    public static final Block POWER_BRICKS = block("power_bricks", new Block(dev.googology.survival.BlockBalance.apply("power_bricks",BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_LOG).mapColor(MapColor.TERRACOTTA_ORANGE))));
    public static final Block EPSILON_TURF = block("epsilon_turf", new Block(dev.googology.survival.BlockBalance.apply("epsilon_turf",BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_CYAN))));
    public static final Block DREAD_LOG = block("dread_log", new RotatedPillarBlock(dev.googology.survival.BlockBalance.apply("dread_log",BlockBehaviour.Properties.ofFullCopy(Blocks.WARPED_STEM).mapColor(MapColor.PLANT).sound(SoundType.WOOD))));
    public static final Block DREAD_LEAVES = block("dread_leaves", new net.minecraft.world.level.block.TintedParticleLeavesBlock(0.01f,dev.googology.survival.BlockBalance.apply("dread_leaves",BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).randomTicks().sound(SoundType.GRASS).noOcclusion().isValidSpawn((state,world,pos,type)->type==net.minecraft.world.entity.EntityType.OCELOT||type==net.minecraft.world.entity.EntityType.PARROT).isSuffocating((state,world,pos)->false).isViewBlocking((state,world,pos)->false))));
    public static final Block ORDINAL_CRYSTAL = translucent("ordinal_crystal", new TransparentBlock(dev.googology.survival.BlockBalance.apply("ordinal_crystal",BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).sound(SoundType.AMETHYST).noOcclusion().lightLevel(s -> 5))));
    public static final Block RIDGE_LAMINA = landscape("ridge_lamina",false);
    public static final Block PROJECTION_GLASS = landscape("projection_glass",true);
    public static final Block BEAF_MARBLE = landscape("beaf_marble",false);
    public static final Block BIRD_SHALE = landscape("bird_shale",false);
    public static final Block CONWAY_LINK = landscape("conway_link",false);
    public static final Block VEBLEN_PETAL = landscape("veblen_petal",true);
    public static final Block LIMIT_STONE = landscape("limit_stone",false);
    public static final Block RANK_AMBER = landscape("rank_amber",false);
    public static final Block LOGIC_IVORY = landscape("logic_ivory",false);
    public static final Block RAYO_ROSE = landscape("rayo_rose",true);
    public static final Block SET_JADE = landscape("set_jade",false);
    public static final Block LIMIT_LAMINA = landscape("limit_lamina",false);
    public static final Block SET_GLASS = landscape("set_glass",true);
    public static final Block FORMULA_STONE = landscape("formula_stone",false);
    public static final Block PROOF_STONE = landscape("proof_stone",false);
    public static final Block TURING_TAPE = block("turing_tape",new dev.googology.block.TuringTapeBlock(dev.googology.survival.BlockBalance.apply("turing_tape",BlockBehaviour.Properties.ofFullCopy(Blocks.STONE))));
    public static final Block SEQUENCE_CORE = relic("sequence_core");
    public static final Block POWER_TOWER_CORE = relic("power_tower_core");
    public static final Block HYDRA_BUD = relic("hydra_bud");
    public static final Block BOUNDARY_CORE = relic("boundary_core");
    public static final Block LAVER_CORE = relic("laver_core");
    public static final Block LAVER_COURT_NODE = block("laver_court_node",new Block(dev.googology.survival.BlockBalance.apply("laver_court_node",dev.googology.survival.BlockBalance.amethystSettings())));
    public static final Block LAVER_COURT_BLANK = block("laver_court_blank",new Block(dev.googology.survival.BlockBalance.apply("laver_court_blank",dev.googology.survival.BlockBalance.amethystSettings())));
    public static final Block LAVER_INLAY = block("laver_inlay",new Block(dev.googology.survival.BlockBalance.apply("laver_inlay",BlockBehaviour.Properties.ofFullCopy(Blocks.STONE))));
    public static final Block ASTRA_CRITICAL_CORE = relic("astra_critical_core");
    public static final Block GUOGAO_HEART = relic("guogao_heart");
    // Retain the placed block ID; all warm decorative materials are independent of gold currency.
    public static final Block STAR_GOLD = block("star_gold", new Block(dev.googology.survival.BlockBalance.apply("star_gold",BlockBehaviour.Properties.ofFullCopy(Blocks.GLOWSTONE).lightLevel(s -> 15))));
    public static final Block SUN_PATTERN_TILES = block("sun_pattern_tiles", new Block(dev.googology.survival.BlockBalance.apply("sun_pattern_tiles",BlockBehaviour.Properties.ofFullCopy(Blocks.YELLOW_TERRACOTTA))));
    public static final Block AMBER_INLAY = block("amber_inlay", new Block(dev.googology.survival.BlockBalance.apply("amber_inlay",BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_SANDSTONE).lightLevel(s -> 10))));
    public static final Block MATRIX_ARCHIVE_CERAMIC = relic("matrix_archive_ceramic");
    public static final Block RECURSIVE_BRONZE = relic("recursive_bronze");
    public static final Block HYDRA_JADE = relic("hydra_jade");
    public static final Block AXIOM_PORCELAIN = relic("axiom_porcelain");
    public static final Block RESONANT_SILK = relic("resonant_silk");
    public static final Block ASTRA_COMPUTE_CRYSTAL = relic("astra_compute_crystal");
    public static final Block GUOGAO_HEART_RESIN = relic("guogao_heart_resin");
    public static final Block GUOGAO_LANTERN = lantern("guogao_lantern");
    public static final Block CYAN_LANTERN = lantern("cyan_guogao_lantern");
    public static final Block ROSE_LANTERN = lantern("rose_guogao_lantern");
    public static final Block LIME_LANTERN = lantern("lime_guogao_lantern");
    public static final Block VIOLET_LANTERN = lantern("violet_guogao_lantern");
    public static final Block SCARLET_LANTERN = lantern("scarlet_guogao_lantern");
    public static final Block MOSAIC_LIGHT = block("mosaic_light", new dev.googology.block.MosaicLightBlock(dev.googology.survival.BlockBalance.apply("mosaic_light",BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN))));
    public static final Block ASTRA_WEAVE = block("astra_weave", new dev.googology.block.AstraWeaveBlock(dev.googology.survival.BlockBalance.apply("astra_weave",BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK))));
    public static final Block AMBER_GUOGAO = jelly("amber_guogao");
    public static final Block BERRY_GUOGAO = jelly("berry_guogao");
    public static final Block LIME_GUOGAO = jelly("lime_guogao");
    public static final Block AZURE_GUOGAO = jelly("azure_guogao");
    public static final Block PLAIN_AMBER_GUOGAO = jelly("plain_amber_guogao");
    public static final Block PLAIN_BERRY_GUOGAO = jelly("plain_berry_guogao");
    public static final Block PLAIN_LIME_GUOGAO = jelly("plain_lime_guogao");
    public static final Block PLAIN_AZURE_GUOGAO = jelly("plain_azure_guogao");

    public static final Block OUTER_RETURN_FRAME = block("outer_return_frame", new dev.googology.block.ReturnFrameBlock(dev.googology.survival.BlockBalance.apply("outer_return_frame",BlockBehaviour.Properties.of().noOcclusion()),0));
    public static final Block INNER_RETURN_FRAME = block("inner_return_frame", new dev.googology.block.ReturnFrameBlock(dev.googology.survival.BlockBalance.apply("inner_return_frame",BlockBehaviour.Properties.of().noOcclusion()),1));
    public static final Block GUOGAO_RETURN_FRAME = block("guogao_return_frame", new dev.googology.block.ReturnFrameBlock(dev.googology.survival.BlockBalance.apply("guogao_return_frame",BlockBehaviour.Properties.of().noOcclusion()),2));

    public static final Block PORTAL_FRAME = block("guogao_portal_frame", new GoogologyPortalFrameBlock(dev.googology.survival.BlockBalance.apply("guogao_portal_frame",BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(3.5f).noOcclusion().lightLevel(s -> 8))));
    public static final Block PORTAL = block("guogao_portal", new GoogologyPortalBlock(dev.googology.survival.BlockBalance.apply("guogao_portal",BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noCollision().noOcclusion().lightLevel(s -> 12).noLootTable().sound(SoundType.GLASS))));
    public static final Block INNER_PORTAL = block("inner_portal", new GoogologyPortalBlock(dev.googology.survival.BlockBalance.apply("inner_portal",BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noCollision().noOcclusion().lightLevel(s -> 12).noLootTable().sound(SoundType.GLASS))));
    public static final Block FRUIT_PORTAL = block("fruit_portal", new GoogologyPortalBlock(dev.googology.survival.BlockBalance.apply("fruit_portal",BlockBehaviour.Properties.of().strength(-1.0f,3600000.0f).noCollision().noOcclusion().lightLevel(s->8).noLootTable().sound(SoundType.GLASS))));
    public static final Block EPSILON_BLOOM = block("epsilon_bloom", new FlowerBlock(MobEffects.SLOW_FALLING, 8, dev.googology.survival.BlockBalance.apply("epsilon_bloom",BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(s -> 5))));
    public static final Block EMOJI_FLOWER = block("emoji_flower", new FlowerBlock(MobEffects.SLOW_FALLING, 8, dev.googology.survival.BlockBalance.apply("emoji_flower",BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(s -> 4))));
    public static final Block OMEGA_BLOOM = flower("omega_bloom");
    public static final Block GREAT_OMEGA_BLOOM = flower("great_omega_bloom");
    public static final Block ZETA_VINE = flower("zeta_vine");
    public static final Block PSI_FERN = flower("psi_fern");
    public static final Block PHI_BLOOM = flower("phi_bloom");
    public static final Block[] ORDINAL_PLANTS = {EPSILON_BLOOM, OMEGA_BLOOM, GREAT_OMEGA_BLOOM, ZETA_VINE, PSI_FERN, PHI_BLOOM};
    public static final Block EPSILON_SYMBOL = symbol("epsilon_symbol");
    public static final Block OMEGA_SYMBOL = symbol("omega_symbol");
    public static final Block GREAT_OMEGA_SYMBOL = symbol("great_omega_symbol");
    public static final Block ZETA_SYMBOL = symbol("zeta_symbol");
    public static final Block PSI_SYMBOL = symbol("psi_symbol");
    public static final Block PHI_SYMBOL = symbol("phi_symbol");
    public static final Block[] SYMBOLS = {EPSILON_SYMBOL,OMEGA_SYMBOL,GREAT_OMEGA_SYMBOL,ZETA_SYMBOL,PSI_SYMBOL,PHI_SYMBOL};
    public static final Block ABSENCE_GLASS = translucent("absence_glass", new TransparentBlock(dev.googology.survival.BlockBalance.apply("absence_glass",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(.4f).noOcclusion().lightLevel(s->2))));
    public static final Block LHO_LETTER_L = translucent("lho_letter_l", new TransparentBlock(dev.googology.survival.BlockBalance.apply("lho_letter_l",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().lightLevel(s->5))));
    public static final Block LHO_LETTER_H = translucent("lho_letter_h", new TransparentBlock(dev.googology.survival.BlockBalance.apply("lho_letter_h",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().lightLevel(s->5))));
    public static final Block LHO_LETTER_O = translucent("lho_letter_o", new TransparentBlock(dev.googology.survival.BlockBalance.apply("lho_letter_o",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().lightLevel(s->5))));
    public static final Block LHO_TRACE = translucent("lho_trace", new AbsenceBlock(dev.googology.survival.BlockBalance.apply("lho_trace",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noCollision().noOcclusion().noLootTable().lightLevel(s -> 6))));
    public static final Block FFFZ_TRACE = translucent("fffz_trace", new AbsenceBlock(dev.googology.survival.BlockBalance.apply("fffz_trace",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noCollision().noOcclusion().noLootTable().lightLevel(s -> 5))));
    public static final Block FOS_TRACE = translucent("fos_trace", new AbsenceBlock(dev.googology.survival.BlockBalance.apply("fos_trace",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noCollision().noOcclusion().noLootTable().lightLevel(s -> 5))));
    public static final Block LHO_HYDRA_PSI = translucent("lho_hydra_psi",new dev.googology.block.VanishingHydraBlock(dev.googology.survival.BlockBalance.apply("lho_hydra_psi",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(.25f).noOcclusion().noLootTable().lightLevel(s->5))));
    public static final Block LHO_HYDRA_Z = translucent("lho_hydra_z",new dev.googology.block.VanishingHydraBlock(dev.googology.survival.BlockBalance.apply("lho_hydra_z",BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(.25f).noOcclusion().noLootTable().lightLevel(s->5))));
    public static final Block[] SANCTUARY_RELICS = {MATRIX_ARCHIVE_CERAMIC,RECURSIVE_BRONZE,HYDRA_JADE,ABSENCE_GLASS,RESONANT_SILK,ASTRA_COMPUTE_CRYSTAL,GUOGAO_HEART_RESIN,AXIOM_PORCELAIN};
    public static final Block LAVER_MAT = block("laver_mat", new Block(dev.googology.survival.BlockBalance.apply("laver_mat",BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.PLANT))));
    public static final Block GIANT_LAVER = block("giant_laver", new Block(dev.googology.survival.BlockBalance.apply("giant_laver",BlockBehaviour.Properties.ofFullCopy(Blocks.DRIED_KELP_BLOCK).noOcclusion().strength(.35f).mapColor(MapColor.PLANT))));
    public static final Block LAVER_VEIN = block("laver_vein", new RotatedPillarBlock(dev.googology.survival.BlockBalance.apply("laver_vein",BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).strength(2).mapColor(MapColor.PLANT))));
    public static final Block LAVER_PLANKS = block("laver_planks", new Block(dev.googology.survival.BlockBalance.apply("laver_planks",BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_PLANKS).mapColor(MapColor.PLANT).lightLevel(s -> 5))));
    public static final Block BASIC_LAVER_PATTERN = block("basic_laver_pattern", new Block(dev.googology.survival.BlockBalance.apply("basic_laver_pattern",BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_MOSAIC).mapColor(MapColor.WARPED_NYLIUM).lightLevel(s -> 6))));
    public static final Block LTY_YARN = block("lty_yarn", new LtyChimeBlock(dev.googology.survival.BlockBalance.apply("lty_yarn",BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).noOcclusion().lightLevel(s -> 6))));
    public static final Block TIANYI_FIBER = block("tianyi_fiber", new LtyChimeBlock(dev.googology.survival.BlockBalance.apply("tianyi_fiber",BlockBehaviour.Properties.ofFullCopy(Blocks.LIGHT_BLUE_WOOL).lightLevel(s -> 3))));
    public static final Block WHITE_FIBER = block("white_fiber", new LtyChimeBlock(dev.googology.survival.BlockBalance.apply("white_fiber",BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).lightLevel(s -> 3))));
    public static final Block Y_LOG = block("y_log", new RotatedPillarBlock(dev.googology.survival.BlockBalance.apply("y_log",BlockBehaviour.Properties.ofFullCopy(Blocks.BIRCH_LOG).mapColor(MapColor.ICE))));
    public static final Block Y_LEAVES = block("y_leaves", new net.minecraft.world.level.block.TintedParticleLeavesBlock(0.01f,dev.googology.survival.BlockBalance.apply("y_leaves",BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).mapColor(MapColor.COLOR_LIGHT_BLUE))));
    public static final Block TREE_NODE_RED = block("tree_node_red",new dev.googology.block.PortableRelicBlock(dev.googology.survival.BlockBalance.apply("tree_node_red",dev.googology.survival.BlockBalance.amethystSettings().lightLevel(s->7))));
    public static final Block TREE_NODE_GREEN = block("tree_node_green",new dev.googology.block.PortableRelicBlock(dev.googology.survival.BlockBalance.apply("tree_node_green",dev.googology.survival.BlockBalance.amethystSettings().lightLevel(s->7))));
    public static final Block TREE_NODE_BLUE = block("tree_node_blue",new dev.googology.block.PortableRelicBlock(dev.googology.survival.BlockBalance.apply("tree_node_blue",dev.googology.survival.BlockBalance.amethystSettings().lightLevel(s->7))));
    public static final Block[] TREE_NODES={TREE_NODE_RED,TREE_NODE_GREEN,TREE_NODE_BLUE};
    public static final Block IBLP_BLANK = block("iblp_blank", new Block(dev.googology.survival.BlockBalance.apply("iblp_blank",BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_MOSAIC).lightLevel(s -> 5))));
    public static final Block IBLP_NODE = block("iblp_node", new Block(dev.googology.survival.BlockBalance.apply("iblp_node",BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_MOSAIC).lightLevel(s -> 12))));
    public static final Block IBLP_MARKED = block("iblp_marked", new Block(dev.googology.survival.BlockBalance.apply("iblp_marked",BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_MOSAIC).lightLevel(s -> 13))));
    public static final Block ASTRA_MARBLE = block("astra_marble", new Block(dev.googology.survival.BlockBalance.apply("astra_marble",BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).lightLevel(s -> 10))));
    public static final Block ASTRA_MINT = block("astra_mint", new Block(dev.googology.survival.BlockBalance.apply("astra_mint",BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).mapColor(MapColor.GRASS).lightLevel(s -> 10))));
    public static final Block ASTRA_LIGHT = block("astra_light", new Block(dev.googology.survival.BlockBalance.apply("astra_light",BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).lightLevel(s -> 15))));
    public static final Block ASTRA_CLOUD = block("astra_cloud", new Block(dev.googology.survival.BlockBalance.apply("astra_cloud",BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).mapColor(MapColor.SNOW).lightLevel(s -> 6))));
    public static final Block ASTRA_CLOUD_SHADE = block("astra_cloud_shade", new Block(dev.googology.survival.BlockBalance.apply("astra_cloud_shade",BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL).mapColor(MapColor.GRASS).lightLevel(s -> 6))));
    public static final Block OFFICE_DESK = block("office_desk",new Block(dev.googology.survival.BlockBalance.apply("office_desk",BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).noOcclusion())));
    public static final Block OFFICE_MONITOR = block("office_monitor",new Block(dev.googology.survival.BlockBalance.apply("office_monitor",BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(s->4))));
    public static final Block SERVER_RACK = block("server_rack",new Block(dev.googology.survival.BlockBalance.apply("server_rack",BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(s->3))));
    public static final Block Y_SEQUENCE_STONE = block("y_sequence_stone", new Block(dev.googology.survival.BlockBalance.apply("y_sequence_stone",BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.ICE))));

    public static final Block ORDINAL_BRICKS = block("ordinal_bricks",new OrdinalBrickBlock(dev.googology.survival.BlockBalance.apply("ordinal_bricks",BlockBehaviour.Properties.ofFullCopy(Blocks.STONE))));
    public static final Block[] PLAIN_LIGHTS={
            plainLamp("amber_light",0),plainLamp("cyan_light",1),plainLamp("rose_light",2),
            plainLamp("lime_light",3),plainLamp("violet_light",4),plainLamp("scarlet_light",5)};
    private static Block plainLamp(String id,int color){return block(id,new ChristmasLightBlock(dev.googology.survival.BlockBalance.apply(id,BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN)),color));}
    public static final Block[] SEQUENCE_LIGHTS={
            digitLamp("amber_sequence_light",0),digitLamp("cyan_sequence_light",1),digitLamp("rose_sequence_light",2),
            digitLamp("lime_sequence_light",3),digitLamp("violet_sequence_light",4),digitLamp("scarlet_sequence_light",5)};
    private static Block digitLamp(String id,int color){return block(id,new ChristmasDigitBlock(dev.googology.survival.BlockBalance.apply(id,BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN)),color));}
    public static BlockState ordinalBrick(int n){return ORDINAL_BRICKS.defaultBlockState().setValue(OrdinalBrickBlock.NUMBER,n);}
    public static int ordinalValue(BlockState state) {return state.is(ORDINAL_BRICKS)?state.getValue(OrdinalBrickBlock.NUMBER):-1;}

    public static final Item GUOGAO_SLICE = Registry.register(BuiltInRegistries.ITEM, GoogologyMod.id("guogao_slice"), new Item(itemSettings("guogao_slice").food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6f).build())));
    public static final Item RETURN_TOKEN = Registry.register(BuiltInRegistries.ITEM, GoogologyMod.id("return_token"), new Item(itemSettings("return_token")));
    public static final Item ORDINAL_SHARD = Registry.register(BuiltInRegistries.ITEM, GoogologyMod.id("ordinal_shard"), new Item(itemSettings("ordinal_shard")));
    public static final Block[] JELLIES = {AMBER_GUOGAO, BERRY_GUOGAO, LIME_GUOGAO, AZURE_GUOGAO};
    public static final Block[] ALL_JELLIES = {AMBER_GUOGAO, BERRY_GUOGAO, LIME_GUOGAO, AZURE_GUOGAO, PLAIN_AMBER_GUOGAO, PLAIN_BERRY_GUOGAO, PLAIN_LIME_GUOGAO, PLAIN_AZURE_GUOGAO};
    public static final Block[] LANTERNS = {GUOGAO_LANTERN, CYAN_LANTERN, ROSE_LANTERN, LIME_LANTERN, VIOLET_LANTERN, SCARLET_LANTERN};

    private static Item.Properties itemSettings(String name) {return new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM,GoogologyMod.id(name)));}

    private static Block block(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, GoogologyMod.id(name), block);
        var settings=itemSettings(name).useBlockDescriptionPrefix();
        if(CoreGrades.ROOTS.contains(name))settings.rarity(CoreGrades.rarity(name,1));
        if(name.equals("dread_log")||name.equals("dread_leaves"))settings.fireResistant();
        Registry.register(BuiltInRegistries.ITEM, GoogologyMod.id(name), block instanceof dev.googology.block.StatefulDecorBlock
                ? new dev.googology.block.VariantDecorItem(block,settings) : new BlockItem(block, settings));
        ALL.add(block);
        return block;
    }
    private static Block relic(String name){
        var settings=dev.googology.survival.BlockBalance.apply(name,dev.googology.survival.BlockBalance.amethystSettings());
        boolean clear=name.equals("guogao_heart")||name.equals("sequence_core")||name.equals("laver_core")||name.equals("hydra_bud")||name.equals("astra_critical_core")||name.equals("power_tower_core")||name.equals("boundary_core");
        Block block=clear?new dev.googology.block.PortableRelicBlock(settings.noOcclusion()):new dev.googology.block.PortableRelicBlock(settings);
        if(clear)TRANSLUCENT.add(block);
        Registry.register(BuiltInRegistries.BLOCK,GoogologyMod.id(name),block);
        Registry.register(BuiltInRegistries.ITEM,GoogologyMod.id(name),new BlockItem(block,itemSettings(name).useBlockDescriptionPrefix().rarity(CoreGrades.rarity(name,1)).fireResistant()));
        ALL.add(block);return block;
    }

    private static Block translucent(String name, Block block) {
        TRANSLUCENT.add(block);
        return block(name, block);
    }
    private static Block landscape(String name,boolean clear){
        var settings=dev.googology.survival.BlockBalance.apply(name,BlockBehaviour.Properties.ofFullCopy(clear?Blocks.GLASS:Blocks.CALCITE));
        return clear?translucent(name,new TransparentBlock(settings.noOcclusion())):block(name,new Block(settings));
    }

    private static Block jelly(String name) {
        return translucent(name, new FruitJellyBlock(dev.googology.survival.BlockBalance.apply(name,BlockBehaviour.Properties.ofFullCopy(Blocks.HONEY_BLOCK).strength(0.5f).noOcclusion().lightLevel(s -> 4).sound(SoundType.SLIME_BLOCK))));
    }

    private static Block lantern(String name) {
        return block(name, new EmojiLanternBlock(dev.googology.survival.BlockBalance.apply(name,BlockBehaviour.Properties.ofFullCopy(Blocks.SEA_LANTERN).noOcclusion().lightLevel(s -> 15).emissiveRendering((state,world,pos)->true))));
    }

    private static Block flower(String name) {
        return block(name, new FlowerBlock(MobEffects.SLOW_FALLING, 8, dev.googology.survival.BlockBalance.apply(name,BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION).lightLevel(s -> 6))));
    }
    private static Block symbol(String name) { return block(name,new Block(dev.googology.survival.BlockBalance.apply(name,dev.googology.survival.BlockBalance.amethystSettings().lightLevel(s->9)))); }

    public static void initialize() { CoreGrades.initialize(); }
}
