package dev.guogaology.outer.registry;
import dev.guogaology.outer.GuogaologyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import java.util.*;
import java.util.function.Function;
/** Only donor-specific food and crafting supplies. Shared gear is registered by the host. */
public final class ModItems {
    public static final Map<String,Item> BY_ID=new LinkedHashMap<>();
    public static final List<Item> ORDERED_ITEMS=new ArrayList<>();
    public static final Item STELLAR_STONE=simple("stellar_stone"),FRUIT_CAKE_GEL=simple("fruit_cake_gel"),NUKE_MUSHROOM=simple("nuke_mushroom");
    public static final Item LOQUAT=registerTracked("loquat",Item::new,new Item.Properties().food(ModFoods.LOQUAT,ModFoods.LOQUAT_CONSUMABLE));
    public static final Item FRUIT_CAKE=dev.guogaology.GuogaologyBlocks.GUOGAO_SLICE;
    public static final Item GUMMY=registerTracked("gummy",Item::new,new Item.Properties().food(ModFoods.GUMMY,ModFoods.GUMMY_CONSUMABLE));
    public static final Item WHITE_RICE=registerTracked("white_rice",Item::new,new Item.Properties().food(ModFoods.WHITE_RICE,ModFoods.WHITE_RICE_CONSUMABLE));
    private static Item simple(String name){return registerTracked(name,Item::new,new Item.Properties());}
    public static Item registerTracked(String name,Function<Item.Properties,Item> factory,Item.Properties settings){
        var key=ResourceKey.create(Registries.ITEM,GuogaologyMod.id(name));var item=Registry.register(BuiltInRegistries.ITEM,key,factory.apply(settings.setId(key)));BY_ID.put(name,item);ORDERED_ITEMS.add(item);return item;
    }
    public static Item get(String name){return Objects.requireNonNull(BY_ID.get(name),name);}
    public static void initialize(){}
}
