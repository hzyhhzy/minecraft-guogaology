package dev.guogaology.world;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Workbook values and expansions replayed against the user's supplied iBLP rules. */
public final class NotationCatalog {
    public record Bms(int sourceRow, String source, int[][] rows) {
        public int width() { return rows[0].length; }
        public int height() { return rows.length; }
    }
    public record Iblp(int sourceRow, int[] operations, String expression, int[] steps, int[][] rows, int[][] marks) {
        public int size() { return rows.length; }
        /** The diagram uses row indices 1..n and column indices 0..n. */
        public int cell(int row, int column) {
            if (row < 1 || row > size() || Arrays.binarySearch(rows[row - 1], column) < 0) return 0;
            return Arrays.binarySearch(marks[row - 1], column) >= 0 ? 2 : 1;
        }
    }
    private static final class Data {
        static final List<Bms> BMS;
        static final List<Bms> HALL_BMS;
        static final List<Iblp> IBLP;
        static final List<Iblp> SMALL;
        static final List<Iblp> MEDIUM;
        static final List<Iblp> TABLES;
        static final List<List<Iblp>> SIZES;
        static {
            try (var stream = NotationCatalog.class.getResourceAsStream("/data/guogaology/notation/catalog.json")) {
                if (stream == null) throw new IllegalStateException("The notation catalog is missing from the mod JAR");
                var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                var gson = new Gson();
                BMS = List.of(gson.fromJson(json.get("bms"), Bms[].class));
                HALL_BMS = BMS.stream().filter(p->p.height()==3&&p.width()>=7).toList();
                IBLP = List.of(gson.fromJson(json.get("iblp"), Iblp[].class));
                SMALL = IBLP.stream().filter(p -> p.size() <= 10).toList();
                MEDIUM = IBLP.stream().filter(p -> p.size() <= 30).toList();
                // n data rows need columns 0..n, plus a separate step strip.
                // 4..69 data rows give 5..70 pattern columns, plus a step strip.
                TABLES = IBLP.stream().filter(p -> p.size() >= 4 && p.size() <= 69).toList();
                SIZES = java.util.stream.IntStream.rangeClosed(4,69).mapToObj(size -> TABLES.stream().filter(p -> p.size()==size).toList()).filter(p -> !p.isEmpty()).toList();
                if (BMS.size() != 1070 || HALL_BMS.isEmpty() || IBLP.isEmpty() || SMALL.isEmpty()) throw new IllegalStateException("Incomplete notation catalog");
            } catch (Exception e) { throw new ExceptionInInitializerError(e); }
        }
    }
    private static int index(long seed, int count) {
        seed = (seed ^ (seed >>> 30)) * 0xbf58476d1ce4e5b9L;
        seed = (seed ^ (seed >>> 27)) * 0x94d049bb133111ebL;
        return (int) Math.floorMod(seed ^ (seed >>> 31), (long) count);
    }
    public static Bms bms(long seed) { return Data.BMS.get(index(seed, Data.BMS.size())); }
    /** Each panel draws from complete three-row workbook expressions, then displays their first seven columns. */
    public static Bms hallBms(long seed) { return Data.HALL_BMS.get(index(seed,Data.HALL_BMS.size())); }
    public static List<Bms> hallBmsPool() { return Data.HALL_BMS; }
    public static Iblp iblp(long seed, int maxRows) {
        var pool = maxRows <= 10 ? Data.SMALL : maxRows <= 30 ? Data.MEDIUM : Data.IBLP;
        return pool.get(index(seed, pool.size()));
    }
    public static List<Bms> allBms() { return Data.BMS; }
    public static Iblp table(long seed) {
        // Sample size before expression so common source sizes cannot overwhelm the landscape.
        int target=4+(int)(Math.pow(WorldNoise.unit(WorldNoise.mix(seed+197)),1.7)*66);
        var pool=Data.SIZES.stream().min(java.util.Comparator.comparingInt(p -> Math.abs(p.getFirst().size()-target))).orElseThrow();
        return pool.get(index(seed,pool.size()));
    }
    public static List<Iblp> allIblp() { return Data.IBLP; }
    private NotationCatalog() {}
}
