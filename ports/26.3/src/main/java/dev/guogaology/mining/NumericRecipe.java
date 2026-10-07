package dev.guogaology.mining;

import dev.guogaology.*;
import dev.guogaology.block.NumberStoneBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** 26.x recipes retain result templates until registry components have been bound. */
public final class NumericRecipe extends ShapedRecipe {
    private final Recipe.CommonInfo info;
    private final CraftingRecipe.CraftingBookInfo book;
    private final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;
    private static final MapCodec<ShapedRecipe> NUMERIC_CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Recipe.CommonInfo.MAP_CODEC.forGetter(r->((NumericRecipe)r).info),
        CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r->((NumericRecipe)r).book),
        ShapedRecipePattern.MAP_CODEC.forGetter(r->((NumericRecipe)r).pattern),
        ItemStackTemplate.CODEC.fieldOf("result").forGetter(r->((NumericRecipe)r).result)
    ).apply(i,NumericRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf,ShapedRecipe> NUMERIC_STREAM=StreamCodec.composite(
        Recipe.CommonInfo.STREAM_CODEC,r->((NumericRecipe)r).info,
        CraftingRecipe.CraftingBookInfo.STREAM_CODEC,r->((NumericRecipe)r).book,
        ShapedRecipePattern.STREAM_CODEC,r->((NumericRecipe)r).pattern,
        ItemStackTemplate.STREAM_CODEC,r->((NumericRecipe)r).result,NumericRecipe::new);
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER=new RecipeSerializer<>(NUMERIC_CODEC,NUMERIC_STREAM);
    private NumericRecipe(Recipe.CommonInfo info,CraftingRecipe.CraftingBookInfo book,ShapedRecipePattern pattern,ItemStackTemplate result){
        super(info,book,pattern,result);this.info=info;this.book=book;this.pattern=pattern;this.result=result;
    }
    public static void register(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,GuogaologyMod.id("numeric_tool"),SERIALIZER);}
    public static int digit(ItemStack s){return s.getItem() instanceof BlockItem item&&item.getBlock() instanceof NumberStoneBlock stone?stone.digit:-1;}
    @Override public ItemStack assemble(CraftingInput input){
        var result=super.assemble(input);double sum=0;int count=0;
        for(int i=0;i<input.size();i++){int d=digit(input.getItem(i));if(d>=0&&d<=9){sum+=d;count++;}}
        if(count!=3)return ItemStack.EMPTY;
        result.set(MiningContent.DIGIT,(double)EquipmentRules.digit(sum/count));GearData.refresh(result);return result;
    }
    @Override public RecipeSerializer<ShapedRecipe> getSerializer(){return SERIALIZER;}
}
