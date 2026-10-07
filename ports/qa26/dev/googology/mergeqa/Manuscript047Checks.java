package dev.googology.mergeqa;

import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import dev.googology.portal.PortalTravel;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Development-only real menus, player mixins and native realm-transfer checks; no terrain fixtures. */
public final class Manuscript047Checks {
    public static void display(ServerPlayer player){
        player.closeContainer();player.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        player.setItemSlot(EquipmentSlot.OFFHAND,book("power_tower_core_lv3","hydra_bud_lv3","lho_trace_lv3","laver_core_lv2","boundary_core_lv3","ordinal_crystal_lv4"));
        player.getInventory().setItem(9,item("guogao_heart_lv3"));player.getInventory().setItem(10,item("astra_critical_core_lv2"));player.getInventory().setItem(11,item("sequence_core_lv3"));
        DenxiManuscript.open(player,net.minecraft.world.InteractionHand.OFF_HAND);
    }
    private static int checks;
    private static final List<EquipmentSlot> PLAYER_SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final AABB FIXTURE_AREA=new AABB(508,496,508,517,505,517);
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    private static void near(double actual,double expected,String label){check(Math.abs(actual-expected)<Math.max(.0001,Math.abs(expected)*.00001),label+": "+actual+" != "+expected);}
    public static int count(){return checks;}
    private static ItemStack item(String id){var value=BuiltInRegistries.ITEM.getValue(Identifier.parse("googology:"+id));if(value==null||value==Items.AIR)throw new AssertionError("fixture item missing: "+id);return new ItemStack(value);}
    private static ItemStack book(String... cores){var value=item("true_omega_manuscript");GearData.setCores(value,Arrays.stream(cores).map(Manuscript047Checks::item).toList());GearData.refresh(value);return value;}
    @SuppressWarnings("unchecked") private static Map<UUID,Object> map(Class<?> owner,String name)throws Exception{var field=owner.getDeclaredField(name);field.setAccessible(true);return (Map<UUID,Object>)field.get(null);}
    @SuppressWarnings("unchecked") private static Set<UUID> landing()throws Exception{var field=ManuscriptEffects.class.getDeclaredField("LANDING");field.setAccessible(true);return (Set<UUID>)field.get(null);}
    private static Object copyEntry(Object entry){return entry instanceof Map<?,?> values?new HashMap<>(values):entry;}
    private static void restoreEntry(Map<UUID,Object> values,UUID id,Object saved){if(saved==null)values.remove(id);else values.put(id,saved);}
    private static void move(ServerPlayer player,ServerLevel world,Vec3 position){player.teleport(new TeleportTransition(world,position,Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));player.setOnGround(true);}
    private static void empty(ServerPlayer player){for(int i=0;i<player.getInventory().getContainerSize();i++)player.getInventory().setItem(i,ItemStack.EMPTY);for(var slot:PLAYER_SLOTS)player.setItemSlot(slot,ItemStack.EMPTY);}
    private static Set<UUID> drops(ServerPlayer player){var values=new HashSet<UUID>();for(var entity:player.level().getEntitiesOfClass(ItemEntity.class,player.getBoundingBox().inflate(4)))values.add(entity.getUUID());return values;}
    private static int loose(ServerPlayer player,Item core){int total=0;for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(stack.is(core))total+=stack.getCount();}return total;}
    private static float nativeHorizontal(ServerPlayer player)throws Exception{var method=Player.class.getDeclaredMethod("getFlyingSpeed");method.setAccessible(true);return (float)method.invoke(player);}
    private static double ascent(double velocity,double gravity){double height=0;while(velocity>0){height+=velocity;velocity=(velocity-gravity)*.98;}return height;}
    private static void portalTick(MinecraftServer server)throws Exception{var method=PortalTravel.class.getDeclaredMethod("tick",MinecraftServer.class);method.setAccessible(true);method.invoke(null,server);}

    public static void run(ServerPlayer player)throws Exception{
        checks=0;var id=player.getUUID();var inventory=player.getInventory();var savedInventory=new ArrayList<ItemStack>();for(int i=0;i<inventory.getContainerSize();i++)savedInventory.add(inventory.getItem(i));
        var savedEquipment=new EnumMap<EquipmentSlot,ItemStack>(EquipmentSlot.class);for(var slot:PLAYER_SLOTS)savedEquipment.put(slot,player.getItemBySlot(slot));
        var oldWorld=player.level();var oldPosition=player.position();var oldVelocity=player.getDeltaMovement();float yaw=player.getYRot(),pitch=player.getXRot(),health=player.getHealth(),absorption=player.getAbsorptionAmount();double fall=player.fallDistance;
        var oldMode=player.gameMode();var oldMenu=player.containerMenu;var oldCursor=oldMenu.getCarried();int selected=inventory.getSelectedSlot(),ticks=player.tickCount,cooldown=player.getPortalCooldown(),invulnerableTime=player.invulnerableTime;
        boolean grounded=player.onGround(),sprinting=player.isSprinting(),noGravity=player.isNoGravity();var abilities=player.getAbilities();boolean mayfly=abilities.mayfly,flying=abilities.flying,invulnerable=abilities.invulnerable,instabuild=abilities.instabuild,mayBuild=abilities.mayBuild;float flySpeed=abilities.getFlyingSpeed(),walkSpeed=abilities.getWalkingSpeed();
        var effects=new ArrayList<>(player.getActiveEffects());var tags=new HashSet<>(player.entityTags());boolean owned=ManuscriptEffects.ownsFlight(player),landing=landing().contains(id);
        var savedFlight=copyEntry(map(ManuscriptEffects.class,"FLIGHTS").get(id));var savedEffects=copyEntry(map(ManuscriptEffects.class,"EFFECTS").get(id));var savedSafe=map(PortalTravel.class,"LAST_SAFE").get(id);
        var home=oldWorld.getServer().overworld();var savedFixtureDrops=new HashSet<UUID>();for(var entity:home.getEntitiesOfClass(ItemEntity.class,FIXTURE_AREA))savedFixtureDrops.add(entity.getUUID());
        try{
            player.containerMenu=player.inventoryMenu;empty(player);player.setGameMode(GameType.SURVIVAL);player.setOnGround(true);ManuscriptEffects.tick(player);player.removeAllEffects();player.setNoGravity(false);player.tickCount=81;player.setSprinting(false);player.setHealth(20);player.setAbsorptionAmount(0);
            abilities.mayfly=false;abilities.flying=false;abilities.invulnerable=false;move(player,oldWorld.getServer().overworld(),new Vec3(512.5,500,512.5));
            menus(player);flight(player);potions(player);realmFalls(player);
            System.out.println("MANUSCRIPT047_CHECKS_OK checks="+checks+" bound book sockets / transfer and grade limits / packet locks / jump and flight ownership / real potions / realm fall thresholds");
        }finally{
            if(player.containerMenu instanceof ManuscriptMenu)player.closeContainer();player.containerMenu=player.inventoryMenu;
            empty(player);player.setOnGround(true);ManuscriptEffects.tick(player);player.removeAllEffects();
            for(int i=0;i<savedInventory.size();i++)inventory.setItem(i,savedInventory.get(i));for(var slot:savedEquipment.keySet())player.setItemSlot(slot,savedEquipment.get(slot));inventory.setSelectedSlot(selected);
            player.setGameMode(oldMode);move(player,oldWorld,oldPosition);player.tickCount=ticks;ManuscriptEffects.tick(player);player.removeAllEffects();
            restoreEntry(map(ManuscriptEffects.class,"FLIGHTS"),id,savedFlight);restoreEntry(map(ManuscriptEffects.class,"EFFECTS"),id,savedEffects);restoreEntry(map(PortalTravel.class,"LAST_SAFE"),id,savedSafe);
            if(landing)landing().add(id);else landing().remove(id);((ManuscriptFlightAccess)player).googology$setManuscriptFlight(owned);for(var effect:effects)player.addEffect(effect);
            for(var tag:new ArrayList<>(player.entityTags()))player.removeTag(tag);for(var tag:tags)player.addTag(tag);
            abilities.mayfly=mayfly;abilities.flying=flying;abilities.invulnerable=invulnerable;abilities.instabuild=instabuild;abilities.mayBuild=mayBuild;abilities.setFlyingSpeed(flySpeed);abilities.setWalkingSpeed(walkSpeed);player.onUpdateAbilities();
            player.setNoGravity(noGravity);player.setSprinting(sprinting);player.setOnGround(grounded);player.setDeltaMovement(oldVelocity);player.fallDistance=fall;player.setPortalCooldown(cooldown);player.invulnerableTime=invulnerableTime;player.setYRot(yaw);player.setXRot(pitch);player.setAbsorptionAmount(absorption);player.setHealth(Math.min(health,player.getMaxHealth()));player.containerMenu=oldMenu;oldMenu.setCarried(oldCursor);
            for(var entity:home.getEntitiesOfClass(ItemEntity.class,FIXTURE_AREA))if(!savedFixtureDrops.contains(entity.getUUID()))entity.discard();
        }
    }
    private static void menus(ServerPlayer player){
        empty(player);player.setItemSlot(EquipmentSlot.OFFHAND,book("laver_core"));check(player.getOffhandItem().getItem().use(player.level(),player,net.minecraft.world.InteractionHand.OFF_HAND)==net.minecraft.world.InteractionResult.PASS&&player.containerMenu==player.inventoryMenu,"offhand never intercepts ordinary use");check(DenxiManuscript.open(player,net.minecraft.world.InteractionHand.OFF_HAND),"inventory button opens real offhand book");check(!DenxiManuscript.open(player,net.minecraft.world.InteractionHand.OFF_HAND),"repeated open request rejected while editing");player.closeContainer();
        empty(player);var main=book("power_tower_core_lv3");var off=book("laver_core_lv2");player.setItemSlot(EquipmentSlot.MAINHAND,main);player.setItemSlot(EquipmentSlot.OFFHAND,off);
        main.getItem().use(player.level(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        check(player.containerMenu instanceof ManuscriptMenu&&((ManuscriptMenu)player.containerMenu).gear()==off,"two held manuscripts prefer offhand editor");
        check(GearData.profile(ManuscriptEffects.held(player)).equals(GearData.profile(off)),"two held manuscripts only activate offhand cores");player.closeContainer();

        for(boolean offhand:new boolean[]{true,false}){
            empty(player);player.getInventory().setSelectedSlot(2);var original=item("omega_manuscript");int bound=offhand?40:2;player.getInventory().setItem(bound,original);
            var menu=new ManuscriptMenu(61,player.getInventory(),player.level(),bound,original);player.containerMenu=menu;
            check(menu.gear()==original&&player.getInventory().getItem(bound)==original,"original book bound in "+(offhand?"offhand":"mainhand"));check(menu.stillValid(player)&&menu.capacity()==2,"valid tier-one menu has two sockets");check(!original.isDamageableItem(),"manuscript has no durability");
            check(!menu.slots.get(0).mayPickup(player)&&!menu.slots.get(0).mayPlace(item("epsilon_manuscript")),"book display cannot move or replace original");check(menu.quickMoveStack(player,0).isEmpty(),"shift click cannot extract displayed book");menu.clicked(0,0,ContainerInput.PICKUP,player);check(menu.getCarried().isEmpty(),"display pickup leaves cursor empty");
            var high=item("astra_critical_core_lv3");var highItem=high.getItem();player.getInventory().setItem(9,high);check(!menu.canInsertCore(high)&&!menu.quickMoveStack(player,11).isEmpty()&&player.getInventory().getItem(9).isEmpty()&&loose(player,highItem)==1&&menu.installed()==0,"incompatible Astra core sorts normally while rejected by manuscript sockets");
            var core=item("sequence_core");var coreItem=core.getItem();core.setCount(3);player.getInventory().setItem(9,core);check(menu.canInsertCore(core),"usable core highlighted by real slot predicate");menu.clicked(11,0,ContainerInput.QUICK_MOVE,player);check(GearData.cores(original).size()==2&&loose(player,coreItem)==1,"actual Shift fills two real sockets and retains excess");int excessSlot=menu.slots.stream().filter(s->s.index>=EnhancementMenu.INVENTORY&&s.getItem().is(coreItem)).findFirst().orElseThrow().index;check(!menu.canInsertCore(menu.getSlot(excessSlot).getItem())&&!menu.quickMoveStack(player,excessSlot).isEmpty()&&GearData.cores(original).size()==2,"full book sorts excess without adding a socket");
            check(ManuscriptEffects.level(ManuscriptEffects.held(player),0)==0,"installed cores wait until closing to activate");check(GearData.snapshot(player).book().isEmpty(),"defense/health snapshot also remains unchanged during editing");check(GearData.cores(original).size()+loose(player,coreItem)==3,"installed and loose core conservation");menu.quickMoveStack(player,1);check(GearData.cores(original).size()==1&&loose(player,coreItem)==2,"shift removal returns exactly one core");int looseSlot=menu.slots.stream().filter(s->s.index>=EnhancementMenu.INVENTORY&&s.getItem().is(coreItem)).findFirst().orElseThrow().index;menu.quickMoveStack(player,looseSlot);check(GearData.cores(original).size()==2&&loose(player,coreItem)==1,"reinsert persists on original book");
            menu.clicked(11,bound,ContainerInput.SWAP,player);check(player.getInventory().getItem(bound)==original&&GearData.cores(original).size()==2,"slot swap cannot replace bound book");
            player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,BlockPos.ZERO,Direction.DOWN));check(player.getInventory().getItem(bound)==original,"independent swap-hand packet is locked");
            if(!offhand){int slot=38+bound;menu.clicked(slot,0,ContainerInput.THROW,player);check(player.getInventory().getItem(bound)==original&&menu.quickMoveStack(player,slot).isEmpty(),"bound hotbar slot rejects throw and shift");player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS,BlockPos.ZERO,Direction.DOWN));check(player.getInventory().getItem(bound)==original,"independent drop packet is locked");player.connection.handleSetCarriedItem(new ServerboundSetCarriedItemPacket(3));check(player.getInventory().getSelectedSlot()==bound&&menu.stillValid(player),"independent hotbar-selection packet is locked");}
            var before=drops(player);player.closeContainer();check(player.getInventory().getItem(bound)==original&&GearData.cores(original).size()==2&&loose(player,coreItem)==1,"close preserves original and core totals");check(ManuscriptEffects.level(ManuscriptEffects.held(player),0)==1,"closing activates edited cores");check(drops(player).equals(before),"close creates no core-copy drops");check(!menu.stillValid(player),"closed menu is invalid");
            var stale=new ManuscriptMenu(62,player.getInventory(),player.level(),bound,original);player.containerMenu=stale;player.getInventory().setItem(bound,ItemStack.EMPTY);check(!stale.stillValid(player)&&stale.quickMoveStack(player,1).isEmpty(),"removed original invalidates stale socket requests");player.getInventory().setItem(bound,original);player.closeContainer();check(GearData.cores(original).size()+loose(player,coreItem)==3,"stale menu closure does not duplicate cores");
        }
        empty(player);var dragged=item("omega_manuscript");player.getInventory().setItem(40,dragged);var dragMenu=new ManuscriptMenu(64,player.getInventory(),player.level(),40,dragged);player.containerMenu=dragMenu;
        var dragCore=item("sequence_core");var dragItem=dragCore.getItem();dragCore.setCount(2);player.getInventory().setItem(9,dragCore);dragMenu.clicked(11,0,ContainerInput.PICKUP,player);check(dragMenu.getCarried().getCount()==2,"actual pickup moves two cores onto cursor");
        dragMenu.clicked(-999,0,ContainerInput.QUICK_CRAFT,player);dragMenu.clicked(1,1,ContainerInput.QUICK_CRAFT,player);dragMenu.clicked(2,1,ContainerInput.QUICK_CRAFT,player);dragMenu.clicked(-999,2,ContainerInput.QUICK_CRAFT,player);check(dragMenu.getCarried().isEmpty()&&GearData.cores(dragged).size()==2,"actual drag spreads one core into each real socket");
        dragMenu.clicked(1,0,ContainerInput.PICKUP,player);check(dragMenu.getCarried().getCount()==1&&GearData.cores(dragged).size()==1,"actual socket pickup updates original component");dragMenu.clicked(12,0,ContainerInput.PICKUP,player);check(dragMenu.getCarried().isEmpty()&&loose(player,dragItem)==1,"removed cursor core deposits into inventory");player.closeContainer();check(GearData.cores(dragged).size()+loose(player,dragItem)==2,"drag and pickup preserve total cores after closing");
        String[] books={"omega_manuscript","epsilon_manuscript","gamma_manuscript","true_omega_manuscript"};int[] capacities={2,3,4,6},grades={3,3,3,3};
        for(int tier=0;tier<books.length;tier++){
            empty(player);var original=item(books[tier]);player.getInventory().setItem(40,original);var menu=new ManuscriptMenu(63,player.getInventory(),player.level(),40,original);player.containerMenu=menu;
            check(menu.capacity()==capacities[tier]&&menu.rank()==grades[tier],"four manuscript capacity/grade contract "+tier);check(!menu.slots.get(1+capacities[tier]).mayPlace(item("sequence_core")),"locked excess socket rejects input "+tier);
            for(var core:EquipmentRules.CORES)check(menu.canInsertCore(item(core))!=(GearData.type(item(core))==5),"eight allowed manuscript families and critical rejection: "+tier+" / "+core);
            check(menu.canInsertCore(item("ordinal_crystal_lv4"))&&menu.canInsertCore(item("sequence_core_lv3")),"all manuscript tiers accept displayed Lv3 regional and ordinal cores "+tier);player.closeContainer();
        }
        empty(player);ManuscriptEffects.tick(player);
    }
    private static void flight(ServerPlayer player)throws Exception{
        player.setGameMode(GameType.SURVIVAL);player.setSprinting(false);double walk=player.getAttributeValue(Attributes.MOVEMENT_SPEED);player.setDeltaMovement(Vec3.ZERO);player.jumpFromGround();float baseline=(float)player.getDeltaMovement().y;
        player.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core","boundary_core","boundary_core_lv3"));ManuscriptEffects.tick(player);check(ManuscriptEffects.jumpBlocks(player)==2,"Lv1 jumps survive alongside Lv3 flight");player.setDeltaMovement(Vec3.ZERO);player.jumpFromGround();near(ascent(player.getDeltaMovement().y,player.getGravity())-ascent(baseline,player.getGravity()),2,"actual native jump gains two blocks");
        var abilities=player.getAbilities();check(abilities.mayfly&&ManuscriptEffects.ownsFlight(player),"survival flight permission and synchronized ownership granted");abilities.flying=true;player.setOnGround(false);
        near(nativeHorizontal(player),.05,"ordinary Lv3 native horizontal speed");near(ManuscriptEffects.verticalSpeed(player,.77f),.05,"ordinary Lv3 vertical speed");player.setSprinting(true);near(nativeHorizontal(player),.05,"vanilla sprint alone does not own manuscript acceleration");player.setSprinting(false);ManuscriptEffects.setFlightSprint(player,true);near(nativeHorizontal(player),.1,"ordinary Lv3 dedicated sprint horizontal");near(ManuscriptEffects.verticalSpeed(player,.77f),.1,"ordinary Lv3 dedicated sprint vertical");
        abilities.flying=false;ManuscriptEffects.tick(player);check(!ManuscriptEffects.flightSprint(player),"landing clears dedicated sprint");ManuscriptEffects.setFlightSprint(player,true);check(!ManuscriptEffects.flightSprint(player),"packet cannot accelerate while not flying");abilities.flying=true;
        player.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv2"));ManuscriptEffects.tick(player);near(nativeHorizontal(player),.05/6,"outer Lv2 is half the installed speed");near(ManuscriptEffects.verticalSpeed(player,.77f),.05/6,"outer Lv2 vertical half speed");ManuscriptEffects.setFlightSprint(player,true);check(!ManuscriptEffects.flightSprint(player),"Lv2 rejects sprint intent");player.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv3"));ManuscriptEffects.tick(player);
        move(player,player.level().getServer().getLevel(GoogologyMod.DIMENSION),new Vec3(512.5,500,512.5));ManuscriptEffects.tick(player);check(abilities.mayfly&&!abilities.flying&&ManuscriptEffects.ownsFlight(player),"realm change safely resets active flight before granting permission");abilities.flying=true;near(nativeHorizontal(player),.05,"deep non-sprint retains ordinary horizontal speed");near(ManuscriptEffects.verticalSpeed(player,.77f),.05,"deep non-sprint retains ordinary vertical speed");ManuscriptEffects.setFlightSprint(player,true);near(nativeHorizontal(player),.4,"deep dedicated sprint horizontal eightfold");near(ManuscriptEffects.verticalSpeed(player,.77f),.4,"deep dedicated sprint vertical eightfold");
        player.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv2"));ManuscriptEffects.tick(player);check(!ManuscriptEffects.flightSprint(player),"changing to Lv2 clears owned sprint");player.setSprinting(false);near(player.getAttributeValue(Attributes.MOVEMENT_SPEED),walk*1.5,"Lv2 adds fifty percent walking speed");near(nativeHorizontal(player),.05/3,"deep Lv2 retains installed horizontal speed");near(ManuscriptEffects.verticalSpeed(player,.77f),.05/3,"deep Lv2 retains installed vertical speed");ManuscriptEffects.setFlightSprint(player,true);near(nativeHorizontal(player),.05/3,"Lv2 sprint gives no horizontal boost");near(ManuscriptEffects.verticalSpeed(player,.77f),.05/3,"Lv2 sprint gives no vertical boost");
        player.setOnGround(false);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(player);check(!abilities.mayfly&&!abilities.flying&&!ManuscriptEffects.ownsFlight(player),"unequip removes owned survival permission");check(ManuscriptEffects.fallImmune(player),"airborne unequip has landing-only safety");player.setOnGround(true);ManuscriptEffects.tick(player);check(!ManuscriptEffects.fallImmune(player),"landing clears safety without retaining fall immunity");
        player.setGameMode(GameType.CREATIVE);abilities.flying=true;player.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv3"));ManuscriptEffects.tick(player);ManuscriptEffects.setFlightSprint(player,true);check(!ManuscriptEffects.flightSprint(player),"creative rejects manuscript sprint packets");check(!ManuscriptEffects.ownsFlight(player)&&abilities.mayfly&&abilities.flying,"creative flight is never claimed");player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(player);check(abilities.mayfly&&abilities.flying,"unequip preserves creative flight");near(ManuscriptEffects.horizontalSpeed(player,.77f),.77,"creative speed hook preserves another speed value");
        player.setGameMode(GameType.SURVIVAL);abilities.mayfly=true;abilities.flying=true;player.setItemSlot(EquipmentSlot.OFFHAND,book("boundary_core_lv3"));ManuscriptEffects.tick(player);check(!ManuscriptEffects.ownsFlight(player),"pre-existing external flight is not claimed");player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(player);check(abilities.mayfly&&abilities.flying,"unequip preserves pre-existing external flight");near(ManuscriptEffects.verticalSpeed(player,.66f),.66,"external vertical speed retained");abilities.mayfly=false;abilities.flying=false;player.setSprinting(false);player.setOnGround(true);move(player,player.level().getServer().overworld(),new Vec3(512.5,500,512.5));
    }
    private static void potions(ServerPlayer player){
        player.removeAllEffects();var original=book("lho_trace_lv2","boundary_core_lv2");player.setItemSlot(EquipmentSlot.OFFHAND,original);ManuscriptEffects.tick(player);
        for(var type:List.of(MobEffects.NIGHT_VISION,MobEffects.FIRE_RESISTANCE)){var owned=player.getEffect(type);check(owned!=null&&owned.isInfiniteDuration()&&!owned.isVisible(),"owned passive is long-lived and particle-free: "+type);var real=new MobEffectInstance(type,600,1,false,true,true);check(player.addEffect(real)&&player.getEffect(type)==real,"real potion replaces owned layer: "+type);}
        player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(player);for(var type:List.of(MobEffects.NIGHT_VISION,MobEffects.FIRE_RESISTANCE)){var real=player.getEffect(type);check(real!=null&&real.getDuration()==600&&real.getAmplifier()==1&&real.isVisible(),"real potion survives book removal: "+type);}
        player.setItemSlot(EquipmentSlot.OFFHAND,original);ManuscriptEffects.tick(player);player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);ManuscriptEffects.tick(player);for(var type:List.of(MobEffects.NIGHT_VISION,MobEffects.FIRE_RESISTANCE))check(player.getEffect(type).getDuration()==600,"existing real potion is not claimed on re-equip: "+type);player.removeAllEffects();
    }
    private static void realmFalls(ServerPlayer player)throws Exception{
        var server=player.level().getServer();var outer=server.getLevel(GoogologyMod.OUTER);var inner=server.getLevel(GoogologyMod.DIMENSION);var hell=server.getLevel(GoogologyMod.GUOGAO);
        check(outer!=null&&inner!=null&&hell!=null,"all three realm-transfer fixtures exist");check(map(PortalTravel.class,"PENDING").isEmpty()&&map(PortalTravel.class,"BLOCKED").isEmpty(),"threshold QA runs before queued portal fixtures");
        empty(player);player.setGameMode(GameType.SURVIVAL);player.setOnGround(true);ManuscriptEffects.tick(player);player.setHealth(20);player.invulnerableTime=0;
        move(player,outer,new Vec3(3.25,-499.99,-7.5));player.fallDistance=123;portalTick(server);check(player.level()==outer,"Outer Y=-499.99 does not transfer");near(player.getY(),-499.99,"Outer threshold preserves source position");near(player.fallDistance,123,"source fall distance is not prematurely cleared");check(!player.getAbilities().invulnerable&&!player.getAbilities().mayfly,"threshold path does not grant source invulnerability or flight");
        move(player,outer,new Vec3(3.25,-500,-7.5));player.fallDistance=321;player.setDeltaMovement(0,-2,0);portalTick(server);check(player.level()==inner,"Outer Y=-500 enters Inner");near(player.getX(),13,"Outer to Inner x scales fourfold");near(player.getZ(),-30,"Outer to Inner z scales fourfold");near(player.getY(),500,"Outer to Inner arrives at Y=500");near(player.fallDistance,0,"Outer transfer clears accumulated fall distance");near(player.getDeltaMovement().y,0,"Outer transfer clears source fall velocity");check(player.getPortalCooldown()==40,"Outer transfer applies portal cooldown");
        move(player,inner,new Vec3(2.25,-119,6.5));player.fallDistance=77;portalTick(server);check(player.level()==inner,"Inner Y=-119 does not transfer");near(player.fallDistance,77,"Inner source fall distance retained above threshold");move(player,inner,new Vec3(2.25,-120,6.5));player.fallDistance=222;portalTick(server);check(player.level()==hell,"Inner Y=-120 still enters Underworld");near(player.getX(),9,"Inner to Underworld x scales fourfold");near(player.getZ(),26,"Inner to Underworld z scales fourfold");near(player.getY(),500,"Inner to Underworld arrives at Y=500");near(player.fallDistance,0,"Inner transfer resets fall distance");
    }
}
