package dev.guogaology.client.recipebook;

import java.util.Set;

/** Client presentation only; recipe categories and server recipe IDs remain vanilla. */
public final class RecipeBookSections {
    public enum Section {
        CORES("gui.guogaology.recipebook.cores"), GGG("gui.guogaology.recipebook.ggg");
        private final String translationKey;
        Section(String translationKey){this.translationKey=translationKey;}
        public String translationKey(){return translationKey;}
    }
    private static final Set<String> CORES=Set.of("sequence_core","power_tower_core","hydra_bud","lho_trace","laver_core","astra_critical_core","guogao_heart","boundary_core","ordinal_crystal");
    private static final Set<String> WOOD_PRODUCTS=Set.of("log","wood","stem","hyphae","stripped_log","stripped_wood","planks","stairs","slab","fence","fence_gate","door","trapdoor","button","pressure_plate","sign","wall_sign","hanging_sign","wall_hanging_sign","boat","chest_boat","block","mosaic","mosaic_stairs","mosaic_slab","raft","chest_raft");
    // Union of the actual vanilla wood families in the four supported cached resource packs.
    private static final Set<String> VANILLA_WOODS=Set.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","pale_oak","poplar","bamboo","crimson","warped");
    private RecipeBookSections(){}
    public static boolean isModItem(String id){return id!=null&&id.startsWith("guogaology:");}
    public static boolean isCore(String id){
        if(!isModItem(id))return false;
        String path=id.substring("guogaology:".length());
        for(String core:CORES)if(path.equals(core)||path.equals(core+"_lv2")||path.equals(core+"_lv3")||(core.equals("ordinal_crystal")&&path.equals(core+"_lv4")))return true;
        return false;
    }
    public static boolean isCommonWood(String id){
        if(id==null)return false;
        if(id.startsWith("minecraft:")){
            String path=id.substring("minecraft:".length());if(path.startsWith("stripped_"))path=path.substring("stripped_".length());
            for(String family:VANILLA_WOODS)if(path.startsWith(family+"_")&&WOOD_PRODUCTS.contains(path.substring(family.length()+1)))return true;
        }else if(isModItem(id)){
            String path=id.substring("guogaology:".length());
            for(String family: new String[]{"laver_","dread_"})if(path.startsWith(family))return WOOD_PRODUCTS.contains(path.substring(family.length()));
        }
        return false;
    }
    /** Modern displays omit the original recipe ID; mod ingredients also identify conversions to vanilla items. */
    public static boolean includes(Section section,String recipeId,String resultId,boolean modIngredient){
        if(section==Section.CORES)return isCore(resultId);
        return !isCore(resultId)&&!isCommonWood(resultId)&&(isModItem(recipeId)||isModItem(resultId)||modIngredient);
    }
    /** Preserve 27px native button hit boxes, including at the minimum 240px GUI height. */
    public static int tabStripTop(int screenHeight,int visibleTabs){
        return Math.max(0,Math.min((screenHeight-166)/2+3,screenHeight-4-visibleTabs*27));
    }
}
