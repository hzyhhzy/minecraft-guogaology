package dev.guogaology.mergeqa;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.*;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Actual native player mining pipeline, including water and bound-book editing. QA only. */
public final class ManuscriptMining043Checks {
    private static int checks;
    private static final BlockPos FIXTURE=new BlockPos(1536,250,1536);
    private static final Identifier SPEED=Identifier.parse("guogaology:qa_manuscript043_speed");
    private static final String[] TIERS={"omega","epsilon","gamma","true_omega"};
    private static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private ManuscriptMining043Checks(){}
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void near(double actual,double expected,String label){
        check(Math.abs(actual-expected)<=Math.max(.0002,Math.abs(expected)*.00002),label+": "+actual+" != "+expected);
    }
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("guogaology:"+name)));}
    private static ItemStack book(int tier,String... cores){
        var stack=item(TIERS[tier-1]+"_manuscript");GearData.setCores(stack,Arrays.stream(cores).map(ManuscriptMining043Checks::item).toList());GearData.refresh(stack);return stack;
    }
    private static void equipment(ServerPlayer p)throws Exception{
        var update=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");update.setAccessible(true);update.invoke(p);
    }
    private static void hold(ServerPlayer p,ItemStack tool,ItemStack book)throws Exception{
        p.setItemSlot(EquipmentSlot.MAINHAND,tool);p.setItemSlot(EquipmentSlot.OFFHAND,book);equipment(p);
    }
    private static void fluid(ServerPlayer p)throws Exception{
        var update=Entity.class.getDeclaredMethod("updateFluidInteraction");update.setAccessible(true);update.invoke(p);
    }
    private static void move(ServerPlayer p,ServerLevel world,Map<ServerLevel,Map<BlockPos,BlockState>> saved)throws Exception{
        check(world!=null,"required realm exists");
        if(!saved.containsKey(world)){
            var cells=new LinkedHashMap<BlockPos,BlockState>();saved.put(world,cells);
            for(var pos:BlockPos.betweenClosed(FIXTURE.offset(-1,0,-1),FIXTURE.offset(1,3,1))){
                cells.put(pos.immutable(),world.getBlockState(pos));world.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
        }
        p.teleport(new TeleportTransition(world,Vec3.atBottomCenterOf(FIXTURE),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
        p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        p.setOnGround(true);fluid(p);check(!p.isEyeInFluid(FluidTags.WATER),"dry mining fixture "+world.dimension());
    }
    public static void run(ServerPlayer p)throws Exception{
        checks=0;
        var oldWorld=p.level();var oldPosition=p.position();var oldVelocity=p.getDeltaMovement();var oldMode=p.gameMode();
        float oldYaw=p.getYRot(),oldPitch=p.getXRot();boolean oldGround=p.onGround(),oldGravity=p.isNoGravity();
        var inventory=p.getInventory();int selected=inventory.getSelectedSlot();var savedInventory=new ArrayList<ItemStack>();
        for(int i=0;i<inventory.getContainerSize();i++)savedInventory.add(inventory.getItem(i));
        var effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();
        var savedFixtures=new LinkedHashMap<ServerLevel,Map<BlockPos,BlockState>>();
        try{
            p.closeContainer();for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,ItemStack.EMPTY);
            for(var slot:SLOTS)p.setItemSlot(slot,ItemStack.EMPTY);equipment(p);p.removeAllEffects();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
            move(p,p.level().getServer().overworld(),savedFixtures);
            near(p.getAttributeValue(Attributes.MINING_EFFICIENCY),0,"unenchanted native mining efficiency baseline");
            near(p.getAttributeValue(Attributes.BLOCK_BREAK_SPEED),1,"native break speed baseline");
            near(p.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED),.2,"native underwater penalty baseline");
            bareAndVanilla(p);toolsAndCores(p);penalties(p);handPriority(p);editing(p);
            var server=p.level().getServer();
            for(var world:List.of(server.overworld(),server.getLevel(GuogaologyMod.OUTER),server.getLevel(GuogaologyMod.DIMENSION),server.getLevel(GuogaologyMod.GUOGAO))){
                move(p,world,savedFixtures);p.removeAllEffects();
                hold(p,new ItemStack(Items.IRON_PICKAXE),book(4,"sequence_core_lv3","sequence_core_lv2","ordinal_crystal_lv4"));
                near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),(6+Math.sqrt(320))*(1.4+Math.sqrt(1.25)),"realm-independent mining "+world.dimension());
            }
            System.out.println("MANUSCRIPT_MINING_043_OK checks="+checks+" real hand/wrong/iron/Omega/EfficiencyV / mixed RSS / additive Haste / fatigue+attribute+water+air / four realms / hand priority / deferred menu activation");
        }finally{
            p.closeContainer();p.containerMenu=p.inventoryMenu;p.removeAllEffects();p.getAttribute(Attributes.BLOCK_BREAK_SPEED).removeModifier(SPEED);
            for(var world:savedFixtures.entrySet())for(var cell:world.getValue().entrySet())world.getKey().setBlock(cell.getKey(),cell.getValue(),2);
            for(int i=0;i<savedInventory.size();i++)inventory.setItem(i,savedInventory.get(i));inventory.setSelectedSlot(selected);equipment(p);
            p.setGameMode(oldMode);p.teleport(new TeleportTransition(oldWorld,oldPosition,oldVelocity,oldYaw,oldPitch,TeleportTransition.DO_NOTHING));
            p.hasChangedDimension();if(!p.connection.hasClientLoaded())p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            p.setNoGravity(oldGravity);p.setOnGround(oldGround);fluid(p);for(var effect:effects)p.addEffect(effect);
        }
    }
    private static ItemStack efficiencyPick(ServerPlayer p){
        var stack=new ItemStack(Items.NETHERITE_PICKAXE);
        stack.enchant(p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY),5);return stack;
    }
    private static void bareAndVanilla(ServerPlayer p)throws Exception{
        var state=Blocks.STONE.defaultBlockState();
        for(int tier=1;tier<=4;tier++){
            hold(p,ItemStack.EMPTY,book(tier));near(p.getDestroySpeed(state),1+.1*tier,"bare manuscript hand tier "+tier);
            near(EquipmentRules.bookMiningBase(tier),.1*tier,"public innate mining rate tier "+tier);
        }
        var tools=List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_AXE),new ItemStack(Items.IRON_PICKAXE),item("true_omega_pickaxe"),efficiencyPick(p));
        double[] speeds={1,1,6,18,35};
        for(int n=0;n<tools.size();n++){
            hold(p,tools.get(n),ItemStack.EMPTY);near(p.getDestroySpeed(state),speeds[n],"native no-book tool "+n);
            p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,1));near(p.getDestroySpeed(state),speeds[n]*1.4,"native no-book HasteII tool "+n);p.removeEffect(MobEffects.HASTE);
        }
    }
    private static void toolsAndCores(ServerPlayer p)throws Exception{
        String[][] sockets={{},{"sequence_core"},{"sequence_core_lv2"},{"sequence_core_lv3"},
                {"sequence_core_lv3","sequence_core_lv2","ordinal_crystal_lv4"},{"sequence_core","sequence_core"}};
        double[] flat={0,4,8,16,Math.sqrt(320),Math.sqrt(32)},rate={0,.25,.5,1,Math.sqrt(1.25),Math.sqrt(.125)};
        for(int n=0;n<sockets.length;n++){
            var book=book(4,sockets[n]);var profile=GearData.profile(book);
            near(EquipmentRules.manuscriptMiningFlat(profile),flat[n],"public flat RSS "+n);
            near(EquipmentRules.manuscriptMiningRate(profile),rate[n],"public rate RSS "+n);
            var tools=List.of(ItemStack.EMPTY,new ItemStack(Items.IRON_AXE),new ItemStack(Items.IRON_PICKAXE),item("true_omega_pickaxe"),efficiencyPick(p));
            double[] nativeSpeed={1,1,6,18,35};
            for(int tool=0;tool<tools.size();tool++){
                hold(p,tools.get(tool),book);double expected=(nativeSpeed[tool]+flat[n])*(1.4+rate[n]);
                near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),expected,"real native tool+Sequence "+n+" / "+tool);
                near(EquipmentRules.manuscriptMiningSpeed(nativeSpeed[tool],4,profile,0),expected,"public mining formula "+n+" / "+tool);
                // Dirt never receives native Efficiency, even on the enchanted pick. It still receives the manuscript flat term.
                near(p.getDestroySpeed(Blocks.DIRT.defaultBlockState()),(1+flat[n])*(1.4+rate[n]),"wrong-tool dirt receives manuscript "+n+" / "+tool);
                p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,1));
                near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),(nativeSpeed[tool]+flat[n])*(1.8+rate[n]),"HasteII joins same additive bucket "+n+" / "+tool);
                p.removeEffect(MobEffects.HASTE);
            }
        }
    }
    private static void penalties(ServerPlayer p)throws Exception{
        hold(p,new ItemStack(Items.IRON_PICKAXE),book(4,"sequence_core_lv3","sequence_core_lv2"));
        p.addEffect(new MobEffectInstance(MobEffects.HASTE,200,1));double boosted=(6+Math.sqrt(320))*(1.8+Math.sqrt(1.25));
        p.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,200,1));
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),boosted*.09,"FatigueII multiplies after manuscript and Haste");
        p.getAttribute(Attributes.BLOCK_BREAK_SPEED).addTransientModifier(new AttributeModifier(SPEED,.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),boosted*.09*1.5,"native break attribute remains after fatigue");
        p.setOnGround(false);near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),boosted*.09*1.5/5,"airborne divides full result by five");p.setOnGround(true);
        for(var pos:BlockPos.betweenClosed(FIXTURE.offset(-1,0,-1),FIXTURE.offset(1,2,1)))p.level().setBlock(pos,Blocks.WATER.defaultBlockState(),2);
        fluid(p);check(p.isEyeInFluid(FluidTags.WATER),"actual eye submerged in QA water");
        near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),boosted*.09*1.5*.2,"water multiplies full result by native submerged speed");
        p.setOnGround(false);near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),boosted*.09*1.5*.2/5,"water and airborne penalties compose after manuscript");
        for(var pos:BlockPos.betweenClosed(FIXTURE.offset(-1,0,-1),FIXTURE.offset(1,2,1)))p.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        fluid(p);p.setOnGround(true);p.getAttribute(Attributes.BLOCK_BREAK_SPEED).removeModifier(SPEED);p.removeAllEffects();
    }
    private static void handPriority(ServerPlayer p)throws Exception{
        var main=book(4,"sequence_core_lv3");var off=book(1,"sequence_core");
        hold(p,main,ItemStack.EMPTY);near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),17*2.4,"mainhand manuscript activates when offhand has none");
        hold(p,main,new ItemStack(Items.STICK));near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),17*2.4,"ordinary offhand does not hide mainhand manuscript");
        hold(p,main,off);near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),5*1.35,"offhand book overrides stronger mainhand book");
        hold(p,main,book(1));near(p.getDestroySpeed(Blocks.STONE.defaultBlockState()),1.1,"empty offhand book still wins over mainhand cores");
    }
    private static void editing(ServerPlayer p)throws Exception{
        var current=book(4,"sequence_core");hold(p,new ItemStack(Items.IRON_PICKAXE),current);
        var state=Blocks.STONE.defaultBlockState();double before=10*1.65,after=22*2.4;
        near(p.getDestroySpeed(state),before,"pre-edit active mining");
        p.getInventory().setItem(9,item("sequence_core_lv3"));
        var menu=new ManuscriptMenu(84,p.getInventory(),p.level(),40,current);p.containerMenu=menu;
        menu.clicked(EnhancementMenu.FIRST_CORE,0,ContainerInput.PICKUP,p);
        check(menu.getCarried().is(item("sequence_core").getItem())&&GearData.profile(current).isEmpty(),"real menu removal immediately transfers ownership");
        near(p.getDestroySpeed(state),before,"removing a core preserves opening mining until close");
        menu.clicked(EnhancementMenu.INVENTORY,0,ContainerInput.PICKUP,p);menu.clicked(EnhancementMenu.FIRST_CORE,0,ContainerInput.PICKUP,p);
        check(menu.getCarried().isEmpty()&&GearData.profile(current).getFirst().level()==3,"real menu inserts upgraded core");
        check(p.getInventory().getItem(9).is(item("sequence_core").getItem()),"old core returned intact to inventory");
        near(p.getDestroySpeed(state),before,"new core remains inactive while editing");
        p.closeContainer();near(p.getDestroySpeed(state),after,"closing commits changed mining effect");
        hold(p,new ItemStack(Items.IRON_PICKAXE),ItemStack.EMPTY);near(p.getDestroySpeed(state),6,"unequipping clears manuscript mining immediately");
    }
}
