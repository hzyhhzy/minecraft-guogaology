package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;
import net.minecraft.tags.DamageTypeTags;
import java.util.*;

/** No mana, charge counter, cooldown UI or extra input. All ownership is server-side. */
public final class ManuscriptEffects {
    private static final String OWNED_FLIGHT="guogaology_manuscript_flight";
    private record Flight(float speed){}
    private static final Map<UUID,Flight> FLIGHTS=new HashMap<>();
    private static final Set<UUID> LANDING=new HashSet<>();
    private ManuscriptEffects(){}
    public static boolean deep(Level world){return world.dimension().equals(GoogologyMod.DIMENSION)||world.dimension().equals(GoogologyMod.GUOGAO);}
    public static ItemStack held(LivingEntity e){var s=e.getOffhandItem();return s.getItem() instanceof DenxiManuscript?s:ItemStack.EMPTY;}
    public static int level(ItemStack book,int type){return GearData.cores(book).stream().filter(c->GearData.type(c)==type).mapToInt(GearData::level).max().orElse(0);}
    public static double points(LivingEntity e,int type){return GearData.points(held(e),type);}
    public static double miningMultiplier(Player player,ItemStack tool){
        double original=GearData.points(tool,8),book=points(player,0);
        return EquipmentRules.multiplier(original+book,deep(player.level()))/EquipmentRules.multiplier(original,tool.getOrDefault(MiningContent.DEEP,false));
    }
    private static void modifier(ServerPlayer p,net.minecraft.core.Holder<Attribute> attribute,String id,double amount){
        var instance=p.getAttribute(attribute);if(instance==null)return;
        var key=GoogologyMod.id(id);var old=instance.getModifier(key);
        if(old!=null&&Math.abs(old.amount()-amount)<1e-8)return;
        instance.removeModifier(key);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(key,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static void release(ServerPlayer p){
        var flight=FLIGHTS.remove(p.getUUID());p.removeTag(OWNED_FLIGHT);if(flight==null)return;
        p.getAbilities().setFlyingSpeed(flight.speed());
        if(!p.isCreative()&&!p.isSpectator()){
            var a=p.getAbilities();boolean flying=a.flying;a.mayfly=false;a.flying=false;
            if(flying&&!p.onGround())LANDING.add(p.getUUID());
        }
        p.onUpdateAbilities();
    }
    public static void tick(ServerPlayer p){
        if(!p.isAlive()){release(p);LANDING.remove(p.getUUID());return;}
        var book=held(p);int flight=level(book,3);
        modifier(p,Attributes.MOVEMENT_SPEED,"manuscript_speed",Math.min(.06,.008*points(p,5)));
        modifier(p,Attributes.MAX_HEALTH,"manuscript_health",Math.min(20,2*points(p,7)));
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
        if(!p.isCreative()&&!p.isSpectator()&&flight>=2){
            var a=p.getAbilities();boolean changed=false;
            if(!a.mayfly){FLIGHTS.put(p.getUUID(),new Flight(a.getFlyingSpeed()));a.mayfly=true;p.addTag(OWNED_FLIGHT);changed=true;}
            if(FLIGHTS.containsKey(p.getUUID())){float speed=(float)(.05+(flight>=3?.02:0)+Math.min(.03,points(p,5)*.003));if(a.getFlyingSpeed()!=speed){a.setFlyingSpeed(speed);changed=true;}}
            if(changed)p.onUpdateAbilities();
        }else release(p);
        if(p.onGround()||p.isInWater())LANDING.remove(p.getUUID());
        if((flight==1||LANDING.contains(p.getUUID()))&&!p.onGround()){
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,40,0,false,false,true));
        }
        if(p.tickCount%80==0&&points(p,2)>0&&p.getHealth()<p.getMaxHealth())p.heal((float)Math.min(3,points(p,2)*.25));
        // Repair is passive and bounded; no durability multiplier is applied twice.
        if(p.tickCount%200==0&&points(p,8)>0)for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var gear=p.getItemBySlot(slot);if(gear.isDamaged())gear.setDamageValue(Math.max(0,gear.getDamageValue()-(int)Math.ceil(points(p,8))));
        }
    }
    public static void initialize(){
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->{
            var p=handler.player;
            if(p.getTags().contains(OWNED_FLIGHT)){
                p.removeTag(OWNED_FLIGHT);
                if(!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.getAbilities().setFlyingSpeed(.05f);p.onUpdateAbilities();p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,200,0,false,false,true));}
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:s.getPlayerList().getPlayers())tick(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{release(handler.player);LANDING.remove(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{for(var p:s.getPlayerList().getPlayers())release(p);FLIGHTS.clear();LANDING.clear();});
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity,source,damage)->{
            if(!(entity instanceof ServerPlayer p)||source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||level(held(p),7)<2)return true;
            // Vanilla hand-held totems are checked first by LivingEntity. Only a lethal hit
            // that still reaches this callback may consume ONE inventory totem.
            for(int i=0;i<p.getInventory().getContainerSize();i++){
                var stack=p.getInventory().getItem(i);if(!stack.is(Items.TOTEM_OF_UNDYING))continue;
                stack.shrink(1);p.setHealth(1);p.removeAllEffects();
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION,900,1));
                p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,100,1));
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,800,0));
                p.level().broadcastEntityEvent(p,(byte)35);return false;
            }
            return true;
        });
    }
}
