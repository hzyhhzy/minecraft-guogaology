package dev.guogaology.block;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

/** Names are derived from the portable appearance, never from the surrounding world. */
public final class VariantDecorItem extends BlockItem {
    public VariantDecorItem(Block block,Settings settings){super(block,settings);}
    private String property(ItemStack stack,String key,String fallback){
        return stack.getOrDefault(DataComponentTypes.BLOCK_STATE,BlockStateComponent.DEFAULT).properties().getOrDefault(key,fallback);
    }
    private int variant(ItemStack stack,String key,int fallback,int max){
        try{int value=Integer.parseInt(property(stack,key,Integer.toString(fallback)));return value>=0&&value<=max?value:fallback;}
        catch(NumberFormatException e){return fallback;}
    }
    private Text emotion(ItemStack stack){return Text.translatable("item.guogaology.lantern_emotion."+variant(stack,"emotion",0,1));}
    @Override public Text getName(ItemStack stack){
        if(getBlock() instanceof GuogaologyPortalFrameBlock)
            return Text.translatable("block.guogaology.guogao_portal_frame.style."+property(stack,"style","0"));
        Text base=super.getName(stack);
        if(getBlock() instanceof EmojiLanternBlock)return Text.translatable("item.guogaology.expression_variant",base,emotion(stack));
        if(getBlock() instanceof MosaicLightBlock||getBlock() instanceof AstraWeaveBlock)
            return Text.translatable("item.guogaology.color_variant",base,variant(stack,"color",0,getBlock() instanceof MosaicLightBlock?15:63)+1);
        if(getBlock() instanceof TuringTapeBlock)return Text.translatable("item.guogaology.expression_variant",base,
                Text.translatable("item.guogaology.tape_ink."+(property(stack,"ink","false").equals("true")?1:0)));
        if(getBlock() instanceof OrdinalBrickBlock)return Text.translatable("item.guogaology.number_variant",base,property(stack,"number","0"));
        if(getBlock() instanceof ChristmasDigitBlock){
            String digit=property(stack,"digit","0");
            return Text.translatable("item.guogaology.number_variant",base,digit);
        }
        return base;
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> out,TooltipType type){
        super.appendTooltip(stack,context,out,type);
        if(getBlock() instanceof MosaicLightBlock||getBlock() instanceof AstraWeaveBlock){
            int count=getBlock() instanceof MosaicLightBlock?16:64;
            out.add(Text.translatable("item.guogaology.palette_color",variant(stack,"color",0,count-1)+1,count));
        }
        if(getBlock() instanceof LtyChimeBlock)out.add(Text.translatable("item.guogaology.chime_note",variant(stack,"note",0,7)+1));
        if(!(getBlock() instanceof EmojiLanternBlock))return;
        out.add(Text.translatable("item.guogaology.lantern_expression",emotion(stack)));
        int part=variant(stack,"part",27,27);
        if(part>=0&&part<27)out.add(Text.translatable("item.guogaology.lantern_tile",part%3+1,part/3%3+1,part/9+1));
        else out.add(Text.translatable("item.guogaology.lantern_whole"));
    }
}
