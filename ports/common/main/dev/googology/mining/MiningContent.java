package dev.googology.mining;

import com.mojang.serialization.Codec;
import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.equipment.*;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import java.util.*;

public final class MiningContent {
    public static final net.minecraft.world.inventory.MenuType<EnhancementMenu> ENHANCEMENT_MENU=Registry.register(BuiltInRegistries.MENU,GoogologyMod.id("enhancement"),new net.minecraft.world.inventory.MenuType<>(EnhancementMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final net.minecraft.world.inventory.MenuType<EnhancementMenu> MANUSCRIPT_MENU=Registry.register(BuiltInRegistries.MENU,GoogologyMod.id("manuscript"),new net.minecraft.world.inventory.MenuType<>(ManuscriptMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final DataComponentType<List<ItemStack>> CORES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,GoogologyMod.id("installed_cores"),DataComponentType.<List<ItemStack>>builder().persistent(ItemStack.OPTIONAL_CODEC.listOf(0,EquipmentRules.MAX_SOCKETS)).build());
    public static final DataComponentType<Double> DIGIT=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,GoogologyMod.id("tool_digit"),DataComponentType.<Double>builder().persistent(Codec.doubleRange(0,9)).build());
    public static final DataComponentType<Integer> RULES=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,GoogologyMod.id("equipment_rules"),DataComponentType.<Integer>builder().persistent(Codec.INT).build());
    public static final DataComponentType<Boolean> DEEP=Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,GoogologyMod.id("deep_profile"),DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());
    public static final Map<Item,GearSpec> GEAR=new LinkedHashMap<>();
    public static final List<Item> ITEMS=new ArrayList<>();
    public static final Item[] MATERIALS=new Item[4];
    public static final Block[][] ORES=new Block[2][4];
    public static final Block[] STORAGE=new Block[4],TABLES=new Block[3];
    public static final Item[][] TOOLS=new Item[5][6];
    public static final Item[] BOWS=new Item[4];
    public record GearSpec(int tier,int kind){}
    public static Item.Properties itemSettings(String id){return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,GoogologyMod.id(id)));}
    public static BlockBehaviour.Properties blockSettings(String id){return BlockBehaviour.Properties.of().strength(3,6).setId(ResourceKey.create(Registries.BLOCK,GoogologyMod.id(id)));}
    private static Item item(String id,Item item){Registry.register(BuiltInRegistries.ITEM,GoogologyMod.id(id),item);ITEMS.add(item);return item;}
    private static Block block(String id,Block block){Registry.register(BuiltInRegistries.BLOCK,GoogologyMod.id(id),block);item(id,new BlockItem(block,itemSettings(id).useBlockDescriptionPrefix()));return block;}
    public static void initialize(){
        for(int t=0;t<4;t++){
            String m=EquipmentRules.MINERALS[t];
            MATERIALS[t]=item(m+"_material",new Item(itemSettings(m+"_material")));
            STORAGE[t]=block(m+"_block",new Block(blockSettings(m+"_block")));
            for(int u=0;u<2;u++){String id=(u==1?"nether_":"")+m+"_ore";ORES[u][t]=block(id,new OrdinalOre(blockSettings(id).strength(3+t*.5f,6).requiresCorrectToolForDrops(),t+1));}
        }
        for(int tier=0;tier<=4;tier++)for(int kind=0;kind<(tier==0?2:6);kind++){
            String material=tier==0?"number":EquipmentRules.MINERALS[tier-1],id=material+"_"+EquipmentRules.KINDS[kind];
            Item.Properties props=itemSettings(id).durability(EquipmentRules.durability(tier,kind)).component(CORES,List.of());
            if(tier==0)props.component(DIGIT,0d);
            if(kind>=2){
                var type=ArmorType.valueOf(EquipmentRules.KINDS[kind].toUpperCase(Locale.ROOT));
                var armor=new ArmorMaterial(30+tier*10,Map.of(),15,SoundEvents.ARMOR_EQUIP_DIAMOND,0,0,TagKey.create(Registries.ITEM,GoogologyMod.id(material+"_repair")),ResourceKey.create(EquipmentAssets.ROOT_ID,GoogologyMod.id(material)));
                props.humanoidArmor(armor,type).durability(EquipmentRules.durability(tier,kind));
            }
            props.enchantable(15);
            if(tier>0)props.repairable(MATERIALS[tier-1]);
            props.attributes(GearData.attributes(tier,kind,List.of(),0));
            Item gear=item(id,new OrdinalGear(props,tier,kind));TOOLS[tier][kind]=gear;GEAR.put(gear,new GearSpec(tier,kind));
        }
        for(int rank=1;rank<=3;rank++){String id="enhancement_table"+(rank==1?"":"_"+rank);TABLES[rank-1]=block(id,new EnhancementTable(blockSettings(id).noOcclusion(),rank));}
        for(int tier=1;tier<=4;tier++){String id=EquipmentRules.MINERALS[tier-1]+"_manuscript";
            Item book=item(id,new DenxiManuscript(itemSettings(id).stacksTo(1).component(CORES,List.of()),tier));GEAR.put(book,new GearSpec(tier,6));
        }
        for(int tier=1;tier<=4;tier++){
            String id=EquipmentRules.MINERALS[tier-1]+"_bow";
            Item bow=item(id,new OrdinalBowItem(itemSettings(id).durability(EquipmentRules.durability(tier,7)).component(CORES,List.of()).repairable(MATERIALS[tier-1]),tier));
            BOWS[tier-1]=bow;GEAR.put(bow,new GearSpec(tier,7));
        }
        NumericRecipe.register();MiningEffects.initialize();ManuscriptEffects.initialize();
    }
}
