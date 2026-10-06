package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import java.util.*;

/** Offhand effects are server-owned; movement prediction reads the same item profile on the client. */
public final class ManuscriptEffects {
    private static final String OWNED_FLIGHT="guogaology_manuscript_flight";
    private static final String OWNED_NIGHT="guogaology_manuscript_night",OWNED_FIRE="guogaology_manuscript_fire";
    private record Flight(String world){}
    private static final Map<UUID,Flight> FLIGHTS=new HashMap<>();
    private static final Set<UUID> LANDING=new HashSet<>();
    private static final Map<UUID,Map<RegistryEntry<StatusEffect>,StatusEffectInstance>> EFFECTS=new HashMap<>();
    private static final List<RegistryEntry<StatusEffect>> LIGHT=List.of(StatusEffects.POISON,StatusEffects.HUNGER,StatusEffects.WEAKNESS);
    private static final List<RegistryEntry<StatusEffect>> STRONG=List.of(StatusEffects.NAUSEA,StatusEffects.SLOWNESS,StatusEffects.WITHER,StatusEffects.BLINDNESS,StatusEffects.DARKNESS);
    private ManuscriptEffects(){}
    public static boolean deep(World world){return world.getRegistryKey().equals(GoogologyMod.DIMENSION)||world.getRegistryKey().equals(GoogologyMod.GUOGAO);}
    public static ItemStack held(LivingEntity entity){var stack=entity.getOffHandStack();if(stack.getItem() instanceof DenxiManuscript)return effective(entity,stack);stack=entity.getMainHandStack();return stack.getItem() instanceof DenxiManuscript?effective(entity,stack):ItemStack.EMPTY;}
    public static ItemStack effective(LivingEntity entity,ItemStack stack){return stack.getItem() instanceof DenxiManuscript&&entity instanceof PlayerEntity player&&player.currentScreenHandler instanceof ManuscriptMenu menu?menu.activeManuscript(stack):stack;}
    public static int level(ItemStack book,int type){return EquipmentRules.highest(GearData.profile(book),type);}
    public static double points(LivingEntity entity,int type){return GearData.points(held(entity),type);}
    public static double miningMultiplier(PlayerEntity player,ItemStack tool,net.minecraft.block.BlockState state){return EquipmentRules.miningMultiplier(GearData.minesWithPick(tool,state)?GearData.profile(tool):List.of(),GearData.profile(held(player)),deep(player.getWorld()));}
    public static int jumpBlocks(LivingEntity player){int count=0;for(var core:GearData.profile(held(player)))if(core.type()==6&&core.level()==1)count++;return count;}
    private static double rise(double velocity,double gravity){double height=0;for(int i=0;i<256&&velocity>0;i++){height+=velocity;velocity=(velocity-gravity)*.98;}return height;}
    /** Solve discrete vanilla ascent so each Lv1 adds one block, including the underworld's lower gravity. */
    public static float jumpVelocity(PlayerEntity player,float vanilla){
        int extra=jumpBlocks(player);double gravity=player.getFinalGravity();if(extra==0||gravity<=0)return vanilla;
        double target=rise(vanilla,gravity)+extra,low=vanilla,high=vanilla+Math.sqrt(2*gravity*extra)+1;
        for(int i=0;i<24;i++){double mid=(low+high)/2;if(rise(mid,gravity)<target)low=mid;else high=mid;}return (float)high;
    }
    public static boolean ownsFlight(PlayerEntity player){return player instanceof ManuscriptFlightAccess access&&access.googology$ownsManuscriptFlight();}
    private static boolean customFlight(PlayerEntity player){return ownsFlight(player)&&player.getAbilities().flying&&!player.hasVehicle()&&!player.isCreative()&&!player.isSpectator();}
    public static boolean controlsFlightSpeed(PlayerEntity player){return customFlight(player)&&level(held(player),6)>=2;}
    public static float horizontalSpeed(PlayerEntity player,float vanilla){
        if(!customFlight(player))return vanilla;int grade=level(held(player),6);
        if(grade==2)return .05f/3;
        return grade>=3?.05f*(player.isSprinting()?(deep(player.getWorld())?8:2):1):vanilla;
    }
    public static float verticalSpeed(PlayerEntity player,float vanilla){
        if(!customFlight(player))return vanilla;int grade=level(held(player),6);
        return grade==2?.05f/3:grade>=3?.05f*(deep(player.getWorld())&&player.isSprinting()?8:1):vanilla;
    }
    public static boolean immune(LivingEntity player,RegistryEntry<StatusEffect> effect){int grade=level(held(player),7);return grade>=2&&LIGHT.contains(effect)||grade>=3&&STRONG.contains(effect);}
    private static String effectTag(RegistryEntry<StatusEffect> type){return type.equals(StatusEffects.NIGHT_VISION)?OWNED_NIGHT:OWNED_FIRE;}
    /** Remove our layer before a real potion overwrites it, avoiding an owned infinite hidden effect. */
    public static void beforeEffect(LivingEntity entity,StatusEffectInstance incoming){
        if(!(entity instanceof ServerPlayerEntity player))return;var owned=EFFECTS.get(player.getUuid());if(owned==null)return;
        var type=incoming.getEffectType();var ours=owned.get(type);
        if(ours!=null&&ours!=incoming&&player.getStatusEffect(type)==ours){owned.remove(type);player.removeCommandTag(effectTag(type));if(owned.isEmpty())EFFECTS.remove(player.getUuid());player.removeStatusEffect(type);}
    }
    private static void effect(ServerPlayerEntity player,RegistryEntry<StatusEffect> type,boolean enabled){
        var owned=EFFECTS.get(player.getUuid());var ours=owned==null?null:owned.get(type);var current=player.getStatusEffect(type);
        if(ours!=null&&current!=ours){owned.remove(type);player.removeCommandTag(effectTag(type));ours=null;}
        if(owned!=null&&owned.isEmpty())EFFECTS.remove(player.getUuid());
        if(!enabled){if(ours!=null){owned.remove(type);player.removeCommandTag(effectTag(type));player.removeStatusEffect(type);}if(owned!=null&&owned.isEmpty())EFFECTS.remove(player.getUuid());return;}
        if(current!=null)return;
        var added=new StatusEffectInstance(type,-1,0,false,false,true);EFFECTS.computeIfAbsent(player.getUuid(),id->new HashMap<>()).put(type,added);if(player.addStatusEffect(added))player.addCommandTag(effectTag(type));
    }
    private static void clearEffects(ServerPlayerEntity player){
        var owned=EFFECTS.remove(player.getUuid());if(owned!=null)for(var entry:owned.entrySet())if(player.getStatusEffect(entry.getKey())==entry.getValue())player.removeStatusEffect(entry.getKey());
        player.removeCommandTag(OWNED_NIGHT);player.removeCommandTag(OWNED_FIRE);
    }
    /** Player saves can outlive in-memory ownership after a crash; real potion replacements remove the marker. */
    private static void recoverEffects(ServerPlayerEntity player){
        for(var type:List.of(StatusEffects.NIGHT_VISION,StatusEffects.FIRE_RESISTANCE))if(player.getCommandTags().contains(effectTag(type))){
            var active=player.getStatusEffect(type);if(active!=null&&active.isInfinite()&&active.getAmplifier()==0&&!active.isAmbient()&&!active.shouldShowParticles())player.removeStatusEffect(type);
            player.removeCommandTag(effectTag(type));
        }
    }
    private static void modifier(ServerPlayerEntity player,RegistryEntry<EntityAttribute> attribute,String id,double amount){
        var instance=player.getAttributeInstance(attribute);if(instance==null)return;var key=GoogologyMod.id(id);var old=instance.getModifier(key);
        if(old!=null&&Math.abs(old.value()-amount)<1e-8)return;instance.removeModifier(key);if(amount!=0)instance.addTemporaryModifier(new EntityAttributeModifier(key,amount,EntityAttributeModifier.Operation.ADD_VALUE));
    }
    private static void release(ServerPlayerEntity player){
        var flight=FLIGHTS.remove(player.getUuid());player.removeCommandTag(OWNED_FLIGHT);if(player instanceof ManuscriptFlightAccess access)access.googology$setManuscriptFlight(false);
        if(flight==null)return;
        if(!player.isCreative()&&!player.isSpectator()){
            var abilities=player.getAbilities();boolean flying=abilities.flying;abilities.allowFlying=false;abilities.flying=false;
            if(flying&&!player.isOnGround())LANDING.add(player.getUuid());
        }
        player.sendAbilitiesUpdate();
    }
    public static void tick(ServerPlayerEntity player){
        if(!player.isAlive()){release(player);clearEffects(player);LANDING.remove(player.getUuid());return;}
        var book=held(player);int flight=level(book,6);var old=FLIGHTS.get(player.getUuid());
        if(old!=null&&!old.world().equals(player.getWorld().getRegistryKey().getValue().toString())){release(player);clearEffects(player);}
        modifier(player,EntityAttributes.GENERIC_MOVEMENT_SPEED,"manuscript_speed",0);
        modifier(player,EntityAttributes.GENERIC_ATTACK_DAMAGE,"manuscript_attack",GearData.bookAttackBonus(player));
        modifier(player,EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE,"manuscript_block_reach",GearData.bookReachBonus(player));
        modifier(player,EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,"manuscript_entity_reach",GearData.bookReachBonus(player));
        modifier(player,EntityAttributes.GENERIC_MAX_HEALTH,"manuscript_health",GearData.bonusHealth(player));
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
        if(!player.isCreative()&&!player.isSpectator()&&flight>=2){
            var abilities=player.getAbilities();
            if(!abilities.allowFlying){FLIGHTS.put(player.getUuid(),new Flight(player.getWorld().getRegistryKey().getValue().toString()));abilities.allowFlying=true;player.addCommandTag(OWNED_FLIGHT);if(player instanceof ManuscriptFlightAccess access)access.googology$setManuscriptFlight(true);player.sendAbilitiesUpdate();}
        }else release(player);
        if(player.isOnGround()||player.isTouchingWater())LANDING.remove(player.getUuid());
        effect(player,StatusEffects.NIGHT_VISION,level(book,3)>=2);effect(player,StatusEffects.FIRE_RESISTANCE,level(book,6)>=2);
        for(var active:new ArrayList<>(player.getStatusEffects()))if(immune(player,active.getEffectType()))player.removeStatusEffect(active.getEffectType());
        var food=player.getHungerManager();int floor=level(book,4)>=3?19:level(book,4)>=2?10:0;if(food.getFoodLevel()<floor)food.setFoodLevel(floor);
        if(player.age%80==0&&player.getHealth()<player.getMaxHealth()){double healing=GearData.healing(player);if(healing>0)player.heal((float)healing);}
    }
    public static boolean fallImmune(LivingEntity player){return level(held(player),6)>=2||LANDING.contains(player.getUuid());}
    public static void initialize(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var player=handler.player;
            recoverEffects(player);
            if(player.getCommandTags().contains(OWNED_FLIGHT)){player.removeCommandTag(OWNED_FLIGHT);if(!player.isCreative()&&!player.isSpectator()){player.getAbilities().allowFlying=false;player.getAbilities().flying=false;LANDING.add(player.getUuid());player.sendAbilitiesUpdate();}}
            tick(player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var player:server.getPlayerManager().getPlayerList())tick(player);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{release(handler.player);clearEffects(handler.player);LANDING.remove(handler.player.getUuid());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var player:server.getPlayerManager().getPlayerList()){release(player);clearEffects(player);}FLIGHTS.clear();LANDING.clear();EFFECTS.clear();});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->!(entity instanceof ServerPlayerEntity)||!(source.isOf(DamageTypes.FALL)&&fallImmune(entity)||source.isOf(DamageTypes.FLY_INTO_WALL)&&level(held(entity),6)>=3));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,damage)->{
            if(!(entity instanceof ServerPlayerEntity player)||source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)||level(held(player),7)<2)return true;
            for(int i=0;i<player.getInventory().size();i++){
                var stack=player.getInventory().getStack(i);if(!stack.isOf(Items.TOTEM_OF_UNDYING))continue;
                stack.decrement(1);player.setHealth(1);clearEffects(player);player.clearStatusEffects();
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,900,1));player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,100,1));player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,800,0));player.getWorld().sendEntityStatus(player,(byte)35);return false;
            }
            return true;
        });
    }
}
