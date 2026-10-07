package dev.guogaology;

import dev.guogaology.mining.MiningContent;
import dev.guogaology.portal.PortalTravel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.ChatFormatting;

public final class GuogaologyCommands {
    private static final String[] DESTINATIONS={"outer","inner","underworld","overworld"};
    private GuogaologyCommands(){}
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
            var root=Commands.literal("guogaology").executes(c->help(c.getSource()))
                    .then(Commands.literal("help").executes(c->help(c.getSource())));
            var tp=Commands.literal("tp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->help(c.getSource()));
            var portals=Commands.literal("portal").executes(c->help(c.getSource()));
            for(String destination:DESTINATIONS){
                tp.then(Commands.literal(destination).executes(c->travel(c.getSource(),destination)));
                if(!destination.equals("overworld"))portals.then(Commands.literal(destination).executes(c->portalKit(c.getSource(),destination)));
            }
            root.then(tp).then(Commands.literal("up").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->up(c.getSource())))
                    .then(Commands.literal("kit").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->help(c.getSource()))
                            .then(portals).then(Commands.literal("return").executes(c->returnKit(c.getSource()))));
            dispatcher.register(root);
        });
    }
    private static void reply(CommandSourceStack source,String key,Object... args){
        source.sendSuccess(()->Component.translatable("command.guogaology."+key,args),false);
    }
    private static int help(CommandSourceStack source){
        reply(source,"title");
        for(String destination:DESTINATIONS)link(source,"tp "+destination,"tp."+destination);
        link(source,"up","up");
        for(String destination:DESTINATIONS)if(!destination.equals("overworld"))link(source,"kit portal "+destination,"kit."+destination);
        link(source,"kit return","kit.return");reply(source,"admin_note");return 1;
    }
    private static void link(CommandSourceStack source,String suffix,String description){
        var command="/guogaology "+suffix;
        var line=Component.literal(command).withStyle(style->style.withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.SuggestCommand(command)))
                .append(Component.literal(" — ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("command.guogaology."+description));
        source.sendSuccess(()->line,false);
    }
    private static ResourceKey<Level> destination(String name){return switch(name){
        case "outer"->GuogaologyMod.OUTER;case "inner"->GuogaologyMod.DIMENSION;
        case "underworld"->GuogaologyMod.GUOGAO;default->Level.OVERWORLD;};}
    private static int travel(CommandSourceStack source,String name)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();var target=destination(name);
        if(player.level().dimension().equals(target)){reply(source,"already_here");return 0;}
        if(!PortalTravel.toDimension(player,target)){reply(source,"unavailable");return 0;}
        if(!player.level().dimension().equals(target))reply(source,"preparing",Component.translatable("command.guogaology.realm."+name));return 1;
    }
    private static int up(CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var realm=source.getPlayerOrException().level().dimension();
        if(realm.equals(GuogaologyMod.GUOGAO))return travel(source,"inner");
        if(realm.equals(GuogaologyMod.DIMENSION))return travel(source,"outer");
        if(realm.equals(GuogaologyMod.OUTER))return travel(source,"overworld");
        reply(source,"no_parent");return 0;
    }
    private static void give(ServerPlayer player,Item item,int count){
        // Offer individual items so unstackable cakes and a full inventory behave correctly.
        for(int i=0;i<count;i++)player.getInventory().placeItemBackInInventory(new ItemStack(item));
    }
    private static int portalKit(CommandSourceStack source,String target)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();
        switch(target){
            case "outer"->{give(player,Items.CAKE,12);give(player,Items.APPLE,1);}
            case "inner"->{for(var block:MiningContent.STORAGE)give(player,block.asItem(),3);give(player,MiningContent.MATERIALS[3],1);}
            case "underworld"->give(player,GuogaologyBlocks.AMBER_GUOGAO.asItem(),13);
            default->throw new IllegalArgumentException(target);
        }
        reply(source,"kit_given",Component.translatable("command.guogaology.realm."+target));return 1;
    }
    private static int returnKit(CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();var realm=player.level().dimension();
        var frame=realm.equals(GuogaologyMod.OUTER)?GuogaologyBlocks.OUTER_RETURN_FRAME:
                realm.equals(GuogaologyMod.DIMENSION)?GuogaologyBlocks.INNER_RETURN_FRAME:
                realm.equals(GuogaologyMod.GUOGAO)?GuogaologyBlocks.GUOGAO_RETURN_FRAME:null;
        if(frame==null){reply(source,"no_parent");return 0;}
        give(player,frame.asItem(),12);give(player,GuogaologyBlocks.RETURN_TOKEN,1);
        reply(source,"return_kit_given");return 1;
    }
}
