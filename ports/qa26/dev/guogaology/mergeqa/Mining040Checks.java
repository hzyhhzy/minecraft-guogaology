package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import java.util.*;

/** Actual 26.2 player/ItemStack/mixin regressions for the 0.3.10 mining changes. */
public final class Mining040Checks {
    private static int checks;
    private Mining040Checks(){}
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    private static void near(double a,double b,String label){check(Math.abs(a-b)<Math.max(.0001,Math.abs(b)*.00001),label+": "+a+" != "+b);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static ItemStack gear(String id,String core,int count){var s=item(id);GearData.setCores(s,Collections.nCopies(count,item(core)));GearData.refresh(s);return s;}
    private static void move(ServerPlayer p,ServerLevel level){
        var pos=new net.minecraft.core.BlockPos(512,250,512);level.getChunk(pos.getX()>>4,pos.getZ()>>4);
        for(var q:net.minecraft.core.BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,3,2)))level.setBlock(q,q.getY()==249?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        p.teleport(new TeleportTransition(level,Vec3.atBottomCenterOf(pos),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.setOnGround(true);
    }
    public static void run(ServerPlayer p){
        checks=0;
        var level=p.level();var position=p.position();var gameMode=p.gameMode();boolean ground=p.onGround();
        int oldTicks=p.tickCount,oldXp=p.totalExperience,oldXpLevel=p.experienceLevel,oldXpDelay=p.takeXpDelay;float oldXpProgress=p.experienceProgress;
        var saved=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);
        for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))saved.put(slot,p.getItemBySlot(slot));
        var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var efficiency=p.getAttribute(Attributes.MINING_EFFICIENCY);var breakSpeed=p.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        var efficiencyKey=Identifier.parse("guogaology:qa_mining_efficiency");var speedKey=Identifier.parse("guogaology:qa_break_speed");
        try{
            for(var slot:saved.keySet())p.setItemSlot(slot,ItemStack.EMPTY);
            p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);move(p,p.level().getServer().overworld());
            harvesting();durabilityCapacity(p);speed(p,efficiency,breakSpeed,efficiencyKey,speedKey);wear(p);disabledLaver(p);breakReturns(p);
            System.out.println("MINING040_CHECKS_OK checks="+checks+" fixed capacity / one wear filter / vanilla Unbreaking / combined mining / Laver inactive");
        }finally{
            efficiency.removeModifier(efficiencyKey);breakSpeed.removeModifier(speedKey);
            p.removeAllEffects();for(var effect:effects)p.addEffect(effect);
            for(var slot:saved.keySet())p.setItemSlot(slot,saved.get(slot));
            p.tickCount=oldTicks;p.totalExperience=oldXp;p.experienceLevel=oldXpLevel;p.experienceProgress=oldXpProgress;p.takeXpDelay=oldXpDelay;
            p.setGameMode(gameMode);p.teleport(new TeleportTransition(level,position,Vec3.ZERO,p.getYRot(),p.getXRot(),TeleportTransition.DO_NOTHING));p.setOnGround(ground);
            ManuscriptEffects.tick(p);
        }
    }
    private static void harvesting(){
        for(int digit=0;digit<=9;digit++){
            var pick=item("number_pickaxe");pick.set(MiningContent.DIGIT,(double)digit);GearData.refresh(pick);
            check(pick.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()),"digit "+digit+" mines stone");
            check(pick.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()),"digit "+digit+" has stone harvest level");
            check(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())==(digit>=5),"digit "+digit+" respects iron harvest gate");
            check(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())==(digit>=8),"digit "+digit+" respects diamond harvest gate");
            for(var ores:MiningContent.ORES)for(var ore:ores)check(pick.isCorrectToolForDrops(ore.defaultBlockState())==(digit>=5),"digit "+digit+" respects four-tier ore gate");
            check(!pick.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()),"pick is not an axe");
        }
        for(int tier=0;tier<4;tier++)check(new ItemStack(MiningContent.TOOLS[tier+1][0]).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),"mineral pick capability retained "+tier);
    }
    private static void durabilityCapacity(ServerPlayer p){
        for(var entry:MiningContent.GEAR.entrySet()){
            var spec=entry.getValue();if(spec.kind()==6)continue;
            var stack=new ItemStack(entry.getKey());GearData.refresh(stack);int base=stack.getMaxDamage();
            stack.setDamageValue(57);
            GearData.setCores(stack,Collections.nCopies(GearData.capacity(stack),item("hydra_bud")));
            check(stack.getMaxDamage()==base&&stack.getDamageValue()==57,"branch leaves base capacity and wear untouched "+entry.getKey());
            for(int i=0;i<20;i++)GearData.refresh(stack);
            check(stack.getDamageValue()==57,"repeated refresh never adds floating-point wear "+entry.getKey());
            GearData.remove(stack,0);check(stack.getMaxDamage()==base&&stack.getDamageValue()==57,"removal preserves existing wear");
        }
        var old=gear("true_omega_pickaxe","hydra_bud_lv3",8);int base=old.getMaxDamage(),oldMax=base*289;
        old.set(DataComponents.MAX_DAMAGE,oldMax);old.setDamageValue(oldMax/3);old.set(MiningContent.RULES,33);
        GearData.refresh(old);
        check(old.getMaxDamage()==base&&old.getDamageValue()==base/3,"old high-capacity equipment migrates by exact wear ratio");
        old.setDamageValue(57);old.set(MiningContent.DEEP,true);GearData.refresh(old);old.set(MiningContent.DEEP,false);GearData.refresh(old);
        check(old.getMaxDamage()==base&&old.getDamageValue()==57,"realm profile changes do not change capacity or wear");
        var book=gear("true_omega_manuscript","ordinal_crystal_lv4",2);var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);vanilla.setDamageValue(20);
        p.setItemSlot(EquipmentSlot.MAINHAND,vanilla);p.setItemSlot(EquipmentSlot.OFFHAND,book);p.tickCount=200;ManuscriptEffects.tick(p);
        check(vanilla.getDamageValue()==20,"ordinal manuscript no longer repairs old damage");
    }
    private static void speed(ServerPlayer p,AttributeInstance efficiency,AttributeInstance breakSpeed,Identifier efficiencyKey,Identifier speedKey){
        var pick=gear("true_omega_pickaxe","ordinal_crystal_lv2",4);var book=gear("true_omega_manuscript","sequence_core_lv2",2);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);
        ((OrdinalGear)pick.getItem()).inventoryTick(pick,p.level(),p,EquipmentSlot.MAINHAND);
        efficiency.addTransientModifier(new AttributeModifier(efficiencyKey,5,AttributeModifier.Operation.ADD_VALUE));
        breakSpeed.addTransientModifier(new AttributeModifier(speedKey,.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,1));
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),(18+p.getAttributeValue(Attributes.MINING_EFFICIENCY))*1.4*p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)*EquipmentRules.multiplier(7,false),"tool + manuscript cores preserve Efficiency / Haste / attribute speed");
        var filled=gear("true_omega_pickaxe","ordinal_crystal_lv4",8);p.setItemSlot(EquipmentSlot.MAINHAND,filled);
        near(p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),1.4*p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)*EquipmentRules.multiplier(3,false),"full ordinal pick does not cancel manuscript on dirt");
        near(p.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()),p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),"non-pick blocks have no phantom tool core divisor");
        efficiency.removeModifier(efficiencyKey);breakSpeed.removeModifier(speedKey);p.removeEffect(MobEffects.HASTE);
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),6*EquipmentRules.multiplier(3,false),"manuscript accelerates vanilla tools");
        move(p,p.level().getServer().getLevel(GuogaologyMod.DIMENSION));p.setItemSlot(EquipmentSlot.MAINHAND,filled);
        ((OrdinalGear)filled.getItem()).inventoryTick(filled,p.level(),p,EquipmentSlot.MAINHAND);
        near(p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),EquipmentRules.multiplier(3,true),"deep manuscript speed on non-pick blocks");
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),18*256,"combined deep points cap once");
        move(p,p.level().getServer().overworld());p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    }
    private static void wear(ServerPlayer p){
        var book=gear("true_omega_manuscript","ordinal_crystal_lv4",2);var pick=gear("true_omega_pickaxe","hydra_bud",2);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);
        near(GearData.wearPoints(pick,p),6,"own branch and offhand ordinal points sum before protection");
        near(EquipmentRules.durabilityFactor(GearData.wearPoints(pick,p)),169,"one shared protection factor");
        pick.set(MiningContent.DEEP,true);near(EquipmentRules.durabilityFactor(GearData.wearPoints(pick,p)),169,"deep profile cannot alter wear protection");pick.set(MiningContent.DEEP,false);
        near(GearData.wearPoints(pick,null),2,"ownerless stack damage has own branch protection only");
        var loose=new ItemStack(Items.DIAMOND_PICKAXE);near(GearData.wearPoints(loose,p),0,"backpack stacks do not inherit manuscript protection");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        pick=gear("true_omega_pickaxe","hydra_bud",1);p.setItemSlot(EquipmentSlot.MAINHAND,pick);pick.hurtAndBreak(9000,p,EquipmentSlot.MAINHAND);
        check(!pick.isEmpty()&&pick.getDamageValue()>800&&pick.getDamageValue()<1200,"tool wear filtered exactly once, expected one ninth");
        var armor=gear("true_omega_chestplate","hydra_bud",1);p.setItemSlot(EquipmentSlot.CHEST,armor);armor.hurtAndBreak(4500,p,EquipmentSlot.CHEST);
        check(!armor.isEmpty()&&armor.getDamageValue()>350&&armor.getDamageValue()<650,"armor uses the same single filter");p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        var unbreaking=gear("true_omega_pickaxe","hydra_bud",1);unbreaking.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING),3);
        p.setItemSlot(EquipmentSlot.MAINHAND,unbreaking);unbreaking.hurtAndBreak(9000,p,EquipmentSlot.MAINHAND);
        check(unbreaking.getDamageValue()>150&&unbreaking.getDamageValue()<350,"vanilla Unbreaking independently stacks with core wear protection");
        var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);p.setItemSlot(EquipmentSlot.MAINHAND,vanilla);p.setItemSlot(EquipmentSlot.OFFHAND,gear("true_omega_manuscript","ordinal_crystal_lv2",1));
        vanilla.hurtAndBreak(9000,p,EquipmentSlot.MAINHAND);
        check(!vanilla.isEmpty()&&vanilla.getDamageValue()>800&&vanilla.getDamageValue()<1200,"manuscript protects vanilla main-hand equipment");
        vanilla.setDamageValue(0);vanilla.hurtWithoutBreaking(9000,p);
        check(vanilla.getDamageValue()>800&&vanilla.getDamageValue()<1200,"nonbreaking wear shares protection exactly once");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    }
    private static void disabledLaver(ServerPlayer p){
        for(var entry:MiningContent.GEAR.entrySet()){
            var stack=new ItemStack(entry.getKey());check(GearData.installationError(stack,item("laver_core"),3).equals("incompatible"),"Laver cannot be newly installed "+entry.getKey());
        }
        var pick=gear("true_omega_pickaxe","laver_core_lv3",1);var book=gear("true_omega_manuscript","laver_core_lv3",1);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);int xp=p.totalExperience;p.takeXpDelay=0;
        var orb=new ExperienceOrb(p.level(),p.getX(),p.getY(),p.getZ(),7);orb.playerTouch(p);
        check(p.totalExperience-xp==7,"legacy Laver core does not modify vanilla experience");
        check(GearData.remove(pick,0).is(item("laver_core_lv3").getItem())&&GearData.cores(pick).isEmpty(),"legacy Laver can be removed intact");
        check(GearData.remove(book,0).is(item("laver_core_lv3").getItem())&&GearData.cores(book).isEmpty(),"legacy manuscript Laver can be removed intact");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    }
    private static void breakReturns(ServerPlayer p){
        var pick=gear("omega_pickaxe","sequence_core",1);GearData.setCores(pick,List.of(item("sequence_core"),item("ordinal_crystal")));
        pick.setDamageValue(pick.getMaxDamage()-1);p.setItemSlot(EquipmentSlot.MAINHAND,pick);
        var old=new HashSet<UUID>();for(var e:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3)))old.add(e.getUUID());
        pick.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);
        var drops=p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3),e->!old.contains(e.getUUID()));
        check(pick.isEmpty()&&drops.size()==2,"broken equipment returns each installed core once");
        check(drops.stream().anyMatch(e->e.getItem().is(item("sequence_core").getItem()))&&drops.stream().anyMatch(e->e.getItem().is(item("ordinal_crystal").getItem())),"dropped cores retain their original identities");
        for(var e:drops)e.discard();
    }
}
