package dev.guogaology.mining;

import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.*;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import java.util.function.Consumer;

public class OrdinalGear extends Item {
    public final int tier,kind;
    public OrdinalGear(Settings properties,int tier,int kind){super(properties);this.tier=tier;this.kind=kind;}
    @Override public void inventoryTick(ItemStack s,World world,Entity entity,int slot,boolean selected){if(!world.isClient&&(s.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION||s.getOrDefault(MiningContent.DEEP,false)!=ManuscriptEffects.deep(world))){s.set(MiningContent.DEEP,ManuscriptEffects.deep(world));GearData.refresh(s);}}
    @Override public float getMiningSpeed(ItemStack stack,BlockState state){return kind==0&&(state.isIn(BlockTags.PICKAXE_MINEABLE)||state.getBlock() instanceof OrdinalOre)?(float)GearData.miningSpeed(stack):1;}
    @Override public boolean isCorrectForDrops(ItemStack stack,BlockState state){
        if(!GearData.minesWithPick(stack,state))return false;
        var incorrect=switch(EquipmentRules.harvestLevel(tier,GearData.digit(stack))){case 0->BlockTags.INCORRECT_FOR_STONE_TOOL;case 1->BlockTags.INCORRECT_FOR_IRON_TOOL;case 2->BlockTags.INCORRECT_FOR_DIAMOND_TOOL;default->BlockTags.INCORRECT_FOR_NETHERITE_TOOL;};
        return !state.isIn(incorrect);
    }
    @Override public boolean postMine(ItemStack stack,World world,BlockState state,BlockPos pos,LivingEntity owner){if(!world.isClient)stack.damage(kind==0?1:2,owner,EquipmentSlot.MAINHAND);return true;}
    @Override public boolean postHit(ItemStack stack,LivingEntity target,LivingEntity attacker){if(kind==1){MiningEffects.burst(stack,target,attacker);MiningEffects.control(stack,target,attacker);}stack.damage(kind==1?1:2,attacker,EquipmentSlot.MAINHAND);return true;}
    @Override public int getEnchantability(){return 15;}
    @Override public boolean canRepair(ItemStack gear,ItemStack ingredient){return tier>0&&ingredient.isOf(MiningContent.MATERIALS[tier-1]);}
    @Override public void appendTooltip(ItemStack s,TooltipContext context,java.util.List<Text> out,TooltipType flag){
        if(kind==0)out.add(Text.translatable("mining.guogaology.mining_speed",EquipmentRules.format(GearData.denxi(s))));
        if(tier==0)out.add(Text.translatable("mining.guogaology.digit",GearData.digit(s)));
        out.add(Text.translatable("mining.guogaology.slots",GearData.cores(s).size(),EquipmentRules.slots(tier,kind)));
        var hint=GearData.modificationHint(s);if(hint!=null)out.add(Text.translatable("mining.guogaology."+hint));
        if(GearData.points(s,3)>0)out.add(Text.translatable("mining.guogaology.wear_factor",EquipmentRules.format(EquipmentRules.wearFactor(GearData.profile(s),java.util.List.of()))));
        for(var c:GearData.cores(s))out.add(Text.literal("• ").append(c.getName()).append(" — ").append(Text.translatable(EquipmentRules.coreEffectKey(kind,GearData.type(c),GearData.level(c)))));
        if(kind==0)out.add(Text.translatable("mining.guogaology.sneak"));
    }
}
