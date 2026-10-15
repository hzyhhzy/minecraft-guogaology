package dev.guogaology.outer.world;

import dev.guogaology.outer.GuogaologyMod;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Deterministic, chunk-contained spiral shafts restored from the 0.5.4.1 JAR.
 * The open bottom, candidate hashes and ring order are part of that version's layout.
 */
public final class SurfaceShaftField {
    public static final int CELL = 113;
    public static final int MARGIN = 24;
    private static final int[][] RING = {{0, 1}, {0, 0}, {1, 0}, {2, 0},
            {2, 1}, {2, 2}, {1, 2}, {0, 2}};

    private SurfaceShaftField() {}

    private static int stepFacing(int index) {
        int[] current = RING[index & 7], next = RING[(index + 1) & 7];
        int dx = next[0] - current[0], dz = next[1] - current[1];
        return dz < 0 ? 0 : dx > 0 ? 1 : dz > 0 ? 2 : 3;
    }

    public static BlockState conwayStep(int facing) {
        Direction direction = switch (facing) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
        // Registry lookup keeps the separately remapped 1.21.1 bridge independent
        // of the host's Yarn-mapped block implementation.
        BlockState state = BuiltInRegistries.BLOCK.getValue(GuogaologyMod.id("conway_link")).defaultBlockState();
        return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction) : state;
    }

    private static long mix(long value) {
        value += -2952168198444100760L;
        value = (value ^ value >>> 30) * 3691564373535L;
        value = (value ^ value >>> 27) * -5916209924272757118L;
        return value ^ value >>> 31;
    }

    public static <T> int apply(long seed, ChunkPos chunk, int minY, int maxY,
            ShaftProbe probe, LhoIslandField.ColumnSink<T> sink,
            LhoIslandField.MaterialPalette<T> palette, StepPalette<T> steps) {
        if (minY > maxY) return 0;
        int chunkX = chunk.getMinBlockX(), chunkZ = chunk.getMinBlockZ(), count = 0;
        int cellX = Math.floorDiv(chunkX, CELL), cellZ = Math.floorDiv(chunkZ, CELL);
        for (int cx = cellX; cx <= cellX + 1; cx++) {
            for (int cz = cellZ; cz <= cellZ + 1; cz++) {
                long hash = mix(seed ^ 8317151724737396737L);
                hash = mix(hash + cx * 67277548617L);
                hash = mix(hash + cz * -4417276706812531889L);
                int x = cx * CELL + MARGIN + Math.floorMod(hash, CELL - 2 * MARGIN);
                int z = cz * CELL + MARGIN + Math.floorMod(mix(hash + 21842582225L), CELL - 2 * MARGIN);
                // Do not load/write neighboring chunks, including at negative coordinates.
                if (x - 1 < chunkX || x + 3 > chunkX + 15 || z - 1 < chunkZ || z + 3 > chunkZ + 15) continue;
                if (!probe.hasData(x + 1, z + 1)) continue;
                int top = probe.top(x + 1, z + 1);
                if (top == Integer.MIN_VALUE || top <= minY || probe.isVoidOrEdge(x + 1, top, z + 1)) continue;

                for (int y = top - 1; y >= minY; y--) {
                    for (int dx = -1; dx <= 3; dx++) for (int dz = -1; dz <= 3; dz++) {
                        if (isRim(dx, dz)) sink.set(x + dx, y, z + dz, palette.of(1));
                    }
                }
                // Seal fluid at the rim without replacing the surrounding surface.
                for (int dx = -1; dx <= 3; dx++) for (int dz = -1; dz <= 3; dz++) {
                    if (isRim(dx, dz) && probe.isFluid(x + dx, top, z + dz))
                        sink.set(x + dx, top, z + dz, palette.of(1));
                }
                for (int y = top; y >= minY; y--) {
                    for (int dx = 0; dx < 3; dx++) for (int dz = 0; dz < 3; dz++)
                        sink.set(x + dx, y, z + dz, palette.of(0));
                }
                for (int y = top; y > minY; y--) {
                    int index = (top - y - 1) & 7;
                    sink.set(x + RING[index][0], y, z + RING[index][1], steps.step(stepFacing(index)));
                }
                count++;
            }
        }
        return count;
    }

    private static boolean isRim(int x, int z) { return x < 0 || x > 2 || z < 0 || z > 2; }

    public interface ShaftProbe {
        boolean hasData(int x, int z);
        int top(int x, int z);
        boolean isVoidOrEdge(int x, int y, int z);
        boolean isFluid(int x, int y, int z);
    }

    public interface StepPalette<T> { T step(int facing); }
}
