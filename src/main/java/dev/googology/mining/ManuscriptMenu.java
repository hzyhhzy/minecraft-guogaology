package dev.googology.mining;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
/** The original held manuscript stays in its inventory slot for the entire session. */
public final class ManuscriptMenu extends EnhancementMenu {
    public ManuscriptMenu(int id,PlayerInventory inventory){super(MiningContent.MANUSCRIPT_MENU,id,inventory,null,BlockPos.ORIGIN,1,-1,ItemStack.EMPTY);}
    public ManuscriptMenu(int id,PlayerInventory inventory,World world,int slot,ItemStack book){super(MiningContent.MANUSCRIPT_MENU,id,inventory,world,BlockPos.ORIGIN,EquipmentRules.grade(((DenxiManuscript)book.getItem()).tier),slot,book);}
}
