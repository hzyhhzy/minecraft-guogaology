package dev.guogaology.mergeqa;

import com.mojang.authlib.GameProfile;
import dev.guogaology.GuogaologyMod;
import dev.guogaology.mining.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Native 0.4.4 grade, station, capacity, component ownership and tooltip regression. */
final class Tier044Checks {
    private static int checks,highestInstalled,stationAttempts,digitalAttempts;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(GuogaologyMod.id(id)));}
    private static ItemStack core(int type,int raw){return item(EquipmentRules.CORES[type]+(raw==1?"":"_lv"+raw));}
    private static ServerPlayer dummy(ServerPlayer real){
        var profile=new GameProfile(UUID.randomUUID(),"Tier044");var p=new ServerPlayer(real.level().getServer(),real.level(),profile,ClientInformation.createDefault());
        p.connection=new ServerGamePacketListenerImpl(real.level().getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setGameMode(GameType.SURVIVAL);p.setPos(real.position());p.getInventory().setSelectedSlot(2);return p;
    }
    private static void empty(ServerPlayer p){p.closeContainer();for(int i=0;i<p.getInventory().getContainerSize();i++)p.getInventory().setItem(i,ItemStack.EMPTY);}
    private static void clearLoose(ServerPlayer p,EnhancementMenu menu){
        check(menu.installed()==0&&menu.getCarried().isEmpty(),"fixture only resets after all socket items have been returned");
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(!menu.manuscript()||i!=40)p.getInventory().setItem(i,ItemStack.EMPTY);
    }
    private static int loose(ServerPlayer p,Item item){int total=p.containerMenu.getCarried().is(item)?p.containerMenu.getCarried().getCount():0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))total+=s.getCount();}return total;}
    private static EnhancementMenu open(ServerPlayer p,BlockPos pos,ItemStack gear,int rank,boolean directBook){
        empty(p);EnhancementMenu menu;
        if(directBook){p.getInventory().setItem(40,gear);menu=new ManuscriptMenu(144,p.getInventory(),p.level(),40,gear);}
        else{p.level().setBlock(pos,MiningContent.TABLES[rank-1].defaultBlockState(),2);menu=new EnhancementMenu(144,p.getInventory(),p.level(),pos,rank);}
        p.containerMenu=menu;if(!directBook)menu.getSlot(0).set(gear);check(menu.stillValid(p),"native menu fixture valid");return menu;
    }
    /** Moves actual stacks through inventory pickup, right-click socket and Shift removal. */
    private static void attempt(ServerPlayer p,EnhancementMenu menu,ItemStack offered,boolean accepted,String label){
        clearLoose(p,menu);Item coreItem=offered.getItem();int raw=GearData.level(offered);offered.setCount(2);p.getInventory().setItem(9,offered);
        check(menu.canInsertCore(offered)==accepted,"real socket predicate "+label);
        check(menu.canSelectInventoryItem(offered)==accepted,"visual emphasis agrees with socket legality "+label);
        check((GearData.installationError(menu.gear(),offered,menu.rank())==null)==accepted,"component installation gate "+label);
        menu.clicked(EnhancementMenu.INVENTORY,0,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==2,"actual inventory pickup "+label);
        menu.clicked(EnhancementMenu.FIRST_CORE,1,ContainerInput.PICKUP,p);
        if(accepted){
            check(menu.installed()==1&&menu.getCarried().getCount()==1,"right click installs exactly one "+label);
            var stored=GearData.cores(menu.gear()).getFirst();check(stored.is(coreItem)&&stored.getCount()==1&&GearData.level(stored)==raw,"original item/raw grade persists on actual gear "+label);
        }else check(menu.installed()==0&&menu.getCarried().getCount()==2,"rejected socket input leaves both items owned by cursor "+label);
        menu.clicked(EnhancementMenu.INVENTORY,0,ContainerInput.PICKUP,p);
        if(accepted)menu.clicked(EnhancementMenu.FIRST_CORE,0,ContainerInput.QUICK_MOVE,p);
        check(menu.getCarried().isEmpty()&&menu.installed()==0&&GearData.cores(menu.gear()).isEmpty()&&loose(p,coreItem)==2,"actual removal returns exactly the two original items "+label);
    }
    private static void tooltip(ItemStack gear,String label){
        var before=gear.copy();var lines=new ArrayList<Component>();gear.getItem().appendHoverText(gear,Item.TooltipContext.EMPTY,TooltipDisplay.DEFAULT,lines::add,TooltipFlag.NORMAL);
        check(lines.stream().noneMatch(c->c.toString().contains("mining.guogaology.grade_limit")),"obsolete grade-limit row absent from real tooltip "+label);
        check(ItemStack.matches(before,gear),"tooltip is read-only "+label);
    }
    private static void capacity(ServerPlayer p,EnhancementMenu menu,int expected,String label){
        clearLoose(p,menu);check(menu.capacity()==expected&&GearData.capacity(menu.gear())==expected,"unchanged exact socket count "+label);
        var fill=item("lho_trace_lv3");var coreItem=fill.getItem();fill.setCount(expected+1);p.getInventory().setItem(9,fill);
        menu.clicked(EnhancementMenu.INVENTORY,0,ContainerInput.QUICK_MOVE,p);
        check(menu.installed()==expected&&loose(p,coreItem)==1,"complete Shift fills original capacity and preserves one excess "+label);
        for(int slot=EnhancementMenu.FIRST_CORE+expected;slot<EnhancementMenu.INVENTORY;slot++)check(!menu.getSlot(slot).mayPlace(core(3,3)),"slots above original capacity remain locked "+label);
        check(!menu.canInsertCore(core(3,3)),"full inventory rejects another socket core "+label);
        for(int slot=EnhancementMenu.FIRST_CORE;slot<EnhancementMenu.FIRST_CORE+expected;slot++)menu.clicked(slot,0,ContainerInput.QUICK_MOVE,p);
        check(menu.installed()==0&&loose(p,coreItem)==expected+1,"all filled slots return their cores once "+label);
    }
    static void run(ServerPlayer real){
        checks=highestInstalled=stationAttempts=digitalAttempts=0;var pos=real.blockPosition().offset(4,0,0);var saved=real.level().getBlockState(pos);var p=dummy(real);
        int[] gearSlots={0,2,4,6,8},bookSlots={0,2,3,4,6};
        try{
            for(int tier=1;tier<=4;tier++)for(int kind=0;kind<8;kind++){
                String id=EquipmentRules.MINERALS[tier-1]+"_"+EquipmentRules.KINDS[kind];var gear=item(id);var spec=MiningContent.GEAR.get(gear.getItem());
                check(spec!=null&&spec.tier()==tier&&spec.kind()==kind,"all four by eight actual registered item kinds "+id);
                check(EquipmentRules.grade(tier)==3,"all mineral equipment grades allow displayed Lv3 "+id);
                var menu=open(p,pos,gear,3,kind==EquipmentRules.MANUSCRIPT);check(menu.rank()==3&&menu.gearGrade()==3,"real menu maximum grade "+id);
                for(int type=0;type<9;type++){
                    boolean allowed=EquipmentRules.compatible(kind,type);attempt(p,menu,core(type,type==8?4:3),allowed,id+" / "+EquipmentRules.CORES[type]+" highest");
                    if(allowed)highestInstalled++;
                }
                capacity(p,menu,kind==EquipmentRules.MANUSCRIPT?bookSlots[tier]:gearSlots[tier],id);tooltip(menu.gear(),id);p.closeContainer();
                if(kind==EquipmentRules.MANUSCRIPT)check(p.getOffhandItem()==gear,"direct manuscript keeps the same original object "+id);
                for(int rank=1;rank<=3;rank++){
                    menu=open(p,pos,item(id),rank,false);
                    check(menu.rank()==rank&&menu.gearGrade()==3,"station rank remains independent of gear maximum "+id+" / "+rank);
                    for(int grade=1;grade<=3;grade++){
                        attempt(p,menu,core(3,grade),grade<=rank,id+" / station="+rank+" regional="+grade);stationAttempts++;
                    }
                    if(kind==EquipmentRules.MANUSCRIPT)for(int raw=1;raw<=4;raw++){
                        attempt(p,menu,core(8,raw),Math.max(1,raw-1)<=rank,id+" / station="+rank+" ordinalRaw="+raw);stationAttempts++;
                    }
                }
            }
            // Numeric tools keep their one-slot, displayed-Lv1 limit even at station III.
            for(int kind=0;kind<2;kind++)for(int digit=0;digit<=9;digit++){
                var gear=new ItemStack(MiningContent.TOOLS[0][kind]);gear.set(MiningContent.DIGIT,(double)digit);GearData.refresh(gear);
                var menu=open(p,pos,gear,3,false);String label="digit="+digit+" / "+EquipmentRules.KINDS[kind];
                check(EquipmentRules.grade(0)==1&&menu.gearGrade()==1&&menu.capacity()==1,"numeric grade and capacity unchanged "+label);
                for(int type=0;type<9;type++)if(EquipmentRules.compatible(kind,type))for(int grade=1;grade<=3;grade++){
                    attempt(p,menu,core(type,grade),grade==1,label+" / "+EquipmentRules.CORES[type]+" Lv"+grade);digitalAttempts++;
                }
                check(GearData.digit(gear)==digit,"core insertion/removal keeps numeric tool digit "+label);tooltip(gear,label);
            }
            System.out.println("TIER_044_OK checks="+checks+" mineralItems=32 highestCompatibleInstalls="+highestInstalled+" stationGradeAttempts="+stationAttempts+" numericTools=20 numericGradeAttempts="+digitalAttempts+" actualClicks=true ownership=conserved slots=unchanged obsoleteGradeTooltip=absent");
        }finally{p.closeContainer();real.level().setBlock(pos,saved,2);}
    }
}
