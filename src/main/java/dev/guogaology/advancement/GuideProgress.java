package dev.guogaology.advancement;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.portal.PortalKind;
import net.minecraft.entity.ItemEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Only server-confirmed actions complete manual guide criteria. */
public final class GuideProgress {
    private GuideProgress(){}
    public static void grant(ServerPlayerEntity player,String name){
        var advancement=player.getServer().getAdvancementLoader().get(GuogaologyMod.id("guide/"+name));
        if(advancement!=null)player.getAdvancementTracker().grantCriterion(advancement,"done");
    }
    public static void portalActivated(ServerWorld world,ItemEntity offering,PortalKind kind){
        if(!(offering.getOwner() instanceof ServerPlayerEntity player))return;
        var source=world.getRegistryKey();
        if(source.equals(GuogaologyMod.OUTER)&&kind==PortalKind.OUTER
                ||source.equals(GuogaologyMod.DIMENSION)&&kind==PortalKind.INNER
                ||source.equals(GuogaologyMod.GUOGAO)&&kind==PortalKind.GUOGAO)grant(player,"portal_return");
    }
    public static void edited(PlayerEntity player,ItemStack gear){
        if(player instanceof ServerPlayerEntity server&&!dev.guogaology.mining.GearData.profile(gear).isEmpty())grant(server,"socketed");
    }
}
