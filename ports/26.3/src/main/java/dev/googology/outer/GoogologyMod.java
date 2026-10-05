package dev.googology.outer;
import net.minecraft.resources.Identifier;
/** Compatibility boundary for the authorized donor module; the host owns shared items. */
public final class GoogologyMod {
    public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("guogaology-outer");
    private static String alias(String name){return switch(name){
            case "bashicu_block" -> "googology:ordinal_bricks";
            case "gummy_block" -> "googology:amber_guogao";
            case "fruit_cake" -> "googology:guogao_slice";
            case "christmas_light" -> "googology:amber_sequence_light";
            case "laver_log" -> "googology:laver_vein";
            case "laver_planks" -> "googology:laver_planks";
            case "laver_leaves" -> "googology:giant_laver";
            case "lho_glass" -> "googology:absence_glass";
            case "ordinal_stone" -> "minecraft:stone";
            case "cobbled_ordinal_stone" -> "minecraft:cobblestone";
            case "andesite_ordinal_stone" -> "minecraft:andesite";
            case "diorite_ordinal_stone" -> "minecraft:diorite";
            case "granite_ordinal_stone" -> "minecraft:granite";
            case "tuff_ordinal_stone" -> "minecraft:tuff";
            case "hell_ordinal_stone" -> "minecraft:netherrack";
            case "christmas_log" -> "minecraft:oak_log";
            case "christmas_wood" -> "minecraft:oak_wood";
            case "christmas_stripped_log" -> "minecraft:stripped_oak_log";
            case "christmas_stripped_wood" -> "minecraft:stripped_oak_wood";
            case "hell_christmas_log" -> "minecraft:dark_oak_log";
            case "hell_christmas_wood" -> "minecraft:dark_oak_wood";
            case "hell_christmas_stripped_log" -> "minecraft:stripped_dark_oak_log";
            case "hell_christmas_stripped_wood" -> "minecraft:stripped_dark_oak_wood";
            case "googology_portal" -> "googology:guogao_portal";
            case "googology_portal_frame" -> "googology:guogao_portal_frame";
            case "omega_stone" -> "googology:omega_material";
            case "omega_ore" -> "googology:omega_ore";
            case "omega_block" -> "googology:omega_block";
            case "omega_pickaxe" -> "googology:omega_pickaxe";
            case "omega_sword" -> "googology:omega_sword";
            case "omega_helmet" -> "googology:omega_helmet";
            case "omega_chestplate" -> "googology:omega_chestplate";
            case "omega_leggings" -> "googology:omega_leggings";
            case "omega_boots" -> "googology:omega_boots";
            case "hell_omega_ore" -> "googology:nether_omega_ore";
            case "epsilon_stone" -> "googology:epsilon_material";
            case "epsilon_ore" -> "googology:epsilon_ore";
            case "epsilon_block" -> "googology:epsilon_block";
            case "epsilon_pickaxe" -> "googology:epsilon_pickaxe";
            case "epsilon_sword" -> "googology:epsilon_sword";
            case "epsilon_helmet" -> "googology:epsilon_helmet";
            case "epsilon_chestplate" -> "googology:epsilon_chestplate";
            case "epsilon_leggings" -> "googology:epsilon_leggings";
            case "epsilon_boots" -> "googology:epsilon_boots";
            case "hell_epsilon_ore" -> "googology:nether_epsilon_ore";
            case "gamma_stone" -> "googology:gamma_material";
            case "gamma_ore" -> "googology:gamma_ore";
            case "gamma_block" -> "googology:gamma_block";
            case "gamma_pickaxe" -> "googology:gamma_pickaxe";
            case "gamma_sword" -> "googology:gamma_sword";
            case "gamma_helmet" -> "googology:gamma_helmet";
            case "gamma_chestplate" -> "googology:gamma_chestplate";
            case "gamma_leggings" -> "googology:gamma_leggings";
            case "gamma_boots" -> "googology:gamma_boots";
            case "hell_gamma_ore" -> "googology:nether_gamma_ore";
            case "true_omega_stone" -> "googology:true_omega_material";
            case "true_omega_ore" -> "googology:true_omega_ore";
            case "true_omega_block" -> "googology:true_omega_block";
            case "true_omega_pickaxe" -> "googology:true_omega_pickaxe";
            case "true_omega_sword" -> "googology:true_omega_sword";
            case "true_omega_helmet" -> "googology:true_omega_helmet";
            case "true_omega_chestplate" -> "googology:true_omega_chestplate";
            case "true_omega_leggings" -> "googology:true_omega_leggings";
            case "true_omega_boots" -> "googology:true_omega_boots";
            case "hell_true_omega_ore" -> "googology:nether_true_omega_ore";
        default -> null;};}
    public static boolean shared(String name){return alias(name)!=null;}
    public static Identifier id(String name){String shared=alias(name);return shared!=null?Identifier.parse(shared):Identifier.fromNamespaceAndPath("googology",name.equals("googology")?"outer":name);}
    public static void initialize(){
        dev.googology.outer.registry.ModBlocks.initialize();
        dev.googology.outer.registry.ModItems.initialize();
        dev.googology.outer.registry.ModEntities.initialize();
        dev.googology.outer.registry.ModBoats.initialize();
        dev.googology.outer.registry.ModSpawnPlacements.initialize();
        dev.googology.outer.registry.ModSpawnEggs.initialize();
        dev.googology.outer.registry.ModChunkGenerators.initialize();
        dev.googology.outer.registry.ModFeatureTypes.initialize();
    }
}
