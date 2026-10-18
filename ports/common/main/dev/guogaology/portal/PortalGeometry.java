package dev.guogaology.portal;

import dev.guogaology.GuogaologyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Physical entry and loaded-only arrival geometry shared by portal routing. */
public final class PortalGeometry {
    private static final int[][] SIDES = {{0, 3}, {3, 0}, {0, -3}, {-3, 0}};
    private static final int[] LATERAL = {0, -1, 1};

    private PortalGeometry() {}

    public static boolean inside(ServerPlayer player, BlockPos center) {
        return player.getX() > center.getX() - .85 && player.getX() < center.getX() + 1.85
                && player.getZ() > center.getZ() - .85 && player.getZ() < center.getZ() + 1.85
                && player.getY() >= center.getY() - .5 && player.getY() <= center.getY() + .5;
    }

    public static BlockPos landing(ServerLevel world, BlockPos center) {
        if (center.getY() > maxGateY(world)) return null;
        for (int[] side : SIDES) {
            for (int lateral : LATERAL) {
                BlockPos feet = center.offset(side[0] + (side[0] == 0 ? lateral : 0), 0,
                        side[1] + (side[1] == 0 ? lateral : 0));
                if (PortalTravel.isSafe(world, feet)) return feet;
            }
        }
        return null;
    }

    public static int maxGateY(ServerLevel world) {
        return world.dimension().equals(GuogaologyMod.DIMENSION) ? 200 : Integer.MAX_VALUE;
    }
}
