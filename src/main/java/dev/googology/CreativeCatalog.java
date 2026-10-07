package dev.googology;

import dev.googology.block.*;
import dev.googology.mining.*;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.item.*;
import net.minecraft.block.Block;
import java.util.*;

/** One purposeful home for each item, with every existing number/color variant retained. */
public final class CreativeCatalog {
    public static final List<String> TABS=List.of("googology","architecture","numbers","lighting","cores","mining","materials");
    private static final Set<String> RARE=Set.of("fffz_trace","fos_trace","lho_hydra_psi","lho_hydra_z","tree_node_red","tree_node_green","tree_node_blue");
    private static final Set<String> BUILDING=Set.of("ordinal_bricks","power_bricks","basic_laver_pattern","iblp_blank","iblp_node","iblp_marked","laver_court_node","laver_court_blank","laver_inlay","lty_yarn","tianyi_fiber","white_fiber","sun_pattern_tiles","amber_inlay","turing_tape","laver_planks");
    private static final Set<String> IMPORTED=Set.of("busy_beaver_spawn_egg","chair","christmas_leaves","deepseek_whale_spawn_egg","dread_boat","dread_button","dread_door","dread_fence","dread_fence_gate","dread_planks","dread_pressure_plate","dread_sapling","dread_sign","dread_slab","dread_stairs","dread_stripped_log","dread_stripped_wood","dread_trapdoor","dread_wall_sign","dread_wood","evil_pig_spawn_egg","fly_y_spawn_egg","fruit_cake_gel","fruit_cake_slime_spawn_egg","fruit_slime_spawn_egg","graham","graham_flower","graham_vine","gummy","hell_loquat_leaves","hell_loquat_sapling","laver_boat","laver_button","laver_door","laver_fence","laver_fence_gate","laver_pressure_plate","laver_sapling","laver_sign","laver_slab","laver_stairs","laver_stripped_log","laver_stripped_wood","laver_trapdoor","laver_wall_sign","laver_wood","loquat","loquat_leaves","loquat_sapling","nuke_chair","nuke_mushroom","nuke_mushroom_cap","nuke_mushroom_stem","snake_head","snake_spawn_egg","stellar_stone","stellar_stone_block","white_rice");
    private CreativeCatalog(){}
    private static String category(Block block){
        String id=Registries.BLOCK.getId(block).getPath();
        if(block instanceof NumberStoneBlock||block instanceof OrdinalBrickBlock)return "numbers";
        if(CoreGrades.ROOTS.contains(id)||block instanceof PortableRelicBlock||RARE.contains(id))return "cores";
        if(block instanceof ChristmasLightBlock||block instanceof EmojiLanternBlock||block instanceof FruitJellyBlock||block==GoogologyBlocks.MOSAIC_LIGHT||block==GoogologyBlocks.STAR_GOLD)return "lighting";
        if(block instanceof ReturnFrameBlock||BUILDING.contains(id)||id.startsWith("astra_")||id.startsWith("office_")||id.equals("server_rack")||block==GoogologyBlocks.PORTAL_FRAME)return "architecture";
        return "googology";
    }
    public static List<ItemStack> entries(String tab){
        var out=new ArrayList<ItemStack>();
        for(Block block:GoogologyBlocks.ALL){
            if(block==GoogologyBlocks.INNER_PORTAL||block==GoogologyBlocks.PORTAL||block==GoogologyBlocks.FRUIT_PORTAL||CoreGrades.upper(block)||block instanceof ChristmasDigitBlock)continue;
            if(category(block).equals(tab)){out.add(new ItemStack(block));CoreGrades.upgrades(block).forEach(b->out.add(new ItemStack(b)));}
        }
        if(tab.equals("numbers")){
            for(int value=1;value<=15;value++)out.add(StatefulDecorBlock.copyAppearance(new ItemStack(GoogologyBlocks.ORDINAL_BRICKS),GoogologyBlocks.ordinalBrick(value)));
            for(var lamp:GoogologyBlocks.SEQUENCE_LIGHTS)for(int value=0;value<=32;value++)out.add(StatefulDecorBlock.copyAppearance(new ItemStack(lamp),lamp.getDefaultState().with(ChristmasDigitBlock.DIGIT,value)));
        }
        if(tab.equals("lighting")){
            for(int color=1;color<16;color++)out.add(StatefulDecorBlock.copyAppearance(new ItemStack(GoogologyBlocks.MOSAIC_LIGHT),GoogologyBlocks.MOSAIC_LIGHT.getDefaultState().with(MosaicLightBlock.COLOR,color)));
        }
        if(tab.equals("architecture")){
            for(int color=1;color<64;color++)out.add(StatefulDecorBlock.copyAppearance(new ItemStack(GoogologyBlocks.ASTRA_WEAVE),GoogologyBlocks.ASTRA_WEAVE.getDefaultState().with(AstraWeaveBlock.COLOR,color)));
            for(int style=1;style<=2;style++)out.add(StatefulDecorBlock.copyAppearance(new ItemStack(GoogologyBlocks.PORTAL_FRAME),GoogologyBlocks.PORTAL_FRAME.getDefaultState().with(GoogologyPortalFrameBlock.STYLE,style)));
        }
        if(tab.equals("mining")){
            for(var e:MiningContent.GEAR.entrySet()){
                if(e.getValue().tier()==0){for(int n=0;n<=9;n++){var gear=new ItemStack(e.getKey());gear.set(MiningContent.DIGIT,(double)n);GearData.refresh(gear);out.add(gear);}}
                else out.add(new ItemStack(e.getKey()));
            }
            for(var table:MiningContent.TABLES)out.add(new ItemStack(table));
        }
        if(tab.equals("materials")){
            for(var item:MiningContent.ITEMS)if(!MiningContent.GEAR.containsKey(item)&&Arrays.stream(MiningContent.TABLES).noneMatch(b->b.asItem()==item))out.add(new ItemStack(item));
            out.add(new ItemStack(GoogologyBlocks.RETURN_TOKEN));out.add(new ItemStack(GoogologyBlocks.ORDINAL_SHARD));out.add(new ItemStack(GoogologyBlocks.GUOGAO_SLICE));
            WorldMaterials.ITEMS.values().forEach(i->out.add(new ItemStack(i)));
        }
        for(var item:Registries.ITEM){
            var id=Registries.ITEM.getId(item);if(!id.getNamespace().equals("googology")||!IMPORTED.contains(id.getPath()))continue;
            String name=id.getPath();
            String section=item instanceof BlockItem?(name.endsWith("_stairs")||name.endsWith("_slab")||name.endsWith("_door")||name.endsWith("_trapdoor")||name.contains("sign")||name.contains("fence")||name.contains("button")||name.contains("plate")||name.contains("chair")?"architecture":"googology"):"materials";
            if(section.equals(tab))out.add(new ItemStack(item));
        }
        return List.copyOf(out);
    }
    public static void initialize(){
        for(String tab:TABS){
            // Presentation-only items never enter entries(), search, recipes or loot.
            var icon=Registry.register(Registries.ITEM,GoogologyMod.id("creative_icon_"+tab),new Item(new Item.Settings().maxCount(1)));
            Registry.register(Registries.ITEM_GROUP,GoogologyMod.id(tab),FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.googology."+tab)).icon(()->new ItemStack(icon))
                    .entries((context,out)->entries(tab).forEach(out::add)).build());
        }
    }
}
