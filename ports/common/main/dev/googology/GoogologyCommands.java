package dev.googology;

import dev.googology.portal.PortalTravel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GoogologyCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
                Commands.literal("googology")
                        .executes(context -> { context.getSource().sendSuccess(() -> Component.translatable("message.googology.guide"), false); return 1; })
                        .then(Commands.literal("return").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (player.level().dimension().equals(GoogologyMod.DIMENSION)||player.level().dimension().equals(GoogologyMod.GUOGAO)) PortalTravel.returnHome(player);
                            return 1;
                        }))
                        .then(Commands.literal("visit").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
                            PortalTravel.travel(context.getSource().getPlayerOrException()); return 1;
                        }))
                        .then(Commands.literal("guogao").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> { PortalTravel.toGuogao(context.getSource().getPlayerOrException());return 1; }))
                        .then(Commands.literal("kit").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            // Cakes are unstackable: offer them individually to respect vanilla stack limits.
                            for (int i = 0; i < 12; i++) player.getInventory().placeItemBackInInventory(new ItemStack(Items.CAKE));
                            player.getInventory().placeItemBackInInventory(new ItemStack(Items.APPLE));
                            player.getInventory().placeItemBackInInventory(new ItemStack(GoogologyBlocks.AMBER_GUOGAO,13));
                            player.displayClientMessage(Component.translatable("message.googology.guide"), false);
                            return 1;
                        }))));
    }
}
