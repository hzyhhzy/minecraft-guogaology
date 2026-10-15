package dev.guogaology.lamp;

import dev.guogaology.GuogaologyBlocks;
import dev.guogaology.block.EmojiLanternBlock;
import dev.guogaology.block.StatefulDecorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Only the input is owned inventory; the result is a disposable one-item preview. */
public final class LampShapingMenu extends ScreenHandler {
    public static final int INPUT=0,OUTPUT=1,PLAYER=2;
    private final PropertyDelegate selection=new ArrayPropertyDelegate(1);
    private final SimpleInventory input=new SimpleInventory(1){@Override public void markDirty(){super.markDirty();refreshOutput();}};
    private final SimpleInventory output=new SimpleInventory(1);
    private final World world;
    private final BlockPos pos;
    private boolean closed;
    public LampShapingMenu(int id,PlayerInventory inventory){this(id,inventory,null,BlockPos.ORIGIN);}
    public LampShapingMenu(int id,PlayerInventory inventory,World world,BlockPos pos){
        super(LampShapingContent.MENU,id);this.world=world;this.pos=pos.toImmutable();addProperties(selection);
        addSlot(new Slot(input,0,222,48){@Override public boolean canInsert(ItemStack stack){return accepts(stack);}});
        addSlot(new Slot(output,0,222,110){
            @Override public boolean canInsert(ItemStack stack){return false;}
            @Override public boolean canTakeItems(PlayerEntity player){return canUse(player)&&accepts(input.getStack(0))&&!getStack().isEmpty();}
            @Override public void onTakeItem(PlayerEntity player,ItemStack stack){
                input.removeStack(0,1);input.markDirty();super.onTakeItem(player,stack);
            }
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,48+col*18,156+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,48+col*18,214));
    }
    public static boolean accepts(ItemStack stack){return !stack.isEmpty()&&stack.getItem() instanceof BlockItem item&&item.getBlock() instanceof EmojiLanternBlock;}
    public int selection(){return selection.get(0);}
    public boolean hasInput(){return accepts(input.getStack(0));}
    public ItemStack preview(int choice){
        if(!LampShapes.valid(choice))return ItemStack.EMPTY;
        ItemStack original=hasInput()?input.getStack(0):new ItemStack(GuogaologyBlocks.GUOGAO_LANTERN);
        var block=((BlockItem)original.getItem()).getBlock();
        return StatefulDecorBlock.copyAppearance(original.copyWithCount(1),block.getDefaultState()
                .with(EmojiLanternBlock.PART,LampShapes.part(choice)).with(EmojiLanternBlock.EMOTION,LampShapes.emotion(choice)));
    }
    private void refreshOutput(){if(world!=null&&!closed)output.setStack(0,hasInput()?preview(selection()):ItemStack.EMPTY);}
    @Override public boolean onButtonClick(PlayerEntity player,int button){
        if(!LampShapes.valid(button)||!canUse(player)||!hasInput())return false;
        selection.set(0,button);refreshOutput();sendContentUpdates();return true;
    }
    @Override public boolean canInsertIntoSlot(ItemStack stack,Slot slot){return slot.id!=OUTPUT&&super.canInsertIntoSlot(stack,slot);}
    @Override public boolean canInsertIntoSlot(Slot slot){return slot.id!=OUTPUT&&super.canInsertIntoSlot(slot);}
    @Override public ItemStack quickMove(PlayerEntity player,int index){
        if(!canUse(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasStack()||!slot.canTakeItems(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getStack(),original=stack.copy();
        if(index==OUTPUT){
            // Work on a copy: inventory callbacks can immediately replace the result preview.
            ItemStack moved=stack.copy();if(!insertItem(moved,PLAYER,slots.size(),true))return ItemStack.EMPTY;
            slot.onTakeItem(player,original);sendContentUpdates();return original;
        }
        boolean moved=index==INPUT?insertItem(stack,PLAYER,slots.size(),true):accepts(stack)&&insertItem(stack,INPUT,INPUT+1,false);
        if(!moved&&index>=PLAYER){int hotbar=PLAYER+27;moved=index<hotbar?insertItem(stack,hotbar,slots.size(),false):insertItem(stack,PLAYER,hotbar,false);}
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setStack(ItemStack.EMPTY);else slot.markDirty();return original;
    }
    @Override public boolean canUse(PlayerEntity player){return !closed&&(world==null||player.isAlive()
            &&world.getBlockState(pos).isOf(LampShapingContent.BLOCK)&&player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64);}
    @Override public void onClosed(PlayerEntity player){
        if(closed)return;closed=true;output.setStack(0,ItemStack.EMPTY);super.onClosed(player);
        if(world!=null)dropInventory(player,input);
    }
}
