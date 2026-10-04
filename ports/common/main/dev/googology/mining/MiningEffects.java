package dev.googology.mining;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.*;
import java.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class MiningEffects {
    private static final Set<Block> VANILLA_ORES=Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS);
    private static final ThreadLocal<Boolean> EXTRA=ThreadLocal.withInitial(()->false);
    private MiningEffects(){}
    public static int experience(int base,ItemStack tool){return Math.max(0,(int)Math.round(base*(1+.25*GearData.points(tool,4))));}
    public static void initialize(){
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if(EXTRA.get()||!(player instanceof ServerPlayer p)||p.isCreative()||p.isShiftKeyDown()||entity!=null)return;
            var tool=p.getMainHandItem();var spec=MiningContent.GEAR.get(tool.getItem());if(spec==null||spec.kind()!=0)return;
            int budget=EquipmentRules.spreadBudget(GearData.points(tool,0));if(budget==0)return;
            var look=p.getLookAngle();Direction normal=Direction.getApproximateNearest(look.x,look.y,look.z);
            var candidates=new ArrayList<BlockPos>();
            for(int a=-4;a<=4;a++)for(int b=-4;b<=4;b++)if(a!=0||b!=0)candidates.add(switch(normal.getAxis()){case X->pos.offset(0,a,b);case Y->pos.offset(a,0,b);case Z->pos.offset(a,b,0);});
            candidates.sort(Comparator.comparingDouble(pos::distSqr));
            EXTRA.set(true);
            try{for(var q:candidates){if(budget<=0||p.getMainHandItem()!=tool||tool.isEmpty())break;if(!world.hasChunkAt(q))continue;var other=world.getBlockState(q);
                if(!eligible(state,other)||world.getBlockEntity(q)!=null||!world.getWorldBorder().isWithinBounds(q))continue;
                if(other.getDestroySpeed(world,q)<0)continue;
                if(!world.mayInteract(p,q))continue;
                if(p.gameMode.destroyBlock(q))budget--;
            }}finally{EXTRA.set(false);}
        });
    }
    private static boolean eligible(BlockState original,BlockState next){
        if(!next.is(BlockTags.MINEABLE_WITH_PICKAXE))return false;
        if(next.getBlock() instanceof OrdinalOre ore)return original.getBlock() instanceof OrdinalOre first&&ore.tier<=first.tier;
        if(VANILLA_ORES.contains(next.getBlock()))return true;
        // Restrict additional excavation to terrain and ores; never core displays or containers.
        return next.is(BlockTags.BASE_STONE_OVERWORLD)||next.is(BlockTags.BASE_STONE_NETHER)||next.getBlock() instanceof dev.googology.block.NumberStoneBlock||next.getBlock()==dev.googology.GoogologyBlocks.ROOTBOUND_STONE||next.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,dev.googology.GoogologyMod.id("ores")));
    }
    public static void burst(ItemStack weapon,LivingEntity target,LivingEntity attacker){
        double points=GearData.points(weapon,5);if(points<=0||!(attacker instanceof ServerPlayer p)||!(target.level() instanceof ServerLevel world))return;
        world.sendParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);
        world.playSound(null,target.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.55f,1.25f);
        for(var other:world.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(3),e->e!=target&&e!=p&&!e.isAlliedTo(p))){
            if(other.distanceToSqr(target)>9||!p.hasLineOfSight(other))continue;
            other.hurtServer(world,p.damageSources().playerAttack(p),(float)(GearData.power(weapon)*points*.15));
        }
    }
}
