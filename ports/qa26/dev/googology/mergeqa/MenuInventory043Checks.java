package dev.googology.mergeqa;

import com.mojang.authlib.GameProfile;
import dev.googology.GoogologyMod;
import dev.googology.mining.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Native inventory actions on dimmed stacks, with the original manuscript lock intact. */
final class MenuInventory043Checks {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(GoogologyMod.id(id)));}
    private static int slot(int inventoryIndex){return inventoryIndex<9?EnhancementMenu.INVENTORY+27+inventoryIndex:EnhancementMenu.INVENTORY+inventoryIndex-9;}
    private static int loose(ServerPlayer p,Item item){int n=p.containerMenu.getCarried().is(item)?p.containerMenu.getCarried().getCount():0;for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);if(s.is(item))n+=s.getCount();}return n;}
    private static int first(ServerPlayer p,Item item,int start,int end){for(int i=start;i<end;i++)if(p.getInventory().getItem(i).is(item))return i;throw new AssertionError("missing moved inventory item "+item);}
    private static void reset(ServerPlayer p,EnhancementMenu menu,int bound){menu.setCarried(ItemStack.EMPTY);for(int i=0;i<p.getInventory().getContainerSize();i++)if(i!=bound)p.getInventory().setItem(i,ItemStack.EMPTY);}
    private static ServerPlayer dummy(ServerPlayer real){
        var profile=new GameProfile(UUID.randomUUID(),"Inventory043");var p=new ServerPlayer(real.level().getServer(),real.level(),profile,ClientInformation.createDefault());
        p.connection=new ServerGamePacketListenerImpl(real.level().getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setGameMode(GameType.SURVIVAL);p.setPos(real.position());p.getInventory().setSelectedSlot(2);return p;
    }
    static void run(ServerPlayer real){
        checks=0;var pos=real.blockPosition().offset(3,0,0);var saved=real.level().getBlockState(pos);
        try{
            real.level().setBlock(pos,MiningContent.TABLES[0].defaultBlockState(),2);
            for(int mode=0;mode<4;mode++){
                var p=dummy(real);int bound=mode==2?2:mode==3?40:-1;var original=bound>=0?item("omega_manuscript"):ItemStack.EMPTY;
                if(bound>=0)p.getInventory().setItem(bound,original);
                EnhancementMenu menu=bound>=0?new ManuscriptMenu(143,p.getInventory(),p.level(),bound,original):new EnhancementMenu(143,p.getInventory(),p.level(),pos,1);
                p.containerMenu=menu;if(mode==1)menu.getSlot(0).set(item("omega_pickaxe"));
                try{ordinaryActions(p,menu,bound);socketRules(p,menu,bound,mode);if(bound>=0)boundRules(p,menu,bound,original);}
                finally{p.closeContainer();}
                if(bound>=0)check(p.getInventory().getItem(bound)==original,"closing keeps the original manuscript in its original slot");
            }
            System.out.println("MENU_INVENTORY_043_OK checks="+checks+" contexts=empty-table/equipped-table/main-book/offhand-book actions=pickup/split/swap/drop/drag/double-click/shift locked-book=preserved sockets=strict");
        }finally{real.level().setBlock(pos,saved,2);}
    }
    private static void ordinaryActions(ServerPlayer p,EnhancementMenu menu,int bound){
        check(!menu.canSelectInventoryItem(new ItemStack(Items.STONE)),"unrelated stone stays visually dimmed");
        check(!menu.canSelectInventoryItem(new ItemStack(Items.APPLE)),"unrelated food stays visually dimmed");
        reset(p,menu,bound);p.getInventory().setItem(9,new ItemStack(Items.STONE,8));
        menu.clicked(slot(9),0,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==8&&p.getInventory().getItem(9).isEmpty(),"left click picks up a dimmed stack");
        menu.clicked(slot(10),0,ContainerInput.PICKUP,p);check(menu.getCarried().isEmpty()&&p.getInventory().getItem(10).getCount()==8,"left click places a dimmed stack elsewhere");
        reset(p,menu,bound);p.getInventory().setItem(9,new ItemStack(Items.STONE,7));
        menu.clicked(slot(9),1,ContainerInput.PICKUP,p);check(menu.getCarried().getCount()==4&&p.getInventory().getItem(9).getCount()==3,"right click splits dimmed stack");
        menu.clicked(slot(10),1,ContainerInput.PICKUP,p);check(p.getInventory().getItem(10).getCount()==1&&menu.getCarried().getCount()==3,"right click places one dimmed item");
        check(loose(p,Items.STONE)==7,"split conserves items");
        reset(p,menu,bound);p.getInventory().setItem(9,new ItemStack(Items.STONE,3));p.getInventory().setItem(7,new ItemStack(Items.APPLE,2));
        menu.clicked(slot(9),7,ContainerInput.SWAP,p);check(p.getInventory().getItem(9).is(Items.APPLE)&&p.getInventory().getItem(7).is(Items.STONE),"number key swaps unrelated dimmed stacks with an unlocked hotbar slot");
        reset(p,menu,bound);menu.setCarried(new ItemStack(Items.STONE,8));
        menu.clicked(-999,0,ContainerInput.QUICK_CRAFT,p);menu.clicked(slot(9),1,ContainerInput.QUICK_CRAFT,p);menu.clicked(slot(10),1,ContainerInput.QUICK_CRAFT,p);menu.clicked(-999,2,ContainerInput.QUICK_CRAFT,p);
        check(menu.getCarried().isEmpty()&&p.getInventory().getItem(9).getCount()==4&&p.getInventory().getItem(10).getCount()==4,"left drag distributes dimmed stacks normally");
        reset(p,menu,bound);menu.setCarried(new ItemStack(Items.STONE,8));
        menu.clicked(-999,4,ContainerInput.QUICK_CRAFT,p);menu.clicked(slot(9),5,ContainerInput.QUICK_CRAFT,p);menu.clicked(slot(10),5,ContainerInput.QUICK_CRAFT,p);menu.clicked(-999,6,ContainerInput.QUICK_CRAFT,p);
        check(menu.getCarried().getCount()==6&&p.getInventory().getItem(9).getCount()==1&&p.getInventory().getItem(10).getCount()==1,"right drag distributes one dimmed item per slot");
        reset(p,menu,bound);menu.setCarried(new ItemStack(Items.STONE,2));p.getInventory().setItem(10,new ItemStack(Items.STONE,3));p.getInventory().setItem(11,new ItemStack(Items.STONE,4));
        menu.clicked(slot(9),0,ContainerInput.PICKUP_ALL,p);check(menu.getCarried().getCount()==9&&p.getInventory().getItem(10).isEmpty()&&p.getInventory().getItem(11).isEmpty(),"double click gathers ordinary dimmed items");
        reset(p,menu,bound);p.getInventory().setItem(9,new ItemStack(Items.STONE,8));menu.clicked(slot(9),0,ContainerInput.QUICK_MOVE,p);
        int hotbar=first(p,Items.STONE,0,9);check(p.getInventory().getItem(9).isEmpty()&&p.getInventory().getItem(hotbar).getCount()==8,"shift sorts dimmed main inventory into hotbar");
        menu.clicked(slot(hotbar),0,ContainerInput.QUICK_MOVE,p);check(p.getInventory().getItem(hotbar).isEmpty()&&p.getInventory().getItem(9).getCount()==8,"shift sorts dimmed hotbar into main inventory");
        reset(p,menu,bound);p.getInventory().setItem(9,new ItemStack(Items.STONE,3));
        var area=p.getBoundingBox().inflate(10);Set<UUID> before=new HashSet<>();for(var entity:p.level().getEntitiesOfClass(ItemEntity.class,area))before.add(entity.getUUID());
        try{
            menu.clicked(slot(9),0,ContainerInput.THROW,p);check(p.getInventory().getItem(9).getCount()==2,"Q throws one dimmed item");
            menu.clicked(slot(9),1,ContainerInput.THROW,p);check(p.getInventory().getItem(9).isEmpty(),"Ctrl-Q throws remaining dimmed stack");
            menu.setCarried(new ItemStack(Items.STONE,4));menu.clicked(-999,0,ContainerInput.PICKUP,p);check(menu.getCarried().isEmpty(),"outside click drops cursor stack");
            int dropped=0;for(var entity:p.level().getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(entity.getUUID())&&entity.getItem().is(Items.STONE))dropped+=entity.getItem().getCount();
            check(dropped==7,"drop paths create exactly seven real loose items");
        }finally{for(var entity:p.level().getEntitiesOfClass(ItemEntity.class,area))if(!before.contains(entity.getUUID()))entity.discard();}
    }
    private static void socketRules(ServerPlayer p,EnhancementMenu menu,int bound,int mode){
        reset(p,menu,bound);String rejected=menu.manuscript()?"astra_critical_core_lv3":"sequence_core_lv3";var high=item(rejected);var highItem=high.getItem();p.getInventory().setItem(9,high);
        check(!menu.canSelectInventoryItem(high),"incompatible book core or over-rank station core is dimmed");menu.clicked(slot(9),0,ContainerInput.QUICK_MOVE,p);
        check(p.getInventory().getItem(9).isEmpty()&&loose(p,highItem)==1&&menu.installed()==0,"shift sorts rejected core without installing or consuming it");
        reset(p,menu,bound);menu.setCarried(item(rejected));menu.clicked(1,0,ContainerInput.PICKUP,p);
        check(menu.getCarried().is(highItem)&&menu.installed()==0,"direct click preserves compatibility and station-rank restrictions");
        menu.setCarried(new ItemStack(Items.STONE));menu.clicked(1,0,ContainerInput.PICKUP,p);menu.clicked(0,0,ContainerInput.PICKUP,p);
        check(menu.getCarried().is(Items.STONE)&&menu.installed()==0,"ordinary stack cannot enter equipment or core input");
        reset(p,menu,bound);
        if(mode==0){p.getInventory().setItem(9,item("omega_pickaxe"));menu.clicked(slot(9),0,ContainerInput.QUICK_MOVE,p);check(menu.gear().is(item("omega_pickaxe").getItem()),"shift still gives equipment input priority");}
        var core=item("sequence_core");var coreItem=core.getItem();core.setCount(menu.capacity()+1);p.getInventory().setItem(9,core);int total=core.getCount();
        check(menu.canSelectInventoryItem(core),"compatible core remains highlighted");menu.quickMoveStack(p,slot(9));
        check(menu.installed()==1&&loose(p,coreItem)==total-1,"one vanilla transfer fills one empty socket before any inventory sorting, mode="+mode);
        // Vanilla's complete Shift gesture repeats quickMoveStack; a single direct
        // invocation intentionally stops after inserting into the first empty slot.
        menu.clicked(slot(9),0,ContainerInput.QUICK_MOVE,p);int excess=first(p,coreItem,0,9);
        check(p.getInventory().getItem(9).isEmpty()&&menu.installed()==menu.capacity()&&loose(p,coreItem)==1,"actual Shift fills all remaining sockets then sorts excess, mode="+mode);
        check(!menu.canSelectInventoryItem(p.getInventory().getItem(excess)),"excess core is dimmed once sockets are full");menu.clicked(slot(excess),0,ContainerInput.QUICK_MOVE,p);
        check(p.getInventory().getItem(excess).isEmpty()&&p.getInventory().getItem(9).getCount()==1&&menu.installed()==menu.capacity()&&loose(p,coreItem)+menu.installed()==total,"full-slot core can be sorted while socket and loose ownership remain exact");
        menu.quickMoveStack(p,1);check(menu.installed()==menu.capacity()-1&&loose(p,coreItem)+menu.installed()==total,"socket removal still transfers the original core exactly once");
    }
    private static void boundRules(ServerPlayer p,EnhancementMenu menu,int bound,ItemStack original){
        menu.setCarried(ItemStack.EMPTY);menu.clicked(0,0,ContainerInput.PICKUP,p);menu.clicked(0,1,ContainerInput.THROW,p);menu.quickMoveStack(p,0);
        check(menu.getCarried().isEmpty()&&menu.gear()==original,"manuscript display cannot be picked up, thrown or quick-moved");
        menu.clicked(slot(9),bound,ContainerInput.SWAP,p);check(p.getInventory().getItem(bound)==original,"number/offhand swap cannot replace the bound manuscript");
        if(bound<9){
            menu.clicked(slot(bound),0,ContainerInput.PICKUP,p);menu.clicked(slot(bound),1,ContainerInput.THROW,p);menu.clicked(slot(bound),0,ContainerInput.QUICK_MOVE,p);
            check(p.getInventory().getItem(bound)==original&&menu.getCarried().isEmpty(),"bound hotbar manuscript remains locked against all direct inventory moves");
            check(!menu.canDragTo(menu.getSlot(slot(bound))),"drag excludes bound hotbar slot");
        }
        check(menu.stillValid(p)&&p.getInventory().getSelectedSlot()==2,"inventory operations preserve the active manuscript transaction");
    }
}
