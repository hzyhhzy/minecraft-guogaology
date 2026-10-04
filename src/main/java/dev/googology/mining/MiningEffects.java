package dev.googology.mining;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.math.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.*;
import net.minecraft.item.ItemStack;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.*;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

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
            if(EXTRA.get()||!(player instanceof ServerPlayerEntity p)||p.isCreative()||p.isSneaking()||entity!=null)return;
            var tool=p.getMainHandStack();var spec=MiningContent.GEAR.get(tool.getItem());if(spec==null||spec.kind()!=0)return;
            int budget=EquipmentRules.spreadBudget(GearData.points(tool,0));if(budget==0)return;
            var look=p.getRotationVec(1f);Direction normal=Direction.getFacing(look.x,look.y,look.z);
            var candidates=new ArrayList<BlockPos>();
            for(int a=-4;a<=4;a++)for(int b=-4;b<=4;b++)if(a!=0||b!=0)candidates.add(switch(normal.getAxis()){case X->pos.add(0,a,b);case Y->pos.add(a,0,b);case Z->pos.add(a,b,0);});
            candidates.sort(Comparator.comparingDouble(pos::getSquaredDistance));
            EXTRA.set(true);
            try{for(var q:candidates){if(budget<=0||p.getMainHandStack()!=tool||tool.isEmpty())break;if(!world.isChunkLoaded(q))continue;var other=world.getBlockState(q);
                if(!eligible(state,other)||world.getBlockEntity(q)!=null||!world.getWorldBorder().contains(q))continue;
                if(other.getHardness(world,q)<0)continue;
                if(!world.canPlayerModifyAt(p,q))continue;
                if(p.interactionManager.tryBreakBlock(q))budget--;
            }}finally{EXTRA.set(false);}
        });
    }
    private static boolean eligible(BlockState original,BlockState next){
        if(!next.isIn(BlockTags.PICKAXE_MINEABLE))return false;
        if(next.getBlock() instanceof OrdinalOre ore)return original.getBlock() instanceof OrdinalOre first&&ore.tier<=first.tier;
        if(VANILLA_ORES.contains(next.getBlock()))return true;
        // Restrict additional excavation to terrain and ores; never core displays or containers.
        return next.isIn(BlockTags.BASE_STONE_OVERWORLD)||next.isIn(BlockTags.BASE_STONE_NETHER)||next.getBlock() instanceof dev.googology.block.NumberStoneBlock||next.getBlock()==dev.googology.GoogologyBlocks.ROOTBOUND_STONE||next.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BLOCK,dev.googology.GoogologyMod.id("ores")));
    }
    public static void burst(ItemStack weapon,LivingEntity target,LivingEntity attacker){
        double points=GearData.points(weapon,5);if(points<=0||!(attacker instanceof ServerPlayerEntity p)||!(target.getWorld() instanceof ServerWorld world))return;
        world.spawnParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);
        world.playSound(null,target.getBlockPos(),SoundEvents.ENTITY_GENERIC_EXPLODE.value(),SoundCategory.PLAYERS,.55f,1.25f);
        for(var other:world.getEntitiesByClass(LivingEntity.class,target.getBoundingBox().expand(3),e->e!=target&&e!=p&&!e.isTeammate(p))){
            if(other.squaredDistanceTo(target)>9||!p.canSee(other))continue;
            other.damage(p.getDamageSources().playerAttack(p),(float)(GearData.power(weapon)*points*.15));
        }
    }
}
