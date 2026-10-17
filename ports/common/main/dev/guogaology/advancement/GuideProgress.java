package dev.guogaology.advancement;

import dev.guogaology.GuogaologyMod;
import dev.guogaology.portal.PortalKind;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Only server-confirmed actions complete manual guide criteria. */
public final class GuideProgress {
    private GuideProgress(){}
    public static void grant(ServerPlayer player,String name){
        var advancement=player.level().getServer().getAdvancements().get(GuogaologyMod.id("guide/"+name));
        if(advancement!=null)player.getAdvancements().award(advancement,"done");
    }
    public static void portalActivated(ServerLevel world,ItemEntity offering,PortalKind kind){
        if(!(offering.getOwner() instanceof ServerPlayer player))return;
        var source=world.dimension();
        if(source.equals(GuogaologyMod.OUTER)&&kind==PortalKind.OUTER
                ||source.equals(GuogaologyMod.DIMENSION)&&kind==PortalKind.INNER
                ||source.equals(GuogaologyMod.GUOGAO)&&kind==PortalKind.GUOGAO)grant(player,"portal_return");
    }
    public static void edited(Player player,ItemStack gear){
        if(player instanceof ServerPlayer server&&!dev.guogaology.mining.GearData.profile(gear).isEmpty())grant(server,"socketed");
    }
}
