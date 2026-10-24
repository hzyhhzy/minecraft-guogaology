package dev.guogaology.mining;

import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class OrdinalGear extends Item {
    public final int tier,kind;
    public OrdinalGear(Properties properties,int tier,int kind){super(properties);this.tier=tier;this.kind=kind;}
    @Override public void inventoryTick(ItemStack s,net.minecraft.server.level.ServerLevel world,Entity entity,EquipmentSlot slot){if((s.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION||s.getOrDefault(MiningContent.DEEP,false)!=ManuscriptEffects.deep(world))){s.set(MiningContent.DEEP,ManuscriptEffects.deep(world));GearData.refresh(s);}}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return kind==0&&(state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.getBlock() instanceof OrdinalOre)?(float)GearData.miningSpeed(stack):1;}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){
        if(!GearData.minesWithPick(stack,state))return false;
        var incorrect=switch(EquipmentRules.harvestLevel(tier,GearData.digit(stack))){case 0->BlockTags.INCORRECT_FOR_STONE_TOOL;case 1->BlockTags.INCORRECT_FOR_IRON_TOOL;case 2->BlockTags.INCORRECT_FOR_DIAMOND_TOOL;default->BlockTags.INCORRECT_FOR_NETHERITE_TOOL;};
        return !state.is(incorrect);
    }
    @Override public boolean mineBlock(ItemStack stack,Level world,BlockState state,BlockPos pos,LivingEntity owner){if(!world.isClientSide())stack.hurtAndBreak(kind==0?1:2,owner,EquipmentSlot.MAINHAND);return true;}
    @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){if(kind==1){MiningEffects.burst(stack,target,attacker);MiningEffects.control(stack,target,attacker);}stack.hurtAndBreak(kind==1?1:2,attacker,EquipmentSlot.MAINHAND);}
    @Override public void appendHoverText(ItemStack s,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        EquipmentTooltips.append(s,tier,kind,out);
    }
}
