package dev.googology.mergeqa;

import dev.googology.*;
import dev.googology.mining.*;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import java.util.*;

/** Loaded recipes, wood fire rules and actual table interactions for 0.3.7. */
final class MaintenanceChecks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ItemStack stack(String id){return id.isEmpty()?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"googology:"+id)));}
    private static void craft(ServerLevel level,String result,int count,String... cells){
        int width=cells.length==1?1:3;var input=CraftingInput.of(width,cells.length/width,Arrays.stream(cells).map(MaintenanceChecks::stack).toList());
        var recipe=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        check(recipe.isPresent(),"loaded recipe matches "+result+" "+Arrays.toString(cells));
        var output=recipe.orElseThrow().value().assemble(input);
        check(output.is(stack(result).getItem())&&output.getCount()==count,"actual crafting output "+result+": "+output);
    }
    static void run(ServerPlayer player){
        var level=player.level().getServer().overworld();
        var shapes=List.of("log","wood","planks","stairs","slab","door","trapdoor","fence","fence_gate","button","pressure_plate","sign","wall_sign");
        for(String family:List.of("christmas","hell_christmas","loquat","hell_loquat"))for(String shape:shapes){
            var id=Identifier.parse("googology:"+family+"_"+shape);
            check(!BuiltInRegistries.BLOCK.containsKey(id)&&!BuiltInRegistries.ITEM.containsKey(id),"retired wood is not registered: "+id);
        }
        for(String family:List.of("christmas","hell_christmas","loquat","hell_loquat"))check(!BuiltInRegistries.ITEM.containsKey(Identifier.parse("googology:"+family+"_boat")),"retired boat removed");
        for(String family:List.of("loquat","hell_loquat"))for(String shape:List.of("leaves","sapling"))check(BuiltInRegistries.BLOCK.containsKey(Identifier.parse("googology:"+family+"_"+shape)),"Loquat foliage preserved: "+family+"_"+shape);
        for(String family:List.of("loquat","hell_loquat")){
            var leaves=BuiltInRegistries.BLOCK.getValue(Identifier.parse("googology:"+family+"_leaves"));
            int fruit=0;var fruitItem=stack("loquat").getItem();
            for(int n=0;n<512;n++)for(var drop:Block.getDrops(leaves.defaultBlockState(),level,new BlockPos(14,240,22),null,player,ItemStack.EMPTY))if(drop.is(fruitItem))fruit+=drop.getCount();
            check(fruit>0,"Loquat foliage still supplies fruit: "+family);
            check(Block.getDrops(leaves.defaultBlockState(),level,new BlockPos(14,240,22),null,player,new ItemStack(Items.SHEARS)).stream().anyMatch(drop->drop.is(leaves.asItem())),"shears recover Loquat foliage: "+family);
        }
        craft(level,"minecraft:oak_planks",4,"minecraft:oak_log");
        craft(level,"minecraft:dark_oak_planks",4,"minecraft:dark_oak_log");
        craft(level,"dread_planks",4,"dread_log");
        craft(level,"laver_planks",4,"laver_vein");
        craft(level,"dread_stairs",4,"dread_planks","","","dread_planks","dread_planks","","dread_planks","dread_planks","dread_planks");
        for(String shape:List.of("log","planks","stairs","slab","door","trapdoor","fence","fence_gate","button","pressure_plate","sign","wall_sign")){
            var id=Identifier.parse("googology:dread_"+shape);check(BuiltInRegistries.BLOCK.containsKey(id),"Dread Fir product registered "+shape);
            var entry=FlammableBlockRegistry.getDefaultInstance().get(BuiltInRegistries.BLOCK.getValue(id));
            check(entry==null||(entry.getIgniteOdds()==0&&entry.getBurnOdds()==0),"Dread Fir cannot burn: "+shape);
        }
        BuiltInRegistries.ITEM.forEach(item->{var id=BuiltInRegistries.ITEM.getKey(item);if(id.getNamespace().equals("googology")&&id.getPath().startsWith("dread_"))check(!level.fuelValues().isFuel(new ItemStack(item)),"Dread Fir is not furnace fuel: "+id);});
        check(dev.googology.outer.registry.ModBoats.boat("dread").fireImmune(),"placed Dread Fir boat is fire immune");
        check(!dev.googology.outer.registry.ModBoats.boat("laver").fireImmune(),"other boats remain flammable");
        for(Block wood:List.of(GoogologyBlocks.LAVER_VEIN,GoogologyBlocks.LAVER_PLANKS,GoogologyBlocks.POWER_BRICKS,Blocks.OAK_LOG)){
            var entry=FlammableBlockRegistry.getDefaultInstance().get(wood);
            check(entry!=null&&entry.getBurnOdds()>0,"other wood burns: "+BuiltInRegistries.BLOCK.getKey(wood));
        }
        craft(level,"enhancement_table_2",1,"ordinal_crystal","ordinal_crystal","ordinal_crystal","ordinal_crystal","enhancement_table","ordinal_crystal","ordinal_crystal","ordinal_crystal","ordinal_crystal");
        craft(level,"enhancement_table_3",1,"ordinal_crystal_lv2","ordinal_crystal_lv2","ordinal_crystal_lv2","ordinal_crystal_lv2","enhancement_table_2","ordinal_crystal_lv2","ordinal_crystal_lv2","ordinal_crystal_lv2","ordinal_crystal_lv2");
        craft(level,"server_rack",1,"minecraft:iron_ingot","compute_chip","minecraft:iron_ingot","compute_chip","minecraft:redstone","compute_chip","minecraft:iron_ingot","compute_chip","minecraft:iron_ingot");
        craft(level,"office_monitor",1,"minecraft:glass","minecraft:glass","minecraft:glass","compute_chip","minecraft:iron_ingot","compute_chip","minecraft:redstone","minecraft:iron_ingot","minecraft:redstone");
        craft(level,"compute_chip",2,"server_rack");craft(level,"compute_chip",2,"office_monitor");
        for(String id:List.of("cobblestone_from_ordinal_stone","cobblestone_from_ordinal_stone_smelting","ordinal_stone_from_cobbled_ordinal_stone","ordinal_stone_from_cobblestone","epsilon_block_unpack","gamma_block_unpack","omega_block_unpack","dread_log_planks","dread_log_charcoal"))
            check(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,Identifier.parse("googology:"+id))).isEmpty(),"retired duplicate recipe removed: "+id);
        var pos=new BlockPos(14,240,22);player.teleportTo(14.5,240,24.5);
        for(int rank=1;rank<=3;rank++){
            var block=MiningContent.TABLES[rank-1];level.setBlock(pos,block.defaultBlockState(),2);
            block.defaultBlockState().useWithoutItem(level,player,new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
            check(player.containerMenu instanceof EnhancementMenu&&((EnhancementMenu)player.containerMenu).rank()==rank,"table opens real rank "+rank+" menu");player.closeContainer();
        }
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        System.out.println("MAINTENANCE_037_OK checks="+checks);
    }
}
