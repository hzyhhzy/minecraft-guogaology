package dev.guogaology;

import dev.guogaology.mining.MiningContent;
import dev.guogaology.portal.PortalTravel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent;
import net.minecraft.util.Formatting;

public final class GuogaologyCommands {
    private static final String[] DESTINATIONS={"outer","inner","underworld","overworld"};
    private GuogaologyCommands(){}
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
            var root=CommandManager.literal("guogaology").executes(c->help(c.getSource()))
                    .then(CommandManager.literal("help").executes(c->help(c.getSource())));
            var tp=CommandManager.literal("tp").requires(source->source.hasPermissionLevel(2)).executes(c->help(c.getSource()));
            var portals=CommandManager.literal("portal").executes(c->help(c.getSource()));
            for(String destination:DESTINATIONS){
                tp.then(CommandManager.literal(destination).executes(c->travel(c.getSource(),destination)));
                if(!destination.equals("overworld"))portals.then(CommandManager.literal(destination).executes(c->portalKit(c.getSource(),destination)));
            }
            root.then(tp).then(CommandManager.literal("up").requires(source->source.hasPermissionLevel(2)).executes(c->up(c.getSource())))
                    .then(CommandManager.literal("kit").requires(source->source.hasPermissionLevel(2)).executes(c->help(c.getSource()))
                            .then(portals).then(CommandManager.literal("return").executes(c->returnKit(c.getSource()))));
            dispatcher.register(root);
        });
    }
    private static void reply(ServerCommandSource source,String key,Object... args){
        source.sendFeedback(()->Text.translatable("command.guogaology."+key,args),false);
    }
    private static int help(ServerCommandSource source){
        reply(source,"title");
        for(String destination:DESTINATIONS)link(source,"tp "+destination,"tp."+destination);
        link(source,"up","up");
        for(String destination:DESTINATIONS)if(!destination.equals("overworld"))link(source,"kit portal "+destination,"kit."+destination);
        link(source,"kit return","kit.return");reply(source,"admin_note");return 1;
    }
    private static void link(ServerCommandSource source,String suffix,String description){
        var command="/guogaology "+suffix;
        var line=Text.literal(command).styled(style->style.withColor(Formatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,command)))
                .append(Text.literal(" — ").formatted(Formatting.GRAY))
                .append(Text.translatable("command.guogaology."+description));
        source.sendFeedback(()->line,false);
    }
    private static RegistryKey<World> destination(String name){return switch(name){
        case "outer"->GuogaologyMod.OUTER;case "inner"->GuogaologyMod.DIMENSION;
        case "underworld"->GuogaologyMod.GUOGAO;default->World.OVERWORLD;};}
    private static int travel(ServerCommandSource source,String name)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrThrow();var target=destination(name);
        if(player.getServerWorld().getRegistryKey().equals(target)){reply(source,"already_here");return 0;}
        if(!PortalTravel.toDimension(player,target)){reply(source,"unavailable");return 0;}
        if(!player.getServerWorld().getRegistryKey().equals(target))reply(source,"preparing",Text.translatable("command.guogaology.realm."+name));return 1;
    }
    private static int up(ServerCommandSource source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var realm=source.getPlayerOrThrow().getServerWorld().getRegistryKey();
        if(realm.equals(GuogaologyMod.GUOGAO))return travel(source,"inner");
        if(realm.equals(GuogaologyMod.DIMENSION))return travel(source,"outer");
        if(realm.equals(GuogaologyMod.OUTER))return travel(source,"overworld");
        reply(source,"no_parent");return 0;
    }
    private static void give(ServerPlayerEntity player,Item item,int count){
        // Offer individual items so unstackable cakes and a full inventory behave correctly.
        for(int i=0;i<count;i++)player.getInventory().offerOrDrop(new ItemStack(item));
    }
    private static int portalKit(ServerCommandSource source,String target)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrThrow();
        switch(target){
            case "outer"->{give(player,Items.CAKE,12);give(player,Items.APPLE,1);}
            case "inner"->{for(var block:MiningContent.STORAGE)give(player,block.asItem(),3);give(player,MiningContent.MATERIALS[3],1);}
            case "underworld"->give(player,GuogaologyBlocks.AMBER_GUOGAO.asItem(),13);
            default->throw new IllegalArgumentException(target);
        }
        reply(source,"kit_given",Text.translatable("command.guogaology.realm."+target));return 1;
    }
    private static int returnKit(ServerCommandSource source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrThrow();var realm=player.getServerWorld().getRegistryKey();
        var frame=realm.equals(GuogaologyMod.OUTER)?GuogaologyBlocks.OUTER_RETURN_FRAME:
                realm.equals(GuogaologyMod.DIMENSION)?GuogaologyBlocks.INNER_RETURN_FRAME:
                realm.equals(GuogaologyMod.GUOGAO)?GuogaologyBlocks.GUOGAO_RETURN_FRAME:null;
        if(frame==null){reply(source,"no_parent");return 0;}
        give(player,frame.asItem(),12);give(player,GuogaologyBlocks.RETURN_TOKEN,1);
        reply(source,"return_kit_given");return 1;
    }
}
