package dev.guogaology.networkqa263;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Optional, read-only JEI API checks. No JEI compile/runtime dependency without JEI installed. */
final class JeiProbe {
    private static final String API="mezz.jei.api.";
    static boolean enabled() {return FabricLoader.getInstance().isModLoaded("jei");}
    private static Object runtime() throws Exception {
        return ((Optional<?>)Class.forName("mezz.jei.common.Internal").getMethod("getOptionalJeiRuntime").invoke(null)).orElse(null);
    }
    static boolean ready() throws Exception {return !enabled()||runtime()!=null;}
    private static Object call(String type,Object target,String name,Object... args) throws Exception {
        for(var method:Class.forName(type).getMethods()) {
            if(!method.getName().equals(name)||method.getParameterCount()!=args.length||!Modifier.isPublic(method.getModifiers()))continue;
            boolean matches=true;var types=method.getParameterTypes();
            for(int i=0;i<args.length;i++)if(args[i]!=null&&!types[i].isInstance(args[i])){matches=false;break;}
            if(matches)return method.invoke(target,args);
        }
        throw new NoSuchMethodException(type+"."+name+"/"+args.length);
    }
    private static Object role(String role) throws Exception {return Class.forName(API+"recipe.RecipeIngredientRole").getField(role).get(null);}
    private static TreeSet<String> ingredientIds(Object supplier,String role) throws Exception {
        var out=new TreeSet<String>();
        for(Object ingredient:(List<?>)call(API+"ingredients.IIngredientSupplier",supplier,"getIngredients",role(role))) {
            var stack=(Optional<?>)call(API+"ingredients.ITypedIngredient",ingredient,"getItemStack");
            if(stack.orElse(null) instanceof ItemStack item&&!item.isEmpty())out.add(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
        }
        return out;
    }
    private static JsonArray array(Iterable<String> values) {var out=new JsonArray();values.forEach(out::add);return out;}
    private static Map<String,Object> recipes(Object manager,Object type) throws Exception {
        var lookup=call(API+"recipe.IRecipeManager",manager,"createRecipeLookup",type);
        var found=new LinkedHashMap<String,Object>();
        try(var entries=(Stream<?>)call(API+"recipe.IRecipeLookup",lookup,"get")) {
            entries.forEach(entry->{
                NetworkProbe.check(entry instanceof RecipeHolder<?>,"JEI native recipe lookup returns actual RecipeHolder");
                found.put(((RecipeHolder<?>)entry).id().identifier().toString(),entry);
            });
        }
        return found;
    }
    private static JsonObject inspectRecipe(Object manager,Object category,Map<String,Object> recipes,String id,
            String output,List<String> requiredInputs) throws Exception {
        Object recipe=recipes.get(id);NetworkProbe.check(recipe!=null,"JEI can query "+id);
        Object supplier=call(API+"recipe.IRecipeManager",manager,"getRecipeIngredients",category,recipe);
        var inputs=ingredientIds(supplier,"INPUT");var outputs=ingredientIds(supplier,"OUTPUT");
        NetworkProbe.check(!inputs.isEmpty(),"JEI recipe inputs not empty "+id);
        NetworkProbe.check(outputs.contains(output),"JEI correct recipe output "+id+" -> "+output+" actual="+outputs);
        for(String required:requiredInputs)NetworkProbe.check(inputs.contains(required),"JEI tag input includes "+required+" for "+id);
        var out=new JsonObject();out.addProperty("id",id);out.add("inputs",array(inputs));out.add("outputs",array(outputs));return out;
    }
    static JsonObject inspectAndShowCrafting() throws Exception {
        var out=new JsonObject();out.addProperty("installed",enabled());
        if(!enabled()){out.addProperty("checked",false);return out;}
        Object runtime=runtime();NetworkProbe.check(runtime!=null,"JEI runtime initialized");
        Object manager=call(API+"runtime.IJeiRuntime",runtime,"getRecipeManager");
        Class<?> types=Class.forName(API+"constants.RecipeTypes");
        Object crafting=types.getField("CRAFTING").get(null),smelting=types.getField("SMELTING").get(null);
        var craft=recipes(manager,crafting);var smelt=recipes(manager,smelting);
        Object craftCategory=call(API+"recipe.IRecipeManager",manager,"getRecipeCategory",crafting);
        Object smeltCategory=call(API+"recipe.IRecipeManager",manager,"getRecipeCategory",smelting);
        var checks=new JsonArray();
        checks.add(inspectRecipe(manager,craftCategory,craft,"minecraft:stick","minecraft:stick",List.of("minecraft:oak_planks","minecraft:birch_planks")));
        checks.add(inspectRecipe(manager,craftCategory,craft,"minecraft:stone_pickaxe","minecraft:stone_pickaxe",List.of("minecraft:stick","minecraft:cobblestone")));
        checks.add(inspectRecipe(manager,smeltCategory,smelt,"minecraft:charcoal","minecraft:charcoal",List.of("minecraft:oak_log","minecraft:birch_log")));
        var custom=new JsonObject();
        for(String id:NetworkProbe.RECIPES)custom.addProperty(id,craft.containsKey("guogaology:"+id));
        if(NetworkProbe.branchPickup())custom.addProperty("hydra_bud",craft.containsKey("guogaology:hydra_bud"));
        out.add("customRecipesAvailable",custom);
        boolean requireCustom=Boolean.getBoolean("guogaology.qa.network263.requireCustom");
        boolean serverJei=Boolean.getBoolean("guogaology.qa.network263.serverJei");
        if(requireCustom||serverJei) {
            var numbers=new ArrayList<String>();for(int digit=0;digit<10;digit++)numbers.add("guogaology:ordinal_stone"+(digit==0?"":"_"+digit));
            if(requireCustom) {
                for(String id:NetworkProbe.RECIPES)checks.add(inspectRecipe(manager,craftCategory,craft,"guogaology:"+id,
                        id.equals("cobblestone_from_number_stones")?"minecraft:cobblestone":"guogaology:"+id,numbers));
            } else {
                // The released0.6.0 server never indexed the NumericRecipe
                // serializer. Only assert its existing supported tag recipe.
                checks.add(inspectRecipe(manager,craftCategory,craft,"guogaology:cobblestone_from_number_stones","minecraft:cobblestone",numbers));
            }
        }
        if(NetworkProbe.branchPickup()&&requireCustom) {
            var required=new ArrayList<String>();NetworkProbe.hydraExpected().getAsJsonArray("requiredInputs").forEach(value->required.add(value.getAsString()));
            checks.add(inspectRecipe(manager,craftCategory,craft,"guogaology:hydra_bud","guogaology:hydra_bud",required));
        }
        Object gui=call(API+"runtime.IJeiRuntime",runtime,"getRecipesGui");
        String shown=NetworkProbe.branchPickup()&&requireCustom?"guogaology:hydra_bud":requireCustom?"guogaology:number_pickaxe":serverJei?"guogaology:cobblestone_from_number_stones":"minecraft:stick";
        call(API+"runtime.IRecipesGui",gui,"showRecipes",craftCategory,List.of(craft.get(shown)),List.of());
        out.addProperty("checked",true);out.addProperty("craftingCount",craft.size());out.addProperty("smeltingCount",smelt.size());
        out.addProperty("shownRecipe",shown);out.add("recipes",checks);
        out.addProperty("serverJei",Boolean.getBoolean("guogaology.qa.network263.serverJei"));
        out.addProperty("fullCustomRecipesRequired",requireCustom);
        return out;
    }
}
