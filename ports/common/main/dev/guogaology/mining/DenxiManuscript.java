package dev.guogaology.mining;

import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/** A passive offhand socket carrier; never spends the installed cores. */
public final class DenxiManuscript extends Item {
    public final int tier;
    public DenxiManuscript(Properties props,int tier){super(props);this.tier=tier;}
    public static boolean open(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(player.level().isClientSide()||!player.isAlive()||player.isSpectator()||player.containerMenu!=player.inventoryMenu||!player.inventoryMenu.getCarried().isEmpty())return false;
        var book=player.getItemInHand(hand);if(!(book.getItem() instanceof DenxiManuscript))return false;
        int slot=hand==net.minecraft.world.InteractionHand.OFF_HAND?40:player.getInventory().getSelectedSlot();
        player.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,p)->new ManuscriptMenu(id,inventory,player.level(),slot,book),book.getHoverName()));
        return true;
    }
    @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level world,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        if(hand==net.minecraft.world.InteractionHand.OFF_HAND)return net.minecraft.world.InteractionResult.PASS;
        var selectedHand=player.getOffhandItem().getItem() instanceof DenxiManuscript?net.minecraft.world.InteractionHand.OFF_HAND:hand;
        if(!world.isClientSide())open(player,selectedHand);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> out,TooltipFlag flag){
        EquipmentTooltips.append(stack,tier,EquipmentRules.MANUSCRIPT,out);
    }
}
