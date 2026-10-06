package dev.googology.mining;

import dev.googology.GoogologyMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.registry.Registries;
import net.minecraft.item.*;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.component.type.AttributeModifierSlot;
import java.util.*;

public final class GearData {
    private record ProfileCache(List<ItemStack> source,List<EquipmentRules.Core> value){}
    private static final Map<ItemStack,ProfileCache> PROFILES=Collections.synchronizedMap(new WeakHashMap<>());
    public static List<EquipmentRules.Core> profile(ItemStack stack){
        var source=coreSlots(stack);var old=PROFILES.get(stack);if(old!=null&&old.source()==source)return old.value();
        var spec=MiningContent.GEAR.get(stack.getItem());
        var value=source.stream().filter(c->!c.isEmpty()&&type(c)>=0&&(spec==null||EquipmentRules.compatible(spec.kind(),type(c)))).map(c->new EquipmentRules.Core(type(c),Math.clamp(level(c),1,type(c)==8?4:3))).toList();
        PROFILES.put(stack,new ProfileCache(source,value));return value;
    }
    public static EquipmentRules.Gear gear(ItemStack stack,int emptyKind){var spec=MiningContent.GEAR.get(stack.getItem());return spec==null?EquipmentRules.Gear.empty(emptyKind):new EquipmentRules.Gear(spec.tier(),spec.kind(),digit(stack),profile(stack));}
    public static EquipmentRules.Snapshot snapshot(LivingEntity owner){return new EquipmentRules.Snapshot(gear(ManuscriptEffects.effective(owner,owner.getMainHandStack()),0),gear(ManuscriptEffects.effective(owner,owner.getOffHandStack()),-1),gear(owner.getEquippedStack(EquipmentSlot.HEAD),2),gear(owner.getEquippedStack(EquipmentSlot.CHEST),3),gear(owner.getEquippedStack(EquipmentSlot.LEGS),4),gear(owner.getEquippedStack(EquipmentSlot.FEET),5),ManuscriptEffects.deep(owner.getWorld()));}
    public static int bookTier(ItemStack stack){var spec=MiningContent.GEAR.get(stack.getItem());return spec!=null&&spec.kind()==EquipmentRules.MANUSCRIPT?spec.tier():0;}
    public static int bookTier(LivingEntity owner){return owner==null?0:bookTier(ManuscriptEffects.held(owner));}
    public static boolean isBow(ItemStack stack){var spec=MiningContent.GEAR.get(stack.getItem());return spec!=null&&spec.kind()==EquipmentRules.BOW;}
    private static final String SILK_MODE="googology_silk_touch";
    public static boolean silkTouch(ItemStack stack){var spec=MiningContent.GEAR.get(stack.getItem());return spec!=null&&spec.kind()==0&&EquipmentRules.highest(profile(stack),2)>0&&stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt().getBoolean(SILK_MODE);}
    public static void setSilkTouch(ItemStack stack,boolean enabled){var spec=MiningContent.GEAR.get(stack.getItem());if(spec==null||spec.kind()!=0)return;var tag=stack.getOrDefault(DataComponentTypes.CUSTOM_DATA,net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();tag.putBoolean(SILK_MODE,enabled&&EquipmentRules.highest(profile(stack),2)>0);stack.set(DataComponentTypes.CUSTOM_DATA,net.minecraft.component.type.NbtComponent.of(tag));}
    public static int bookEfficiency(LivingEntity owner){return owner==null?0:EquipmentRules.efficiencyLevel(profile(ManuscriptEffects.held(owner)));}
    public static double healing(LivingEntity owner){return owner==null?0:EquipmentRules.regen(profile(ManuscriptEffects.held(owner)),EquipmentRules.bookHealingBase(bookTier(owner)));}
    public static double controlDuration(ItemStack weapon,LivingEntity owner){return EquipmentRules.controlDuration(profile(weapon),owner==null?List.of():profile(ManuscriptEffects.held(owner)),EquipmentRules.bookDurationBase(bookTier(owner)));}
    public static double bonusHealth(LivingEntity owner){var state=snapshot(owner);return EquipmentRules.bonusHealth(state.armor(),state.book(),state.deep());}
    public static boolean forbidsEnchantments(ItemStack stack){var spec=MiningContent.GEAR.get(stack.getItem());return spec!=null&&spec.tier()>0;}
    public static int capacity(ItemStack gear){var spec=MiningContent.GEAR.get(gear.getItem());return spec==null?0:EquipmentRules.slots(spec.tier(),spec.kind());}
    public static List<ItemStack> coreSlots(ItemStack s){return s.getOrDefault(MiningContent.CORES,List.of());}
    public static List<ItemStack> cores(ItemStack s){return coreSlots(s).stream().filter(c->!c.isEmpty()).toList();}
    public static int digit(ItemStack s){return EquipmentRules.digit(s.getOrDefault(MiningContent.DIGIT,0d));}
    public static int totalLevels(ItemStack s){int sum=0;for(var c:cores(s))sum+=EquipmentRules.denxiLevel(type(c),level(c));return sum;}
    public static double denxi(ItemStack s){return miningSpeed(s);}
    public static double miningSpeed(ItemStack s){var spec=MiningContent.GEAR.get(s.getItem());return spec==null||spec.kind()!=0?1:EquipmentRules.baseMining(spec.tier(),digit(s))*EquipmentRules.miningMultiplier(profile(s),List.of(),s.getOrDefault(MiningContent.DEEP,false));}
    public static boolean minesWithPick(ItemStack s,net.minecraft.block.BlockState state){var spec=MiningContent.GEAR.get(s.getItem());return spec!=null&&spec.kind()==0&&(state.isIn(net.minecraft.registry.tag.BlockTags.PICKAXE_MINEABLE)||state.getBlock() instanceof OrdinalOre);}
    public static double miningPoints(ItemStack s,net.minecraft.block.BlockState state){return minesWithPick(s,state)?points(s,8):0;}
    public static double wearPoints(ItemStack stack,LivingEntity owner){return wearFactor(stack,owner);}
    public static double wearFactor(ItemStack stack,LivingEntity owner){var spec=MiningContent.GEAR.get(stack.getItem());return EquipmentRules.wearFactor(spec!=null&&spec.kind()!=6?profile(stack):List.of(),List.of());}
    public static int wearCost(ItemStack stack,int amount,LivingEntity owner,java.util.function.DoubleSupplier random){
        var spec=MiningContent.GEAR.get(stack.getItem());
        if(spec!=null&&spec.kind()!=6&&stack.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION)refresh(stack);
        double factor=wearFactor(stack,owner);
        if(spec!=null&&spec.kind()>=2&&spec.kind()<=5&&owner!=null)factor*=ArmorProtectionContext.factor(owner);
        return EquipmentRules.wearCostForFactor(amount,factor,random);
    }
    public static void setCores(ItemStack s,List<ItemStack> slots){
        if(slots.size()>EquipmentRules.MAX_SOCKETS)throw new IllegalArgumentException("Too many sockets");
        var prior=coreSlots(s);boolean same=prior.size()==slots.size();
        for(int i=0;same&&i<slots.size();i++)same=ItemStack.areEqual(prior.get(i),slots.get(i));
        if(same)return;
        s.set(MiningContent.CORES,slots.stream().map(c->c.isEmpty()?ItemStack.EMPTY:c.copyWithCount(1)).toList());if(EquipmentRules.highest(profile(s),2)==0)setSilkTouch(s,false);refresh(s);
    }
    public static int type(ItemStack s){var id=Registries.ITEM.getId(s.getItem());return id.getNamespace().equals("googology")?EquipmentRules.coreType(id.getPath()):-1;}
    public static int level(ItemStack s){return EquipmentRules.coreLevel(Registries.ITEM.getId(s.getItem()).getPath());}
    public static double points(List<ItemStack> cores,int type){double sum=0;for(var c:cores)if(type(c)==type)sum+=EquipmentRules.weight(type,level(c));return sum;}
    public static double points(ItemStack gear,int type){return points(cores(gear),type);}
    public static double power(ItemStack s){var spec=MiningContent.GEAR.get(s.getItem());if(spec==null||spec.kind()==6||spec.kind()==EquipmentRules.BOW)return 0;if(spec.kind()>=2)return EquipmentRules.nativeArmor(spec.tier(),spec.kind());return EquipmentRules.attack(EquipmentRules.baseAttack(spec.tier(),spec.kind(),digit(s)),spec.kind()==1?profile(s):EquipmentRules.without(profile(s),1),List.of(),s.getOrDefault(MiningContent.DEEP,false));}
    public static double baseAttack(ItemStack s){var spec=MiningContent.GEAR.get(s.getItem());if(spec!=null&&spec.kind()<2)return EquipmentRules.baseAttack(spec.tier(),spec.kind(),digit(s));double[] a={1,0,1};s.applyAttributeModifiers(EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(EntityAttributes.GENERIC_ATTACK_DAMAGE))switch(modifier.operation()){case ADD_VALUE->a[0]+=modifier.value();case ADD_MULTIPLIED_BASE->a[1]+=modifier.value();case ADD_MULTIPLIED_TOTAL->a[2]*=1+modifier.value();}});return Math.max(.1,a[0]*(1+a[1])*a[2]);}
    public static double attackWithBook(ItemStack stack,LivingEntity owner){var spec=MiningContent.GEAR.get(stack.getItem());var own=spec!=null&&spec.kind()<2?(spec.kind()==1?profile(stack):EquipmentRules.without(profile(stack),1)):List.<EquipmentRules.Core>of();var book=profile(ManuscriptEffects.held(owner));int tier=bookTier(owner);return EquipmentRules.attack(baseAttack(stack),own,book,ManuscriptEffects.deep(owner.getWorld()),EquipmentRules.bookBase(tier),EquipmentRules.bookAttackHp(tier));}
    public static double bookAttackBonus(LivingEntity owner){var stack=owner.getMainHandStack();var spec=MiningContent.GEAR.get(stack.getItem());var own=spec!=null&&spec.kind()<2?(spec.kind()==1?profile(stack):EquipmentRules.without(profile(stack),1)):List.<EquipmentRules.Core>of();double withoutBook=EquipmentRules.attack(baseAttack(stack),own,List.of(),ManuscriptEffects.deep(owner.getWorld()));return attackWithBook(stack,owner)-withoutBook;}
    public static double bookReachBonus(LivingEntity owner){var stack=owner.getMainHandStack();var spec=MiningContent.GEAR.get(stack.getItem());var own=spec!=null&&spec.kind()<2?profile(stack):List.<EquipmentRules.Core>of();return EquipmentRules.reach(own,profile(ManuscriptEffects.held(owner)))-EquipmentRules.reach(own,List.of());}
    public static int yieldLevel(ItemStack stack,LivingEntity owner,boolean mob){var spec=MiningContent.GEAR.get(stack.getItem());var own=spec!=null&&spec.kind()==(mob?1:0)?profile(stack):List.<EquipmentRules.Core>of();return EquipmentRules.yieldLevel(own,owner==null?List.of():profile(ManuscriptEffects.held(owner)));}
    public static double protectionFactor(LivingEntity owner){var state=snapshot(owner);return EquipmentRules.protectionFactor(state.armor(),state.book(),state.deep(),EquipmentRules.bookBase(state.activeBook().tier()));}
    public static double defense(LivingEntity e){return 8*(protectionFactor(e)-1);}
    public static int armorDisplay(LivingEntity e){int total=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var spec=MiningContent.GEAR.get(e.getEquippedStack(slot).getItem());if(spec!=null)total+=EquipmentRules.visualArmor(spec.tier(),spec.kind());}return total;}
    public static AttributeModifiersComponent attributes(int tier,int kind,List<ItemStack> cores,double digit){return attributes(tier,kind,cores,digit,false);}
    public static AttributeModifiersComponent attributes(int tier,int kind,List<ItemStack> cores,double digit,boolean deep){
        var b=AttributeModifiersComponent.builder();
        AttributeModifierSlot slot=switch(kind){case 2->AttributeModifierSlot.HEAD;case 3->AttributeModifierSlot.CHEST;case 4->AttributeModifierSlot.LEGS;case 5->AttributeModifierSlot.FEET;default->AttributeModifierSlot.MAINHAND;};
        var profile=cores.stream().filter(c->!c.isEmpty()&&type(c)>=0&&EquipmentRules.compatible(kind,type(c))).map(c->new EquipmentRules.Core(type(c),level(c))).toList();
        if(kind<2){
            double power=EquipmentRules.attack(EquipmentRules.baseAttack(tier,kind,digit),kind==1?profile:EquipmentRules.without(profile,1),List.of(),deep);
            b.add(EntityAttributes.GENERIC_ATTACK_DAMAGE,new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,Math.min(2048,Math.max(.1,power))-1,EntityAttributeModifier.Operation.ADD_VALUE),slot);
            b.add(EntityAttributes.GENERIC_ATTACK_SPEED,new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,kind==1?-2.4:-2.8,EntityAttributeModifier.Operation.ADD_VALUE),slot);
        }else if(kind>=2&&kind<=5){
            b.add(EntityAttributes.GENERIC_ARMOR,new EntityAttributeModifier(GoogologyMod.id("armor_"+kind),EquipmentRules.nativeArmor(tier,kind),EntityAttributeModifier.Operation.ADD_VALUE),slot);
            b.add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS,new EntityAttributeModifier(GoogologyMod.id("toughness_"+kind),EquipmentRules.nativeToughness(tier,kind),EntityAttributeModifier.Operation.ADD_VALUE),slot);
        }
        return b.build();
    }
    public static void refresh(ItemStack s){
        var spec=MiningContent.GEAR.get(s.getItem());if(spec==null)return;
        if(forbidsEnchantments(s))s.set(net.minecraft.component.DataComponentTypes.ENCHANTMENTS,net.minecraft.component.type.ItemEnchantmentsComponent.DEFAULT);
        if(spec.kind()==6){s.set(MiningContent.RULES,EquipmentRules.REVISION);return;}
        if(spec.tier()==0)s.set(MiningContent.DIGIT,(double)digit(s));
        int priorMax=s.getMaxDamage(),damage=s.getDamage();
        int max=EquipmentRules.durability(spec.tier(),spec.kind(),digit(s));
        s.set(DataComponentTypes.MAX_DAMAGE,max);s.setDamage(EquipmentRules.rescaledDamage(damage,priorMax,max));
        s.set(MiningContent.RULES,EquipmentRules.REVISION);
        s.set(DataComponentTypes.ATTRIBUTE_MODIFIERS,attributes(spec.tier(),spec.kind(),cores(s),s.getOrDefault(MiningContent.DIGIT,0d),s.getOrDefault(MiningContent.DEEP,false)));
    }
    /** Returns an error translation key, or null. Validation happens again on every click. */
    public static String installationError(ItemStack gear,ItemStack core,int station){
        var spec=MiningContent.GEAR.get(gear.getItem());int type=type(core);
        if(spec==null||type<0)return "invalid";
        if(!EquipmentRules.compatible(spec.kind(),type))return "incompatible";
        int grade=EquipmentRules.stationGrade(type,level(core));
        if(grade>station||grade>EquipmentRules.grade(spec.tier()))return "grade";
        if(cores(gear).size()>=EquipmentRules.slots(spec.tier(),spec.kind()))return "full";
        return null;
    }
    public static String install(ItemStack gear,ItemStack core,int station){
        String error=installationError(gear,core,station);if(error!=null)return error;
        var list=new ArrayList<>(coreSlots(gear));
        int capacity=capacity(gear);
        while(list.size()<capacity)list.add(ItemStack.EMPTY);
        for(int i=0;i<capacity;i++)if(list.get(i).isEmpty()){list.set(i,core.copyWithCount(1));break;}
        setCores(gear,list);core.decrement(1);return null;
    }
    public static ItemStack remove(ItemStack gear,int index){
        if(index<0||index>=coreSlots(gear).size())return ItemStack.EMPTY;
        var list=new ArrayList<>(coreSlots(gear));var result=list.set(index,ItemStack.EMPTY).copy();setCores(gear,list);return result;
    }
}
