package dev.guogaology.lamp;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.EmojiLanternBlock;
import dev.guogaology.block.StatefulDecorBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Only the input is owned inventory; the result is a disposable one-item preview. */
public final class LampShapingMenu extends AbstractContainerMenu {
    public static final int INPUT=0,OUTPUT=1,PLAYER=2;
    private final ContainerData selection=new SimpleContainerData(1);
    private final SimpleContainer input=new SimpleContainer(1){@Override public void setChanged(){super.setChanged();refreshOutput();}};
    private final SimpleContainer output=new SimpleContainer(1);
    private final Level world;
    private final BlockPos pos;
    private boolean closed;
    public LampShapingMenu(int id,Inventory inventory){this(id,inventory,null,BlockPos.ZERO);}
    public LampShapingMenu(int id,Inventory inventory,Level world,BlockPos pos){
        super(LampShapingContent.MENU,id);this.world=world;this.pos=pos.immutable();addDataSlots(selection);
        addSlot(new Slot(input,0,222,48){@Override public boolean mayPlace(ItemStack stack){return accepts(stack);}});
        addSlot(new Slot(output,0,222,110){
            @Override public boolean mayPlace(ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return stillValid(player)&&accepts(input.getItem(0))&&!getItem().isEmpty();}
            @Override public void onTake(Player player,ItemStack stack){
                input.removeItem(0,1);input.setChanged();super.onTake(player,stack);
            }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,48+col*18,156+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,48+col*18,214));
    }
    public static boolean accepts(ItemStack stack){return !stack.isEmpty()&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof EmojiLanternBlock;}
    public int selection(){return selection.get(0);}
    public boolean hasInput(){return accepts(input.getItem(0));}
    public ItemStack preview(int choice){
        if(!LampShapes.valid(choice))return ItemStack.EMPTY;
        ItemStack original=hasInput()?input.getItem(0):new ItemStack(GuogaologyBlocks.GUOGAO_LANTERN);
        var block=((BlockItem)original.getItem()).getBlock();
        return StatefulDecorBlock.copyAppearance(original.copyWithCount(1),block.defaultBlockState()
                .setValue(EmojiLanternBlock.PART,LampShapes.part(choice)).setValue(EmojiLanternBlock.EMOTION,LampShapes.emotion(choice)));
    }
    private void refreshOutput(){if(world!=null&&!closed)output.setItem(0,hasInput()?preview(selection()):ItemStack.EMPTY);}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!LampShapes.valid(button)||!stillValid(player)||!hasInput())return false;
        selection.set(0,button);refreshOutput();broadcastChanges();return true;
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.index!=OUTPUT&&super.canTakeItemForPickAll(stack,slot);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index==OUTPUT){
            // Work on a copy: inventory callbacks can immediately replace the result preview.
            ItemStack moved=stack.copy();if(!moveItemStackTo(moved,PLAYER,slots.size(),true))return ItemStack.EMPTY;
            slot.onTake(player,original);broadcastChanges();return original;
        }
        boolean moved=index==INPUT?moveItemStackTo(stack,PLAYER,slots.size(),true):accepts(stack)&&moveItemStackTo(stack,INPUT,INPUT+1,false);
        if(!moved&&index>=PLAYER){int hotbar=PLAYER+27;moved=index<hotbar?moveItemStackTo(stack,hotbar,slots.size(),false):moveItemStackTo(stack,PLAYER,hotbar,false);}
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return original;
    }
    @Override public boolean stillValid(Player player){return !closed&&(world==null||player.isAlive()
            &&world.getBlockState(pos).is(LampShapingContent.BLOCK)&&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64);}
    @Override public void removed(Player player){
        if(closed)return;closed=true;output.setItem(0,ItemStack.EMPTY);super.removed(player);
        if(world!=null)clearContainer(player,input);
    }
}
