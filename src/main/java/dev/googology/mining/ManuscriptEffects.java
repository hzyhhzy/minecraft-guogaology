package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.entity.effect.*;
import net.minecraft.registry.tag.DamageTypeTags;
import java.util.*;

/** No mana, charge counter, cooldown UI or extra input. All ownership is server-side. */
public final class ManuscriptEffects {
    private static final String OWNED_FLIGHT="guogaology_manuscript_flight";
    private record Flight(float speed){}
    private static final Map<UUID,Flight> FLIGHTS=new HashMap<>();
    private static final Set<UUID> LANDING=new HashSet<>();
    private ManuscriptEffects(){}
    public static boolean deep(World world){return world.getRegistryKey().equals(GoogologyMod.DIMENSION)||world.getRegistryKey().equals(GoogologyMod.GUOGAO);}
    public static ItemStack held(LivingEntity e){var s=e.getOffHandStack();return s.getItem() instanceof DenxiManuscript?s:ItemStack.EMPTY;}
    public static int level(ItemStack book,int type){return GearData.cores(book).stream().filter(c->GearData.type(c)==type).mapToInt(GearData::level).max().orElse(0);}
    public static double points(LivingEntity e,int type){return GearData.points(held(e),type);}
    public static double miningMultiplier(PlayerEntity player,ItemStack tool){
        double original=GearData.points(tool,8),book=points(player,0);
        return EquipmentRules.multiplier(original+book,deep(player.getWorld()))/EquipmentRules.multiplier(original,tool.getOrDefault(MiningContent.DEEP,false));
    }
    private static void modifier(ServerPlayerEntity p,net.minecraft.registry.entry.RegistryEntry<EntityAttribute> attribute,String id,double amount){
        var instance=p.getAttributeInstance(attribute);if(instance==null)return;
        var key=GoogologyMod.id(id);var old=instance.getModifier(key);
        if(old!=null&&Math.abs(old.value()-amount)<1e-8)return;
        instance.removeModifier(key);
        if(amount!=0)instance.addTemporaryModifier(new EntityAttributeModifier(key,amount,EntityAttributeModifier.Operation.ADD_VALUE));
    }
    private static void release(ServerPlayerEntity p){
        var flight=FLIGHTS.remove(p.getUuid());p.removeCommandTag(OWNED_FLIGHT);if(flight==null)return;
        p.getAbilities().setFlySpeed(flight.speed());
        if(!p.isCreative()&&!p.isSpectator()){
            var a=p.getAbilities();boolean flying=a.flying;a.allowFlying=false;a.flying=false;
            if(flying&&!p.isOnGround())LANDING.add(p.getUuid());
        }
        p.sendAbilitiesUpdate();
    }
    public static void tick(ServerPlayerEntity p){
        if(!p.isAlive()){release(p);LANDING.remove(p.getUuid());return;}
        var book=held(p);int flight=level(book,3);
        modifier(p,EntityAttributes.GENERIC_MOVEMENT_SPEED,"manuscript_speed",Math.min(.06,.008*points(p,5)));
        modifier(p,EntityAttributes.GENERIC_MAX_HEALTH,"manuscript_health",Math.min(20,2*points(p,7)));
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
        if(!p.isCreative()&&!p.isSpectator()&&flight>=2){
            var a=p.getAbilities();boolean changed=false;
            if(!a.allowFlying){FLIGHTS.put(p.getUuid(),new Flight(a.getFlySpeed()));a.allowFlying=true;p.addCommandTag(OWNED_FLIGHT);changed=true;}
            if(FLIGHTS.containsKey(p.getUuid())){float speed=(float)(.05+(flight>=3?.02:0)+Math.min(.03,points(p,5)*.003));if(a.getFlySpeed()!=speed){a.setFlySpeed(speed);changed=true;}}
            if(changed)p.sendAbilitiesUpdate();
        }else release(p);
        if(p.isOnGround()||p.isTouchingWater())LANDING.remove(p.getUuid());
        if((flight==1||LANDING.contains(p.getUuid()))&&!p.isOnGround()){
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,40,0,false,false,true));
        }
        if(p.age%80==0&&points(p,2)>0&&p.getHealth()<p.getMaxHealth())p.heal((float)Math.min(3,points(p,2)*.25));
        // Repair is passive and bounded; no durability multiplier is applied twice.
        if(p.age%200==0&&points(p,8)>0)for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var gear=p.getEquippedStack(slot);if(gear.isDamaged())gear.setDamage(Math.max(0,gear.getDamage()-(int)Math.ceil(points(p,8))));
        }
    }
    public static void initialize(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var p=handler.player;
            if(p.getCommandTags().contains(OWNED_FLIGHT)){
                p.removeCommandTag(OWNED_FLIGHT);
                if(!p.isCreative()&&!p.isSpectator()){p.getAbilities().allowFlying=false;p.getAbilities().flying=false;p.getAbilities().setFlySpeed(.05f);p.sendAbilitiesUpdate();p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING,200,0,false,false,true));}
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerManager().getPlayerList())tick(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{release(handler.player);LANDING.remove(handler.player.getUuid());});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var p:s.getPlayerManager().getPlayerList())release(p);FLIGHTS.clear();LANDING.clear();});
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,damage)->{
            if(!(entity instanceof ServerPlayerEntity p)||source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)||level(held(p),7)<2)return true;
            // Vanilla hand-held totems are checked first by LivingEntity. Only a lethal hit
            // that still reaches this callback may consume ONE inventory totem.
            for(int i=0;i<p.getInventory().size();i++){
                var stack=p.getInventory().getStack(i);if(!stack.isOf(Items.TOTEM_OF_UNDYING))continue;
                stack.decrement(1);p.setHealth(1);p.clearStatusEffects();
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,900,1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,100,1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,800,0));
                p.getWorld().sendEntityStatus(p,(byte)35);return false;
            }
            return true;
        });
    }
}
