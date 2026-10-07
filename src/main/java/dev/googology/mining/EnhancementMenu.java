package dev.googology.mining;

import net.minecraft.util.math.BlockPos;
import net.minecraft.screen.*;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.*;
import net.minecraft.screen.slot.*;
import net.minecraft.item.*;
import net.minecraft.world.World;
import java.util.*;

/** Saved item components own socket contents; the menu never creates a second inventory of cores. */
public class EnhancementMenu extends ScreenHandler {
    public static final int GEAR=0,FIRST_CORE=1,INVENTORY=11;
    private final SimpleInventory contents=new SimpleInventory(INVENTORY){@Override public void markDirty(){super.markDirty();synchronizeCores();}};
    private final PropertyDelegate data=new ArrayPropertyDelegate(4);
    private final World world;
    private final BlockPos pos;
    private final PlayerEntity owner;
    private final int boundSlot;
    private final ItemStack boundBook;
    // Read-only effect snapshots: never insert these copies into slots or drops.
    private final ItemStack openingMain,openingOff;
    private ItemStack trackedGear=ItemStack.EMPTY;
    private boolean closed,syncing;
    public EnhancementMenu(int id,PlayerInventory inventory){this(id,inventory,null,BlockPos.ORIGIN,1);}
    public EnhancementMenu(int id,PlayerInventory inventory,World world,BlockPos pos,int rank){this(MiningContent.ENHANCEMENT_MENU,id,inventory,world,pos,rank,-1,ItemStack.EMPTY);}
    protected EnhancementMenu(ScreenHandlerType<?> type,int id,PlayerInventory inventory,World world,BlockPos pos,int rank,int boundSlot,ItemStack book){
        super(type,id);this.world=world;this.pos=pos.toImmutable();owner=inventory.player;this.boundSlot=boundSlot;boundBook=book;
        openingMain=owner.getMainHandStack().copy();openingOff=owner.getOffHandStack().copy();
        data.set(0,rank);data.set(2,boundSlot);data.set(1,world!=null&&ManuscriptEffects.deep(world)?1:0);addProperties(data);
        for(int i=0;i<INVENTORY;i++){
            final int n=i;int x=i==0?84:28+((i-FIRST_CORE)%5)*28,y=i==0?38:78+((i-FIRST_CORE)/5)*26;
            addSlot(new Slot(contents,i,x,y){
                @Override public boolean canInsert(ItemStack s){return n==GEAR?!manuscript()&&MiningContent.GEAR.containsKey(s.getItem()):accepts(n-FIRST_CORE,s);}
                @Override public boolean canTakeItems(PlayerEntity p){return n!=GEAR||!manuscript();}
                @Override public int getMaxItemCount(){return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addPlayerSlot(inventory,col+row*9+9,12+col*18,150+row*18);
        for(int col=0;col<9;col++)addPlayerSlot(inventory,col,12+col*18,208);
        if(world!=null&&manuscript())contents.setStack(GEAR,book);
    }
    private void addPlayerSlot(PlayerInventory inventory,int index,int x,int y){
        addSlot(new Slot(inventory,index,x,y){
            @Override public boolean canInsert(ItemStack s){return index!=lockedSlot()&&super.canInsert(s);}
            @Override public boolean canTakeItems(PlayerEntity p){return index!=lockedSlot()&&super.canTakeItems(p);}
        });
    }
    public boolean manuscript(){return this instanceof ManuscriptMenu;}
    /** Item ownership is saved on every move; only passive activation waits until close. */
    public ItemStack activeManuscript(ItemStack actual){
        if(closed||!manuscript()||world!=null&&!boundValid())return actual;
        int slot=lockedSlot();
        if(slot==40&&actual==owner.getOffHandStack())return openingOff;
        if(slot>=0&&slot<9&&owner.getInventory().selectedSlot==slot&&actual==owner.getMainHandStack())return openingMain;
        return actual;
    }
    private int lockedSlot(){return data.get(2);}
    public boolean locksHotbar(){return manuscript()&&lockedSlot()!=40;}
    public boolean blocksSelection(int selected){return locksHotbar()&&selected!=lockedSlot();}
    private boolean locked(Slot slot){return manuscript()&&(slot.id==GEAR||slot.id>=INVENTORY&&slot.getIndex()==lockedSlot());}
    public boolean deep(){return data.get(1)!=0;}
    public ItemStack gear(){return contents.getStack(GEAR);}
    public int rank(){return data.get(0);}
    public int capacity(){var spec=MiningContent.GEAR.get(gear().getItem());return spec==null?0:EquipmentRules.slots(spec.tier(),spec.kind());}
    public int gearGrade(){var spec=MiningContent.GEAR.get(gear().getItem());return spec==null?0:EquipmentRules.grade(spec.tier());}
    public int installed(){return GearData.cores(gear()).size();}
    public EquipmentRules.Snapshot preview(){
        var spec=MiningContent.GEAR.get(gear().getItem());
        var current=spec==null?EquipmentRules.Gear.empty(1):new EquipmentRules.Gear(spec.tier(),spec.kind(),GearData.digit(gear()),GearData.profile(gear()));
        return EquipmentRules.itemPreview(current,deep());
    }
    public boolean canToggleSilk(){var spec=MiningContent.GEAR.get(gear().getItem());return spec!=null&&spec.kind()==0&&EquipmentRules.highest(GearData.profile(gear()),2)>0;}
    public boolean silkTouch(){return data.get(3)!=0;}
    @Override public boolean onButtonClick(PlayerEntity player,int button){
        if(button!=0||!canUse(player)||!canToggleSilk())return false;
        GearData.setSilkTouch(gear(),!GearData.silkTouch(gear()));data.set(3,GearData.silkTouch(gear())?1:0);sendContentUpdates();return true;
    }
    public boolean canInsertCore(ItemStack stack){
        if(stack.isEmpty())return false;for(int i=0;i<capacity();i++)if(contents.getStack(FIRST_CORE+i).isEmpty()&&accepts(i,stack))return true;return false;
    }
    /** Highlighting only: ordinary inventory actions never use this predicate. */
    public boolean canSelectInventoryItem(ItemStack stack){
        if(stack.isEmpty())return false;
        return !manuscript()&&gear().isEmpty()?getSlot(GEAR).canInsert(stack):canInsertCore(stack);
    }
    private boolean accepts(int index,ItemStack stack){
        var spec=MiningContent.GEAR.get(gear().getItem());int type=GearData.type(stack);
        if(index<0||index>=capacity()||spec==null)return false;
        if(GearData.isSocketTotem(gear(),stack))return true;
        if(type<0||!EquipmentRules.compatible(spec.kind(),type))return false;
        int grade=EquipmentRules.stationGrade(type,GearData.level(stack));return grade<=rank()&&grade<=gearGrade();
    }
    /** Keep pre-open activation, but debit real ownership before its read-only snapshot.
     * A removed active totem may still be on the cursor/inventory until close.
     * Newly inserted totems cannot activate in this session; snapshots never drop items.
     */
    public boolean consumeActiveTotem(ItemStack actual){
        if(closed||world==null||!manuscript()||!boundValid())return false;
        var active=activeManuscript(actual);
        if(active==actual||GearData.socketTotems(active)==0)return false;
        synchronizeCores();boolean consumed=false;
        for(int i=FIRST_CORE;i<FIRST_CORE+capacity();i++)if(contents.getStack(i).isOf(Items.TOTEM_OF_UNDYING)){contents.setStack(i,ItemStack.EMPTY);consumed=true;break;}
        if(!consumed&&getCursorStack().isOf(Items.TOTEM_OF_UNDYING)){var stack=getCursorStack().copy();stack.decrement(1);setCursorStack(stack);consumed=true;}
        if(!consumed)consumed=ManuscriptEffects.consumeInventoryTotem(owner);
        if(!consumed)return false;
        GearData.consumeSocketTotem(active);sendContentUpdates();return true;
    }
    private boolean boundValid(){return !manuscript()||(boundSlot==40?owner.getOffHandStack()==boundBook:boundSlot>=0&&owner.getInventory().selectedSlot==boundSlot&&owner.getInventory().getStack(boundSlot)==boundBook);}
    private void synchronizeCores(){
        if(world==null||syncing||closed||!boundValid())return;syncing=true;
        try{
            var current=gear();
            if(current!=trackedGear||current.isEmpty()){
                trackedGear=current;if(!current.isEmpty()&&current.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION)GearData.refresh(current);
                var saved=GearData.coreSlots(current);for(int i=0;i<EquipmentRules.MAX_SOCKETS;i++)contents.setStack(FIRST_CORE+i,i<saved.size()?saved.get(i).copy():ItemStack.EMPTY);
            }else{
                var saved=new ArrayList<ItemStack>();for(int i=0;i<capacity();i++)saved.add(contents.getStack(FIRST_CORE+i).copy());GearData.setCores(current,saved);
            }
            data.set(3,GearData.silkTouch(gear())?1:0);
        }finally{syncing=false;}
    }
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player))return;
        if(manuscript()&&(slot==GEAR||(type==SlotActionType.SWAP&&(button==40||button==lockedSlot()))))return;
        if(slot>=INVENTORY&&slot<slots.size()&&slots.get(slot).getIndex()==lockedSlot())return;
        super.onSlotClick(slot,button,type,player);synchronizeCores();
    }
    @Override public boolean canInsertIntoSlot(ItemStack stack,Slot slot){return !locked(slot)&&super.canInsertIntoSlot(stack,slot);}
    @Override public boolean canInsertIntoSlot(Slot slot){return !locked(slot)&&super.canInsertIntoSlot(slot);}
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(!canUse(player)||index<0||index>=slots.size()||manuscript()&&(index==GEAR||index>=INVENTORY&&slots.get(index).getIndex()==lockedSlot()))return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getStack();if(stack.isEmpty())return ItemStack.EMPTY;var copy=stack.copy();
        boolean moved=index<INVENTORY?insertItem(stack,INVENTORY,slots.size(),true):!manuscript()&&MiningContent.GEAR.containsKey(stack.getItem())?insertItem(stack,GEAR,GEAR+1,false):(GearData.type(stack)>=0||GearData.isSocketTotem(gear(),stack))&&insertItem(stack,FIRST_CORE,FIRST_CORE+capacity(),false);
        // Keep enhancement inputs first, then allow ordinary main-inventory/hotbar sorting.
        // Player slots retain their bound-manuscript lock through canInsert/canTakeItems.
        if(!moved&&index>=INVENTORY){int hotbar=INVENTORY+27;moved=index<hotbar?insertItem(stack,hotbar,slots.size(),false):insertItem(stack,INVENTORY,hotbar,false);}
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();synchronizeCores();return copy;
    }
    @Override public boolean canUse(PlayerEntity player){
        if(closed)return false;if(world==null)return true;data.set(1,ManuscriptEffects.deep(player.getWorld())?1:0);
        return manuscript()?player==owner&&player.isAlive()&&boundValid():rank()>=1&&rank()<=3&&world.getBlockState(pos).isOf(MiningContent.TABLES[rank()-1])&&player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;
    }
    @Override public void onClosed(PlayerEntity player){
        if(closed)return;synchronizeCores();closed=true;
        for(int i=FIRST_CORE;i<INVENTORY;i++)contents.setStack(i,ItemStack.EMPTY);
        if(manuscript())contents.setStack(GEAR,ItemStack.EMPTY);
        super.onClosed(player);if(world!=null&&!manuscript())dropInventory(player,contents);
    }
}
