package dev.googology;

import dev.googology.portal.PortalTravel;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class GoogologyCommands {
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
                CommandManager.literal("googology")
                        .executes(context -> { context.getSource().sendFeedback(() -> Text.translatable("message.googology.guide"), false); return 1; })
                        .then(CommandManager.literal("return").requires(source -> source.hasPermissionLevel(2)).executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            if (player.getWorld().getRegistryKey().equals(GoogologyMod.DIMENSION)||player.getWorld().getRegistryKey().equals(GoogologyMod.GUOGAO)) PortalTravel.returnHome(player);
                            return 1;
                        }))
                        .then(CommandManager.literal("visit").requires(source -> source.hasPermissionLevel(2)).executes(context -> {
                            PortalTravel.travel(context.getSource().getPlayerOrThrow()); return 1;
                        }))
                        .then(CommandManager.literal("guogao").requires(source -> source.hasPermissionLevel(2)).executes(context -> { PortalTravel.toGuogao(context.getSource().getPlayerOrThrow());return 1; }))
                        .then(CommandManager.literal("kit").requires(source -> source.hasPermissionLevel(2)).executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            // Cakes are unstackable: offer them individually to respect vanilla stack limits.
                            for (int i = 0; i < 12; i++) player.getInventory().offerOrDrop(new ItemStack(Items.CAKE));
                            player.getInventory().offerOrDrop(new ItemStack(Items.APPLE));
                            player.getInventory().offerOrDrop(new ItemStack(GoogologyBlocks.AMBER_GUOGAO,13));
                            player.sendMessage(Text.translatable("message.googology.guide"), false);
                            return 1;
                        }))));
    }
}
