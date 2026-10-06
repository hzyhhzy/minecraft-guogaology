package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import java.util.*;

/** Offhand effects are server-owned; movement prediction reads the same item profile on the client. */
public final class ManuscriptEffects {
    private static final String OWNED_FLIGHT="guogaology_manuscript_flight";
    private static final String OWNED_NIGHT="guogaology_manuscript_night",OWNED_FIRE="guogaology_manuscript_fire";
    private record Flight(String world){}
    private static final Map<UUID,Flight> FLIGHTS=new HashMap<>();
    private static final Set<UUID> LANDING=new HashSet<>();
    private static final Map<UUID,Map<Holder<MobEffect>,MobEffectInstance>> EFFECTS=new HashMap<>();
    private static final List<Holder<MobEffect>> LIGHT=List.of(MobEffects.POISON,MobEffects.HUNGER,MobEffects.NAUSEA,MobEffects.WEAKNESS,MobEffects.SLOWNESS);
    private static final List<Holder<MobEffect>> STRONG=List.of(MobEffects.WITHER,MobEffects.BLINDNESS,MobEffects.DARKNESS);
    private ManuscriptEffects(){}
    public static boolean deep(Level world){return world.dimension().equals(GoogologyMod.DIMENSION)||world.dimension().equals(GoogologyMod.GUOGAO);}
    public static ItemStack held(LivingEntity entity){var stack=entity.getOffhandItem();return stack.getItem() instanceof DenxiManuscript?stack:ItemStack.EMPTY;}
    public static int level(ItemStack book,int type){return EquipmentRules.highest(GearData.profile(book),type);}
    public static double points(LivingEntity entity,int type){return GearData.points(held(entity),type);}
    public static double miningMultiplier(Player player,ItemStack tool,net.minecraft.world.level.block.state.BlockState state){return EquipmentRules.miningMultiplier(GearData.minesWithPick(tool,state)?GearData.profile(tool):List.of(),GearData.profile(held(player)),deep(player.level()));}
    public static int jumpBlocks(LivingEntity player){int count=0;for(var core:GearData.profile(held(player)))if(core.type()==3&&core.level()==1)count++;return count;}
    private static double rise(double velocity,double gravity){double height=0;for(int i=0;i<256&&velocity>0;i++){height+=velocity;velocity=(velocity-gravity)*.98;}return height;}
    /** Solve discrete vanilla ascent so each Lv1 adds one block, including the underworld's lower gravity. */
    public static float jumpVelocity(Player player,float vanilla){
        int extra=jumpBlocks(player);double gravity=player.getGravity();if(extra==0||gravity<=0)return vanilla;
        double target=rise(vanilla,gravity)+extra,low=vanilla,high=vanilla+Math.sqrt(2*gravity*extra)+1;
        for(int i=0;i<24;i++){double mid=(low+high)/2;if(rise(mid,gravity)<target)low=mid;else high=mid;}return (float)high;
    }
    public static boolean ownsFlight(Player player){return player instanceof ManuscriptFlightAccess access&&access.googology$ownsManuscriptFlight();}
    private static boolean customFlight(Player player){return ownsFlight(player)&&player.getAbilities().flying&&!player.isPassenger()&&!player.isCreative()&&!player.isSpectator();}
    public static float horizontalSpeed(Player player,float vanilla){
        if(!customFlight(player))return vanilla;int grade=level(held(player),3);
        if(grade==2)return .05f/3;
        return grade>=3?.05f*(player.isSprinting()?(deep(player.level())?8:2):1):vanilla;
    }
    public static float verticalSpeed(Player player,float vanilla){
        if(!customFlight(player))return vanilla;int grade=level(held(player),3);
        return grade==2?.05f/3:grade>=3?.05f*(deep(player.level())&&player.isSprinting()?8:1):vanilla;
    }
    public static boolean immune(LivingEntity player,Holder<MobEffect> effect){int grade=level(held(player),7);return grade>=2&&LIGHT.contains(effect)||grade>=3&&STRONG.contains(effect);}
    private static String effectTag(Holder<MobEffect> type){return type.equals(MobEffects.NIGHT_VISION)?OWNED_NIGHT:OWNED_FIRE;}
    /** Remove our layer before a real potion overwrites it, avoiding an owned infinite hidden effect. */
    public static void beforeEffect(LivingEntity entity,MobEffectInstance incoming){
        if(!(entity instanceof ServerPlayer player))return;var owned=EFFECTS.get(player.getUUID());if(owned==null)return;
        var type=incoming.getEffect();var ours=owned.get(type);
        if(ours!=null&&ours!=incoming&&player.getEffect(type)==ours){owned.remove(type);player.removeTag(effectTag(type));if(owned.isEmpty())EFFECTS.remove(player.getUUID());player.removeEffect(type);}
    }
    private static void effect(ServerPlayer player,Holder<MobEffect> type,boolean enabled){
        var owned=EFFECTS.get(player.getUUID());var ours=owned==null?null:owned.get(type);var current=player.getEffect(type);
        if(ours!=null&&current!=ours){owned.remove(type);player.removeTag(effectTag(type));ours=null;}
        if(owned!=null&&owned.isEmpty())EFFECTS.remove(player.getUUID());
        if(!enabled){if(ours!=null){owned.remove(type);player.removeTag(effectTag(type));player.removeEffect(type);}if(owned!=null&&owned.isEmpty())EFFECTS.remove(player.getUUID());return;}
        if(current!=null)return;
        var added=new MobEffectInstance(type,-1,0,false,false,true);EFFECTS.computeIfAbsent(player.getUUID(),id->new HashMap<>()).put(type,added);if(player.addEffect(added))player.addTag(effectTag(type));
    }
    private static void clearEffects(ServerPlayer player){
        var owned=EFFECTS.remove(player.getUUID());if(owned!=null)for(var entry:owned.entrySet())if(player.getEffect(entry.getKey())==entry.getValue())player.removeEffect(entry.getKey());
        player.removeTag(OWNED_NIGHT);player.removeTag(OWNED_FIRE);
    }
    /** Player saves can outlive in-memory ownership after a crash; real potion replacements remove the marker. */
    private static void recoverEffects(ServerPlayer player){
        for(var type:List.of(MobEffects.NIGHT_VISION,MobEffects.FIRE_RESISTANCE))if(player.getTags().contains(effectTag(type))){
            var active=player.getEffect(type);if(active!=null&&active.isInfiniteDuration()&&active.getAmplifier()==0&&!active.isAmbient()&&!active.isVisible())player.removeEffect(type);
            player.removeTag(effectTag(type));
        }
    }
    private static void modifier(ServerPlayer player,Holder<Attribute> attribute,String id,double amount){
        var instance=player.getAttribute(attribute);if(instance==null)return;var key=GoogologyMod.id(id);var old=instance.getModifier(key);
        if(old!=null&&Math.abs(old.amount()-amount)<1e-8)return;instance.removeModifier(key);if(amount!=0)instance.addTransientModifier(new AttributeModifier(key,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void release(ServerPlayer player){
        var flight=FLIGHTS.remove(player.getUUID());player.removeTag(OWNED_FLIGHT);if(player instanceof ManuscriptFlightAccess access)access.googology$setManuscriptFlight(false);
        if(flight==null)return;
        if(!player.isCreative()&&!player.isSpectator()){
            var abilities=player.getAbilities();boolean flying=abilities.flying;abilities.mayfly=false;abilities.flying=false;
            if(flying&&!player.onGround())LANDING.add(player.getUUID());
        }
        player.onUpdateAbilities();
    }
    public static void tick(ServerPlayer player){
        if(!player.isAlive()){release(player);clearEffects(player);LANDING.remove(player.getUUID());return;}
        var book=held(player);int flight=level(book,3);var old=FLIGHTS.get(player.getUUID());
        if(old!=null&&!old.world().equals(player.level().dimension().identifier().toString())){release(player);clearEffects(player);}
        modifier(player,Attributes.MOVEMENT_SPEED,"manuscript_speed",0);
        modifier(player,Attributes.ATTACK_DAMAGE,"manuscript_attack",GearData.bookAttackBonus(player));
        modifier(player,Attributes.BLOCK_INTERACTION_RANGE,"manuscript_block_reach",GearData.bookReachBonus(player));
        modifier(player,Attributes.ENTITY_INTERACTION_RANGE,"manuscript_entity_reach",GearData.bookReachBonus(player));
        modifier(player,Attributes.MAX_HEALTH,"manuscript_health",GearData.bonusHealth(player));
        if(player.getHealth()>player.getMaxHealth())player.setHealth(player.getMaxHealth());
        if(!player.isCreative()&&!player.isSpectator()&&flight>=2){
            var abilities=player.getAbilities();
            if(!abilities.mayfly){FLIGHTS.put(player.getUUID(),new Flight(player.level().dimension().identifier().toString()));abilities.mayfly=true;player.addTag(OWNED_FLIGHT);if(player instanceof ManuscriptFlightAccess access)access.googology$setManuscriptFlight(true);player.onUpdateAbilities();}
        }else release(player);
        if(player.onGround()||player.isInWater())LANDING.remove(player.getUUID());
        effect(player,MobEffects.NIGHT_VISION,level(book,4)>=2);effect(player,MobEffects.FIRE_RESISTANCE,level(book,5)>=2);
        for(var active:new ArrayList<>(player.getActiveEffects()))if(immune(player,active.getEffect()))player.removeEffect(active.getEffect());
        if(player.tickCount%80==0&&player.getHealth()<player.getMaxHealth()){double healing=EquipmentRules.regen(GearData.profile(book));if(healing>0)player.heal((float)healing);}
    }
    public static boolean fallImmune(LivingEntity player){return level(held(player),3)>=2||LANDING.contains(player.getUUID());}
    public static void initialize(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var player=handler.player;
            recoverEffects(player);
            if(player.getTags().contains(OWNED_FLIGHT)){player.removeTag(OWNED_FLIGHT);if(!player.isCreative()&&!player.isSpectator()){player.getAbilities().mayfly=false;player.getAbilities().flying=false;LANDING.add(player.getUUID());player.onUpdateAbilities();}}
            tick(player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var player:server.getPlayerList().getPlayers())tick(player);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{release(handler.player);clearEffects(handler.player);LANDING.remove(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(var player:server.getPlayerList().getPlayers()){release(player);clearEffects(player);}FLIGHTS.clear();LANDING.clear();EFFECTS.clear();});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->!(entity instanceof ServerPlayer)||!(source.is(DamageTypes.FALL)&&fallImmune(entity)||source.is(DamageTypes.FLY_INTO_WALL)&&level(held(entity),3)>=3));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,damage)->{
            if(!(entity instanceof ServerPlayer player)||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||level(held(player),7)<2)return true;
            for(int i=0;i<player.getInventory().getContainerSize();i++){
                var stack=player.getInventory().getItem(i);if(!stack.is(Items.TOTEM_OF_UNDYING))continue;
                stack.shrink(1);player.setHealth(1);clearEffects(player);player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,800,0));player.level().broadcastEntityEvent(player,(byte)35);return false;
            }
            return true;
        });
    }
}
