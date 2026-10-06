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
import net.minecraft.entity.effect.*;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.enchantment.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.registry.RegistryKeys;
import dev.googology.mixin.ExtraDamageAccessor;

public final class MiningEffects {
    private static final Set<Block> VANILLA_ORES=Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS);
    private static final ThreadLocal<Boolean> EXTRA=ThreadLocal.withInitial(()->false);
    private MiningEffects(){}
    public static void initialize(){
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if(EXTRA.get()||!(player instanceof ServerPlayerEntity p)||p.isCreative()||p.isSneaking()||entity!=null)return;
            var tool=p.getMainHandStack();var spec=MiningContent.GEAR.get(tool.getItem());if(spec==null||spec.kind()!=0)return;
            int budget=EquipmentRules.spreadBudget(GearData.profile(tool),GearData.profile(ManuscriptEffects.held(p)),ManuscriptEffects.deep(world));if(budget==0)return;
            var look=p.getRotationVec(1f);Direction normal=Direction.getFacing(look.x,look.y,look.z);
            var candidates=new ArrayList<BlockPos>();
            for(int a=-4;a<=4;a++)for(int b=-4;b<=4;b++)if(a!=0||b!=0)candidates.add(switch(normal.getAxis()){case X->pos.add(0,a,b);case Y->pos.add(a,0,b);case Z->pos.add(a,b,0);});
            candidates.sort(Comparator.comparingDouble(pos::getSquaredDistance));
            EXTRA.set(true);
            try{for(var q:candidates){if(budget<=0||p.getMainHandStack()!=tool||tool.isEmpty())break;if(!world.isChunkLoaded(q))continue;var other=world.getBlockState(q);
                if(!eligible(state,other)||world.getBlockEntity(q)!=null||!world.getWorldBorder().contains(q))continue;
                if(other.getHardness(world,q)<0||!tool.isSuitableFor(other))continue;
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
        double coefficient=EquipmentRules.criticalCoefficient(GearData.profile(weapon),List.of(),false);if(coefficient<=0||!(attacker instanceof ServerPlayerEntity p)||!(target.getWorld() instanceof ServerWorld world))return;
        explode(world,target,p,p,(float)(GearData.attackWithBook(weapon,p)*coefficient));
    }
    /** Kept for old callers; manuscripts no longer grant projectile bursts. */
    public static void projectileBurst(ProjectileEntity projectile,LivingEntity target,float actualDamage){}
    private static void explode(ServerWorld world,LivingEntity target,ServerPlayerEntity p,Entity source,float amount){
        world.spawnParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);
        world.playSound(null,target.getBlockPos(),SoundEvents.ENTITY_GENERIC_EXPLODE.value(),SoundCategory.PLAYERS,.55f,1.25f);
        var damage=p.getDamageSources().explosion(source,p);
        for(var other:world.getEntitiesByClass(LivingEntity.class,target.getBoundingBox().expand(3),e->e!=p&&!e.isTeammate(p))){
            if(other.squaredDistanceTo(target)>9||other!=target&&!target.canSee(other))continue;
            if(other==target){int timer=other.timeUntilRegen;var accessor=(ExtraDamageAccessor)other;float previous=accessor.googology$lastDamage();other.timeUntilRegen=0;try{other.damage(damage,amount);}finally{other.timeUntilRegen=timer;accessor.googology$lastDamage(previous);}}
            else other.damage(damage,amount);
        }
    }
    public static void control(ItemStack weapon,LivingEntity target,LivingEntity attacker){
        if(!(attacker instanceof ServerPlayerEntity)||!target.isAlive()||target==attacker||target.isTeammate(attacker))return;
        int level=EquipmentRules.highest(GearData.profile(weapon),7);if(level==0)return;int ticks=(int)Math.round(20*GearData.controlDuration(weapon,attacker));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,ticks,level-1));
        if(level==1)target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,ticks,1));
        else{
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER,ticks,level==2?1:2));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,ticks,level-2));
            if(level>=3){target.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,ticks,0));if(attacker.getRandom().nextFloat()<.2f)target.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS,ticks,0));}
        }
    }
    /** Modifies a loot-context copy; the real tool and its enchantments are untouched. */
    public static ItemStack lootTool(BlockState state,ServerWorld world,Entity entity,ItemStack tool){
        if(tool.isEmpty()||!(entity instanceof LivingEntity owner)||state.hasBlockEntity())return tool;
        var lookup=world.getRegistryManager().getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        var fortune=lookup.getOrThrow(Enchantments.FORTUNE);
        if(GearData.silkTouch(tool)){
            if(state.isToolRequired()&&!tool.isSuitableFor(state))return tool;
            var copy=tool.copy();var builder=new ItemEnchantmentsComponent.Builder(copy.getOrDefault(DataComponentTypes.ENCHANTMENTS,ItemEnchantmentsComponent.DEFAULT));
            builder.set(fortune,0);builder.set(lookup.getOrThrow(Enchantments.SILK_TOUCH),1);copy.set(DataComponentTypes.ENCHANTMENTS,builder.build());return copy;
        }
        var id=net.minecraft.registry.Registries.BLOCK.getId(state.getBlock());
        if(id.getNamespace().equals("googology")&&(EquipmentRules.coreType(id.getPath())>=0||Arrays.stream(MiningContent.STORAGE).anyMatch(b->b==state.getBlock())))return tool;
        for(var property:state.getProperties())if(property.getName().equals("player_placed")&&Boolean.TRUE.equals(state.get(property)))return tool;
        int level=GearData.yieldLevel(tool,owner,false);if(level<=0)return tool;
        level+=EnchantmentHelper.getLevel(fortune,tool);
        var copy=tool.copy();var builder=new ItemEnchantmentsComponent.Builder(copy.getOrDefault(DataComponentTypes.ENCHANTMENTS,ItemEnchantmentsComponent.DEFAULT));builder.set(fortune,level);copy.set(DataComponentTypes.ENCHANTMENTS,builder.build());return copy;
    }
}
