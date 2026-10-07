package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.*;
import com.google.gson.JsonParser;
import java.util.*;

/** Rev47 real server inventories, native attributes, loot, damage and targeting paths. */
public final class Core047Checks {
    private static int checks;
    private static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final String[] TIERS={"omega","epsilon","gamma","true_omega"};
    private static final String[] ARMOR={"helmet","chestplate","leggings","boots"};
    private static final EquipmentSlot[] ARMOR_SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<Math.max(.003,Math.abs(expected)*.00002),label+": "+actual+" != "+expected);}
    private static ItemStack item(String id){var value=BuiltInRegistries.ITEM.getValue(Identifier.parse("googology:"+id));check(value!=null&&value!=Items.AIR,"registered fixture "+id);return new ItemStack(value);}
    private static ItemStack gear(String id,String... cores){var value=item(id);GearData.setCores(value,Arrays.stream(cores).map(Core047Checks::item).toList());GearData.refresh(value);return value;}
    private static ItemStack book(String... cores){return gear("true_omega_manuscript",cores);}
    private static void equipment(ServerPlayer p)throws Exception{
        // Use vanilla's normal equipment-change path so the assertion catches duplicate material attributes.
        var method=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");method.setAccessible(true);method.invoke(p);
        ManuscriptEffects.tick(p);
    }
    private static void empty(ServerPlayer p)throws Exception{for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);equipment(p);p.removeAllEffects();p.setAbsorptionAmount(0);p.setHealth(p.getMaxHealth());p.invulnerableTime=0;}
    private static void move(ServerPlayer p,ServerLevel world){world.getChunk(0,1);p.teleport(new TeleportTransition(world,new Vec3(8.5,250,24.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());p.setOnGround(true);check(!p.isChangingDimension()&&p.connection.hasClientLoaded(),"real realm transfer handshake "+world.dimension());}
    public static void run(ServerPlayer p)throws Exception{
        checks=0;var oldWorld=p.level();var oldPos=p.position();var oldVelocity=p.getDeltaMovement();var oldMode=p.gameMode();int oldTick=p.tickCount,oldAir=p.getAirSupply(),oldInvulnerable=p.invulnerableTime;float oldHealth=p.getHealth(),oldAbsorption=p.getAbsorptionAmount();boolean oldGravity=p.isNoGravity(),oldGround=p.onGround();
        var savedInventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)savedInventory.add(p.getInventory().getItem(i));int selected=p.getInventory().getSelectedSlot();var effects=List.copyOf(p.getActiveEffects());int food=p.getFoodData().getFoodLevel();float saturation=p.getFoodData().getSaturationLevel();
        try{
            p.closeContainer();for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);empty(p);p.tickCount=81;
            Manuscript047Checks.run(p);
            rarity(p);sanctuaryLoot(p);compatibilityAndHarvest(p);silkMenu(p);lootAndMining(p);
            var server=p.level().getServer();var worlds=List.of(server.overworld(),server.getLevel(GoogologyMod.OUTER),server.getLevel(GoogologyMod.DIMENSION),server.getLevel(GoogologyMod.GUOGAO));
            for(int i=0;i<worlds.size();i++){var world=worlds.get(i);check(world!=null,"four real dimensions exist "+i);move(p,world);empty(p);check(ManuscriptEffects.deep(world)==(i>=2)&&GearData.snapshot(p).deep()==(i>=2),"dimension depth "+i);armorAndDamage(p,i>=2);healthAndHandPriority(p,i>=2);}
            move(p,server.overworld());empty(p);wearAndBreak(p);water(p);passives(p);targeting(p);
            Bow047Checks.run(p);
            System.out.println("CORE047_CHECKS_OK checks="+checks+" native armor/toughness and weighted protection in four worlds / sonic+void / durability / book hand priority / real loot+silk menu / water+stealth / 27 flat icons+original OrdinalLv1+28 rarities");
        }finally{
            p.closeContainer();p.containerMenu=p.inventoryMenu;empty(p);for(int i=0;i<savedInventory.size();i++)p.getInventory().setItem(i,savedInventory.get(i));p.getInventory().setSelectedSlot(selected);p.setGameMode(oldMode);p.teleport(new TeleportTransition(oldWorld,oldPos,oldVelocity,p.getYRot(),p.getXRot(),TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());p.tickCount=oldTick;equipment(p);p.removeAllEffects();for(var effect:effects)p.addEffect(effect);p.getFoodData().setFoodLevel(food);p.getFoodData().setSaturation(saturation);p.setNoGravity(oldGravity);p.setOnGround(oldGround);p.setAirSupply(oldAir);p.setAbsorptionAmount(oldAbsorption);p.setHealth(Math.min(oldHealth,p.getMaxHealth()));p.invulnerableTime=oldInvulnerable;
        }
    }
    private static void compatibilityAndHarvest(ServerPlayer p){
        for(var entry:MiningContent.GEAR.entrySet()){
            var spec=entry.getValue();var stack=new ItemStack(entry.getKey());GearData.refresh(stack);check(stack.getOrDefault(MiningContent.RULES,0)==47,"fresh gear revision "+entry.getKey());check(GearData.capacity(stack)==EquipmentRules.slots(spec.tier(),spec.kind()),"registered capacity "+entry.getKey());
            for(int type=0;type<9;type++){var error=GearData.installationError(stack,item(EquipmentRules.CORES[type]),3);check((error==null)==EquipmentRules.compatible(spec.kind(),type),"real installation compatibility "+spec.kind()+" / "+type);}
            if(spec.kind()==6)check(!stack.isDamageableItem(),"manuscript has no durability");else{int max=stack.getMaxDamage();stack.setDamageValue(57);GearData.refresh(stack);check(max==stack.getMaxDamage()&&stack.getDamageValue()==57,"base durability remains fixed");}
        }
        for(int digit=0;digit<=9;digit++){var pick=item("number_pickaxe");pick.set(MiningContent.DIGIT,(double)digit);GearData.refresh(pick);near(GearData.miningSpeed(pick),4+5*digit/9d,"linear numeric mining "+digit);check(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())==(digit>=5),"native iron harvest gate "+digit);check(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())==(digit>=8),"native diamond harvest gate "+digit);check(!GearData.forbidsEnchantments(pick),"numeric gear remains enchantable");}
        var legacy=gear("true_omega_pickaxe","ordinal_crystal_lv4","sequence_core_lv3");check(GearData.profile(legacy).stream().noneMatch(c->c.type()==8)&&GearData.cores(legacy).size()==2,"old incompatible stored core is inactive and still recoverable");check(!GearData.remove(legacy,0).isEmpty(),"stored incompatible core remains removable");
    }
    private static void armorAndDamage(ServerPlayer p,boolean deep)throws Exception{
        for(int tier=0;tier<4;tier++){
            empty(p);for(int slot=0;slot<4;slot++)p.setItemSlot(ARMOR_SLOTS[slot],gear(TIERS[tier]+"_"+ARMOR[slot]));equipment(p);
            near(p.getAttributeValue(Attributes.ARMOR),new int[]{16,18,20,22}[tier],"actual complete native armor "+tier);near(p.getAttributeValue(Attributes.ARMOR_TOUGHNESS),new int[]{4,8,12,16}[tier],"actual complete native toughness "+tier);near(GearData.protectionFactor(p),1,"bare armor has no custom multiplier");
        }
        String[][] cores={{"boundary_core","boundary_core_lv2"},{"boundary_core_lv2"},{"boundary_core_lv3","boundary_core"},{"boundary_core_lv3"}};
        for(int slot=0;slot<4;slot++)p.setItemSlot(ARMOR_SLOTS[slot],gear("true_omega_"+ARMOR[slot],cores[slot]));p.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv2","ordinal_crystal_lv4"));equipment(p);
        double weighted=.2*Math.sqrt(.25*.25+.5*.5)+.4*.5+.25*Math.sqrt(1+.25*.25)+.15;
        double factor=(1+2*weighted)*(deep?1.2*1.5*1.4:1+.2+.5+.4);near(GearData.protectionFactor(p),factor,"four independent RSS then armor weights then book "+deep);
        for(String source:List.of("attack","generic","sonic","void")){
            p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);p.invulnerableTime=0;var damage=switch(source){case "attack"->new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MOB_ATTACK));case "sonic"->new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.SONIC_BOOM));case "void"->p.damageSources().fellOutOfWorld();default->p.damageSources().generic();};
            float before=p.getHealth();check(p.hurtServer(p.level(),damage,8),"native damage entry "+source);double nativeDamage=source.equals("attack")?8*(1-Math.clamp(22-8d/(2+16d/4),22*.2,20)/25):8;near(before-p.getHealth(),nativeDamage/factor,"native armor then manuscript protection for "+source+" / "+deep);
        }
        empty(p);
    }
    private static void healthAndHandPriority(ServerPlayer p,boolean deep)throws Exception{
        double nativeEntityReach=p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),nativeBlockReach=p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        p.setItemSlot(EquipmentSlot.OFFHAND,book("guogao_heart_lv2","guogao_heart_lv3","ordinal_crystal_lv4","power_tower_core_lv3"));equipment(p);near(p.getMaxHealth(),20+4*Math.sqrt(13),"only fruit RSS adds health, realm invariant "+deep);
        p.setItemSlot(EquipmentSlot.MAINHAND,book("guogao_heart_lv3","guogao_heart_lv3","hydra_bud_lv3"));equipment(p);near(p.getMaxHealth(),20+4*Math.sqrt(13),"offhand wins; mainhand book does not stack");check(ManuscriptEffects.held(p)==p.getOffhandItem(),"actual offhand precedence");near(GearData.bookReachBonus(p),0,"inactive mainhand contributes no core reach");near(p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),nativeEntityReach,"inactive mainhand branch adds no reach");
        p.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.TORCH));equipment(p);near(p.getMaxHealth(),20+12*Math.sqrt(2),"mainhand book activates when offhand is ordinary item");near(p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),nativeEntityReach+3,"actual branch manuscript entity reach");near(p.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE),nativeBlockReach+3,"actual branch manuscript block reach");check(ManuscriptEffects.held(p)==p.getMainHandItem(),"mainhand fallback");
        empty(p);near(p.getMaxHealth(),20,"unequipped life modifier is cleaned");near(p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE),nativeEntityReach,"unequipped reach is cleaned");
        p.setItemSlot(EquipmentSlot.OFFHAND,item("omega_manuscript"));equipment(p);near(GearData.protectionFactor(p),1.05,"bare tier-one book defense in either realm");near(GearData.attackWithBook(p.getMainHandItem(),p),deep?1.05:1.5,"bare tier-one book attack in either hand/realm");empty(p);
    }
    private static void silkMenu(ServerPlayer p)throws Exception{
        move(p,p.level().getServer().overworld());empty(p);var pos=p.blockPosition();var old=p.level().getBlockState(pos);check(p.level().getBlockEntity(pos)==null,"silk menu fixture avoids existing block entities");
        try{
            p.level().setBlock(pos,MiningContent.TABLES[2].defaultBlockState(),2);var pick=gear("true_omega_pickaxe","hydra_bud_lv3");var menu=new EnhancementMenu(73,p.getInventory(),p.level(),pos,3);p.containerMenu=menu;menu.getSlot(0).set(pick);
            check(menu.canToggleSilk()&&!menu.silkTouch(),"branch pick starts in yield mode");var before=GearData.cores(pick).stream().map(ItemStack::copy).toList();check(!menu.clickMenuButton(p,1)&&!GearData.silkTouch(pick),"unknown toggle button is rejected");
            p.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(73,0));check(menu.silkTouch()&&GearData.silkTouch(pick),"real server button-zero request enables silk");p.connection.handleContainerButtonClick(new ServerboundContainerButtonClickPacket(73,0));check(!menu.silkTouch()&&!GearData.silkTouch(pick),"second button-zero request restores yield");check(ItemStack.listMatches(before,GearData.cores(pick)),"silk toggle never consumes/rewrites cores");
            menu.getSlot(0).set(gear("true_omega_sword","hydra_bud_lv3"));check(!menu.canToggleSilk()&&!menu.clickMenuButton(p,0),"silk is pick-only");menu.getSlot(0).set(gear("true_omega_pickaxe"));check(!menu.canToggleSilk()&&!menu.clickMenuButton(p,0),"silk needs branch installed");menu.getSlot(0).set(ItemStack.EMPTY);p.closeContainer();check(!menu.clickMenuButton(p,0),"closed menu rejects stale button requests");
        }finally{p.closeContainer();p.level().setBlock(pos,old,2);}
    }
    private static void lootAndMining(ServerPlayer p)throws Exception{
        empty(p);var world=p.level();var registry=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);var fortune=registry.getOrThrow(Enchantments.FORTUNE);var silk=registry.getOrThrow(Enchantments.SILK_TOUCH);var looting=registry.getOrThrow(Enchantments.LOOTING);
        var pick=gear("true_omega_pickaxe","hydra_bud_lv3","hydra_bud_lv2");p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book("hydra_bud_lv3","ordinal_crystal_lv4"));var before=pick.copy();var ore=Blocks.DIAMOND_ORE.defaultBlockState();var copy=MiningEffects.lootTool(ore,world,p,pick);check(EnchantmentHelper.getItemEnchantmentLevel(fortune,copy)==6,"highest branch gives Fortune6; held book adds none");int sum=0,max=0;for(int i=0;i<128;i++){int n=Block.getDrops(ore,world,p.blockPosition(),null,p,pick).stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();check(n>=1&&n<=7,"real Fortune6 drop bounds");sum+=n;max=Math.max(n,max);}check(sum>250&&max==7,"actual diamond table uses Fortune6");check(ItemStack.matches(before,pick),"loot evaluates a copy of the tool");
        GearData.setSilkTouch(pick,true);copy=MiningEffects.lootTool(ore,world,p,pick);check(EnchantmentHelper.getItemEnchantmentLevel(fortune,copy)==0&&EnchantmentHelper.getItemEnchantmentLevel(silk,copy)==1,"silk overrides effective Fortune");var drops=Block.getDrops(ore,world,p.blockPosition(),null,p,pick);check(drops.size()==1&&drops.getFirst().is(Items.DIAMOND_ORE)&&drops.getFirst().getCount()==1,"real silk diamond ore table");GearData.remove(pick,0);check(GearData.silkTouch(pick),"remaining branch keeps silk mode");GearData.remove(pick,1);check(!GearData.silkTouch(pick),"removing final branch clears silk flag");
        var numeric=gear("number_pickaxe","hydra_bud");numeric.set(MiningContent.DIGIT,9d);GearData.refresh(numeric);numeric.enchant(fortune,3);check(EnchantmentHelper.getItemEnchantmentLevel(fortune,MiningEffects.lootTool(ore,world,p,numeric))==5,"native Fortune adds to branch on numeric tools");
        p.setItemSlot(EquipmentSlot.MAINHAND,gear("true_omega_sword","hydra_bud_lv2","hydra_bud"));check(EnchantmentHelper.getEnchantmentLevel(looting,p)==4,"native Looting entry uses highest sword branch");
        p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));p.setItemSlot(EquipmentSlot.OFFHAND,book("sequence_core_lv3","sequence_core_lv2","ordinal_crystal_lv4"));equipment(p);near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),(6+Math.sqrt(320))*(1.4+Math.sqrt(1.25)),"manuscript flat and rate RSS after native iron speed");near(p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),(1+Math.sqrt(320))*(1.4+Math.sqrt(1.25)),"manuscript flat bonus also applies on wrong-tool dirt");p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,1));near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),(6+Math.sqrt(320))*(1.8+Math.sqrt(1.25)),"Haste joins manuscript additive rate bucket");p.removeAllEffects();empty(p);
    }
    private static void wearAndBreak(ServerPlayer p)throws Exception{
        empty(p);var pick=gear("true_omega_pickaxe","lho_trace","lho_trace_lv2");p.setItemSlot(EquipmentSlot.MAINHAND,pick);p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace_lv3","ordinal_crystal_lv4"));near(GearData.wearFactor(pick,p),20,"item Empty protection sums 4+16 without book");near(GearData.wearFactor(new ItemStack(Items.DIAMOND_PICKAXE),p),1,"book Empty does not protect unrelated items");pick.hurtAndBreak(6000,p,EquipmentSlot.MAINHAND);check(!pick.isEmpty()&&pick.getDamageValue()>210&&pick.getDamageValue()<390,"native tool wear entry filters at one twentieth");
        var chest=gear("true_omega_chestplate","lho_trace");p.setItemSlot(EquipmentSlot.CHEST,chest);p.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv3"));equipment(p);double factor=4*GearData.protectionFactor(p);near(GearData.wearCost(chest,100,p,()->0),100,"armor wear may spend under zero sample");check(GearData.wearCost(chest,100,p,()->1/factor+.0001)==0,"armor custom protection multiplies Empty wear factor");chest.hurtAndBreak(3000,p,EquipmentSlot.CHEST);check(!chest.isEmpty()&&chest.getDamageValue()>250&&chest.getDamageValue()<440,"real armor durability owner applies defense protection");
        chest.setDamageValue(0);int wear=0;for(int i=0;i<128;i++){p.setHealth(p.getMaxHealth());p.invulnerableTime=0;check(p.hurtServer(p.level(),new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MOB_ATTACK)),8),"armor damage fixture hit");}wear=chest.getDamageValue();check(wear>8&&wear<65,"real native hurtArmor also uses owner protection; wear="+wear);empty(p);armorBreakSnapshot(p);
        var fragile=gear("omega_pickaxe","sequence_core","hydra_bud");fragile.setDamageValue(fragile.getMaxDamage()-1);p.setItemSlot(EquipmentSlot.MAINHAND,fragile);var prior=new HashSet<UUID>();for(var e:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4)))prior.add(e.getUUID());fragile.hurtAndBreak(1,p,EquipmentSlot.MAINHAND);var returned=p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4),e->!prior.contains(e.getUUID()));check(fragile.isEmpty()&&returned.size()==2,"actual break returns exactly two intact installed cores");check(returned.stream().anyMatch(e->e.getItem().is(item("sequence_core").getItem()))&&returned.stream().anyMatch(e->e.getItem().is(item("hydra_bud").getItem())),"returned core families intact");for(var e:returned)e.discard();empty(p);
    }
    private static void armorBreakSnapshot(ServerPlayer p)throws Exception{
        empty(p);var health=p.getAttribute(Attributes.MAX_HEALTH);double base=health.getBaseValue();var prior=new HashSet<UUID>();for(var entity:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4)))prior.add(entity.getUUID());
        try{
            health.setBaseValue(200);var chest=gear("true_omega_chestplate","boundary_core_lv3");chest.setDamageValue(chest.getMaxDamage()-1);p.setItemSlot(EquipmentSlot.CHEST,chest);equipment(p);p.setHealth(p.getMaxHealth());p.invulnerableTime=0;double protectionBefore=GearData.protectionFactor(p);near(protectionBefore,1.8,"breaking chest has known pre-hit Boundary protection");float hp=p.getHealth();check(p.hurtServer(p.level(),new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MOB_ATTACK)),128),"real damageArmor-before-damage entry");check(chest.isEmpty(),"chest truly breaks during vanilla damageArmor");
            double armor=p.getArmorValue(),tough=p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);double vanilla=128*(1-Math.clamp(armor-128/(2+tough/4),armor*.2,20)/25);near(hp-p.getHealth(),vanilla/protectionBefore,"same hit retains pre-break protection snapshot after damageArmor");near(GearData.protectionFactor(p),1,"future hits lose destroyed chest protection");
            empty(p);var helmet=gear("true_omega_helmet","boundary_core_lv3");helmet.setDamageValue(helmet.getMaxDamage()-1);p.setItemSlot(EquipmentSlot.HEAD,helmet);equipment(p);p.setHealth(p.getMaxHealth());p.invulnerableTime=0;protectionBefore=GearData.protectionFactor(p);near(protectionBefore,1.4,"falling anvil starts with known weighted helmet defense");hp=p.getHealth();check(p.hurtServer(p.level(),new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FALLING_ANVIL)),128),"falling anvil real helmet-before-armor entry");check(helmet.isEmpty(),"falling anvil actually breaks the Boundary helmet before armor calculation");armor=p.getArmorValue();tough=p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);vanilla=96*(1-Math.clamp(armor-96/(2+tough/4),armor*.2,20)/25);near(hp-p.getHealth(),vanilla/protectionBefore,"outer damage frame retains pre-hit F through falling-anvil helmet wear");near(GearData.protectionFactor(p),1,"future hits lose destroyed helmet defense");
        }finally{
            health.setBaseValue(base);empty(p);for(var entity:p.level().getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(4)))if(!prior.contains(entity.getUUID()))entity.discard();
        }
    }
    private static void water(ServerPlayer p)throws Exception{
        empty(p);var method=LivingEntity.class.getDeclaredMethod("decreaseAirSupply",int.class);method.setAccessible(true);double nativeWater=p.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY),nativeMining=p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        for(int lv=1;lv<=3;lv++){String core="laver_core"+(lv==1?"":"_lv"+lv);p.setItemSlot(EquipmentSlot.HEAD,gear("true_omega_helmet",core));p.setItemSlot(EquipmentSlot.FEET,gear("true_omega_boots",core));equipment(p);near(p.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY),Math.max(nativeWater,lv/3d),"actual water movement attribute Lv"+lv);near(p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED),Math.max(nativeMining,1),"actual Aqua Affinity attribute Lv"+lv);int consumed=0;for(int n=0;n<4096;n++)if((int)method.invoke(p,300)<300)consumed++;check(lv==3?consumed==0:Math.abs(consumed-4096*(lv==1?.25:1d/16))<(lv==1?160:85),"real oxygen consumption Lv"+lv+" / "+consumed);}
        empty(p);near(p.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY),nativeWater,"water modifier disappears with boots");near(p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED),nativeMining,"underwater modifier disappears with helmet");p.setItemSlot(EquipmentSlot.OFFHAND,book("laver_core_lv3"));equipment(p);near(p.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY),nativeWater,"Laver book does not affect water gear");check((int)method.invoke(p,300)==299,"Laver book does not affect respiration");empty(p);
    }
    private static void passives(ServerPlayer p)throws Exception{
        empty(p);p.setItemSlot(EquipmentSlot.OFFHAND,book("ordinal_crystal_lv4"));equipment(p);p.setHealth(5);p.tickCount=80;ManuscriptEffects.tick(p);near(p.getHealth(),5,"ordinal has no standalone healing");p.setItemSlot(EquipmentSlot.OFFHAND,book("laver_core_lv2","laver_core_lv3","ordinal_crystal_lv4"));p.getFoodData().setFoodLevel(2);p.getFoodData().setSaturation(0);ManuscriptEffects.tick(p);near(p.getHealth(),5+Math.sqrt(13)*1.9,"real four-second heal includes tier4 base and ordinal");check(p.getFoodData().getFoodLevel()==19&&p.getFoodData().getSaturationLevel()==0,"Laver Lv3 food floor19 without saturation refill");check(!p.hasEffect(MobEffects.NIGHT_VISION),"Laver book no longer grants night vision");p.tickCount=81;p.setItemSlot(EquipmentSlot.OFFHAND,book("laver_core_lv2"));p.getFoodData().setFoodLevel(1);ManuscriptEffects.tick(p);check(p.getFoodData().getFoodLevel()==10,"Laver Lv2 food floor10");
        p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace_lv2","boundary_core_lv2"));ManuscriptEffects.tick(p);check(p.hasEffect(MobEffects.NIGHT_VISION)&&p.hasEffect(MobEffects.FIRE_RESISTANCE),"Empty night vision and Boundary fire immunity");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(p);check(!p.hasEffect(MobEffects.NIGHT_VISION)&&!p.hasEffect(MobEffects.FIRE_RESISTANCE),"unequip clears only owned night/fire passives");
        p.setItemSlot(EquipmentSlot.OFFHAND,book("guogao_heart_lv2"));equipment(p);for(var type:List.of(MobEffects.POISON,MobEffects.HUNGER,MobEffects.WEAKNESS)){p.addEffect(new MobEffectInstance(type,200));check(!p.hasEffect(type),"FruitLv2 admission immunity "+type);}p.addEffect(new MobEffectInstance(MobEffects.NAUSEA,200));p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200));check(p.hasEffect(MobEffects.NAUSEA)&&p.hasEffect(MobEffects.SLOWNESS),"FruitLv2 allows Lv3-only debuffs");p.setItemSlot(EquipmentSlot.OFFHAND,book("guogao_heart_lv3"));ManuscriptEffects.tick(p);check(!p.hasEffect(MobEffects.NAUSEA)&&!p.hasEffect(MobEffects.SLOWNESS),"FruitLv3 cleans existing strong debuffs");empty(p);p.tickCount=81;
    }
    private static void targeting(ServerPlayer p)throws Exception{
        empty(p);var world=p.level();var zombie=EntityTypes.ZOMBIE.create(world,EntitySpawnReason.COMMAND);var enderman=EntityTypes.ENDERMAN.create(world,EntitySpawnReason.COMMAND);check(zombie!=null&&enderman!=null,"vanilla target fixtures");zombie.snapTo(p.getX()+8,p.getY(),p.getZ(),0,0);enderman.snapTo(zombie.position());zombie.setNoAi(true);enderman.setNoAi(true);
        var normalGoal=new NearestAttackableTargetGoal<>(zombie,net.minecraft.world.entity.player.Player.class,false);var search=NearestAttackableTargetGoal.class.getDeclaredMethod("findTarget");search.setAccessible(true);var selected=NearestAttackableTargetGoal.class.getDeclaredField("target");selected.setAccessible(true);zombie.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(16);search.invoke(normalGoal);check(selected.get(normalGoal)==p,"real ordinary target search acquires player before stealth");p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace_lv2"));normalGoal.setTarget(null);search.invoke(normalGoal);check(selected.get(normalGoal)==null,"real nearest-target mixin scopes candidate filtering");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);
        // Exercise range boundaries and special exceptions through the native predicate too.
        var condition=TargetingConditions.forCombat().range(16).ignoreLineOfSight();check(condition.test(world,zombie,p),"unmodified normal hostile candidate accepted");
        p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace"));var prior=OrdinaryTargeting.enter(zombie);try{check(!condition.test(world,zombie,p),"Lv1 shrinks ordinary scan to thirty percent");zombie.snapTo(p.getX()+3,p.getY(),p.getZ(),0,0);check(condition.test(world,zombie,p),"Lv1 nearby candidate still accepted");}finally{OrdinaryTargeting.leave(prior);}
        p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace_lv2"));prior=OrdinaryTargeting.enter(zombie);try{check(!condition.test(world,zombie,p),"Lv2 prevents fresh ordinary targeting");}finally{OrdinaryTargeting.leave(prior);}check(condition.test(world,zombie,p),"unscoped special/direct predicates remain vanilla");prior=OrdinaryTargeting.enter(enderman);try{check(condition.test(world,enderman,p),"neutral anger rules remain vanilla");}finally{OrdinaryTargeting.leave(prior);}
        zombie.tickCount=42;zombie.setLastHurtByMob(p);var revenge=new HurtByTargetGoal(zombie);check(revenge.canUse(),"Lv2 permits vanilla retaliation");p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace_lv3"));check(!revenge.canUse(),"Lv3 blocks ordinary retaliation");enderman.setLastHurtByMob(p);check(!OrdinaryTargeting.revengeBlocked(enderman),"neutral retaliation exception preserved");
        empty(p);var slime=EntityTypes.SLIME.create(world,EntitySpawnReason.COMMAND);check(slime!=null,"Enemy-interface slime fixture");slime.snapTo(p.getX()+8,p.getY(),p.getZ(),0,0);slime.setNoAi(true);var slimeGoal=new NearestAttackableTargetGoal<>(slime,net.minecraft.world.entity.player.Player.class,false);search.invoke(slimeGoal);check(selected.get(slimeGoal)==p,"vanilla slime nearest-player goal acquires player");p.setItemSlot(EquipmentSlot.OFFHAND,book("lho_trace_lv2"));slimeGoal.setTarget(null);search.invoke(slimeGoal);check(selected.get(slimeGoal)==null,"ordinary Enemy-interface slime respects Empty stealth");empty(p);slime.discard();zombie.discard();enderman.discard();
    }
    private static void rarity(ServerPlayer p)throws Exception{
        int icons=0;
        for(int type=0;type<9;type++)for(int level=1;level<=(type==8?4:3);level++){
            String id=EquipmentRules.CORES[type]+(level==1?"":"_lv"+level);var stack=item(id);var expected=type==8?new Rarity[]{Rarity.COMMON,Rarity.UNCOMMON,Rarity.RARE,Rarity.EPIC}[level-1]:new Rarity[]{Rarity.UNCOMMON,Rarity.RARE,Rarity.EPIC}[level-1];check(stack.getRarity()==expected,"real item rarity "+id);
            check(EquipmentRules.displayedCoreGrade(type,level)==(type==8?level-1:level),"material stage/display grade mapping "+id);icons++;
        }check(icons==28,"all28 core items present without world-model mutation");
    }
    public static void auditIcons(net.minecraft.client.Minecraft client)throws Exception{
        var resources=client.getResourceManager();int icons=0;
        for(String language:List.of("zh_cn","en_us"))try(var reader=resources.getResourceOrThrow(Identifier.parse("googology:lang/"+language+".json")).openAsReader()){
            var text=JsonParser.parseReader(reader).getAsJsonObject();String crystal=language.equals("zh_cn")?"序数晶体":"Ordinal Crystal",core=language.equals("zh_cn")?"序数晶核":"Ordinal Core";
            check(text.get("block.googology.ordinal_crystal").getAsString().equals(crystal),"ungraded crystal display "+language);
            for(int stage=2;stage<=4;stage++)check(text.get("block.googology.ordinal_crystal_lv"+stage).getAsString().equals(core+" · Lv"+(stage-1)),"ordinal displayed grade "+language+" / "+stage);
        }
        for(int type=0;type<9;type++)for(int level=1;level<=(type==8?4:3);level++){
            String id=EquipmentRules.CORES[type]+(level==1?"":"_lv"+level);
            try(var reader=resources.getResourceOrThrow(Identifier.parse("googology:models/item/"+id+".json")).openAsReader()){var model=JsonParser.parseReader(reader).getAsJsonObject();boolean original=type==8&&level==1;check(model.get("parent").getAsString().equals(original?"googology:block/ordinal_crystal":"minecraft:item/generated"),"client inventory model and original OrdinalLv1 exception "+id);check(resources.getResource(Identifier.parse("googology:textures/item/"+id+".png")).isPresent()!=original,"own flat texture except original OrdinalLv1 "+id);}
            check(resources.getResource(Identifier.parse("googology:items/"+id+".json")).isPresent(),"modern client item entrypoint "+id);icons++;
        }
        for(var tier:TIERS){check(resources.getResource(Identifier.parse("googology:items/"+tier+"_bow.json")).isPresent(),"modern client bow entrypoint "+tier);for(int stage=0;stage<3;stage++)check(resources.getResource(Identifier.parse("googology:models/item/bows/"+tier+"_bow_pulling_"+stage+".json")).isPresent(),"bow pull resource "+tier+" / "+stage);}
        check(icons==28,"client inventory covers27 flat models and original OrdinalLv1");System.out.println("CORE047_CLIENT_ICONS_OK items="+icons+" flat=27 ordinalOriginal3D=1 bows=4 drawVariants=12");
    }
    static void sanctuaryLoot(ServerPlayer p){
        String[] themes={"matrix","power","hydra","absence","weaver","astra","frontier","guogao"};int[] types={0,1,2,3,4,5,6,7};var world=p.level();var params=new LootParams.Builder(world).withParameter(LootContextParams.ORIGIN,p.position()).withOptionalParameter(LootContextParams.THIS_ENTITY,p).create(LootContextParamSets.CHEST);
        for(int theme=0;theme<8;theme++)for(boolean main:new boolean[]{true,false}){
            var key=ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("googology:chests/"+themes[theme]+(main?"_sanctum":"_ruin")));var table=world.getServer().reloadableRegistries().getLootTable(key);check(table!=LootTable.EMPTY,"actual loaded landmark loot "+key);
            int minRegional=99,maxRegional=0,minOrdinal=99,maxOrdinal=0;
            for(int sample=0;sample<512;sample++){
                var chest=new SimpleContainer(27);table.fill(chest,params,47000+sample);int regional=0,ordinal=0,occupied=0;
                for(int slot=0;slot<27;slot++){var stack=chest.getItem(slot);if(stack.isEmpty())continue;occupied++;int type=GearData.type(stack);if(type<0)continue;check(type==types[theme]||type==8,"theme loot contains no foreign core "+key);check(GearData.level(stack)==(main?2:1),"raw material grade: main regional Lv2 / Ordinal Core Lv1; side natural crystal ungraded "+key);if(type==8)ordinal+=stack.getCount();else regional+=stack.getCount();}
                check(main?regional==2&&ordinal==2:regional>=2&&regional<=3&&ordinal>=8&&ordinal<=15,"actual guaranteed core counts "+key+" / "+regional+" / "+ordinal);check(occupied<=27,"actual chest fits27slots "+key);minRegional=Math.min(minRegional,regional);maxRegional=Math.max(maxRegional,regional);minOrdinal=Math.min(minOrdinal,ordinal);maxOrdinal=Math.max(maxOrdinal,ordinal);
            }
            if(!main)check(minRegional==2&&maxRegional==3&&minOrdinal==8&&maxOrdinal==15,"loaded uniform distributions reach specified endpoints "+key);
        }
        System.out.println("CORE047_LOOT_OK tables=16 actualChestFills=8192 mainRegionalLv2+OrdinalCoreLv1=2+2 sideRegionalLv1=2..3 ungradedCrystals=8..15");
    }
    public static void displayIcons(ServerPlayer p){
        p.closeContainer();var icons=new SimpleContainer(54);int n=0;for(int type=0;type<9;type++)for(int level=1;level<=(type==8?4:3);level++){var stack=item(EquipmentRules.CORES[type]+(level==1?"":"_lv"+level));stack.setCount(new int[]{16,32,64,64}[level-1]);icons.setItem(n++,stack);}for(var tier:TIERS)icons.setItem(n++,item(tier+"_bow"));p.openMenu(new SimpleMenuProvider((id,inventory,who)->ChestMenu.sixRows(id,inventory,icons),Component.literal("晶核等级与四档序数弓 · Rev47")));
    }
    public static void displaySilk(ServerPlayer p){
        p.closeContainer();var pos=p.blockPosition();p.level().setBlock(pos,MiningContent.TABLES[2].defaultBlockState(),2);p.openMenu(new SimpleMenuProvider((id,inventory,who)->new EnhancementMenu(id,inventory,p.level(),pos,3),Component.translatable("block.googology.enhancement_table_3")));var menu=(EnhancementMenu)p.containerMenu;menu.getSlot(0).set(gear("true_omega_pickaxe","hydra_bud_lv3","lho_trace_lv3","sequence_core_lv3"));menu.clickMenuButton(p,0);menu.broadcastChanges();for(int type=0;type<9;type++)p.getInventory().setItem(9+type,item(EquipmentRules.CORES[type]+"_lv3"));menu.broadcastChanges();
    }
}
