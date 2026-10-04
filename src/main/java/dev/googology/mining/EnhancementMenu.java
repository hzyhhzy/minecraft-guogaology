package dev.googology.mining;

import net.minecraft.util.math.BlockPos;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.entity.player.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.*;
import net.minecraft.item.*;
import net.minecraft.world.World;
import java.util.*;

/** Inventory-style sockets. The gear owns the saved cores; socket copies never drop on close. */
public final class EnhancementMenu extends ScreenHandler {
    public static final int GEAR=0, FIRST_CORE=1, INVENTORY=11;
    private final SimpleInventory contents=new SimpleInventory(INVENTORY){@Override public void markDirty(){super.markDirty();synchronizeCores();}};
    private final PropertyDelegate data=new ArrayPropertyDelegate(1);
    private final World world;
    private final BlockPos pos;
    private ItemStack trackedGear=ItemStack.EMPTY;
    private boolean closed,syncing;
    public EnhancementMenu(int id,PlayerInventory inventory){this(id,inventory,null,BlockPos.ORIGIN,1);}
    public EnhancementMenu(int id,PlayerInventory inventory,World world,BlockPos pos,int rank){
        super(MiningContent.ENHANCEMENT_MENU,id);
        this.world=world;this.pos=pos.toImmutable();data.set(0,rank);addProperties(data);
        for(int i=0;i<INVENTORY;i++){
            final int n=i;
            int x=i==0?84:28+((i-FIRST_CORE)%5)*28, y=i==0?38:78+((i-FIRST_CORE)/5)*26;
            addSlot(new Slot(contents,i,x,y){
                @Override public boolean canInsert(ItemStack s){return n==GEAR?MiningContent.GEAR.containsKey(s.getItem()):accepts(n-FIRST_CORE,s);}
                @Override public int getMaxItemCount(){return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,12+col*18,150+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,12+col*18,208));
    }
    public ItemStack gear(){return contents.getStack(GEAR);}
    public int rank(){return data.get(0);}
    public int capacity(){var spec=MiningContent.GEAR.get(gear().getItem());return spec==null?0:EquipmentRules.slots(spec.tier());}
    public int gearGrade(){var spec=MiningContent.GEAR.get(gear().getItem());return spec==null?0:EquipmentRules.grade(spec.tier());}
    public int installed(){return GearData.cores(gear()).size();}
    /** Exactly the same eligibility as an empty real socket, including station and gear grade. */
    public boolean canInsertCore(ItemStack stack){
        if(stack.isEmpty())return false;
        for(int i=0;i<capacity();i++)if(contents.getStack(FIRST_CORE+i).isEmpty()&&accepts(i,stack))return true;
        return false;
    }
    private boolean accepts(int index,ItemStack s){
        var spec=MiningContent.GEAR.get(gear().getItem());int type=GearData.type(s);
        if(index>=capacity()||spec==null||type<0||!EquipmentRules.compatible(spec.kind(),type))return false;
        int grade=EquipmentRules.stationGrade(type,GearData.level(s));return grade<=rank()&&grade<=gearGrade();
    }
    private void synchronizeCores(){
        if(world==null||syncing||closed)return;
        syncing=true;
        try{
            var current=gear();
            if(current!=trackedGear||current.isEmpty()){
                trackedGear=current;
                if(!current.isEmpty()&&current.getOrDefault(MiningContent.RULES,0)!=EquipmentRules.REVISION)GearData.refresh(current);
                var saved=GearData.coreSlots(current);
                for(int i=0;i<EquipmentRules.MAX_SOCKETS;i++)contents.setStack(FIRST_CORE+i,i<saved.size()?saved.get(i).copy():ItemStack.EMPTY);
            }else{
                var saved=new ArrayList<ItemStack>();
                for(int i=0;i<capacity();i++)saved.add(contents.getStack(FIRST_CORE+i).copy());
                GearData.setCores(current,saved);
            }
        }finally{syncing=false;}
    }
    @Override public void onSlotClick(int slot,int button,SlotActionType type,PlayerEntity player){
        if(!canUse(player))return;
        super.onSlotClick(slot,button,type,player);synchronizeCores();
    }
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(!canUse(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);var s=slot.getStack();if(s.isEmpty())return ItemStack.EMPTY;var copy=s.copy();
        boolean moved=index<INVENTORY?insertItem(s,INVENTORY,slots.size(),true):MiningContent.GEAR.containsKey(s.getItem())?insertItem(s,GEAR,GEAR+1,false):GearData.type(s)>=0&&insertItem(s,FIRST_CORE,FIRST_CORE+capacity(),false);
        if(!moved)return ItemStack.EMPTY;
        if(s.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();
        synchronizeCores();return copy;
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&(world==null||(rank()>=1&&rank()<=3&&world.getBlockState(pos).isOf(MiningContent.TABLES[rank()-1])&&player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64));}
    @Override public void onClosed(PlayerEntity player){
        if(closed)return;synchronizeCores();closed=true;
        for(int i=FIRST_CORE;i<INVENTORY;i++)contents.setStack(i,ItemStack.EMPTY);
        super.onClosed(player);if(world!=null)dropInventory(player,contents);
    }
}
