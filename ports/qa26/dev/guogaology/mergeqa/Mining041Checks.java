package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import java.util.*;

/** Real ItemStack, player, damage and mixin regressions for the final core roles. */
public final class Mining041Checks {
    private static int checks;
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    private static void near(double a,double b,String label){check(Math.abs(a-b)<Math.max(.0001,Math.abs(b)*.00001),label+": "+a+" != "+b);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+id)));}
    private static ItemStack gear(String id,String core,int count){var s=item(id);GearData.setCores(s,Collections.nCopies(count,item(core)));GearData.refresh(s);return s;}
    private static void move(ServerPlayer p,ServerLevel level){
        var pos=new BlockPos(512,250,512);level.getChunk(pos.getX()>>4,pos.getZ()>>4);
        for(var q:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,3,2)))level.setBlock(q,q.getY()==249?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        p.teleport(new TeleportTransition(level,Vec3.atBottomCenterOf(pos),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());check(!p.isChangingDimension()&&p.connection.hasClientLoaded(),"fixture transfer handshake ready");p.setOnGround(true);
    }
    public static void run(ServerPlayer p){
        checks=0;var oldLevel=p.level();var oldPos=p.position();var oldMode=p.gameMode();int oldTicks=p.tickCount;float oldHealth=p.getHealth();
        var saved=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);
        for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))saved.put(slot,p.getItemBySlot(slot));
        var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        try {
            for(var slot:saved.keySet())p.setItemSlot(slot,ItemStack.EMPTY);p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);move(p,p.level().getServer().overworld());
            harvest();capacityAndCompatibility(p);speed(p);wear(p);healingAndPassives(p);defense(p);breakReturns(p);
            System.out.println("MINING041_CHECKS_OK checks="+checks+" harvest / shared speed / Laver wear and healing / passives / void and sonic protection / returned cores");
        } finally {
            p.closeContainer();p.removeAllEffects();for(var effect:effects)p.addEffect(effect);
            for(var slot:saved.keySet())p.setItemSlot(slot,saved.get(slot));p.tickCount=oldTicks;
            p.setGameMode(oldMode);p.teleport(new TeleportTransition(oldLevel,oldPos,Vec3.ZERO,p.getYRot(),p.getXRot(),TeleportTransition.DO_NOTHING));p.setOnGround(true);ManuscriptEffects.tick(p);p.setHealth(Math.min(oldHealth,p.getMaxHealth()));
        }
    }
    private static void harvest(){
        for(int digit=0;digit<=9;digit++){
            var pick=item("number_pickaxe");pick.set(MiningContent.DIGIT,(double)digit);GearData.refresh(pick);
            check(pick.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()),"digit mines stone "+digit);
            check(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())==(digit>=5),"iron gate "+digit);
            check(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())==(digit>=8),"diamond gate "+digit);
            check(!GearData.forbidsEnchantments(pick),"numeric tools remain enchantable "+digit);
        }
    }
    private static void capacityAndCompatibility(ServerPlayer p){
        for(var entry:MiningContent.GEAR.entrySet()){
            var spec=entry.getValue();var stack=new ItemStack(entry.getKey());GearData.refresh(stack);
            check(GearData.installationError(stack,item("laver_core"),3)==null,"Laver enabled on gear/book "+entry.getKey());
            check(GearData.forbidsEnchantments(stack)==(spec.tier()>0),"only ordinal series forbidden enchants");
            if(spec.kind()==6){check(!stack.isDamageableItem()&&stack.getMaxDamage()==0,"book no durability");continue;}
            int max=stack.getMaxDamage();stack.setDamageValue(57);GearData.setCores(stack,List.of(item("laver_core")));
            for(int i=0;i<20;i++)GearData.refresh(stack);
            check(stack.getMaxDamage()==max&&stack.getDamageValue()==57,"refresh preserves base capacity and damage");
            GearData.remove(stack,0);check(stack.getMaxDamage()==max&&stack.getDamageValue()==57,"removal preserves wear");
        }
        var pick=gear("true_omega_pickaxe","sequence_core_lv3",2);var book=gear("true_omega_manuscript","sequence_core_lv3",3);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);
        near(GearData.yieldLevel(pick,p,false),15,"yield tool and book sum");
        check(GearData.installationError(item("true_omega_sword"),item("hydra_bud"),3).equals("incompatible"),"branch is no longer sword durability");
        var enchantment=p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        var numeric=item("number_pickaxe");numeric.enchant(enchantment,3);GearData.refresh(numeric);check(numeric.isEnchanted(),"numeric enchant retained");
        pick.enchant(enchantment,3);GearData.refresh(pick);check(!pick.isEnchanted(),"ordinal enchant stripped at canonical refresh");
    }
    private static void speed(ServerPlayer p){
        var pick=gear("true_omega_pickaxe","ordinal_crystal_lv2",4);var book=gear("true_omega_manuscript","hydra_bud_lv2",2);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);
        ((OrdinalGear)pick.getItem()).inventoryTick(pick,p.level(),p,EquipmentSlot.MAINHAND);p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,1));
        double m=Math.sqrt(4.5)+.24;
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),(18+p.getAttributeValue(Attributes.MINING_EFFICIENCY))*1.4*p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)*m,"preserve base/Haste and combine ordinal once");
        near(p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),1.4*p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED)*Math.sqrt(4.5),"book boost survives off-pick block");
        p.removeEffect(MobEffects.HASTE);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),6*Math.sqrt(4.5),"book boost vanilla pick");
        move(p,p.level().getServer().getLevel(GuogaologyMod.DIMENSION));p.setItemSlot(EquipmentSlot.MAINHAND,pick);((OrdinalGear)pick.getItem()).inventoryTick(pick,p.level(),p,EquipmentSlot.MAINHAND);
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),18*4.5*(1+2*.48),"deep speed gets one universal multiplier");
        near(p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),4.5,"deep book affects dirt");
        move(p,p.level().getServer().overworld());p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    }
    private static void wear(ServerPlayer p){
        var pick=gear("true_omega_pickaxe","laver_core",2);var book=gear("true_omega_manuscript","ordinal_crystal_lv4",2);
        p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book);
        near(GearData.wearFactor(pick,p),32+16*Math.sqrt(.5),"wear and ordinal book combine once");
        near(GearData.wearFactor(pick,null),32,"ownerless wear uses own Laver");
        near(GearData.wearFactor(new ItemStack(Items.DIAMOND_PICKAXE),p),1,"loose stacks do not inherit book");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);pick=gear("true_omega_pickaxe","laver_core",1);p.setItemSlot(EquipmentSlot.MAINHAND,pick);pick.hurtAndBreak(9000,p,EquipmentSlot.MAINHAND);
        check(!pick.isEmpty()&&pick.getDamageValue()>460&&pick.getDamageValue()<680,"real wear filter once at 1/16");
        var armor=gear("true_omega_chestplate","laver_core",1);p.setItemSlot(EquipmentSlot.CHEST,armor);armor.hurtAndBreak(4500,p,EquipmentSlot.CHEST);
        check(!armor.isEmpty()&&armor.getDamageValue()>205&&armor.getDamageValue()<365,"same armor wear filter");p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.OFFHAND,book);var vanilla=new ItemStack(Items.DIAMOND_PICKAXE);p.setItemSlot(EquipmentSlot.MAINHAND,vanilla);
        near(GearData.wearFactor(vanilla,p),1+16*Math.sqrt(.5),"held vanilla inherits universal book wear");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
    }
    private static void healingAndPassives(ServerPlayer p){
        var book=item("true_omega_manuscript");GearData.setCores(book,List.of(item("laver_core_lv2"),item("laver_core_lv3")));p.setItemSlot(EquipmentSlot.OFFHAND,book);p.setHealth(5);p.tickCount=80;ManuscriptEffects.tick(p);
        near(p.getHealth(),5+Math.sqrt(13),"healing 4-second RMS");check(p.hasEffect(MobEffects.NIGHT_VISION),"Laver Lv2+ night vision");
        p.setItemSlot(EquipmentSlot.OFFHAND,gear("true_omega_manuscript","astra_critical_core_lv2",1));p.tickCount=81;ManuscriptEffects.tick(p);check(p.hasEffect(MobEffects.FIRE_RESISTANCE),"critical Lv2+ fire resistance");
        var hp=gear("true_omega_manuscript","guogao_heart_lv3",3);p.setItemSlot(EquipmentSlot.OFFHAND,hp);ManuscriptEffects.tick(p);near(p.getMaxHealth(),20+12*Math.sqrt(3),"health no old 20 bonus cap");
        p.addEffect(new MobEffectInstance(MobEffects.POISON,200));p.addEffect(new MobEffectInstance(MobEffects.WITHER,200));ManuscriptEffects.tick(p);check(!p.hasEffect(MobEffects.POISON)&&!p.hasEffect(MobEffects.WITHER),"Lv3 white-list immunity");
        p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(p);near(p.getMaxHealth(),20,"removed HP modifier restored");p.removeAllEffects();p.setHealth(20);
    }
    private static void defense(ServerPlayer p){
        p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        for(var pair:List.of(new Object[]{EquipmentSlot.HEAD,"true_omega_helmet"},new Object[]{EquipmentSlot.CHEST,"true_omega_chestplate"},new Object[]{EquipmentSlot.LEGS,"true_omega_leggings"},new Object[]{EquipmentSlot.FEET,"true_omega_boots"}))p.setItemSlot((EquipmentSlot)pair[0],item((String)pair[1]));
        for(String source:List.of("generic","sonic","void")){
            p.setHealth(20);p.setAbsorptionAmount(0);p.invulnerableTime=0;
            var damage=switch(source){case "sonic"->new net.minecraft.world.damagesource.DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM));case "void"->p.damageSources().fellOutOfWorld();default->p.damageSources().generic();};
            check(p.hurtServer(p.level(),damage,8),"damage applied "+source);near(20-p.getHealth(),2,"base protection includes "+source);
        }
        p.setHealth(20);for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);ManuscriptEffects.tick(p);
    }
    private static void breakReturns(ServerPlayer p){
        var pick=item("omega_pickaxe");GearData.setCores(pick,List.of(item("sequence_core"),item("ordinal_crystal")));pick.setDamageValue(pick.getMaxDamage()-1);p.setItemSlot(EquipmentSlot.MAINHAND,pick);
        var old=new HashSet<UUID>();for(var e:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3)))old.add(e.getUUID());
        // A level-one universal also protects wear; repeat until the genuine break occurs.
        for(int i=0;i<100&&!pick.isEmpty();i++)pick.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);
        var drops=p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3),e->!old.contains(e.getUUID()));
        check(pick.isEmpty()&&drops.size()==2,"broken gear returns cores exactly once");
        check(drops.stream().anyMatch(e->e.getItem().is(item("sequence_core").getItem()))&&drops.stream().anyMatch(e->e.getItem().is(item("ordinal_crystal").getItem())),"returned identities");for(var e:drops)e.discard();
    }
}
