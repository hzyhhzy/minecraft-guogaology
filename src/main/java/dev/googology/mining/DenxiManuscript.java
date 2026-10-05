package dev.googology.mining;

import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import java.util.function.Consumer;

/** A passive offhand socket carrier; never spends the installed cores. */
public final class DenxiManuscript extends Item {
    public final int tier;
    public DenxiManuscript(Settings props,int tier){super(props);this.tier=tier;}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,java.util.List<Text> out,TooltipType flag){
        out.add(Text.translatable("mining.googology.manuscript.passive"));
        out.add(Text.translatable("mining.googology.slots",GearData.cores(stack).size(),EquipmentRules.slots(tier,6),EquipmentRules.grade(tier)));
        for(var core:GearData.cores(stack))out.add(Text.literal("• ").append(core.getName()).append(" — ").append(Text.translatable("mining.googology.manuscript.effect."+GearData.type(core))));
        if(ManuscriptEffects.level(stack,7)>=2)out.add(Text.translatable("mining.googology.manuscript.totem"));
    }
}
