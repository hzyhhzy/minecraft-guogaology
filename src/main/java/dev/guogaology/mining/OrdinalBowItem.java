package dev.guogaology.mining;
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
    @Override public int getEnchantability(){return 1;}
    @Override public boolean canRepair(ItemStack bow,ItemStack ingredient){return ingredient.isOf(MiningContent.MATERIALS[tier-1]);}
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,List<Text> out,TooltipType flag){
        EquipmentTooltips.append(stack,tier,EquipmentRules.BOW,out::add);
    }
}
