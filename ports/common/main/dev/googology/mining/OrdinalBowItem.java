package dev.googology.mining;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** Vanilla draw animation/time; ordinary vanilla arrows are launched with snapshots. */
public final class OrdinalBowItem extends BowItem {
    public final int tier;
    public OrdinalBowItem(Properties properties,int tier){super(properties);this.tier=tier;}
    @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity owner,int remaining){return BowEffects.release(stack,level,owner,getPowerForTime(getUseDuration(stack,owner)-remaining),tier);}
    @Override public void inventoryTick(ItemStack s,ServerLevel world,Entity owner,EquipmentSlot slot){if(s.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION||s.getOrDefault(MiningContent.DEEP,false)!=ManuscriptEffects.deep(world)){s.set(MiningContent.DEEP,ManuscriptEffects.deep(world));GearData.refresh(s);}}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        out.accept(Component.translatable("mining.googology.slots",GearData.cores(stack).size(),EquipmentRules.slots(tier,7)));
        out.accept(Component.translatable("mining.googology.bow_speed_tooltip",EquipmentRules.format(EquipmentRules.arrowSpeedMultiplier(tier))));
        int branch=EquipmentRules.highest(GearData.profile(stack),2);
        if(branch>=1)out.accept(Component.translatable("mining.googology.bow_infinity"));if(branch>=2)out.accept(Component.translatable("mining.googology.bow_multishot"));if(branch>=3)out.accept(Component.translatable("mining.googology.bow_pierce"));
        out.accept(Component.translatable("mining.googology.bow_ammo"));
        if(EquipmentRules.highest(GearData.profile(stack),3)>0)out.accept(Component.translatable("mining.googology.wear_factor",EquipmentRules.format(EquipmentRules.wearFactor(GearData.profile(stack),java.util.List.of()))));
        for(var core:GearData.cores(stack))out.accept(Component.literal("• ").append(core.getHoverName()).append(" — ").append(Component.translatable(EquipmentRules.coreEffectKey(EquipmentRules.BOW,GearData.type(core),GearData.level(core)))));
    }
}
