package dev.guogaology.mining;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.*;
import java.util.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.registries.Registries;
import dev.guogaology.mixin.ExtraDamageAccessor;

public final class MiningEffects {
    private static final Set<Block> VANILLA_ORES=Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS);
    private static final ThreadLocal<Boolean> EXTRA=ThreadLocal.withInitial(()->false);
    private MiningEffects(){}
    public static void initialize(){
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if(EXTRA.get()||!(player instanceof ServerPlayer p)||p.isCreative()||p.isShiftKeyDown()||entity!=null)return;
            var tool=p.getMainHandItem();var spec=MiningContent.GEAR.get(tool.getItem());if(spec==null||spec.kind()!=0)return;
            int budget=EquipmentRules.spreadBudget(GearData.profile(tool),GearData.profile(ManuscriptEffects.held(p)),ManuscriptEffects.deep(world));if(budget==0)return;
            var look=p.getLookAngle();Direction normal=Direction.getApproximateNearest(look.x,look.y,look.z);
            var candidates=new ArrayList<BlockPos>();
            for(int a=-4;a<=4;a++)for(int b=-4;b<=4;b++)if(a!=0||b!=0)candidates.add(switch(normal.getAxis()){case X->pos.offset(0,a,b);case Y->pos.offset(a,0,b);case Z->pos.offset(a,b,0);});
            candidates.sort(Comparator.comparingDouble(pos::distSqr));
            EXTRA.set(true);
            try{for(var q:candidates){if(budget<=0||p.getMainHandItem()!=tool||tool.isEmpty())break;if(q.getY()<pos.getY())continue;if(!world.hasChunkAt(q))continue;var other=world.getBlockState(q);
                if(!eligible(state,other)||world.getBlockEntity(q)!=null||!world.getWorldBorder().isWithinBounds(q))continue;
                if(other.getDestroySpeed(world,q)<0||!tool.isCorrectToolForDrops(other))continue;
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
        return next.is(BlockTags.BASE_STONE_OVERWORLD)||next.is(BlockTags.BASE_STONE_NETHER)||next.getBlock() instanceof dev.guogaology.block.NumberStoneBlock||next.getBlock()==dev.guogaology.GuogaologyBlocks.ROOTBOUND_STONE||next.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,dev.guogaology.GuogaologyMod.id("ores")));
    }
    public static void burst(ItemStack weapon,LivingEntity target,LivingEntity attacker){
        double coefficient=EquipmentRules.criticalCoefficient(GearData.profile(weapon),List.of(),false);if(coefficient<=0||!(attacker instanceof ServerPlayer p)||!(target.level() instanceof ServerLevel world))return;
        explode(world,target,p,p,(float)(GearData.attackWithBook(weapon,p)*coefficient));
    }
    /** Kept for old callers; manuscripts no longer grant projectile bursts. */
    public static void projectileBurst(Projectile projectile,LivingEntity target,float actualDamage){}
    private static void explode(ServerLevel world,LivingEntity target,ServerPlayer p,Entity source,float amount){
        world.sendParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+.5,target.getZ(),1,0,0,0,0);
        world.playSound(null,target.blockPosition(),SoundEvents.GENERIC_EXPLODE.value(),SoundSource.PLAYERS,.55f,1.25f);
        var damage=p.damageSources().explosion(source,p);
        for(var other:world.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(3),e->e!=p&&!e.isAlliedTo(p))){
            if(other.distanceToSqr(target)>9||other!=target&&!target.hasLineOfSight(other))continue;
            if(other==target){int timer=other.invulnerableTime;var accessor=(ExtraDamageAccessor)other;float previous=accessor.guogaology$lastDamage();other.invulnerableTime=0;try{other.hurtServer(world,damage,amount);}finally{other.invulnerableTime=timer;accessor.guogaology$lastDamage(previous);}}
            else other.hurtServer(world,damage,amount);
        }
    }
    public static void control(ItemStack weapon,LivingEntity target,LivingEntity attacker){
        if(!(attacker instanceof ServerPlayer)||!target.isAlive()||target==attacker||target.isAlliedTo(attacker))return;
        int level=EquipmentRules.highest(GearData.profile(weapon),7);if(level==0)return;int ticks=(int)Math.round(20*GearData.controlDuration(weapon,attacker));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,ticks,level-1));
        if(level==1)target.addEffect(new MobEffectInstance(MobEffects.POISON,ticks,1));
        else{
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,ticks,level==2?1:2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,ticks,level-2));
            if(level>=3){target.addEffect(new MobEffectInstance(MobEffects.NAUSEA,ticks,0));if(attacker.getRandom().nextFloat()<.2f)target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,ticks,0));}
        }
    }
    /** Modifies a loot-context copy; the real tool and its enchantments are untouched. */
    public static ItemStack lootTool(BlockState state,ServerLevel world,Entity entity,ItemStack tool){
        if(tool.isEmpty()||!(entity instanceof LivingEntity owner)||state.hasBlockEntity())return tool;
        var lookup=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var fortune=lookup.getOrThrow(Enchantments.FORTUNE);
        if(GearData.silkTouch(tool)){
            if(state.requiresCorrectToolForDrops()&&!tool.isCorrectToolForDrops(state))return tool;
            var copy=tool.copy();var builder=new ItemEnchantments.Mutable(copy.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY));
            builder.set(fortune,0);builder.set(lookup.getOrThrow(Enchantments.SILK_TOUCH),1);copy.set(DataComponents.ENCHANTMENTS,builder.toImmutable());return copy;
        }
        var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(id.getNamespace().equals("guogaology")&&(EquipmentRules.coreType(id.getPath())>=0||Arrays.stream(MiningContent.STORAGE).anyMatch(b->b==state.getBlock())))return tool;
        for(var property:state.getProperties())if(property.getName().equals("player_placed")&&Boolean.TRUE.equals(state.getValue(property)))return tool;
        int level=GearData.yieldLevel(tool,owner,false);if(level<=0)return tool;
        level+=EnchantmentHelper.getItemEnchantmentLevel(fortune,tool);
        var copy=tool.copy();var builder=new ItemEnchantments.Mutable(copy.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY));builder.set(fortune,level);copy.set(DataComponents.ENCHANTMENTS,builder.toImmutable());return copy;
    }
}
