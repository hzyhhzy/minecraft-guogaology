package dev.googology.block;

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
    @Override public Text getName(ItemStack stack){
        if(getBlock() instanceof GoogologyPortalFrameBlock)
            return Text.translatable("block.googology.guogao_portal_frame.style."+property(stack,"style","0"));
        Text base=super.getName(stack);
        if(getBlock() instanceof OrdinalBrickBlock)return Text.translatable("item.googology.number_variant",base,property(stack,"number","0"));
        if(getBlock() instanceof ChristmasDigitBlock){
            String digit=property(stack,"digit","33");
            return digit.equals("33")?Text.translatable("item.googology.blank_variant",base):Text.translatable("item.googology.number_variant",base,digit);
        }
        return base;
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> out,TooltipType type){
        super.appendTooltip(stack,context,out,type);
        if(!(getBlock() instanceof EmojiLanternBlock))return;
        int part;
        try{part=Integer.parseInt(property(stack,"part","27"));}catch(NumberFormatException e){part=27;}
        if(part>=0&&part<27)out.add(Text.translatable("item.googology.lantern_tile",part%3+1,part/3%3+1,part/9+1));
        else out.add(Text.translatable("item.googology.lantern_whole"));
    }
}
