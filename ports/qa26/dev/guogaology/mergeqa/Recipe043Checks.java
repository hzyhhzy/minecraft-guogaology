package dev.guogaology.mergeqa;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** Loaded native recipe matching and assembly; no player/world mutation. */
final class Recipe043Checks {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static ItemStack stack(String id){return id.isEmpty()?ItemStack.EMPTY:new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id.contains(":")?id:"guogaology:"+id)));}
    private static CraftingInput input(String... cells){int width=cells.length==1?1:3;return CraftingInput.of(width,cells.length/width,Arrays.stream(cells).map(Recipe043Checks::stack).toList());}
    private static ItemStack output(ServerLevel level,CraftingInput input){
        var match=level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        return match.isEmpty()?ItemStack.EMPTY:match.orElseThrow().value().assemble(input);
    }
    private static void craft(ServerLevel level,String result,String... cells){
        var out=output(level,input(cells));
        check(out.is(stack(result).getItem())&&out.getCount()==1,"native recipe output "+result+" from "+Arrays.toString(cells)+": "+out);
    }
    private static void rejects(ServerLevel level,String result,String... cells){
        check(!output(level,input(cells)).is(stack(result).getItem()),"wrong ingredients rejected for "+result+": "+Arrays.toString(cells));
    }
    private static String[] basic(String top){return new String[]{"",top,"","minecraft:book","minecraft:crafting_table","minecraft:book","minecraft:cobblestone","minecraft:cobblestone","minecraft:cobblestone"};}
    private static String[] upgrade(String core,String prior){return new String[]{core,core,core,core,prior,core,core,core,core};}
    static void run(ServerPlayer player){
        checks=0;var level=player.level().getServer().overworld();
        for(int digit=0;digit<10;digit++)craft(level,"minecraft:cobblestone","ordinal_stone"+(digit==0?"":"_"+digit));
        for(String other:List.of("minecraft:stone","omega_material","epsilon_material","gamma_material","true_omega_material","ordinal_crystal"))rejects(level,"minecraft:cobblestone",other);
        rejects(level,"minecraft:cobblestone","ordinal_stone","ordinal_stone_1","");
        for(int digit=0;digit<10;digit++)rejects(level,"ordinal_stone"+(digit==0?"":"_"+digit),"minecraft:cobblestone");
        craft(level,"enhancement_table",basic("ordinal_crystal"));
        rejects(level,"enhancement_table",basic("omega_material"));
        rejects(level,"enhancement_table",basic("ordinal_crystal_lv2"));
        rejects(level,"enhancement_table",basic(""));
        craft(level,"enhancement_table_2",upgrade("ordinal_crystal","enhancement_table"));
        craft(level,"enhancement_table_3",upgrade("ordinal_crystal_lv2","enhancement_table_2"));
        rejects(level,"enhancement_table_2",upgrade("ordinal_crystal_lv2","enhancement_table"));
        rejects(level,"enhancement_table_3",upgrade("ordinal_crystal","enhancement_table_2"));
        var shortAdvanced=upgrade("ordinal_crystal","enhancement_table");shortAdvanced[0]="";rejects(level,"enhancement_table_2",shortAdvanced);
        var shortUltimate=upgrade("ordinal_crystal_lv2","enhancement_table_2");shortUltimate[0]="";rejects(level,"enhancement_table_3",shortUltimate);
        System.out.println("RECIPE043_CHECKS_OK checks="+checks+" ten digit stones one-way 1:1 / basic ungraded crystal / unchanged higher tables");
    }
}
