package dev.guogaology.mining;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Saved, platform-independent 5 by 5 vertical mining stencil. */
public record MiningPattern(int reference, int mask) {
    public static final int SIZE = 5, CELL_COUNT = SIZE * SIZE, DEFAULT_REFERENCE = 12;
    public static final int ALL_CELLS_MASK = (1 << CELL_COUNT) - 1;
    /** Convenient Lv3 single-core default; actual equipment always uses its current budget. */
    public static final MiningPattern DEFAULT = defaultForBudget(4);

    /** User-defined defaults for two through twelve total blocks, including the central reference. */
    public static MiningPattern defaultForBudget(int budget) {
        int[] cells = switch (Math.max(0, Math.min(11, budget))) {
            case 0 -> new int[]{};
            case 1 -> new int[]{17};
            case 2 -> new int[]{7,17};
            case 3 -> new int[]{11,13,17};
            case 4 -> new int[]{7,11,13,17};
            case 5 -> new int[]{11,13,16,17,18};
            case 6 -> new int[]{7,11,13,16,17,18};
            case 7 -> new int[]{6,8,11,13,16,17,18};
            case 8 -> new int[]{6,7,8,11,13,16,17,18};
            case 9 -> new int[]{2,6,7,8,11,13,16,17,18};
            case 10 -> new int[]{1,3,6,7,8,11,13,16,17,18};
            default -> new int[]{1,2,3,6,7,8,11,13,16,17,18};
        };
        int mask = 0;
        for (int cell : cells) mask |= 1 << cell;
        return new MiningPattern(DEFAULT_REFERENCE, mask);
    }

    public MiningPattern {
        if (!validCell(reference)) reference = DEFAULT_REFERENCE;
        mask &= ALL_CELLS_MASK & ~(1 << reference);
    }

    public static boolean validCell(int cell) { return cell >= 0 && cell < CELL_COUNT; }
    public boolean contains(int cell) { return validCell(cell) && (mask & (1 << cell)) != 0; }
    public int count() { return Integer.bitCount(mask); }
    public int horizontal(int cell) { return cell % SIZE - reference % SIZE; }
    public int vertical(int cell) { return reference / SIZE - cell / SIZE; }

    public MiningPattern toggle(int cell) {
        return !validCell(cell) || cell == reference ? this : new MiningPattern(reference, mask ^ (1 << cell));
    }

    /** Moving the origin does not silently turn the previous origin into a selected mining cell. */
    public MiningPattern withReference(int cell) {
        return validCell(cell) ? new MiningPattern(cell, mask) : this;
    }

    /** Select the fixed budget-limited subset before looking at air, tool suitability or permissions. */
    public List<Integer> active(int budget) {
        if (budget <= 0 || mask == 0) return List.of();
        var cells = new ArrayList<Integer>();
        for (int cell = 0; cell < CELL_COUNT; cell++) if (contains(cell)) cells.add(cell);
        cells.sort(Comparator.comparingInt((Integer cell) -> Math.abs(horizontal(cell)) + Math.abs(vertical(cell)))
                .thenComparingInt(Integer::intValue));
        return List.copyOf(cells.subList(0, Math.min(budget, cells.size())));
    }

    public int activeMask(int budget) {
        int selected = 0;
        for (int cell : active(budget)) selected |= 1 << cell;
        return selected;
    }
}
