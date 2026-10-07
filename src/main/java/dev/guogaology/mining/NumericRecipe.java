package dev.guogaology.mining;

import dev.guogaology.*;
import dev.guogaology.block.NumberStoneBlock;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.Registries;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import com.mojang.serialization.MapCodec;
import net.minecraft.item.*;
import net.minecraft.recipe.*;
import net.minecraft.recipe.input.CraftingRecipeInput;
import java.util.Optional;

/** A visible shaped recipe; only the output's numeric strength is dynamic. */
public final class NumericRecipe extends ShapedRecipe {
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER=new Serializer();
    private final ShapedRecipe source;
    private NumericRecipe(ShapedRecipe source){
        super(source.getGroup(),source.getCategory(),new RawShapedRecipe(source.getWidth(),source.getHeight(),source.getIngredients(),Optional.empty()),source.getResult(null),source.showNotification());
        this.source=source;
    }
    public static void register(){Registry.register(Registries.RECIPE_SERIALIZER,GuogaologyMod.id("numeric_tool"),SERIALIZER);}
    public static int digit(ItemStack s){return s.getItem() instanceof BlockItem item&&item.getBlock() instanceof NumberStoneBlock stone?stone.digit:-1;}
    @Override public ItemStack craft(CraftingRecipeInput input,RegistryWrapper.WrapperLookup lookup){
        var result=super.craft(input,lookup);double sum=0;int count=0;
        for(int i=0;i<input.getSize();i++){int d=digit(input.getStackInSlot(i));if(d>=0&&d<=9){sum+=d;count++;}}
        if(count!=3)return ItemStack.EMPTY;
        result.set(MiningContent.DIGIT,(double)EquipmentRules.digit(sum/count));GearData.refresh(result);return result;
    }
    @Override public RecipeSerializer<ShapedRecipe> getSerializer(){return SERIALIZER;}
    public static final class Serializer implements RecipeSerializer<ShapedRecipe>{
        public MapCodec<ShapedRecipe> codec(){return ShapedRecipe.Serializer.CODEC.xmap(s->new NumericRecipe(s),s->((NumericRecipe)s).source);}
        public PacketCodec<RegistryByteBuf,ShapedRecipe> packetCodec(){return ShapedRecipe.Serializer.PACKET_CODEC.xmap(s->new NumericRecipe(s),s->((NumericRecipe)s).source);}
    }
}
