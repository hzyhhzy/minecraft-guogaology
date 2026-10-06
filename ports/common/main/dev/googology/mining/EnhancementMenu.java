package dev.googology.mining;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Saved item components own socket contents; the menu never creates a second inventory of cores. */
public class EnhancementMenu extends AbstractContainerMenu {
    public static final int GEAR=0,FIRST_CORE=1,INVENTORY=11;
    private final SimpleContainer contents=new SimpleContainer(INVENTORY){@Override public void setChanged(){super.setChanged();synchronizeCores();}};
    private final ContainerData data=new SimpleContainerData(4);
    private final Level world;
    private final BlockPos pos;
    private final Player owner;
    private final int boundSlot;
    private final ItemStack boundBook;
    private ItemStack trackedGear=ItemStack.EMPTY;
    private boolean closed,syncing;
    public EnhancementMenu(int id,Inventory inventory){this(id,inventory,null,BlockPos.ZERO,1);}
    public EnhancementMenu(int id,Inventory inventory,Level world,BlockPos pos,int rank){this(MiningContent.ENHANCEMENT_MENU,id,inventory,world,pos,rank,-1,ItemStack.EMPTY);}
    protected EnhancementMenu(MenuType<?> type,int id,Inventory inventory,Level world,BlockPos pos,int rank,int boundSlot,ItemStack book){
        super(type,id);this.world=world;this.pos=pos.immutable();owner=inventory.player;this.boundSlot=boundSlot;boundBook=book;
        data.set(0,rank);data.set(2,boundSlot);data.set(1,world!=null&&ManuscriptEffects.deep(world)?1:0);addDataSlots(data);
        for(int i=0;i<INVENTORY;i++){
            final int n=i;int x=i==0?84:28+((i-FIRST_CORE)%5)*28,y=i==0?38:78+((i-FIRST_CORE)/5)*26;
            addSlot(new Slot(contents,i,x,y){
                @Override public boolean mayPlace(ItemStack s){return n==GEAR?!manuscript()&&MiningContent.GEAR.containsKey(s.getItem()):accepts(n-FIRST_CORE,s);}
                @Override public boolean mayPickup(Player p){return n!=GEAR||!manuscript();}
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addPlayerSlot(inventory,col+row*9+9,12+col*18,150+row*18);
        for(int col=0;col<9;col++)addPlayerSlot(inventory,col,12+col*18,208);
        if(world!=null&&manuscript())contents.setItem(GEAR,book);
    }
    private void addPlayerSlot(Inventory inventory,int index,int x,int y){
        addSlot(new Slot(inventory,index,x,y){
            @Override public boolean mayPlace(ItemStack s){return index!=lockedSlot()&&super.mayPlace(s);}
            @Override public boolean mayPickup(Player p){return index!=lockedSlot()&&super.mayPickup(p);}
        });
    }
    public boolean manuscript(){return this instanceof ManuscriptMenu;}
    private int lockedSlot(){return data.get(2);}
    public boolean locksHotbar(){return manuscript()&&lockedSlot()!=40;}
    public boolean blocksSelection(int selected){return locksHotbar()&&selected!=lockedSlot();}
    private boolean locked(Slot slot){return manuscript()&&(slot.index==GEAR||slot.index>=INVENTORY&&slot.getContainerSlot()==lockedSlot());}
    public boolean deep(){return data.get(1)!=0;}
    public ItemStack gear(){return contents.getItem(GEAR);}
    public int rank(){return data.get(0);}
    public int capacity(){var spec=MiningContent.GEAR.get(gear().getItem());return spec==null?0:EquipmentRules.slots(spec.tier(),spec.kind());}
    public int gearGrade(){var spec=MiningContent.GEAR.get(gear().getItem());return spec==null?0:EquipmentRules.grade(spec.tier());}
    public int installed(){return GearData.cores(gear()).size();}
    public EquipmentRules.Snapshot preview(){
        var snapshot=GearData.snapshot(owner);var spec=MiningContent.GEAR.get(gear().getItem());
        if(spec==null)return snapshot;
        var current=new EquipmentRules.Gear(spec.tier(),spec.kind(),GearData.digit(gear()),GearData.profile(gear()));
        var main=snapshot.mainhand();var off=snapshot.offhand();
        var head=snapshot.helmet();var chest=snapshot.chestplate();var legs=snapshot.leggings();var feet=snapshot.boots();
        switch(spec.kind()){case 0,1,7->main=current;case 2->head=current;case 3->chest=current;case 4->legs=current;case 5->feet=current;case 6->off=current;}
        return new EquipmentRules.Snapshot(main,off,head,chest,legs,feet,deep());
    }
    public boolean canToggleSilk(){var spec=MiningContent.GEAR.get(gear().getItem());return spec!=null&&spec.kind()==0&&EquipmentRules.highest(GearData.profile(gear()),2)>0;}
    public boolean silkTouch(){return data.get(3)!=0;}
    @Override public boolean clickMenuButton(Player player,int button){
        if(button!=0||!stillValid(player)||!canToggleSilk())return false;
        GearData.setSilkTouch(gear(),!GearData.silkTouch(gear()));data.set(3,GearData.silkTouch(gear())?1:0);broadcastChanges();return true;
    }
    public boolean canInsertCore(ItemStack stack){
        if(stack.isEmpty())return false;for(int i=0;i<capacity();i++)if(contents.getItem(FIRST_CORE+i).isEmpty()&&accepts(i,stack))return true;return false;
    }
    private boolean accepts(int index,ItemStack stack){
        var spec=MiningContent.GEAR.get(gear().getItem());int type=GearData.type(stack);
        if(index<0||index>=capacity()||spec==null||type<0||!EquipmentRules.compatible(spec.kind(),type))return false;
        int grade=EquipmentRules.stationGrade(type,GearData.level(stack));return grade<=rank()&&grade<=gearGrade();
    }
    private boolean boundValid(){return !manuscript()||(boundSlot==40?owner.getOffhandItem()==boundBook:boundSlot>=0&&owner.getInventory().getSelectedSlot()==boundSlot&&owner.getInventory().getItem(boundSlot)==boundBook);}
    private void synchronizeCores(){
        if(world==null||syncing||closed||!boundValid())return;syncing=true;
        try{
            var current=gear();
            if(current!=trackedGear||current.isEmpty()){
                trackedGear=current;if(!current.isEmpty()&&current.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION)GearData.refresh(current);
                var saved=GearData.coreSlots(current);for(int i=0;i<EquipmentRules.MAX_SOCKETS;i++)contents.setItem(FIRST_CORE+i,i<saved.size()?saved.get(i).copy():ItemStack.EMPTY);
            }else{
                var saved=new ArrayList<ItemStack>();for(int i=0;i<capacity();i++)saved.add(contents.getItem(FIRST_CORE+i).copy());GearData.setCores(current,saved);
            }
            data.set(3,GearData.silkTouch(gear())?1:0);
        }finally{syncing=false;}
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!stillValid(player))return;
        if(manuscript()&&(slot==GEAR||(type==ClickType.SWAP&&(button==40||button==lockedSlot()))))return;
        if(slot>=INVENTORY&&slot<slots.size()&&slots.get(slot).getContainerSlot()==lockedSlot())return;
        super.clicked(slot,button,type,player);synchronizeCores();
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return !locked(slot)&&super.canTakeItemForPickAll(stack,slot);}
    @Override public boolean canDragTo(Slot slot){return !locked(slot)&&super.canDragTo(slot);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size()||manuscript()&&(index==GEAR||index>=INVENTORY&&slots.get(index).getContainerSlot()==lockedSlot()))return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();
        boolean moved=index<INVENTORY?moveItemStackTo(stack,INVENTORY,slots.size(),true):!manuscript()&&MiningContent.GEAR.containsKey(stack.getItem())?moveItemStackTo(stack,GEAR,GEAR+1,false):GearData.type(stack)>=0&&moveItemStackTo(stack,FIRST_CORE,FIRST_CORE+capacity(),false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();synchronizeCores();return copy;
    }
    @Override public boolean stillValid(Player player){
        if(closed)return false;if(world==null)return true;data.set(1,ManuscriptEffects.deep(player.level())?1:0);
        return manuscript()?player==owner&&player.isAlive()&&boundValid():rank()>=1&&rank()<=3&&world.getBlockState(pos).is(MiningContent.TABLES[rank()-1])&&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;
    }
    @Override public void removed(Player player){
        if(closed)return;synchronizeCores();closed=true;
        for(int i=FIRST_CORE;i<INVENTORY;i++)contents.setItem(i,ItemStack.EMPTY);
        if(manuscript())contents.setItem(GEAR,ItemStack.EMPTY);
        super.removed(player);if(world!=null&&!manuscript())clearContainer(player,contents);
    }
}
