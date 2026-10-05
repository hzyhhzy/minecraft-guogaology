package dev.googology.outer;
import net.minecraft.resources.Identifier;
/** Compatibility boundary for the authorized donor module; the host owns shared items. */
public final class GoogologyMod {
    public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("guogaology-outer");
    private static String alias(String name){return switch(name){
            case "bashicu_block" -> "ordinal_bricks";
            case "gummy_block" -> "amber_guogao";
            case "fruit_cake" -> "guogao_slice";
            case "christmas_light" -> "mosaic_light";
            case "laver_log" -> "laver_vein";
            case "laver_planks" -> "laver_planks";
            case "googology_portal" -> "guogao_portal";
            case "googology_portal_frame" -> "guogao_portal_frame";
            case "omega_stone" -> "omega_material";
            case "omega_ore" -> "omega_ore";
            case "omega_block" -> "omega_block";
            case "omega_pickaxe" -> "omega_pickaxe";
            case "omega_sword" -> "omega_sword";
            case "omega_helmet" -> "omega_helmet";
            case "omega_chestplate" -> "omega_chestplate";
            case "omega_leggings" -> "omega_leggings";
            case "omega_boots" -> "omega_boots";
            case "hell_omega_ore" -> "nether_omega_ore";
            case "epsilon_stone" -> "epsilon_material";
            case "epsilon_ore" -> "epsilon_ore";
            case "epsilon_block" -> "epsilon_block";
            case "epsilon_pickaxe" -> "epsilon_pickaxe";
            case "epsilon_sword" -> "epsilon_sword";
            case "epsilon_helmet" -> "epsilon_helmet";
            case "epsilon_chestplate" -> "epsilon_chestplate";
            case "epsilon_leggings" -> "epsilon_leggings";
            case "epsilon_boots" -> "epsilon_boots";
            case "hell_epsilon_ore" -> "nether_epsilon_ore";
            case "gamma_stone" -> "gamma_material";
            case "gamma_ore" -> "gamma_ore";
            case "gamma_block" -> "gamma_block";
            case "gamma_pickaxe" -> "gamma_pickaxe";
            case "gamma_sword" -> "gamma_sword";
            case "gamma_helmet" -> "gamma_helmet";
            case "gamma_chestplate" -> "gamma_chestplate";
            case "gamma_leggings" -> "gamma_leggings";
            case "gamma_boots" -> "gamma_boots";
            case "hell_gamma_ore" -> "nether_gamma_ore";
            case "true_omega_stone" -> "true_omega_material";
            case "true_omega_ore" -> "true_omega_ore";
            case "true_omega_block" -> "true_omega_block";
            case "true_omega_pickaxe" -> "true_omega_pickaxe";
            case "true_omega_sword" -> "true_omega_sword";
            case "true_omega_helmet" -> "true_omega_helmet";
            case "true_omega_chestplate" -> "true_omega_chestplate";
            case "true_omega_leggings" -> "true_omega_leggings";
            case "true_omega_boots" -> "true_omega_boots";
            case "hell_true_omega_ore" -> "nether_true_omega_ore";
        default -> null;};}
    public static boolean shared(String name){return alias(name)!=null;}
    public static Identifier id(String name){String shared=alias(name);return Identifier.fromNamespaceAndPath(shared==null?"googology_outer":"googology",shared==null?name:shared);}
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
