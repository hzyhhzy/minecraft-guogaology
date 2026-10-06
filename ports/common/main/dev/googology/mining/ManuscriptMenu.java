package dev.googology.mining;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
/** The original held manuscript stays in its inventory slot for the entire session. */
public final class ManuscriptMenu extends EnhancementMenu {
    public ManuscriptMenu(int id,Inventory inventory){super(MiningContent.MANUSCRIPT_MENU,id,inventory,null,BlockPos.ZERO,1,-1,ItemStack.EMPTY);}
    public ManuscriptMenu(int id,Inventory inventory,Level world,int slot,ItemStack book){super(MiningContent.MANUSCRIPT_MENU,id,inventory,world,BlockPos.ZERO,EquipmentRules.grade(((DenxiManuscript)book.getItem()).tier),slot,book);}
}
