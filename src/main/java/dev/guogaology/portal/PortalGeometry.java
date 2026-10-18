package dev.guogaology.portal;

import dev.guogaology.GuogaologyMod;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Physical entry and loaded-only arrival geometry shared by portal routing. */
public final class PortalGeometry {
    private static final int[][] SIDES = {{0, 3}, {3, 0}, {0, -3}, {-3, 0}};
    private static final int[] LATERAL = {0, -1, 1};

    private PortalGeometry() {}

    public static boolean inside(ServerPlayerEntity player, BlockPos center) {
        return player.getX() > center.getX() - .85 && player.getX() < center.getX() + 1.85
                && player.getZ() > center.getZ() - .85 && player.getZ() < center.getZ() + 1.85
                && player.getY() >= center.getY() - .5 && player.getY() <= center.getY() + .5;
    }

    public static BlockPos landing(ServerWorld world, BlockPos center) {
        if (center.getY() > maxGateY(world)) return null;
        for (int[] side : SIDES) {
            for (int lateral : LATERAL) {
                BlockPos feet = center.add(side[0] + (side[0] == 0 ? lateral : 0), 0,
                        side[1] + (side[1] == 0 ? lateral : 0));
                if (PortalTravel.isSafe(world, feet)) return feet;
            }
        }
        return null;
    }

    public static int maxGateY(ServerWorld world) {
        return world.getRegistryKey().equals(GuogaologyMod.DIMENSION) ? 200 : Integer.MAX_VALUE;
    }
}
