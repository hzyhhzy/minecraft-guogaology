package dev.guogaology.mining;

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
import dev.guogaology.mixin.ExtraDamageAccessor;

public final class MiningEffects {
    private static final Set<Block> VANILLA_ORES=Set.of(
        Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
        Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
        Blocks.NETHER_QUARTZ_ORE, Blocks.NETHER_GOLD_ORE, Blocks.ANCIENT_DEBRIS);
    private static final ThreadLocal<Boolean> EXTRA=ThreadLocal.withInitial(()->false);
    private record MiningStart(net.minecraft.world.World world,BlockPos pos,ItemStack tool,BlockState original,Direction facing,MiningPattern pattern,List<EquipmentRules.Core> profile){}
    private static final Map<ServerPlayerEntity,MiningStart> STARTS=new WeakHashMap<>();
    private MiningEffects(){}
    /** Called on the server's native START_DESTROY_BLOCK action, before a successful break. */
    public static void captureStart(ServerPlayerEntity player,BlockPos pos){
        cancelStart(player);
        net.minecraft.world.World world=player.getWorld();
        if(!player.isAlive()||player.isCreative()||player.isSneaking()||player.isSpectator()||!world.isChunkLoaded(pos))return;
        var tool=player.getMainHandStack();var spec=MiningContent.GEAR.get(tool.getItem());
        if(spec==null||spec.kind()!=0||EquipmentRules.spreadBudget(GearData.profile(tool),List.of(),false)<=0)return;
        var original=world.getBlockState(pos);
        if(!originAvailable(world,player,pos))return;
        STARTS.put(player,new MiningStart(world,pos.toImmutable(),tool,original,player.getHorizontalFacing(),GearData.miningPattern(tool),GearData.profile(tool)));
    }
    public static void cancelStart(ServerPlayerEntity player){STARTS.remove(player);}
    public static void initialize(){
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->cancelStart(handler.player));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->STARTS.clear());
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player,origin,destination)->cancelStart(player));
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->{
            if(EXTRA.get()||!(player instanceof ServerPlayerEntity p))return;
            var start=STARTS.remove(p);
            // Native attack may change properties (notably redstone LIT) before the same block is mined.
            if(start==null||!p.isAlive()||p.isCreative()||p.isSneaking()||entity!=null||start.world()!=world||!start.pos().equals(pos)||start.original().getBlock()!=state.getBlock())return;
            var tool=p.getMainHandStack();
            if(start.tool()!=tool||tool.isEmpty()||!start.pattern().equals(GearData.miningPattern(tool))||!start.profile().equals(GearData.profile(tool)))return;
            int budget=EquipmentRules.spreadBudget(GearData.profile(tool),List.of(),false);
            // Freeze the selected cells first: skipped air/protected blocks never recruit replacements.
            var candidates=patternTargets(pos,start.pattern(),budget,start.facing());
            EXTRA.set(true);
            try{for(var q:candidates){
                if(p.getMainHandStack()!=tool||tool.isEmpty())break;
                if(canBreak(world,p,state,q,tool))p.interactionManager.tryBreakBlock(q);
            }}finally{EXTRA.set(false);}
        });
    }
    /** Player-facing horizontal yaw fixes a vertical stencil even when looking at a floor or ceiling. */
    public static List<BlockPos> patternTargets(BlockPos origin,MiningPattern pattern,int budget,Direction facing){
        if(facing.getAxis()==Direction.Axis.Y)return List.of();
        var right=facing.rotateYClockwise();var targets=new ArrayList<BlockPos>();
        for(int cell:pattern.active(budget)){
            int horizontal=pattern.horizontal(cell),vertical=pattern.vertical(cell);
            targets.add(origin.add(right.getOffsetX()*horizontal,vertical,right.getOffsetZ()*horizontal));
        }
        return List.copyOf(targets);
    }
    /** Read-only client preview. The server repeats every permission and tool check before mining. */
    public static List<BlockPos> previewTargets(net.minecraft.world.World world,net.minecraft.entity.player.PlayerEntity player,BlockPos origin,Direction facing,ItemStack tool){
        var spec=MiningContent.GEAR.get(tool.getItem());
        if(spec==null||spec.kind()!=0||!player.isAlive()||player.isCreative()||player.isSneaking()||!world.isChunkLoaded(origin))return List.of();
        if(!originAvailable(world,player,origin))return List.of();
        var original=world.getBlockState(origin);
        int budget=EquipmentRules.spreadBudget(GearData.profile(tool),List.of(),false);
        return patternTargets(origin,GearData.miningPattern(tool),budget,facing).stream().filter(q->canBreak(world,player,original,q,tool)).toList();
    }
    public static boolean previewEligible(net.minecraft.world.World world,net.minecraft.entity.player.PlayerEntity player,BlockPos origin,BlockPos target){
        if(!world.isChunkLoaded(origin))return false;
        var original=world.getBlockState(origin);
        return originAvailable(world,player,origin)&&canBreak(world,player,original,target,player.getMainHandStack());
    }
    private static boolean originAvailable(net.minecraft.world.World world,net.minecraft.entity.player.PlayerEntity player,BlockPos pos){
        if(!world.isChunkLoaded(pos)||!world.getWorldBorder().contains(pos))return false;
        var state=world.getBlockState(pos);
        return !state.isAir()&&world.getBlockEntity(pos)==null&&state.getHardness(world,pos)>=0&&world.canPlayerModifyAt(player,pos);
    }
    private static boolean canBreak(net.minecraft.world.World world,net.minecraft.entity.player.PlayerEntity player,BlockState original,BlockPos target,ItemStack tool){
        if(!world.isChunkLoaded(target)||!world.getWorldBorder().contains(target))return false;
        var next=world.getBlockState(target);
        if(!eligible(original,next)||world.getBlockEntity(target)!=null)return false;
        if(next.getHardness(world,target)<0||!tool.isSuitableFor(next))return false;
        return world.canPlayerModifyAt(player,target);
    }
    private static boolean eligible(BlockState original,BlockState next){
        if(!next.isIn(BlockTags.PICKAXE_MINEABLE))return false;
        if(next.getBlock() instanceof OrdinalOre ore)return original.getBlock() instanceof OrdinalOre first&&ore.tier<=first.tier;
        if(VANILLA_ORES.contains(next.getBlock()))return true;
        // Restrict additional excavation to terrain and ores; never core displays or containers.
        return next.isIn(BlockTags.BASE_STONE_OVERWORLD)||next.isIn(BlockTags.BASE_STONE_NETHER)||next.getBlock() instanceof dev.guogaology.block.NumberStoneBlock||next.getBlock()==dev.guogaology.GuogaologyBlocks.ROOTBOUND_STONE||next.isIn(net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BLOCK,dev.guogaology.GuogaologyMod.id("ores")));
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
            if(other==target){int timer=other.timeUntilRegen;var accessor=(ExtraDamageAccessor)other;float previous=accessor.guogaology$lastDamage();other.timeUntilRegen=0;try{other.damage(damage,amount);}finally{other.timeUntilRegen=timer;accessor.guogaology$lastDamage(previous);}}
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
    /** XP needs the eligible virtual Silk Touch, but no virtual Fortune or tool mutation. */
    public static ItemStack experienceTool(BlockState state,ServerWorld world,Entity owner,ItemStack tool){
        return !tool.isEmpty()&&GearData.silkTouch(tool)?lootTool(state,world,owner,tool):tool;
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
        if(id.getNamespace().equals("guogaology")&&(EquipmentRules.coreType(id.getPath())>=0||Arrays.stream(MiningContent.STORAGE).anyMatch(b->b==state.getBlock())))return tool;
        for(var property:state.getProperties())if(property.getName().equals("player_placed")&&Boolean.TRUE.equals(state.get(property)))return tool;
        int level=GearData.yieldLevel(tool,owner,false);if(level<=0)return tool;
        level+=EnchantmentHelper.getLevel(fortune,tool);
        var copy=tool.copy();var builder=new ItemEnchantmentsComponent.Builder(copy.getOrDefault(DataComponentTypes.ENCHANTMENTS,ItemEnchantmentsComponent.DEFAULT));builder.set(fortune,level);copy.set(DataComponentTypes.ENCHANTMENTS,builder.build());return copy;
    }
}
