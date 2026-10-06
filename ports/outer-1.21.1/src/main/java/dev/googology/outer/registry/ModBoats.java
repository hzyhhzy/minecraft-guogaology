package dev.googology.outer.registry;

import dev.googology.outer.GoogologyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.function.Supplier;

/** Shared wood hulls as the newer Boat(type, level, dropSupplier) constructor. */
public final class ModBoats {
    private static final Map<String,EntityType<Boat>> BOATS=new LinkedHashMap<>();
    public static void initialize(){
        if(!BOATS.isEmpty())return;
        for(String wood:ModBlocks.WOOD_IDS){
            var id=GoogologyMod.id(wood+"_boat");
            EntityType.Builder<Boat> hull=EntityType.Builder.<Boat>of((t,w)->new WoodenBoat(t,w,()->ModItems.get(wood+"_boat")),MobCategory.MISC)
                .sized(1.375F,.5625F).clientTrackingRange(10);
            if(wood.equals("dread"))hull.fireImmune();
            EntityType<Boat> type=Registry.register(BuiltInRegistries.ENTITY_TYPE,id,hull.build(id.toString()));
            BOATS.put(wood,type);
            ModItems.registerTracked(wood+"_boat",p->new dev.googology.outer.item.OuterBoatItem(type,p),ModBlocks.woodItemProperties(wood+"_boat").stacksTo(1));
        }
    }
    public static EntityType<Boat> boat(String wood){return Objects.requireNonNull(BOATS.get(wood));}
    public static Map<String,EntityType<Boat>> all(){return Collections.unmodifiableMap(BOATS);}
    public static final class WoodenBoat extends Boat {
        private final Supplier<Item> drop;
        WoodenBoat(EntityType<? extends Boat> type,Level world,Supplier<Item> drop){super(type,world);this.drop=drop;}
        @Override public Item getDropItem(){return drop.get();}
        @Override public ItemStack getPickResult(){return new ItemStack(drop.get());}
    }
}
