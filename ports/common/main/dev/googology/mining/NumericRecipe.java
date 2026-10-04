package dev.googology.mining;

import dev.googology.*;
import dev.googology.block.NumberStoneBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import java.util.Optional;

/** A visible shaped recipe; only the output's numeric strength is dynamic. */
public final class NumericRecipe extends ShapedRecipe {
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER=new Serializer();
    private final ShapedRecipe source;
    private NumericRecipe(ShapedRecipe source){
        super(source.group(),source.category(),new ShapedRecipePattern(source.getWidth(),source.getHeight(),source.getIngredients(),Optional.empty()),source.assemble(CraftingInput.EMPTY,null),source.showNotification());
        this.source=source;
    }
    public static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,GoogologyMod.id("numeric_tool"),SERIALIZER);}
    public static int digit(ItemStack s){return s.getItem() instanceof BlockItem item&&item.getBlock() instanceof NumberStoneBlock stone?stone.digit:-1;}
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider lookup){
        var result=super.assemble(input,lookup);double sum=0;int count=0;
        for(int i=0;i<input.size();i++){int d=digit(input.getItem(i));if(d>=0&&d<=9){sum+=d;count++;}}
        if(count!=3)return ItemStack.EMPTY;
        result.set(MiningContent.DIGIT,(double)EquipmentRules.digit(sum/count));GearData.refresh(result);return result;
    }
    @Override public RecipeSerializer<ShapedRecipe> getSerializer(){return SERIALIZER;}
    public static final class Serializer implements RecipeSerializer<ShapedRecipe>{
        public MapCodec<ShapedRecipe> codec(){return ShapedRecipe.Serializer.CODEC.xmap(s->new NumericRecipe(s),s->((NumericRecipe)s).source);}
        public StreamCodec<RegistryFriendlyByteBuf,ShapedRecipe> streamCodec(){return ShapedRecipe.Serializer.STREAM_CODEC.map(s->new NumericRecipe(s),s->((NumericRecipe)s).source);}
    }
}
