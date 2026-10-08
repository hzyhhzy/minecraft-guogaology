package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.core.Direction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native 26.2 fall/collision damage, modifiers, enchantments and realm changes. QA only. */
public final class InnerLanding043Checks {
    private static int checks;
    private static final Identifier SAFE_ADD=Identifier.parse("guogaology:qa_inner_safe_add"),SAFE_RATE=Identifier.parse("guogaology:qa_inner_safe_rate");
    private static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private InnerLanding043Checks(){}
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<.003,label+": "+actual+" != "+expected);}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static ItemStack book(int... levels){var stack=item("true_omega_manuscript");GearData.setCores(stack,Arrays.stream(levels).mapToObj(level->item("boundary_core"+(level==1?"":"_lv"+level))).toList());return stack;}
    private static void equipment(ServerPlayer p)throws Exception{var update=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");update.setAccessible(true);update.invoke(p);}
    private static void clear(ServerPlayer p)throws Exception{
        p.closeContainer();for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);equipment(p);p.setOnGround(true);p.setSprinting(false);ManuscriptEffects.tick(p);p.removeAllEffects();
        p.getAbilities().mayfly=false;p.getAbilities().flying=false;p.getAbilities().invulnerable=false;p.setAbsorptionAmount(0);p.invulnerableTime=0;
    }
    private static void move(ServerPlayer p,ServerLevel world){
        p.teleport(new TeleportTransition(world,new Vec3(8.5,250,24.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
        p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());p.setOnGround(true);
    }
    private static DamageSource source(ServerPlayer p,ResourceKey<DamageType> type){return new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));}
    private static double impact(ServerPlayer p,ResourceKey<DamageType> type,float amount){
        p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);p.invulnerableTime=0;
        float before=p.getHealth();p.hurtServer(p.level(),source(p,type),amount);return before-p.getHealth();
    }
    private static double fall(ServerPlayer p,double distance){
        p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);p.invulnerableTime=0;
        float before=p.getHealth();p.causeFallDamage(distance,1,source(p,DamageTypes.FALL));return before-p.getHealth();
    }
    private static double spike(ServerPlayer p){
        p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);p.invulnerableTime=0;
        float before=p.getHealth();
        var tip=Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(PointedDripstoneBlock.TIP_DIRECTION,Direction.UP).setValue(PointedDripstoneBlock.THICKNESS,SpeleothemThickness.TIP);
        tip.getBlock().fallOn(p.level(),tip,p.blockPosition(),p,15);
        return before-p.getHealth();
    }
    public static void run(ServerPlayer p)throws Exception{
        checks=0;var oldWorld=p.level();var oldPosition=p.position();var oldVelocity=p.getDeltaMovement();var oldMode=p.gameMode();
        float yaw=p.getYRot(),pitch=p.getXRot(),health=p.getHealth(),absorption=p.getAbsorptionAmount();double oldFall=p.fallDistance;
        boolean oldGround=p.onGround(),oldGravity=p.isNoGravity(),oldSprint=p.isSprinting();int ticks=p.tickCount,invulnerableTime=p.invulnerableTime;
        var inventory=p.getInventory();int selected=inventory.getSelectedSlot();var saved=new ArrayList<ItemStack>();for(int n=0;n<inventory.getContainerSize();n++)saved.add(inventory.getItem(n));
        var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var maximum=p.getAttribute(Attributes.MAX_HEALTH);double oldMaximum=maximum.getBaseValue();
        var abilities=p.getAbilities();boolean mayfly=abilities.mayfly,flying=abilities.flying,invulnerable=abilities.invulnerable;
        try{
            p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.tickCount=81;clear(p);maximum.setBaseValue(200);
            var server=p.level().getServer();var inner=server.getLevel(GuogaologyMod.DIMENSION);
            check(GuogaologyMod.DIMENSION.identifier().toString().equals("guogaology:guogaology"),"exact Inner registry ID");
            for(var world:List.of(server.overworld(),server.getLevel(GuogaologyMod.OUTER),inner,server.getLevel(GuogaologyMod.GUOGAO))){
                move(p,world);clear(p);boolean isInner=world==inner,isGuogao=world.dimension().equals(GuogaologyMod.GUOGAO);double factor=isInner?.5:1;
                near(p.getAttribute(Attributes.SAFE_FALL_DISTANCE).getValue(),3,"native safe attribute remains stored as three "+world.dimension());
                near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),isInner?6:3,"effective safe fall distance only doubles in Inner "+world.dimension());
                near(impact(p,DamageTypes.FALL,8),8*factor,"raw normal fall impact once "+world.dimension());
                near(impact(p,DamageTypes.FLY_INTO_WALL,8),8*factor,"raw elytra impact once "+world.dimension());
                near(impact(p,DamageTypes.GENERIC,8),8,"unrelated damage unchanged "+world.dimension());
                near(impact(p,DamageTypes.STALAGMITE,8),8*factor,"stalagmite raw damage follows realm half "+world.dimension());
                near(impact(p,DamageTypes.ENDER_PEARL,8),8*factor,"ender pearl follows native fall tag "+world.dimension());
                near(spike(p),isGuogao?0:(isInner?23:29)*factor,"actual unprotected pointed-dripstone landing "+world.dimension());
                near(fall(p,7),isGuogao?0:isInner?.5:4,"native landing combines threshold and impact once "+world.dimension());
                near(fall(p,isInner?6:3),0,"safe-height native landing remains harmless "+world.dimension());
                p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,400,1));double jumpSafe=p.getAttribute(Attributes.SAFE_FALL_DISTANCE).getValue();
                check(jumpSafe>3,"native Jump Boost raises safe distance");
                near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),jumpSafe*(isInner?2:1),"Jump Boost safe distance composes multiplicatively "+world.dimension());
                near(fall(p,jumpSafe*(isInner?2:1)),0,"Jump Boost native safe landing "+world.dimension());
                near(fall(p,jumpSafe*(isInner?2:1)+1),isGuogao?0:factor,"first harmful block after Jump Boost threshold "+world.dimension());
                p.removeAllEffects();
                protection(p,factor);walking(p);editing(p);clear(p);
            }
            move(p,inner);clear(p);var safe=p.getAttribute(Attributes.SAFE_FALL_DISTANCE);
            safe.addTransientModifier(new AttributeModifier(SAFE_ADD,2,AttributeModifier.Operation.ADD_VALUE));
            safe.addTransientModifier(new AttributeModifier(SAFE_RATE,.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            near(safe.getValue(),7.5,"native safe modifiers compose before realm effect");near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),15,"whole effective safe distance doubles");
            near(fall(p,15),0,"modified native safe threshold");near(fall(p,16),.5,"modified threshold has one half-point first hit");
            p.setItemSlot(EquipmentSlot.OFFHAND,book(1));equipment(p);
            near(safe.getValue(),7.5,"Boundary I does not mutate stored safe attribute");near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),30,"native safe modifiers then Inner x2 and Boundary I x2");
            near(fall(p,30),0,"quadrupled modified native safe threshold");near(fall(p,31),.25/GearData.protectionFactor(p),"first harmful block includes independent quarter impact and book protection");
            p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);equipment(p);
            safe.removeModifier(SAFE_ADD);safe.removeModifier(SAFE_RATE);
            var zombie=EntityTypes.ZOMBIE.create(inner,EntitySpawnReason.COMMAND);check(zombie!=null,"living-entity fixture");
            try{
                near(zombie.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),6,"world rule also applies to nonplayer living entities");
                float before=zombie.getHealth();zombie.hurtServer(inner,source(p,DamageTypes.FALL),8);near(before-zombie.getHealth(),4,"nonplayer fall damage halves once");
            }finally{if(zombie!=null)zombie.discard();}
            move(p,server.getLevel(GuogaologyMod.OUTER));clear(p);near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),3,"leaving Inner immediately restores native threshold");
            System.out.println("INNER_LANDING_043_OK checks="+checks+" exact Inner / fall-tag+actual stalagmite+ender pearl / independent Boundary I halves+safe x2 / Jump Boost+native modifiers / Feather Falling / all grades RSS walk+sprint / native flight / hand priority+editing / immunity / nonplayer");
        }finally{
            p.getAttribute(Attributes.SAFE_FALL_DISTANCE).removeModifier(SAFE_ADD);p.getAttribute(Attributes.SAFE_FALL_DISTANCE).removeModifier(SAFE_RATE);
            clear(p);maximum.setBaseValue(oldMaximum);for(int n=0;n<saved.size();n++)inventory.setItem(n,saved.get(n));inventory.setSelectedSlot(selected);equipment(p);
            p.setGameMode(oldMode);p.teleport(new TeleportTransition(oldWorld,oldPosition,oldVelocity,yaw,pitch,TeleportTransition.DO_NOTHING));
            p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            p.tickCount=ticks;p.setOnGround(oldGround);p.setSprinting(oldSprint);ManuscriptEffects.tick(p);p.removeAllEffects();for(var effect:effects)p.addEffect(effect);
            abilities.mayfly=mayfly;abilities.flying=flying;abilities.invulnerable=invulnerable;p.onUpdateAbilities();p.setNoGravity(oldGravity);p.fallDistance=oldFall;p.invulnerableTime=invulnerableTime;
            p.setAbsorptionAmount(absorption);p.setHealth(Math.min(health,p.getMaxHealth()));
        }
    }
    private static void protection(ServerPlayer p,double realm)throws Exception{
        p.setItemSlot(EquipmentSlot.OFFHAND,book(1));equipment(p);double protection=GearData.protectionFactor(p);
        near(impact(p,DamageTypes.FALL,8),8*realm*.5/protection,"Boundary I half fall stacks with realm half");
        near(impact(p,DamageTypes.STALAGMITE,8),8*realm*.5/protection,"Boundary I half stone-spike stacks with realm half");
        near(impact(p,DamageTypes.ENDER_PEARL,8),8*realm*.5/protection,"Boundary I half ender-pearl damage follows fall tag");
        near(impact(p,DamageTypes.FALLING_STALACTITE,8),8/protection,"falling stalactite is not a fall-family injury");
        near(impact(p,DamageTypes.FLY_INTO_WALL,8),8*realm*.5/protection,"Boundary I half collision stacks with realm half");
        double safe=p.getAttribute(Attributes.SAFE_FALL_DISTANCE).getValue()*2/realm;
        near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),safe,"Boundary I safety x2 independently of realm");
        near(fall(p,safe),0,"Boundary I native threshold harmless");
        boolean underworld=p.level().dimension().equals(GuogaologyMod.GUOGAO);
        near(spike(p),underworld?0:(realm==.5?11:23)*realm*.5/protection,"Boundary I actual stone-spike combines safe height and half damage");
        near(fall(p,safe+1),underworld?0:realm*.5/protection,"Boundary I first harmful block after doubled threshold");
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,400,1));double jumpSafe=p.getAttribute(Attributes.SAFE_FALL_DISTANCE).getValue()*2/realm;
        near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),jumpSafe,"Jump Boost receives both independent safety multipliers");
        near(fall(p,jumpSafe+1),underworld?0:realm*.5/protection,"Jump Boost plus Boundary I native landing");p.removeAllEffects();
        var boots=new ItemStack(Items.DIAMOND_BOOTS);boots.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FEATHER_FALLING),4);
        p.setItemSlot(EquipmentSlot.FEET,boots);equipment(p);
        near(impact(p,DamageTypes.FALL,8),8*realm*.5*.52/GearData.protectionFactor(p),"Feather Falling IV, book protection, Boundary I and realm multiply");
        near(impact(p,DamageTypes.STALAGMITE,8),8*realm*.5*.52/GearData.protectionFactor(p),"Feather Falling IV stacks identically on stone-spike damage");
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,400,0));
        near(impact(p,DamageTypes.FALL,8),8*realm*.5*.52*.8/GearData.protectionFactor(p),"native Resistance remains an additional multiplier");
        near(impact(p,DamageTypes.STALAGMITE,8),8*realm*.5*.52*.8/GearData.protectionFactor(p),"native Resistance stacks on stone-spike damage");p.removeAllEffects();
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,book(2));equipment(p);
        near(impact(p,DamageTypes.FALL,8),0,"Boundary II fall immunity survives realm rule");
        near(impact(p,DamageTypes.STALAGMITE,8),0,"Boundary II stone-spike immunity without active flight");
        near(impact(p,DamageTypes.ENDER_PEARL,8),0,"Boundary II ender-pearl immunity without active flight");
        near(spike(p),0,"Boundary II actual stone-spike immunity without active flight");
        near(impact(p,DamageTypes.FALLING_STALACTITE,8),8/GearData.protectionFactor(p),"Boundary II does not exempt falling stalactites");
        near(impact(p,DamageTypes.FLY_INTO_WALL,8),8*realm/GearData.protectionFactor(p),"Boundary II collision still follows ordinary protection");
        p.setItemSlot(EquipmentSlot.OFFHAND,book(3));equipment(p);
        near(impact(p,DamageTypes.FALL,8),0,"Boundary III fall immunity survives realm rule");
        ManuscriptEffects.tick(p);p.getAbilities().flying=true;
        near(fall(p,15),0,"owned flight protects real normal-fall callback");
        p.setHealth(p.getMaxHealth());p.invulnerableTime=0;float beforeSpike=p.getHealth();
        p.causeFallDamage(15,1,source(p,DamageTypes.STALAGMITE));
        near(p.getHealth(),beforeSpike,"owned flight protects fall-tag stone-spike callback");
        near(spike(p),0,"Boundary III actual pointed-dripstone immunity while flying");
        near(impact(p,DamageTypes.STALAGMITE,8),0,"Boundary III raw stone-spike immunity");
        near(impact(p,DamageTypes.ENDER_PEARL,8),0,"Boundary III raw ender-pearl immunity");
        near(impact(p,DamageTypes.FALLING_STALACTITE,8),8/GearData.protectionFactor(p),"Boundary III does not exempt falling stalactites");
        p.setHealth(p.getMaxHealth());p.invulnerableTime=0;beforeSpike=p.getHealth();
        p.causeFallDamage(15,1,source(p,DamageTypes.GENERIC));
        check(underworld?p.getHealth()==beforeSpike:p.getHealth()<beforeSpike,"owned permission does not exempt non-fall-tag callback");
        check(p.getAbilities().mayfly&&p.getAbilities().flying,"fall wrapper restores permission without cancelling flight");
        near(impact(p,DamageTypes.FLY_INTO_WALL,8),0,"Boundary III elytra collision immunity survives realm rule");
        for(int[] cores:new int[][]{{1,1,1},{1,2},{1,3}}){
            p.setItemSlot(EquipmentSlot.OFFHAND,book(cores));equipment(p);
            near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),safe,"presence of Boundary I doubles safety once "+Arrays.toString(cores));
            near(impact(p,DamageTypes.FALL,8),cores[cores.length-1]>=2?0:8*realm*.5/GearData.protectionFactor(p),"repeated and mixed Boundary fall protection "+Arrays.toString(cores));
            near(impact(p,DamageTypes.STALAGMITE,8),cores[cores.length-1]>=2?0:8*realm*.5/GearData.protectionFactor(p),"repeated and mixed Boundary stone-spike protection "+Arrays.toString(cores));
            near(impact(p,DamageTypes.FLY_INTO_WALL,8),cores[cores.length-1]>=3?0:8*realm*.5/GearData.protectionFactor(p),"mixed Boundary II retains Boundary I collision half "+Arrays.toString(cores));
        }
    }
    private static float nativeFlight(ServerPlayer p)throws Exception{var method=Player.class.getDeclaredMethod("getFlyingSpeed");method.setAccessible(true);return (float)method.invoke(p);}
    private static void walking(ServerPlayer p)throws Exception{
        clear(p);double base=p.getAttributeValue(Attributes.MOVEMENT_SPEED);boolean deep=ManuscriptEffects.deep(p.level());
        for(int grade=1;grade<=3;grade++)for(int count:new int[]{1,2,4,6}){
            clear(p);int[] cores=new int[count];Arrays.fill(cores,grade);p.setItemSlot(EquipmentSlot.OFFHAND,book(cores));equipment(p);ManuscriptEffects.tick(p);
            double rate=.5*Math.sqrt(count),walk=base*(1+rate);String label=" grade="+grade+" count="+count+" world="+p.level().dimension();
            near(EquipmentRules.boundaryWalkRate(GearData.profile(ManuscriptEffects.held(p))),rate,"all Boundary grades have identical RSS walk potency"+label);
            var modifier=p.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(GuogaologyMod.id("manuscript_speed"));
            check(modifier!=null&&modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,"native movement total multiplier"+label);
            near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),walk,"actual walking attribute"+label);
            p.setSprinting(true);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),walk*1.3,"native sprint retains its multiplier"+label);
            p.addEffect(new MobEffectInstance(MobEffects.SPEED,400,0));near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),walk*1.3*1.2,"native Speed effect multiplies with walk boost"+label);p.removeEffect(MobEffects.SPEED);p.setSprinting(false);
            if(grade>=2){
                check(ManuscriptEffects.ownsFlight(p),"native manuscript flight owned"+label);p.getAbilities().flying=true;p.setOnGround(false);
                double slow=.05/(deep?3:6);
                near(nativeFlight(p),grade==2?slow:.05,"walking bonus does not enter native flight"+label);
                near(ManuscriptEffects.verticalSpeed(p,.77f),grade==2?slow:.05,"walking bonus does not enter vertical flight"+label);
                ManuscriptEffects.setFlightSprint(p,true);near(nativeFlight(p),grade==2?slow:deep?.2:.1,"dedicated sprint flight is independent of walking bonus"+label);
                near(ManuscriptEffects.verticalSpeed(p,.77f),grade==2?slow:deep?.2:.1,"dedicated sprint has matching vertical coefficient"+label);
            }
        }
        clear(p);p.setItemSlot(EquipmentSlot.OFFHAND,book(1,2,3));equipment(p);ManuscriptEffects.tick(p);
        near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),base*(1+.5*Math.sqrt(3)),"mixed grades use count RSS, not level RSS");
        clear(p);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),base,"unequipping removes all manuscript walk speed");
        p.setItemSlot(EquipmentSlot.MAINHAND,book(1));equipment(p);ManuscriptEffects.tick(p);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),base*1.5,"mainhand book walk activates");
        p.setItemSlot(EquipmentSlot.OFFHAND,book());equipment(p);ManuscriptEffects.tick(p);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),base,"empty offhand book overrides mainhand walk boost");
        near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),p.getAttribute(Attributes.SAFE_FALL_DISTANCE).getValue()*(p.level().dimension().equals(GuogaologyMod.DIMENSION)?2:1),"empty offhand book also overrides Boundary I safety");clear(p);
    }
    private static void editing(ServerPlayer p)throws Exception{
        clear(p);double base=p.getAttributeValue(Attributes.MOVEMENT_SPEED),safe=p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        var current=book(1,1);p.setItemSlot(EquipmentSlot.OFFHAND,current);equipment(p);ManuscriptEffects.tick(p);
        var menu=new ManuscriptMenu(86,p.getInventory(),p.level(),40,current);p.containerMenu=menu;
        for(int index=0;index<2;index++){
            menu.clicked(EnhancementMenu.FIRST_CORE+index,0,ContainerInput.PICKUP,p);check(!menu.getCarried().isEmpty(),"real menu transfers Boundary core to cursor");
            p.getInventory().setItem(9+index,ItemStack.EMPTY);menu.clicked(EnhancementMenu.INVENTORY+index,0,ContainerInput.PICKUP,p);
        }
        check(GearData.profile(current).isEmpty()&&menu.getCarried().isEmpty(),"real book empty while opening snapshot retains cores");
        ManuscriptEffects.tick(p);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),base*(1+.5*Math.sqrt(2)),"walk uses opening snapshot during editing");
        near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),safe*2,"safe fall uses opening snapshot during editing");
        double realm=p.level().dimension().equals(GuogaologyMod.DIMENSION)?.5:1;
        near(impact(p,DamageTypes.FLY_INTO_WALL,8),8*realm*.5/GearData.protectionFactor(p),"impact half uses opening snapshot during editing");
        near(impact(p,DamageTypes.STALAGMITE,8),8*realm*.5/GearData.protectionFactor(p),"stone-spike half uses opening snapshot during editing");
        p.closeContainer();ManuscriptEffects.tick(p);near(p.getAttributeValue(Attributes.MOVEMENT_SPEED),base,"closed menu activates empty book walking");
        near(p.getAttributeValue(Attributes.SAFE_FALL_DISTANCE),safe,"closed menu activates empty book safety");
        near(impact(p,DamageTypes.FLY_INTO_WALL,8),8*realm/GearData.protectionFactor(p),"closed menu removes Lv1 impact half");
        near(impact(p,DamageTypes.STALAGMITE,8),8*realm/GearData.protectionFactor(p),"closed menu removes Lv1 stone-spike half");clear(p);
    }
}
