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
    private int variant(ItemStack stack,String key,int fallback,int max){
        try{int value=Integer.parseInt(property(stack,key,Integer.toString(fallback)));return value>=0&&value<=max?value:fallback;}
        catch(NumberFormatException e){return fallback;}
    }
    private Component emotion(ItemStack stack){return Component.translatable("item.googology.lantern_emotion."+variant(stack,"emotion",0,1));}
    @Override public Component getName(ItemStack stack){
        if(getBlock() instanceof GoogologyPortalFrameBlock)
            return Component.translatable("block.googology.guogao_portal_frame.style."+property(stack,"style","0"));
        Component base=super.getName(stack);
        if(getBlock() instanceof EmojiLanternBlock)return Component.translatable("item.googology.expression_variant",base,emotion(stack));
        if(getBlock() instanceof MosaicLightBlock||getBlock() instanceof AstraWeaveBlock)
            return Component.translatable("item.googology.color_variant",base,variant(stack,"color",0,getBlock() instanceof MosaicLightBlock?15:63)+1);
        if(getBlock() instanceof TuringTapeBlock)return Component.translatable("item.googology.expression_variant",base,
                Component.translatable("item.googology.tape_ink."+(property(stack,"ink","false").equals("true")?1:0)));
        if(getBlock() instanceof OrdinalBrickBlock)return Component.translatable("item.googology.number_variant",base,property(stack,"number","0"));
        if(getBlock() instanceof ChristmasDigitBlock){
            String digit=property(stack,"digit","0");
            return Component.translatable("item.googology.number_variant",base,digit);
        }
        return base;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        super.appendHoverText(stack,context,display,out,flag);
        if(getBlock() instanceof MosaicLightBlock||getBlock() instanceof AstraWeaveBlock){
            int count=getBlock() instanceof MosaicLightBlock?16:64;
            out.accept(Component.translatable("item.googology.palette_color",variant(stack,"color",0,count-1)+1,count));
        }
        if(getBlock() instanceof LtyChimeBlock)out.accept(Component.translatable("item.googology.chime_note",variant(stack,"note",0,7)+1));
        if(!(getBlock() instanceof EmojiLanternBlock))return;
        out.accept(Component.translatable("item.googology.lantern_expression",emotion(stack)));
        int part=variant(stack,"part",27,27);
        if(part>=0&&part<27)out.accept(Component.translatable("item.googology.lantern_tile",part%3+1,part/3%3+1,part/9+1));
        else out.accept(Component.translatable("item.googology.lantern_whole"));
    }
}
