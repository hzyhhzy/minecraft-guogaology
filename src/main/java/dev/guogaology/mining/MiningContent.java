package dev.guogaology.mining;

import com.mojang.serialization.Codec;
import dev.guogaology.GuogaologyMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.component.*;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.item.*;
import net.minecraft.block.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvents;
import java.util.*;

public final class MiningContent {
    public static final net.minecraft.screen.ScreenHandlerType<EnhancementMenu> ENHANCEMENT_MENU=Registry.register(Registries.SCREEN_HANDLER,GuogaologyMod.id("enhancement"),new net.minecraft.screen.ScreenHandlerType<>(EnhancementMenu::new,net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));
    public static final net.minecraft.screen.ScreenHandlerType<EnhancementMenu> MANUSCRIPT_MENU=Registry.register(Registries.SCREEN_HANDLER,GuogaologyMod.id("manuscript"),new net.minecraft.screen.ScreenHandlerType<>(ManuscriptMenu::new,net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));
    public static final ComponentType<List<ItemStack>> CORES=Registry.register(Registries.DATA_COMPONENT_TYPE,GuogaologyMod.id("installed_cores"),ComponentType.<List<ItemStack>>builder().codec(ItemStack.OPTIONAL_CODEC.listOf(0,EquipmentRules.MAX_SOCKETS)).build());
    public static final ComponentType<Double> DIGIT=Registry.register(Registries.DATA_COMPONENT_TYPE,GuogaologyMod.id("tool_digit"),ComponentType.<Double>builder().codec(Codec.doubleRange(0,9)).build());
    public static final ComponentType<Integer> RULES=Registry.register(Registries.DATA_COMPONENT_TYPE,GuogaologyMod.id("equipment_rules"),ComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final ComponentType<Boolean> DEEP=Registry.register(Registries.DATA_COMPONENT_TYPE,GuogaologyMod.id("deep_profile"),ComponentType.<Boolean>builder().codec(Codec.BOOL).build());
    public static final Map<Item,GearSpec> GEAR=new LinkedHashMap<>();
    public static final List<Item> ITEMS=new ArrayList<>();
    public static final Item[] MATERIALS=new Item[4];
    public static final Block[][] ORES=new Block[2][4];
    public static final Block[] STORAGE=new Block[4],TABLES=new Block[3];
    public static final Item[][] TOOLS=new Item[5][6];
    public static final Item[] BOWS=new Item[4];
    public record GearSpec(int tier,int kind){}
    private static Item item(String id,Item item){Registry.register(Registries.ITEM,GuogaologyMod.id(id),item);ITEMS.add(item);return item;}
    private static Block block(String id,Block block){Registry.register(Registries.BLOCK,GuogaologyMod.id(id),block);item(id,new BlockItem(block,new Item.Settings()));return block;}
    public static void initialize(){
        for(int t=0;t<4;t++){
            String m=EquipmentRules.MINERALS[t];
            MATERIALS[t]=item(m+"_material",new Item(new Item.Settings()));
            STORAGE[t]=block(m+"_block",new Block(AbstractBlock.Settings.create().strength(3,6)));
            for(int u=0;u<2;u++){String id=(u==1?"nether_":"")+m+"_ore";ORES[u][t]=block(id,new OrdinalOre(AbstractBlock.Settings.create().strength(3+t*.5f,6).requiresTool(),t+1));}
        }
        for(int tier=0;tier<=4;tier++){
            String material=tier==0?"number":EquipmentRules.MINERALS[tier-1];
            RegistryEntry<ArmorMaterial> armor=null;
            if(tier>0){final int t=tier;armor=Registry.registerReference(Registries.ARMOR_MATERIAL,GuogaologyMod.id(material),new ArmorMaterial(Arrays.stream(ArmorItem.Type.values()).collect(java.util.stream.Collectors.toMap(v->v,v->EquipmentRules.visualArmor(t,switch(v){case HELMET->2;case CHESTPLATE->3;case LEGGINGS->4;case BOOTS->5;default->0;}))),15,SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,()->Ingredient.ofItems(MATERIALS[t-1]),List.of(new ArmorMaterial.Layer(GuogaologyMod.id(material))),0,0));}
            for(int kind=0;kind<(tier==0?2:6);kind++){
                String id=material+"_"+EquipmentRules.KINDS[kind];
                var props=new Item.Settings().maxDamage(EquipmentRules.durability(tier,kind)).component(CORES,List.of()).attributeModifiers(GearData.attributes(tier,kind,List.of(),0));
                if(tier==0)props.component(DIGIT,0d);
                Item gear=kind<2?new OrdinalGear(props,tier,kind):new OrdinalArmor(armor,ArmorItem.Type.valueOf(EquipmentRules.KINDS[kind].toUpperCase(Locale.ROOT)),props,tier,kind);
                item(id,gear);TOOLS[tier][kind]=gear;GEAR.put(gear,new GearSpec(tier,kind));
            }
        }
        for(int rank=1;rank<=3;rank++){String id="enhancement_table"+(rank==1?"":"_"+rank);TABLES[rank-1]=block(id,new EnhancementTable(AbstractBlock.Settings.create().strength(3,6).nonOpaque(),rank));}
        for(int tier=1;tier<=4;tier++){String id=EquipmentRules.MINERALS[tier-1]+"_manuscript";
            Item book=item(id,new DenxiManuscript(new Item.Settings().maxCount(1).component(CORES,List.of()),tier));GEAR.put(book,new GearSpec(tier,6));
        }
        for(int tier=1;tier<=4;tier++){
            String id=EquipmentRules.MINERALS[tier-1]+"_bow";
            Item bow=item(id,new OrdinalBowItem(new Item.Settings().maxDamage(EquipmentRules.durability(tier,7)).component(CORES,List.of()),tier));
            BOWS[tier-1]=bow;GEAR.put(bow,new GearSpec(tier,7));
        }
        NumericRecipe.register();MiningEffects.initialize();ManuscriptEffects.initialize();OpenManuscriptPayload.initialize();
    }
}
