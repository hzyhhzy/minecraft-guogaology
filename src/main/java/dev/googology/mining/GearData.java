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
    public static int capacity(ItemStack gear){var spec=MiningContent.GEAR.get(gear.getItem());return spec==null?0:EquipmentRules.slots(spec.tier(),spec.kind());}
    public static List<ItemStack> coreSlots(ItemStack s){return s.getOrDefault(MiningContent.CORES,List.of());}
    public static List<ItemStack> cores(ItemStack s){return coreSlots(s).stream().filter(c->!c.isEmpty()).toList();}
    public static int digit(ItemStack s){return EquipmentRules.digit(s.getOrDefault(MiningContent.DIGIT,0d));}
    public static int totalLevels(ItemStack s){int sum=0;for(var c:cores(s))sum+=EquipmentRules.denxiLevel(type(c),level(c));return sum;}
    public static double denxi(ItemStack s){return miningSpeed(s);}
    public static double miningSpeed(ItemStack s){var spec=MiningContent.GEAR.get(s.getItem());return spec==null||spec.kind()!=0?1:EquipmentRules.baseMining(spec.tier(),digit(s))*EquipmentRules.multiplier(points(s,8),s.getOrDefault(MiningContent.DEEP,false));}
    public static void setCores(ItemStack s,List<ItemStack> slots){
        if(slots.size()>EquipmentRules.MAX_SOCKETS)throw new IllegalArgumentException("Too many sockets");
        var prior=coreSlots(s);boolean same=prior.size()==slots.size();
        for(int i=0;same&&i<slots.size();i++)same=ItemStack.areEqual(prior.get(i),slots.get(i));
        if(same)return;
        s.set(MiningContent.CORES,slots.stream().map(c->c.isEmpty()?ItemStack.EMPTY:c.copyWithCount(1)).toList());refresh(s);
    }
    public static int type(ItemStack s){var id=Registries.ITEM.getId(s.getItem());return id.getNamespace().equals("googology")?EquipmentRules.coreType(id.getPath()):-1;}
    public static int level(ItemStack s){return EquipmentRules.coreLevel(Registries.ITEM.getId(s.getItem()).getPath());}
    public static double points(List<ItemStack> cores,int type){double sum=0;for(var c:cores)if(type(c)==type)sum+=EquipmentRules.weight(type,level(c));return sum;}
    public static double points(ItemStack gear,int type){return points(cores(gear),type);}
    public static double power(ItemStack s){var spec=MiningContent.GEAR.get(s.getItem());if(spec==null||spec.kind()==6)return 0;double base=spec.kind()>=2?EquipmentRules.basePower(spec.tier()):EquipmentRules.baseAttack(spec.tier(),spec.kind(),digit(s));return base*EquipmentRules.multiplier(points(s,8)+(spec.kind()==1?points(s,1):spec.kind()>=2?points(s,6):0),s.getOrDefault(MiningContent.DEEP,false));}
    public static double defense(LivingEntity e){
        double base=0,points=0,ownHealth=0;
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var s=e.getEquippedStack(slot);var spec=MiningContent.GEAR.get(s.getItem());if(spec!=null&&spec.kind()>=2&&spec.kind()<6){double share=EquipmentRules.armorShare(spec.kind());base+=EquipmentRules.basePower(spec.tier())*share;points+=(points(s,6)+points(s,7)+points(s,8))*share;ownHealth+=60*Math.min(1,points(s,7)/16)*share;}}
        points+=ManuscriptEffects.points(e,6)+ManuscriptEffects.points(e,7);
        double multiplier=EquipmentRules.multiplier(points,ManuscriptEffects.deep(e.getWorld()));
        double healthRatio=1+(ownHealth+Math.min(20,2*ManuscriptEffects.points(e,7)))/20;
        return 8*Math.max(0,(1+base/8)*multiplier/healthRatio-1);
    }
    public static int armorDisplay(LivingEntity e){int total=0;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){var spec=MiningContent.GEAR.get(e.getEquippedStack(slot).getItem());if(spec!=null)total+=EquipmentRules.visualArmor(spec.tier(),spec.kind());}return total;}
    public static AttributeModifiersComponent attributes(int tier,int kind,List<ItemStack> cores,double digit){return attributes(tier,kind,cores,digit,false);}
    public static AttributeModifiersComponent attributes(int tier,int kind,List<ItemStack> cores,double digit,boolean deep){
        var b=AttributeModifiersComponent.builder();
        AttributeModifierSlot slot=switch(kind){case 2->AttributeModifierSlot.HEAD;case 3->AttributeModifierSlot.CHEST;case 4->AttributeModifierSlot.LEGS;case 5->AttributeModifierSlot.FEET;default->AttributeModifierSlot.MAINHAND;};
        double base=kind>=2?EquipmentRules.basePower(tier):EquipmentRules.baseAttack(tier,kind,digit);
        double power=base*EquipmentRules.multiplier(points(cores,8)+(kind==1?points(cores,1):0),deep);
        if(kind<2){
            b.add(EntityAttributes.GENERIC_ATTACK_DAMAGE,new EntityAttributeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID,Math.min(1023,Math.max(.1,power))-1,EntityAttributeModifier.Operation.ADD_VALUE),slot);
            b.add(EntityAttributes.GENERIC_ATTACK_SPEED,new EntityAttributeModifier(Item.BASE_ATTACK_SPEED_MODIFIER_ID,kind==1?-2.4:-2.8,EntityAttributeModifier.Operation.ADD_VALUE),slot);
            double reach=.5*points(cores,3);
            b.add(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE,new EntityAttributeModifier(GoogologyMod.id("reach"),reach,EntityAttributeModifier.Operation.ADD_VALUE),slot);
            b.add(EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE,new EntityAttributeModifier(GoogologyMod.id("reach"),reach,EntityAttributeModifier.Operation.ADD_VALUE),slot);
        }else {b.add(EntityAttributes.GENERIC_ARMOR,new EntityAttributeModifier(GoogologyMod.id("armor_"+kind),EquipmentRules.visualArmor(tier,kind),EntityAttributeModifier.Operation.ADD_VALUE),slot);b.add(EntityAttributes.GENERIC_MAX_HEALTH,new EntityAttributeModifier(GoogologyMod.id("health_"+kind),60*Math.min(1,points(cores,7)/16)*EquipmentRules.armorShare(kind),EntityAttributeModifier.Operation.ADD_VALUE),slot);}
        return b.build();
    }
    public static void refresh(ItemStack s){
        var spec=MiningContent.GEAR.get(s.getItem());if(spec==null)return;
        if(spec.kind()==6){s.set(MiningContent.RULES,EquipmentRules.REVISION);return;}
        if(spec.tier()==0)s.set(MiningContent.DIGIT,(double)digit(s));
        double worn=s.getMaxDamage()==0?0:(double)s.getDamage()/s.getMaxDamage();
        int max=(int)Math.round(EquipmentRules.durability(spec.tier(),spec.kind(),digit(s))*Math.pow(1+points(s,2),2));
        s.set(DataComponentTypes.MAX_DAMAGE,max);s.setDamage(Math.min(max-1,(int)Math.ceil(worn*max)));
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
