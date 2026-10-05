package dev.googology.mining;

import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** A passive offhand socket carrier; never spends the installed cores. */
public final class DenxiManuscript extends Item {
    public final int tier;
    public DenxiManuscript(Properties props,int tier){super(props);this.tier=tier;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        out.accept(Component.translatable("mining.googology.manuscript.passive"));
        out.accept(Component.translatable("mining.googology.slots",GearData.cores(stack).size(),EquipmentRules.slots(tier,6),EquipmentRules.grade(tier)));
        for(var core:GearData.cores(stack))out.accept(Component.literal("• ").append(core.getHoverName()).append(" — ").append(Component.translatable("mining.googology.manuscript.effect."+GearData.type(core))));
        if(ManuscriptEffects.level(stack,7)>=2)out.accept(Component.translatable("mining.googology.manuscript.totem"));
    }
}
