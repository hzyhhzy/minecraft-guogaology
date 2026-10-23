package dev.guogaology.mergeqa;

import com.google.gson.*;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Opt-in balance acceptance through real native release, impact, attack and disk NBT paths. */
public final class Combat0611Checks {
    private static final String[] TIERS={"omega","epsilon","gamma","true_omega"};
    private static final double[] SPEED={3,3.9,4.8,6},SWORD_BASE={8,10,12,16};
    private static final double[] SWORD_HP={3,6,9},SWORD_RATE={1,2,4},BOW_COEFF={.5,1,1.5},BOOK_HP={2,4,6},BOOK_RATE={.5,1,2};
    private static final EquipmentSlot[] SLOTS={EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final JsonArray ROWS=new JsonArray();
    private static final ThreadLocal<List<AbstractArrow>> SPAWNS=new ThreadLocal<>();
    private static int checks,arrows,melee,disk,oldGear;
    public static boolean enabled(){return Boolean.getBoolean("guogaology.combat0611");}
    /** QA-only observer of successful native addFreshEntity; never creates or modifies an arrow. */
    public static void observeSpawn(AbstractArrow arrow){var active=SPAWNS.get();if(active!=null)active.add(arrow);}
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError("combat0611: "+label);}
    private static void near(double actual,double expected,String label){check(Double.isFinite(actual)&&Math.abs(actual-expected)<Math.max(.003,Math.abs(expected)*.00001),label+" actual="+actual+" expected="+expected);}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static ItemStack gear(String id,int... grades){var value=item(id);var cores=new ArrayList<ItemStack>();for(int grade:grades)cores.add(item("power_tower_core"+(grade==1?"":"_lv"+grade)));GearData.setCores(value,cores);GearData.refresh(value);return value;}
    private static int[] copies(int count,int grade){var result=new int[count];Arrays.fill(result,grade);return result;}
    private static double rss(int[] grades,double[] values){double total=0;for(int grade:grades)total+=values[grade-1]*values[grade-1];return Math.sqrt(total);}
    private static boolean deep(ServerLevel level){return level.dimension().equals(GuogaologyMod.DIMENSION)||level.dimension().equals(GuogaologyMod.GUOGAO);}
    private static void equipment(ServerPlayer p)throws Exception{for(var slot:SLOTS){var s=p.getItemBySlot(slot);if(!s.isEmpty())s.getItem().inventoryTick(s,p.level(),p,slot);}var method=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");method.setAccessible(true);method.invoke(p);ManuscriptEffects.tick(p);}
    private static void move(ServerPlayer p,ServerLevel world)throws Exception{
        world.getChunk(0,1);p.teleport(new TeleportTransition(world,new Vec3(8.5,290,24.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));p.hasChangedDimension();
        if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());p.setOnGround(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setSprinting(false);equipment(p);
    }
    /** Records what native damage entry actually receives; superclass still performs the real hit. */
    private static final class ProbeCow extends Cow {
        float incoming=Float.NaN;boolean accepted;
        ProbeCow(ServerLevel world){super(EntityTypes.COW,world);setNoAi(true);setNoGravity(true);getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);setHealth(1000);setPos(8.5,290,28.5);}
        @Override public boolean hurtServer(ServerLevel level,DamageSource source,float amount){incoming=amount;accepted=super.hurtServer(level,source,amount);return accepted;}
    }
    private static ProbeCow target(ServerLevel world,List<Entity> cleanup){var cow=new ProbeCow(world);check(world.addFreshEntity(cow),"native target inserted");cleanup.add(cow);return cow;}
    private static void clear(List<Entity> cleanup){for(var value:cleanup)value.discard();cleanup.clear();}
    private static AbstractArrow fire(ServerPlayer p,ItemStack bow,ItemStack book,int ticks,List<Entity> cleanup)throws Exception{
        p.getInventory().clearContent();for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.MAINHAND,bow);p.setItemSlot(EquipmentSlot.OFFHAND,book);p.getInventory().setItem(19,new ItemStack(Items.ARROW,2));equipment(p);
        // A newly FULL destination can still be outside the entity query index for
        // this tick. Observe successful native insertion instead of forcing world
        // visibility or mistaking a spatial-query delay for a missing projectile.
        var spawned=new ArrayList<AbstractArrow>();SPAWNS.set(spawned);boolean fired;
        try{p.startUsingItem(InteractionHand.MAIN_HAND);var nativeBow=(BowItem)bow.getItem();fired=nativeBow.releaseUsing(bow,p.level(),p,nativeBow.getUseDuration(bow,p)-ticks);}finally{p.stopUsingItem();SPAWNS.remove();}
        cleanup.addAll(spawned);check(fired&&spawned.size()==1,"actual release spawns one vanilla arrow, count="+spawned.size());arrows++;
        return spawned.getFirst();
    }
    private static double arrowBaseDamage(AbstractArrow arrow)throws Exception{var field=AbstractArrow.class.getDeclaredField("baseDamage");field.setAccessible(true);return field.getDouble(arrow);}
    private static void hit(AbstractArrow arrow,ProbeCow cow)throws Exception{var method=AbstractArrow.class.getDeclaredMethod("onHitEntity",EntityHitResult.class);method.setAccessible(true);try{method.invoke(arrow,new EntityHitResult(cow));}catch(InvocationTargetException e){throw new AssertionError("native onHitEntity",e.getCause());}check(cow.accepted&&Float.isFinite(cow.incoming),"native impact accepted real damage");}
    private static void row(String kind,String name,ServerLevel world,double expected,double actual){var r=new JsonObject();r.addProperty("kind",kind);r.addProperty("case",name);r.addProperty("dimension",world.dimension().identifier().toString());r.addProperty("expected",expected);r.addProperty("actual",actual);ROWS.add(r);}
    private static double bookAdd(int tier,int[] grades){return tier+rss(grades,BOOK_HP);}
    private static double bookFactor(int tier,int[] grades){return (1+.1*tier)*(1+rss(grades,BOOK_RATE));}
    private static double coefficient(boolean deep,int[] grades){return deep?2*(1+rss(grades,BOOK_RATE)):2+rss(grades,BOW_COEFF);}
    // Native onHitEntity casts Vec3.length to float; any non-null weapon also
    // casts baseDamage through EnchantmentHelper(float), then promotes both values
    // back to double before multiplying and ceiling. Preserve integer boundaries.
    private static double expectedArrow(boolean deep,double speed,double coefficient,int bookTier,int[] bookGrades,float draw){double nativeDamage=Math.ceil((double)(float)speed*(double)(float)coefficient);return deep?nativeDamage*bookFactor(bookTier,bookGrades):nativeDamage+bookAdd(bookTier,bookGrades)*draw;}
    private static void arrowCase(ServerPlayer p,int tier,int[] own,int bookTier,int[] bookGrades,boolean powerV,int ticks,String name,List<Entity> cleanup)throws Exception{
        var bow=gear(TIERS[tier-1]+"_bow",own);var book=bookTier==0?ItemStack.EMPTY:gear(TIERS[bookTier-1]+"_manuscript",bookGrades);boolean inner=deep(p.level());
        if(powerV){bow.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),5);check(bow.isEnchanted()&&"enchanted".equals(GearData.installationError(bow,item("power_tower_core"),3)),"PowerV rejects socket installation");}
        if(own.length>0){check(GearData.forbidsEnchantments(bow),"core bow occupies enchant path");var copy=bow.copy();copy.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER),5);check(!copy.isEnchanted(),"native enchant cannot bypass installed cores");}
        float draw=BowItem.getPowerForTime(ticks);double speed=SPEED[tier-1]*draw,coefficient=coefficient(inner,own);var a=fire(p,bow,book,ticks,cleanup);
        check(Math.abs(a.getDeltaMovement().length()-speed)<.12,"actual initial velocity retains native launch scatter "+name+" actual="+a.getDeltaMovement().length()+" expected="+speed);near(arrowBaseDamage(a),coefficient,"own Tower modifies vanilla coefficient "+name);
        // Native release keeps its direction scatter. Use exact accepted impact speed here,
        // so the acceptance boundary does not depend on random trajectory inaccuracy.
        a.setDeltaMovement(speed,0,0);a.setCritArrow(false);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);equipment(p);
        var cow=target(p.level(),cleanup);double expected=expectedArrow(inner,speed,coefficient+(powerV?3:0),bookTier,bookGrades,draw);hit(a,cow);near(cow.incoming,expected,"real noncritical hit "+name);near(1000-cow.getHealth(),Math.min(1000,expected),"health loss follows actual hit "+name);row("arrow",name,p.level(),expected,cow.incoming);clear(cleanup);
    }
    private static void swordCase(ServerPlayer p,int tier,int[] own,int bookTier,int[] bookGrades,String name,List<Entity> cleanup)throws Exception{
        p.getInventory().clearContent();for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);var sword=gear(TIERS[tier-1]+"_sword",own);p.setItemSlot(EquipmentSlot.MAINHAND,sword);p.setItemSlot(EquipmentSlot.OFFHAND,bookTier==0?ItemStack.EMPTY:gear(TIERS[bookTier-1]+"_manuscript",bookGrades));equipment(p);
        boolean inner=deep(p.level());double weapon=inner?SWORD_BASE[tier-1]*(1+rss(own,SWORD_RATE)):SWORD_BASE[tier-1]+rss(own,SWORD_HP);double expected=inner?weapon*bookFactor(bookTier,bookGrades):weapon+bookAdd(bookTier,bookGrades);
        near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),expected,"native equipped attack attribute "+name);near(GearData.attackWithBook(sword,p),expected,"public combat helper "+name);
        var field=LivingEntity.class.getDeclaredField("attackStrengthTicker");field.setAccessible(true);field.setInt(p,1000);p.setOnGround(true);p.setSprinting(false);p.fallDistance=0;var cow=target(p.level(),cleanup);p.attack(cow);check(cow.accepted,"real Player.attack reaches native damage entry");near(cow.incoming,expected,"real fully cooled noncritical sword hit "+name);row("sword",name,p.level(),expected,cow.incoming);melee++;clear(cleanup);
    }
    private static AbstractArrow diskReload(AbstractArrow arrow,ServerLevel world,String name,List<Entity> cleanup)throws Exception{
        var save=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,world.registryAccess());arrow.saveWithoutId(save);var folder=FabricLoader.getInstance().getGameDir().resolve("combat0611-nbt");Files.createDirectories(folder);var file=folder.resolve(name+".nbt");NbtIo.writeCompressed(save.buildResult(),file);
        arrow.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);var restored=arrow.getType().create(world,EntitySpawnReason.COMMAND);check(restored instanceof AbstractArrow,"native arrow reconstructed after disk read");restored.load(TagValueInput.create(ProblemReporter.DISCARDING,world.registryAccess(),NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap())));cleanup.add(restored);disk++;return (AbstractArrow)restored;
    }
    private static void persistence(ServerPlayer p,List<Entity> cleanup)throws Exception{
        int[] own={3,3},grades={2,3};var a=fire(p,gear("true_omega_bow",own),gear("true_omega_manuscript",grades),20,cleanup);double c=coefficient(deep(p.level()),own);a.setDeltaMovement(6,0,0);a.setCritArrow(false);var before=((ArrowShotAccess)a).guogaology$shot().save();var restored=diskReload(a,p.level(),"new-"+(deep(p.level())?"inner":"outer"),cleanup);near(arrowBaseDamage(restored),c,"new arrow coefficient survives vanilla disk NBT");check(((ArrowShotAccess)restored).guogaology$shot().save().equals(before),"new final manuscript payload survives disk unchanged");p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);equipment(p);var cow=target(p.level(),cleanup);hit(restored,cow);double expected=expectedArrow(deep(p.level()),6,c,4,grades,1);near(cow.incoming,expected,"reloaded in-flight arrow keeps launch book after unequip");row("persistence","new-arrow",p.level(),expected,cow.incoming);clear(cleanup);
        // Literal pre-0611 schema: old arrows have a native coefficient of 2 and
        // store both old carrier effects in final extraHp/factor. Do not reinterpret it.
        a=fire(p,gear("true_omega_bow"),ItemStack.EMPTY,20,cleanup);var current=((ArrowShotAccess)a).guogaology$shot();String old="{\"volley\":\""+current.volley+"\",\"owner\":\""+current.owner+"\",\"team\":\"\",\"extraHp\":7,\"factor\":2,\"draw\":1,\"burst\":0,\"guogao\":0,\"seconds\":0,\"burstUsed\":false,\"livingHits\":0}";var legacy=BowShotData.load(old);check(legacy!=null,"literal old payload accepted");((ArrowShotAccess)a).guogaology$shot(legacy);a.setBaseDamage(2);a.setDeltaMovement(3,0,0);a.setCritArrow(false);restored=diskReload(a,p.level(),"legacy-"+(deep(p.level())?"inner":"outer"),cleanup);near(arrowBaseDamage(restored),2,"old arrow native coefficient not rebalanced on load");cow=target(p.level(),cleanup);hit(restored,cow);near(cow.incoming,19,"old final 6x2+7 damage preserved exactly");row("persistence","legacy-payload",p.level(),19,cow.incoming);clear(cleanup);
    }
    private static void critical(ServerPlayer p,List<Entity> cleanup)throws Exception{
        var observed=new HashSet<Integer>();int nativeBase=(int)Math.ceil(6*coefficient(deep(p.level()),new int[]{3}));double add=bookAdd(4,new int[]{3}),factor=bookFactor(4,new int[]{3});
        for(int i=0;i<24;i++){var a=fire(p,gear("true_omega_bow",3),gear("true_omega_manuscript",3),20,cleanup);check(a.isCritArrow(),"full draw preserves native random critical flag");a.setDeltaMovement(6,0,0);var cow=target(p.level(),cleanup);hit(a,cow);double nativeDamage=deep(p.level())?cow.incoming/factor:cow.incoming-add;int value=(int)Math.round(nativeDamage);near(nativeDamage,value,"book remains outside native critical integer roll");check(value>=nativeBase&&value<=nativeBase+nativeBase/2+1,"native critical bounds apply to modified coefficient");observed.add(value);clear(cleanup);}check(observed.size()>1,"native random critical outcomes are not replaced by a fixed average");
    }
    private static void legacyGear(ServerPlayer p)throws Exception{
        for(boolean inner:new boolean[]{false,true}){
            var old=gear("true_omega_sword",3,3);old.set(MiningContent.RULES,49);old.set(MiningContent.DEEP,inner);old.setDamageValue(123);
            double oldAttack=inner?16*(1+Math.sqrt(2)):16+3*Math.sqrt(2);
            old.set(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE,new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,oldAttack-1,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED,new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,-2.4,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build());
            var before=GearData.coreSlots(old).stream().map(ItemStack::copy).toList();int max=old.getMaxDamage();var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,p.registryAccess());output.store("item",ItemStack.CODEC,old);var folder=FabricLoader.getInstance().getGameDir().resolve("combat0611-nbt");Files.createDirectories(folder);var file=folder.resolve("legacy-gear-"+deep(p.level())+"-"+inner+".nbt");NbtIo.writeCompressed(output.buildResult(),file);var input=TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap()));var restored=input.read("item",ItemStack.CODEC).orElseThrow();
            check(restored.getOrDefault(MiningContent.RULES,0)==49,"old revision survives actual item codec and disk NBT before refresh");check(restored.getDamageValue()==123&&restored.getMaxDamage()==max&&ItemStack.listMatches(before,GearData.coreSlots(restored)),"old item codec retains damage, max and exact core components");
            // Native inventory callback owns migration, without a custom save rewriter.
            restored.getItem().inventoryTick(restored,p.level(),p,EquipmentSlot.MAINHAND);check(restored.getOrDefault(MiningContent.RULES,0)==EquipmentRules.REVISION&&EquipmentRules.REVISION>49,"native inventory refresh upgrades rules revision");check(restored.getDamageValue()==123&&restored.getMaxDamage()==max&&ItemStack.listMatches(before,GearData.coreSlots(restored)),"refresh retains ownership and damage ratio");p.setItemSlot(EquipmentSlot.MAINHAND,restored);p.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);equipment(p);double expected=deep(p.level())?16*(1+4*Math.sqrt(2)):16+9*Math.sqrt(2);near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),expected,"legacy sword receives new native attack modifiers");oldGear++;
        }
    }
    private static void overCap(ServerPlayer p,List<Entity> cleanup)throws Exception{
        p.getInventory().clearContent();for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);var sword=gear("true_omega_sword",copies(8,3));var book=gear("true_omega_manuscript",copies(4,3));var cores=new ArrayList<>(GearData.coreSlots(book));cores.add(item("ordinal_crystal_lv4"));cores.add(item("ordinal_crystal_lv4"));GearData.setCores(book,cores);GearData.refresh(book);p.setItemSlot(EquipmentSlot.MAINHAND,sword);p.setItemSlot(EquipmentSlot.OFFHAND,book);equipment(p);
        double expected=16*(1+4*Math.sqrt(8))*1.4*(1+2*Math.sqrt(4))*(1+.4*Math.sqrt(2));check(expected>2048,"deep optimized manuscript crosses native attack attribute cap");near(GearData.attackWithBook(sword,p),expected,"full source calculation survives native cap");near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),2048,"native attribute itself retains its normal engine cap");
        var field=LivingEntity.class.getDeclaredField("attackStrengthTicker");field.setAccessible(true);field.setInt(p,1000);p.setOnGround(true);p.setSprinting(false);p.fallDistance=0;var cow=target(p.level(),cleanup);p.attack(cow);check(cow.accepted,"over-cap actual Player.attack accepted");near(cow.incoming,expected,"actual mixed-source optimized hit is not truncated to 2048");row("sword","mixed-ordinal-over-native-cap",p.level(),expected,cow.incoming);melee++;clear(cleanup);
    }
    private static void externalCap(ServerPlayer p,List<Entity> cleanup)throws Exception{
        var attribute=p.getAttribute(Attributes.ATTACK_DAMAGE);var id=GuogaologyMod.id("qa_combat0611_external");
        try{
            p.getInventory().clearContent();for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.MAINHAND,gear("true_omega_sword"));equipment(p);attribute.addTransientModifier(new AttributeModifier(id,5000,AttributeModifier.Operation.ADD_VALUE));
            for(boolean withBook:new boolean[]{false,true}){
                p.setItemSlot(EquipmentSlot.OFFHAND,withBook?gear("true_omega_manuscript"):ItemStack.EMPTY);equipment(p);near(p.getAttributeValue(Attributes.ATTACK_DAMAGE),2048,"unrelated oversized modifier keeps normal attribute clipping");double expected=2048+(withBook?(deep(p.level())?6.4:4):0);var field=LivingEntity.class.getDeclaredField("attackStrengthTicker");field.setAccessible(true);field.setInt(p,1000);p.setOnGround(true);p.setSprinting(false);p.fallDistance=0;var cow=target(p.level(),cleanup);p.attack(cow);check(cow.accepted,"external modifier real attack reaches native damage entry");near(cow.incoming,expected,"recover only manuscript contribution, preserve other source clipping");row("sword",withBook?"external-cap-with-bare-book":"external-cap-no-book",p.level(),expected,cow.incoming);melee++;clear(cleanup);
            }
        }finally{attribute.removeModifier(id);equipment(p);}
    }
    public static void run(ServerPlayer p)throws Exception{
        checks=arrows=melee=disk=oldGear=0;while(!ROWS.isEmpty())ROWS.remove(0);var oldWorld=p.level();var position=p.position();var velocity=p.getDeltaMovement();float yaw=p.getYRot(),pitch=p.getXRot();boolean ground=p.onGround();var mode=p.gameMode();var inventory=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)inventory.add(p.getInventory().getItem(i).copy());var cleanup=new ArrayList<Entity>();
        try{
            p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.setSprinting(false);var server=p.level().getServer();int realm=0;
            for(var world:List.of(server.overworld(),server.getLevel(GuogaologyMod.OUTER),server.getLevel(GuogaologyMod.DIMENSION),server.getLevel(GuogaologyMod.GUOGAO))){
                check(world!=null,"all four actual realms available");move(p,world);
                for(int tier=1;tier<=4;tier++){
                    arrowCase(p,tier,new int[0],0,new int[0],false,20,"bare-tier"+tier,cleanup);
                    for(int grade=1;grade<=3;grade++){arrowCase(p,tier,new int[]{grade},0,new int[0],false,20,"tier"+tier+"-core"+grade,cleanup);swordCase(p,tier,new int[]{grade},0,new int[0],"tier"+tier+"-core"+grade,cleanup);}
                    swordCase(p,4,new int[0],tier,new int[0],"bare-book-tier"+tier,cleanup);near(GearData.protectionFactor(p),1+.05*tier,"bare book defense does not inherit attack buff");
                }
                for(var own:List.of(new int[]{1,3},new int[]{1,2,3},copies(8,3))){arrowCase(p,4,own,0,new int[0],false,20,"rss-"+Arrays.toString(own),cleanup);swordCase(p,4,own,0,new int[0],"rss-"+Arrays.toString(own),cleanup);}
                arrowCase(p,4,copies(8,3),4,copies(6,3),false,20,"omega-max-weapon-and-book",cleanup);swordCase(p,4,copies(8,3),4,copies(6,3),"omega-max-weapon-and-book",cleanup);
                arrowCase(p,4,new int[0],0,new int[0],true,20,"powerV-no-book",cleanup);arrowCase(p,4,new int[0],4,copies(6,3),true,20,"powerV-max-book",cleanup);
                arrowCase(p,4,new int[]{3},4,new int[]{2,3},false,10,"half-draw-book-scales-after-native",cleanup);
                if(deep(world))overCap(p,cleanup);externalCap(p,cleanup);if(realm==0||realm==2){persistence(p,cleanup);critical(p,cleanup);legacyGear(p);}realm++;
            }
            var output=new JsonObject();output.addProperty("success",true);output.addProperty("checks",checks);output.addProperty("realArrowsReleased",arrows);output.addProperty("realPlayerAttacks",melee);output.addProperty("compressedArrowDiskRoundTrips",disk);output.addProperty("legacyGearDiskRefreshes",oldGear);output.addProperty("realDimensions",4);output.addProperty("criticalTrials",48);output.addProperty("version",FabricLoader.getInstance().getModContainer("guogaology").orElseThrow().getMetadata().getVersion().getFriendlyString());output.add("cases",ROWS);output.addProperty("scope","Real native release and damage entry. Exact nominal impact speed is explicitly set after measuring native launch speed; trajectory/range is not simulated. Probe subclass records incoming damage, then delegates to real native Cow hurtServer. Old-schema controls are synthetic revision49 gear/payloads saved/reloaded through real compressed entity/item NBT and native inventory refresh, not a full old-world upgrade.");Files.writeString(FabricLoader.getInstance().getGameDir().resolve("combat0611-native.json"),new GsonBuilder().setPrettyPrinting().create().toJson(output)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("COMBAT0611_NATIVE_OK checks="+checks+" arrows="+arrows+" melee="+melee+" disk="+disk);
        }finally{clear(cleanup);p.stopUsingItem();p.getInventory().clearContent();for(int i=0;i<inventory.size();i++)p.getInventory().setItem(i,inventory.get(i));p.setGameMode(mode);p.teleport(new TeleportTransition(oldWorld,position,velocity,yaw,pitch,TeleportTransition.DO_NOTHING));p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());p.setOnGround(ground);equipment(p);}
    }
}
