package dev.googology.mining;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.*;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import java.util.List;

public final class OrdinalBowItem extends BowItem {
    public final int tier;
    public OrdinalBowItem(Settings properties,int tier){super(properties);this.tier=tier;}
    @Override public void onStoppedUsing(ItemStack stack,World world,LivingEntity owner,int remaining){BowEffects.release(stack,world,owner,getPullProgress(getMaxUseTime(stack,owner)-remaining),tier);}
    @Override public void inventoryTick(ItemStack s,World world,Entity owner,int slot,boolean selected){if(!world.isClient&&(s.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION||s.getOrDefault(MiningContent.DEEP,false)!=ManuscriptEffects.deep(world))){s.set(MiningContent.DEEP,ManuscriptEffects.deep(world));GearData.refresh(s);}}
    @Override public int getEnchantability(){return 0;}
    @Override public boolean canRepair(ItemStack bow,ItemStack ingredient){return ingredient.isOf(MiningContent.MATERIALS[tier-1]);}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> out,TooltipType flag){
        out.add(Text.translatable("mining.googology.slots",GearData.cores(stack).size(),EquipmentRules.slots(tier,7),EquipmentRules.grade(tier)));
        out.add(Text.translatable("mining.googology.bow_speed_tooltip",EquipmentRules.format(EquipmentRules.arrowSpeedMultiplier(tier))));
        int branch=EquipmentRules.highest(GearData.profile(stack),2);
        if(branch>=1)out.add(Text.translatable("mining.googology.bow_infinity"));if(branch>=2)out.add(Text.translatable("mining.googology.bow_multishot"));if(branch>=3)out.add(Text.translatable("mining.googology.bow_pierce"));
        out.add(Text.translatable("mining.googology.bow_ammo"));
        if(EquipmentRules.highest(GearData.profile(stack),3)>0)out.add(Text.translatable("mining.googology.wear_factor",EquipmentRules.format(EquipmentRules.wearFactor(GearData.profile(stack),List.of()))));
        for(var core:GearData.cores(stack))out.add(Text.literal("• ").append(core.getName()).append(" — ").append(Text.translatable("mining.googology.effect."+GearData.type(core))));
    }
}
