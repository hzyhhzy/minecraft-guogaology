package dev.guogaology.mining;
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
        EquipmentTooltips.append(stack,tier,EquipmentRules.BOW,out);
    }
}
