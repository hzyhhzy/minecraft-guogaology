package dev.googology.mining;

import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class OrdinalGear extends Item {
    public final int tier,kind;
    public OrdinalGear(Properties properties,int tier,int kind){super(properties);this.tier=tier;this.kind=kind;}
    @Override public void inventoryTick(ItemStack s,net.minecraft.server.level.ServerLevel world,Entity entity,EquipmentSlot slot){if(s.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION)GearData.refresh(s);}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return kind==0&&(state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.getBlock() instanceof OrdinalOre)?(float)GearData.miningSpeed(stack):1;}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,BlockState state){return kind==0&&(state.is(BlockTags.MINEABLE_WITH_PICKAXE)||state.getBlock() instanceof OrdinalOre);}
    @Override public boolean mineBlock(ItemStack stack,Level world,BlockState state,BlockPos pos,LivingEntity owner){if(!world.isClientSide())stack.hurtAndBreak(kind==0?1:2,owner,EquipmentSlot.MAINHAND);return true;}
    @Override public void hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){stack.hurtAndBreak(kind==1?1:2,attacker,EquipmentSlot.MAINHAND);if(kind==1)MiningEffects.burst(stack,target,attacker);}
    @Override public float getAttackDamageBonus(Entity target,float current,DamageSource source){
        if(!(source.getEntity() instanceof Player p)||p.getMainHandItem().getItem()!=this)return 0;
        double actual=Math.max(.1,GearData.power(p.getMainHandItem()));
        return (float)(current*(actual/Math.min(1023,actual)-1));
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        if(kind>=2)out.accept(Component.translatable("mining.googology.defense",EquipmentRules.format(GearData.power(s)*EquipmentRules.armorShare(kind))));
        if(kind==0)out.accept(Component.translatable("mining.googology.denxi",EquipmentRules.format(GearData.denxi(s))));
        if(tier==0)out.accept(Component.translatable("mining.googology.digit",GearData.digit(s)));
        out.accept(Component.translatable("mining.googology.slots",GearData.cores(s).size(),EquipmentRules.slots(tier),EquipmentRules.grade(tier)));
        for(var c:GearData.cores(s))out.accept(Component.literal("• ").append(c.getHoverName()).append(" — ").append(Component.translatable("mining.googology.effect."+GearData.type(c))));
        if(kind==0)out.accept(Component.translatable("mining.googology.sneak"));
    }
}
