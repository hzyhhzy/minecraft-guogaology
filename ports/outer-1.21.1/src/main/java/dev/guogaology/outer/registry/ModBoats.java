package dev.guogaology.outer.registry;

import dev.guogaology.outer.GuogaologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.function.Supplier;

/** Shared wood hulls as the newer Boat(type, level, dropSupplier) constructor. */
public final class ModBoats {
    private static final Map<String,EntityType<Boat>> BOATS=new LinkedHashMap<>();
    private static final Map<String,EntityType<Boat>> CHEST_BOATS=new LinkedHashMap<>();
    public static void initialize(){
        if(!BOATS.isEmpty())return;
        for(String wood:ModBlocks.WOOD_IDS){
            var id=GuogaologyMod.id(wood+"_boat");
            EntityType.Builder<Boat> hull=EntityType.Builder.<Boat>of((t,w)->new WoodenBoat(t,w,()->ModItems.get(wood+"_boat"),()->ModBlocks.wood(wood,"planks")),MobCategory.MISC)
                .sized(1.375F,.5625F).clientTrackingRange(10);
            if(wood.equals("dread"))hull.fireImmune();
            EntityType<Boat> type=Registry.register(BuiltInRegistries.ENTITY_TYPE,id,hull.build(id.toString()));
            BOATS.put(wood,type);
            ModItems.registerTracked(wood+"_boat",p->new dev.guogaology.outer.item.OuterBoatItem(type,p),ModBlocks.woodItemProperties(wood+"_boat").stacksTo(1));
            var chestId=GuogaologyMod.id(wood+"_chest_boat");
            EntityType.Builder<Boat> chestHull=EntityType.Builder.<Boat>of((t,w)->new WoodenChestBoat(t,w,()->ModItems.get(wood+"_chest_boat"),()->ModBlocks.wood(wood,"planks")),MobCategory.MISC)
                .sized(1.375F,.5625F).clientTrackingRange(10);
            if(wood.equals("dread"))chestHull.fireImmune();
            EntityType<Boat> chestType=Registry.register(BuiltInRegistries.ENTITY_TYPE,chestId,chestHull.build(chestId.toString()));
            CHEST_BOATS.put(wood,chestType);
            ModItems.registerTracked(wood+"_chest_boat",p->new dev.guogaology.outer.item.OuterBoatItem(chestType,p),ModBlocks.woodItemProperties(wood+"_chest_boat").stacksTo(1));
        }
    }
    public static EntityType<Boat> boat(String wood){return Objects.requireNonNull(BOATS.get(wood));}
    public static EntityType<Boat> chestBoat(String wood){return Objects.requireNonNull(CHEST_BOATS.get(wood),wood);}
    public static Map<String,EntityType<Boat>> all(){return Collections.unmodifiableMap(BOATS);}
    public static final class WoodenBoat extends Boat {
        private final Supplier<Item> drop;
        private final Supplier<ItemLike> planks;
        private boolean fallingDrops;
        WoodenBoat(EntityType<? extends Boat> type,Level world,Supplier<Item> drop,Supplier<ItemLike> planks){super(type,world);this.drop=drop;this.planks=planks;}
        @Override public Item getDropItem(){return drop.get();}
        @Override public ItemStack getPickResult(){return new ItemStack(drop.get());}
        @Override protected void checkFallDamage(double height,boolean onGround,BlockState state,BlockPos pos){
            // Preserve vanilla's fall/status/drop-rule checks without changing saved Boat.Type.
            boolean previous=fallingDrops;fallingDrops=true;
            try{super.checkFallDamage(height,onGround,state,pos);}finally{fallingDrops=previous;}
        }
        @Override public ItemEntity spawnAtLocation(ItemLike item){
            return super.spawnAtLocation(fallingDrops&&item==getVariant().getPlanks()?planks.get():item);
        }
    }
    /** Native chest inventory, menu, entity saving and removal drops. */
    public static final class WoodenChestBoat extends ChestBoat {
        private final Supplier<Item> drop;
        private final Supplier<ItemLike> planks;
        private boolean fallingDrops;
        WoodenChestBoat(EntityType<? extends Boat> type,Level world,Supplier<Item> drop,Supplier<ItemLike> planks){super(type,world);this.drop=drop;this.planks=planks;}
        @Override public Item getDropItem(){return drop.get();}
        @Override public ItemStack getPickResult(){return new ItemStack(drop.get());}
        @Override protected void checkFallDamage(double height,boolean onGround,BlockState state,BlockPos pos){
            boolean previous=fallingDrops;fallingDrops=true;
            try{super.checkFallDamage(height,onGround,state,pos);}finally{fallingDrops=previous;}
        }
        @Override public ItemEntity spawnAtLocation(ItemLike item){
            return super.spawnAtLocation(fallingDrops&&item==getVariant().getPlanks()?planks.get():item);
        }
    }

}
