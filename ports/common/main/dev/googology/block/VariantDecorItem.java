package dev.googology.block;

import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** Names are derived from the portable appearance, never from the surrounding world. */
public final class VariantDecorItem extends BlockItem {
    public VariantDecorItem(Block block,Properties settings){super(block,settings);}
    private String property(ItemStack stack,String key,String fallback){
        return stack.getOrDefault(DataComponents.BLOCK_STATE,BlockItemStateProperties.EMPTY).properties().getOrDefault(key,fallback);
    }
    @Override public Component getName(ItemStack stack){
        if(getBlock() instanceof GoogologyPortalFrameBlock)
            return Component.translatable("block.googology.guogao_portal_frame.style."+property(stack,"style","0"));
        Component base=super.getName(stack);
        if(getBlock() instanceof OrdinalBrickBlock)return Component.translatable("item.googology.number_variant",base,property(stack,"number","0"));
        if(getBlock() instanceof ChristmasDigitBlock){
            String digit=property(stack,"digit","33");
            return digit.equals("33")?Component.translatable("item.googology.blank_variant",base):Component.translatable("item.googology.number_variant",base,digit);
        }
        return base;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        super.appendHoverText(stack,context,display,out,flag);
        if(!(getBlock() instanceof EmojiLanternBlock))return;
        int part;
        try{part=Integer.parseInt(property(stack,"part","27"));}catch(NumberFormatException e){part=27;}
        if(part>=0&&part<27)out.accept(Component.translatable("item.googology.lantern_tile",part%3+1,part/3%3+1,part/9+1));
        else out.accept(Component.translatable("item.googology.lantern_whole"));
    }
}
