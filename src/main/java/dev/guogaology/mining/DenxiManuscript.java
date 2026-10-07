package dev.guogaology.mining;

import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import java.util.function.Consumer;

/** A passive offhand socket carrier; never spends the installed cores. */
public final class DenxiManuscript extends Item {
    public final int tier;
    public DenxiManuscript(Settings props,int tier){super(props);this.tier=tier;}
    public static boolean open(net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.Hand hand){
        if(player.getWorld().isClient||!player.isAlive()||player.isSpectator()||player.currentScreenHandler!=player.playerScreenHandler||!player.playerScreenHandler.getCursorStack().isEmpty())return false;
        var book=player.getStackInHand(hand);if(!(book.getItem() instanceof DenxiManuscript))return false;
        int slot=hand==net.minecraft.util.Hand.OFF_HAND?40:player.getInventory().selectedSlot;
        player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory((id,inventory,p)->new ManuscriptMenu(id,inventory,player.getWorld(),slot,book),book.getName()));
        return true;
    }
    @Override public net.minecraft.util.TypedActionResult<ItemStack> use(net.minecraft.world.World world,net.minecraft.entity.player.PlayerEntity player,net.minecraft.util.Hand hand){
        if(hand==net.minecraft.util.Hand.OFF_HAND)return net.minecraft.util.TypedActionResult.pass(player.getStackInHand(hand));
        var selectedHand=player.getOffHandStack().getItem() instanceof DenxiManuscript?net.minecraft.util.Hand.OFF_HAND:hand;
        if(!world.isClient)open(player,selectedHand);
        return net.minecraft.util.TypedActionResult.success(player.getStackInHand(hand),world.isClient);
    }
    @Override public void appendTooltip(ItemStack stack,TooltipContext context,java.util.List<Text> out,TooltipType flag){
        out.add(Text.translatable("mining.guogaology.manuscript.passive"));
        out.add(Text.translatable("mining.guogaology.manuscript.open"));
        out.add(Text.translatable("mining.guogaology.slots",GearData.cores(stack).size(),EquipmentRules.slots(tier,6)));

        for(var core:GearData.cores(stack))out.add(Text.literal("• ").append(core.getName()).append(" — ").append(Text.translatable(core.isOf(Items.TOTEM_OF_UNDYING)?"mining.guogaology.manuscript.socket_totem":"mining.guogaology.manuscript.effect."+GearData.type(core))));
        if(ManuscriptEffects.level(stack,7)>=2)out.add(Text.translatable("mining.guogaology.manuscript.totem"));
    }
}
